(ns event-data-patch.patches.license
  (:require [clj-time.core :as clj-time]
            [clj-time.format :as clj-time-format]))

(def cc0 "https://creativecommons.org/publicdomain/zero/1.0/")

(def sources
 {"twitter" cc0
  "datacite" cc0
  "hypothesis" cc0
  "wikipedia" cc0
  "newsfeed" cc0
  "wordpressdotcom" cc0
  "reddit" cc0
  "reddit-links" cc0})

(defn patch
  "Ensure license has a license field, if from a known source."
  [event]
  (if-let [license (sources (:source_id event))]
    (assoc event :license license)
    event))
