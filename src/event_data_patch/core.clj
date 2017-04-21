(ns event-data-patch.core
  (:require [event-data-patch.storage :as storage]
            [clojure.data.json :as json]
            [clojure.tools.logging :as log]
            [clj-time.core :as clj-time]
            [clj-time.format :as clj-time-format]
            [clj-time.periodic :as clj-time-periodic]
            [clj-time.coerce :as clj-time-coerce]
            [event-data-common.storage.store :as store]
            [event-data-common.storage.store :refer [Store]]
            [event-data-common.storage.s3 :as s3]
            [event-data-common.date :as date]
            [event-data-common.date :as date]
            [config.core :refer [env]]
            ; Patch namespaces.
            [event-data-patch.patches.field-names :as field-names]
            [event-data-patch.patches.timestamp :as timestamp]
            [event-data-patch.patches.tweet-id :as tweet-id]))



(def patches
  "Combined patches as a function."
  ; NB last function is applied first. There are some dependencies between functions.
  (comp field-names/patch
        timestamp/patch
        tweet-id/patch))


(defn clear-storage
  "Clear all stored data so we can ingest from scratch and set up indexes."
  []
  (storage/clear-original)
  (storage/clear-working))


(defn ingest-all-epoch
  "Ingest Events from entire Epoch into 'original'"
  []
  (let [epoch-start (clj-time-coerce/from-string (:epoch-start env))
        epoch-end (clj-time-coerce/from-string (:epoch-end env))
        date-range (take-while #(clj-time/before? % (clj-time/plus epoch-end (clj-time/days 1))) (clj-time-periodic/periodic-seq epoch-start (clj-time/days 1)))]
    (doseq [date date-range]
      (log/info "Ingesting for" (clj-time-format/unparse date/yyyy-mm-dd-format date))
      (let [date-str (clj-time-format/unparse date/yyyy-mm-dd-format date)
            events (storage/events-for-date date-str)]
        (log/info "Got" (count events) "for" date-str)
        (doseq [event events]
          (storage/insert-original event))))))

(defn verify-all-epoch
  "Verify that all Events exist in the 'original' collection from the entire Epoch."
  []
    (let [epoch-start (clj-time-coerce/from-string (:epoch-start env))
        epoch-end (clj-time-coerce/from-string (:epoch-end env))
        date-range (take-while #(clj-time/before? % (clj-time/plus epoch-end (clj-time/days 1))) (clj-time-periodic/periodic-seq epoch-start (clj-time/days 1)))]
    (doseq [date date-range]
      (log/info "Verifying Events for" (clj-time-format/unparse date/yyyy-mm-dd-format date))
      (let [date-str (clj-time-format/unparse date/yyyy-mm-dd-format date)
            [event-ids-missing total-events] (storage/missing-events-for-date date-str)]
        (doseq [event-id event-ids-missing]
          (log/error "Missing" event-id))))))

(defn patch-working
  "Update the 'working' collection with patched events from the 'original' collection"
  []
  (storage/clear-working)
  (storage/patch-working patches))

(defn -main
  "This is meant to be run from the REPL."
  [& args])
