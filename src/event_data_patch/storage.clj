(ns event-data-patch.storage
  (:require [clojure.data.json :as json]
            [clojure.tools.logging :as log]
            [clj-time.core :as clj-time]
            [clj-time.format :as clj-time-format]
            [clj-time.periodic :as clj-time-periodic]
            [event-data-common.storage.store :as store]
            [event-data-common.storage.store :refer [Store]]
            [event-data-common.storage.s3 :as s3]
            [event-data-common.date :as date]
            [config.core :refer [env]]
            [monger.collection :as mc]
            [monger.core :as mg]
            ; Not directly used, but converts clj-time dates in the background.
            [monger.joda-time]
            [monger.operators :as o]
            [monger.query :as q])
    (:import [org.bson.types ObjectId]
           [com.mongodb DB WriteConcern]))

(def s3-storage (delay (s3/build (:s3-key env) (:s3-secret env) (:s3-region-name env) (:s3-bucket-name env))))

(def mongo-db (delay (:db (mg/connect-via-uri (:mongodb-uri env)))))

(def date-format
  (:date-time-no-ms clj-time-format/formatters))

; Prefixes as short as possible to help with S3 load balancing.
(def day-prefix
  "Prefix under which events are stored with their date, e.g. 'd/2016-11-27/86accb20-1c8f-483d-8567-52ad031ba190'"
  "d/")

(def event-prefix
  "Prefix under which events are stored for retrieval, e.g. 'e/f0ca191e-b8af-485c-b06b-fbee4cf0423b'"
  "e/")

(def archive-prefix
  "Prefix under which per-day archives are stored."
  "a/")


(defn clear-original
  "Clear all stored data in 'original' so we can ingest from scratch and set up indexes."
  []
  (mg/set-default-write-concern! WriteConcern/ACKNOWLEDGED)
  (mc/drop @mongo-db "original")
  ; Creating indexes will create the collections.
  ; http://stackoverflow.com/questions/5312574/is-it-ok-to-call-ensureindex-on-non-existent-collections
  ; No more indexes, we won't be searching, just brute-force scanning.
  (mc/ensure-index @mongo-db "original" {"id" 1} {:unique true}))


(defn clear-working
  "Clear all stored data in 'working' so we can ingest from scratch and set up indexes."
  []
  (mg/set-default-write-concern! WriteConcern/ACKNOWLEDGED)
  (mc/drop @mongo-db "working")
  ; Creating indexes will create the collections.
  ; http://stackoverflow.com/questions/5312574/is-it-ok-to-call-ensureindex-on-non-existent-collections
  ; No more indexes, we won't be searching, just brute-force scanning.
  (mc/ensure-index @mongo-db "working" {"id" 1} {:unique true}))


(defn events-for-date
  "Fetch and return events for YYYY-MM-DD string."
  [date-str]
  (let [; All events we're looking for will have been stored with the `day-prefix` and the YYYY-MM-DD prefix.
        date-keys (store/keys-matching-prefix @s3-storage (str day-prefix date-str))
        ; We get back a key with the day-prefix. The actual data is stored in the event-prefix.
        ; i.e. "d/YYYY-MM-DD/e/1234" -> "1234"
        prefix-length (+ (.length day-prefix) (.length "YYYY-MM-DD/"))
        event-keys (doall (map #(str event-prefix (.substring ^String % prefix-length)) date-keys))
        num-keys (count event-keys)
        ; Retrieve every event for every key.
        ; S3 performs well in parallel, so fetch items in parallel.
        counter (atom 0)
        future-event-blobs (map #(future
                                   (swap! counter inc)
                                   (when (zero? (mod @counter 1000))
                                     (log/info "Retrieving Events for" date-str "retrieved" @counter "/" num-keys " = " (int (* 100 (/ @counter num-keys))) "%"))                                   
                                   [% (store/get-string @s3-storage %)]) event-keys)
        event-blobs (map deref future-event-blobs)
        
        all-events (keep (fn [[id json-str]]
                          (try
                            (json/read-str json-str :key-fn keyword)
                            (catch NullPointerException ex
                              (do
                                (log/error "Can't read event ID from storage" id)
                                ; Don't allow processing to continue or we'll end up with inconsistent data.
                                (throw ex)
                                nil)))) event-blobs)]
    all-events))

(defn missing-events-for-date
  "Find Event IDs that are in the store but not in the database for YYYY-MM-DD string.
   Return [missing-ids, total-count-for-date]"
  [date-str]
  (let [; All events we're looking for will have been stored with the `day-prefix` and the YYYY-MM-DD prefix.
        date-keys (store/keys-matching-prefix @s3-storage (str day-prefix date-str))
        ; We get back a key with the day-prefix. The actual data is stored in the event-prefix.
        ; i.e. "d/2016-08-12/0001f4ee-225c-4c91-ab6e-41f34d8c4bf6" -> "0001f4ee-225c-4c91-ab6e-41f34d8c4bf6"
        prefix-length (+ (.length day-prefix) (.length "YYYY-MM-DD/"))
        
        event-keys (doall (map #(.substring ^String % prefix-length) date-keys))

        missing (filter #(zero? (mc/count @mongo-db "original" {:id %})) event-keys)]
    [missing (count event-keys)]))

(defn insert-original
  [event]
  (mc/update-by-id @mongo-db "original" (:id event) event {:upsert true}))

(defn insert-working
  [event]
  (mc/update-by-id @mongo-db "working" (:id event) event {:upsert true}))

(defn patch-working
  "Update 'working' with patched Events."
  [patch-fn]
  (let [count-all (mc/count @mongo-db "original" {})
        events (mc/find-maps @mongo-db "original" {})
        progress (atom 0)]
  (doseq [event events]
    (let [patched-event (patch-fn event)]
      (insert-working patched-event)
      (swap! progress inc)
      (when (zero? (rem @progress 10000))
        (log/info "Done" @progress "/" count-all "=" (float (/ @progress count-all))))))))
