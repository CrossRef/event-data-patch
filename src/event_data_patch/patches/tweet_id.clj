(ns event-data-patch.patches.tweet-id
  (:require [clj-time.core :as clj-time]
            [clj-time.format :as clj-time-format]))

(def full-format-no-ms (:date-time-no-ms clj-time-format/formatters))
(def full-format (:date-time clj-time-format/formatters))

(defn tweet-id-from-url 
  [url]
  (re-find #"[\d]+$" url))

(defn patch
  "If an event is from Twitter, add the alternative-id to the metadata."
  [event]
  (if-not (= (:source_id event) "twitter")
    event
    (assoc-in event [:subj :alternative-id] (tweet-id-from-url (:subj_id event)))))


