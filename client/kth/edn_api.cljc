(ns firestone.client.kth.edn-api
  (:require [firestone.construct :as construct]
            [firestone.core-api :as core-api]
            [firestone.client.kth.mapper :refer [create-client-state]]
            [firestone.definitions-loader]))

(def game-atom (atom nil))

(defn engine-settings!
  []
  {:supports-redo     false
   :supports-undo     false
   :audio             :auto
   :effect-volume     10
   :background-volume 0})


(defn create-game!
  []
  (println "Creating the game")
  (let [state (reset! game-atom (core-api/create-game!))]
    [(create-client-state state)]))

(defn end-turn!
  [player-id]
  (println "ending the turn")
  (let [state (swap! game-atom core-api/end-turn player-id)]
    (clojure.pprint/pprint state)
    [(create-client-state state)]))

(defn play-minion-card!
  [player-id card-id position target-id]
  (println "playing card")
  (let [state (swap! game-atom core-api/play-card player-id card-id :position position :target-id target-id)]
    [(create-client-state state)]))



