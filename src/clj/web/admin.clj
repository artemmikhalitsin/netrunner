(ns web.admin
  (:require [web.db :refer [db object-id]]
            [web.lobby :refer [all-games]]
            [game.main :as main]
            [game.core :refer [reset-card-defs]]
            [tasks.fetch :refer [fetch-with-db]]
            [web.utils :refer [response]]
            [monger.collection :as mc]
            [monger.operators :refer :all]
            [web.config :refer [frontend-version]]))

(defn wrap-version [handler]
  (fn [request]
    (handler (assoc request :version @frontend-version))))

(defn announcement-handler
  [{{:keys [message]} :params :as req}]
  (doseq [{state :state} (vals @all-games)]
    (when state
      (main/handle-announcement state message)))
  (response 200 {:message "ok"}))

(defn version-handler
  [{{:keys [version]} :params :as req}]
  (reset! frontend-version version)
  (mc/update db "config" {} {$set {:version version}})
  (response 200 {:message "ok" :version version}))

(defn fetch-handler
  [req]
  (try
    (fetch-with-db)
    (reset-card-defs)
    (response 200 {:message "ok"})
    (catch Exception e (do
                         (println "fetch-handler failed:" (.getMessage e))
                         (.printStackTrace e)
                         (response 500 {:message (str "Import data failed: " (.getMessage e))})))))
