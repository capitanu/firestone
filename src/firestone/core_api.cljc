(ns firestone.core-api
  (:require [ysera.test :refer [is= error?]]
            [ysera.error :refer [error]]
            [firestone.construct :refer [create-game
                                         get-fatigue
                                         get-hand
                                         get-player-id-in-turn
                                         get-players
                                         add-minion-to-board
                                         create-minion
                                         create-hero
                                         reset-mana
                                         update-hero
                                         update-minion]]
            [firestone.core :refer [draw-card
                                    get-health
                                    get-attack
                                    get-entity-type
                                    take-fatigue?
                                    reset-minions-attack
                                    remove-minion
                                    remove-minion?
                                    remove-sleeping-minions
                                    set-minion-attacked
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
           (is= (as-> (create-game [{:deck ["Boulderfist Ogre"]}] :player-id-in-turn "p2") $
                    (end-turn $ "p2")
                    (get-hand $ "p1")
                    (map :name $))
                ["Boulderfist Ogre"])
           (error? (-> (create-game)
                       (end-turn "p2"))))}
  [state player-id]
  (when-not (= (get-player-id-in-turn state) player-id)
    (error "The player with id " player-id " is not in turn."))
  (let [player-change-fn {"p1" "p2"
                          "p2" "p1"}]
    (as-> state $
      (update $ :player-id-in-turn player-change-fn)
      (reset-mana $ (player-change-fn player-id))
      (remove-sleeping-minions $)
      (reset-minions-attack $ (player-change-fn player-id))
      (let [s $ pl-id (player-change-fn player-id)]
        (if (take-fatigue? s pl-id)
          (get-fatigue s pl-id)
          (draw-card s pl-id))))))

(defn attack
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
            (as-> (update-minion state target-id :damage-taken (get-attack state attacker-id)) $
              (update-minion $ attacker-id :damage-taken (get-attack state target-id))
              (let [st $]
                (if (remove-minion? st attacker-id)
                  (remove-minion st attacker-id)
                  st))
              (let [st $]
                (if (remove-minion? st target-id)
                  (remove-minion st target-id)
                  st))
              (set-minion-attacked $ attacker-id))
            
            (= type :hero)
            (->
             (update-hero state target-id :damage-taken (get-attack state attacker-id))
             (set-minion-attacked attacker-id))

             :else
             (error "Type of the card is unrecognized")))
    (error "Invalid attack")))
