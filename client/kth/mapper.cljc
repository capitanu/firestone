(ns firestone.client.kth.mapper
  (:require [clojure.spec.alpha :as s]
            [ysera.test :refer [is= is]]
            [firestone.construct :refer [create-game
                                         get-deck
                                         get-hand
                                         get-player
                                         get-player-id-in-turn]]
            [firestone.client.kth.spec :as client-spec]))

(defn check-spec
  [spec value]
  (or (s/valid? spec value)
      (s/explain spec value)))


(defn create-client-hero
  {:test (fn []
           (is (as-> (create-game) $
                     (create-client-hero $ "p1" (:hero (get-player $ "p1")))
                     (check-spec ::client-spec/hero $))))}
  [state player-id hero]
  {:armor            0
   :owner-id         player-id
   :entity-type      :hero
   :attack           0
   :can-attack       false
   :health           30
   :id               (:id hero)
   :mana             5
   :max-health       30
   :max-mana         8
   :name             (:name hero)
   :states           []
   :valid-attack-ids []})

(defn create-client-player
  {:test (fn []
           (is (as-> (create-game) $
                     (create-client-player $ "p1")
                     (check-spec ::client-spec/player $))))}
  [state player-id]
  (let [player (get-player state player-id)]
    {:board-entities []
     :active-secrets []
     :deck-size      (count (get-deck state player-id))
     :hand           (->> (get-hand state player-id))
     :hero           (create-client-hero state player-id (:hero player))
     :id             player-id}))


(defn create-client-state
  {:test (fn []
           (is (->> (create-game)
                    (create-client-state)
                    (check-spec ::client-spec/game-state))))}
  [state]
  {:action-index   0 ; need to increase this for each action performed
   :id             "the game id"
   :player-in-turn (get-player-id-in-turn state)
   :players        (->> ["p1" "p2"]
                        (map (partial create-client-player state)))})
