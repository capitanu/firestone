(ns firestone.core-api
  (:require [ysera.test :refer [is= error?]]
            [ysera.error :refer [error]]
            [firestone.construct :refer [create-game
                                         get-card
                                         get-fatigue
                                         get-hand
                                         get-player-id-in-turn
                                         get-players
                                         add-minion-to-board
                                         create-card
                                         create-hero
                                         create-minion
                                         reset-mana
                                         update-hero
                                         update-minion]]
            [firestone.core :refer [battlecry
                                    draw-card
                                    get-damage-taken
                                    get-health
                                    get-attack
                                    get-entity-type
                                    take-fatigue?
                                    pay-mana
                                    place-card-board
                                    reset-minions-attack
                                    remove-card-hand
                                    remove-minion
                                    remove-minion?
                                    remove-sleeping-minions
                                    set-minion-attacked
                                    valid-attack?
                                    valid-play-card?]]))



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
           (is= (-> (create-game [{:minions [(create-minion "Boulderfist Ogre" :id "bo")]}])
                    (attack "p1" "bo" "h2")
                    (get-in [:players "p2" :hero :damage-taken]))
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
            (as-> (update-minion state target-id :damage-taken (+ (get-attack state attacker-id) (get-damage-taken state target-id))) $
              (update-minion $ attacker-id :damage-taken (+ (get-attack state target-id) (get-damage-taken state attacker-id)))
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
            (-> (update-hero state target-id :damage-taken (+ (get-attack state attacker-id) (get-damage-taken state target-id)))
                (set-minion-attacked attacker-id))

            :else
            (error "Type of the card is unrecognized")))
    (error "Invalid attack")))

(defn play-minion-card
  "Plays a minion card if it is available in the hand"
  {:test (fn []
           (is= (as-> (create-game [{:hand [(create-card "Silver Hand Recruit" :id "bo")]}]) $
                  (play-minion-card $ "p1" "bo" 0)
                  (get-in $ [:players "p1" :minions])
                  (map :name $))
                ["Silver Hand Recruit"]))}
  [state player-id card-id position]
  (let [card (get-card state card-id)]
    (if (valid-play-card? state player-id card)
      (-> (pay-mana state player-id card)
          (remove-card-hand player-id card)
          (place-card-board player-id card position)
          (battlecry player-id card))
      (error "Not a valid minion to play"))))

