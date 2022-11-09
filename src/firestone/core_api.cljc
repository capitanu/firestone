(ns firestone.core-api
  (:require [ysera.test :refer [is= error?]]
            [ysera.error :refer [error]]
            [firestone.construct :refer [create-game
                                         get-player-id-in-turn
                                         get-players
                                         add-minion-to-board
                                         create-minion
                                         create-hero
                                         update-hero
                                         update-minion]]
            [firestone.core :refer [get-health
                                    get-attack
                                    get-entity-type
                                    valid-attack?]]))



(defn end-turn
  {:test (fn []
           (is= (-> (create-game)
                    (end-turn "p1")
                    (get-player-id-in-turn))
                "p2")
           (is= (-> (create-game)
                    (end-turn "p1")
                    (end-turn "p2")
                    (get-player-id-in-turn))
                "p1")
           (error? (-> (create-game)
                       (end-turn "p2"))))}
  [state player-id]
  (when-not (= (get-player-id-in-turn state) player-id)
    (error "The player with id " player-id " is not in turn."))
  (let [player-change-fn {"p1" "p2"
                          "p2" "p1"}]
    (-> state
        (update :player-id-in-turn player-change-fn))))

(defn attack
  ;; Should be slowly generalized to whatever two ids are:
  ;; (minion, minion)
  ;; (hero, minion)
  ;; (minion, hero)
  ;; (hero, hero)
  ;;
  ;; For now, this is only meant to work for minion-minion. I think this is
  ;; better than having multiple functions, for each types of attacks.
  {:test (fn []
           (is= (-> (create-game [{:minions [(create-minion "Silver Hand Recruit" :id "shr")]}
                                    {:minions [(create-minion "Injured Blademaster" :id "ib")]}]
                                   :player-id-in-turn "p1")
                    (attack "p1" "shr" "ib")
                    (get-health "ib"))
                6)
           (is= (-> (create-game [{:minions [(create-minion "Silver Hand Recruit" :id "shr")]
                                   :hero (create-hero "Rexxar" :id "r")}
                                  {:minions [(create-minion "Injured Blademaster" :id "ib")]
                                   :hero (create-hero "Jaina Proudmoore" :id "jp")}]
                                 :player-id-in-turn "p1")
                    (attack "p1" "shr" "jp")
                    (get-health "jp"))
                29))}
  [state player-id attacker-id target-id]
  (if (valid-attack? state player-id attacker-id target-id)
    (let [type (get-entity-type state target-id)]
       (cond (= type :minion)
             (update-minion state target-id :damage-taken (get-attack state attacker-id))

             (= type :hero)
             (update-hero state target-id :damage-taken (get-attack state attacker-id))

             :else
             "Shouldn't get here."))))

