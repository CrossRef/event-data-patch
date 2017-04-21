(ns event-data-patch.patches.field-names)


(defn patch
  "Rename some fields that have hyphens."
  [event]
  (let [evidence-record (when-let [v (:evidence-record event)] {:evidence_record v})
        updated-reason (when-let [v (:updated-reason event)] {:updated_reason v})
        updated-date (when-let [v (:updated-date event)] {:updated_date v})
        without (dissoc event :evidence-record :updated-reason :updated-date)]
    (merge without evidence-record updated-reason updated-date)))

