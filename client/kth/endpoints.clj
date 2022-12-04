(ns firestone.client.kth.endpoints
  (:require [firestone.client.kth.edn-api :as edn-api]
            [firestone.client.kth.mapper :refer [check-spec]]
            [firestone.client.kth.spec :as client-spec]
            [ysera.error :refer [error]]
            [clojure.spec.alpha :as s])
  (:import (java.io InputStream)))

(defn decorate-response
  [data]
  {:body    (str data)
   :status  200
   :headers {"Access-Control-Allow-Origin" "https://www.conjoin-it.se"}})


(defn decorate-game-response
  [data]
  (if-not (check-spec ::client-spec/game-states data)
    {:status  500
     :headers {"Access-Control-Allow-Origin" "https://www.conjoin-it.se"}
     :body    (s/explain-str ::client-spec/game-states data)}
    (decorate-response data)))


(defn handler
  [request]
  (time (let [uri (:uri request)
              method (:request-method request)]
          (println uri method)
          (if (= method :options)
            {:status 204}

            (let [params (when (instance? InputStream (:body request))
                           (-> (:body request)
                               (slurp)
                               (read-string)))]
              (println "These are the params: " params)
              (if (= uri "/engine-settings")
                (-> (edn-api/engine-settings!)
                    (decorate-response))
                (-> (cond (= uri "/create-game")
                          (edn-api/create-game!)

                          (= uri "/end-turn")
                          (edn-api/end-turn! (:player-id params))

                          (= uri "/play-minion-card")
                          (edn-api/play-minion-card! (:player-id params) (:card-id params) (:position params) (:target-id params))

                          :else
                          (str "Missing path: " uri))
                    (decorate-game-response))))))))
