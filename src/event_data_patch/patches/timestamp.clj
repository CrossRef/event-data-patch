(ns event-data-patch.patches.timestamp
  (:require [clj-time.core :as clj-time]
            [clj-time.format :as clj-time-format]))

(def full-format-no-ms (:date-time-no-ms clj-time-format/formatters))
(def full-format (:date-time clj-time-format/formatters))

(defn parse-date-full
  "Parse two kinds of dates."
  [date-str]
  (try
    (clj-time-format/parse full-format-no-ms date-str)
    (catch IllegalArgumentException e
      (clj-time-format/parse full-format date-str))))

(defn tidy-date-format
  "Reformat a date string to not use millisecond format."
  [date-str]
  (clj-time-format/unparse full-format-no-ms (parse-date-full date-str)))


(defn patch
  "Normalize Event's timestamps to non-millisecond format."
  [event]
  (assoc event :occurred_at (tidy-date-format (:occurred_at event))
               :timestamp (tidy-date-format (:timestamp event))))
