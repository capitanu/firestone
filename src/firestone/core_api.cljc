(ns firestone.core-api
  (:require [ysera.test :refer [is= error?]]
            [ysera.error :refer [error]]
            [firestone.definitions :refer [get-definition]]
            [firestone.construct :refer [create-game
                                         get-card
                                         get-fatigue
                                         get-hand
                                         get-hero-by-player-id
                                         get-hero-power
                                         get-player-id-in-turn
                                         get-player
                                         get-minion
                                         get-minions
                                         get-max-mana
                                         get-mana
                                         get-players
                                         create-card
                                         create-hero
                                         create-minion
                                         reset-mana
                                         reset-hero-power
                                         set-hero-power
                                         update-hero
                                         update-minion]]
            [firestone.core :refer [battlecry
                                    combo
                                    draw-card
                                    get-character
                                    get-damage-taken
                                    get-health
                                    get-attack
                                    get-entity-type
                                    get-latest-minion
                                    take-fatigue?
                                    pay-mana
                                    place-card-board
                                    play-spell
                                    reset-minions-attack
                                    remove-card-hand
                                    remove-minion
                                    remove-minion?
                                    remove-sleeping-minions
                                    stealth?
                                    sleepy?
                                    set-minion-attacked
                                    trigger-end-turn-effects
                                    use-hero-power
                                    unstealth-minion
                                    valid-attack?
                                    valid-hero-power-use?
                                    valid-play-minion?
                                    valid-play-spell?]]))


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

           (is= (-> (create-game [{:max-mana 0}])
                    (end-turn "p1")
                    (end-turn "p2")
                    (get-max-mana "p1"))
                1)
           (is= (-> (create-game [{:mana 0 :max-mana 0}])
                    (end-turn "p1")
                    (end-turn "p2")
                    (get-mana "p1"))
                1)
           (is= (-> (create-game)
                    (end-turn "p1")
                    (get-in [:players "p2" :hero :damage-taken]))
                1)
           (is= (-> (create-game [{:minions [(create-minion "Nightblade" :id "n")]}]
                                 :minion-ids-summoned-this-turn ["n"])
                    (end-turn "p1")
                    (sleepy? "n"))
                false)
           (error? (-> (create-game)
                       (end-turn "p2"))))}
  [state player-id]
  (when-not (= (get-player-id-in-turn state) player-id)
    (error "The player with id " player-id " is not in turn."))
  (let [player-change-fn {"p1" "p2"
                          "p2" "p1"}]
    (as-> state $
          (reset-mana $ (player-change-fn player-id))
          (remove-sleeping-minions $)
          (trigger-end-turn-effects $ player-id)
          (update $ :player-id-in-turn player-change-fn)
          (reset-minions-attack $ (player-change-fn player-id))
          (reset-hero-power $ (player-change-fn player-id))
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
           (is= (-> (create-game [{:minions [(create-minion "Moroes" :id "m")]}])
                    (attack "p1" "m" "h2")
                    (stealth? "m"))
                nil)
           (error? (-> (create-game [{:minions [(create-minion "Boulderfist Ogre" :id "bo")]} {:minions [(create-minion "Moroes" :id "m")]}])
                    (attack "p1" "bo" "m")
                    (stealth? "m")))
           (is= (-> (create-game [{:minions [(create-minion "Moroes" :id "m1")]} {:hero (create-hero "Rexxar" :id "r") :minions [(create-minion "Moroes" :id "m2")]}])
                    (attack "p1" "m1" "r")
                    (end-turn "p1")
                    (attack "p2" "m2" "m1")
                    (get-minions))
                [])
           (is= (-> (create-game [{:minions [(create-minion "Silver Hand Recruit" :id "shr")]
                                   :hero    (create-hero "Rexxar" :id "r")}
                                  {:minions [(create-minion "Injured Blademaster" :id "ib")]
                                   :hero    (create-hero "Jaina Proudmoore" :id "jp")}]
                                 :player-id-in-turn "p1")
                    (attack "p1" "shr" "jp")
                    (get-health "jp"))
                29)
           (is= (-> (create-game [{:minions [(create-minion "Silver Hand Recruit" :id "shr")]}
                                  {:minions [(create-minion "Injured Blademaster" :id "ib")]}]
                                 :player-id-in-turn "p1")
                    (attack "p1" "shr" "ib")
                    (get-minion "shr"))
                nil))}
  [state player-id attacker-id target-id]
  (if (valid-attack? state player-id attacker-id target-id)
    (let [type (get-entity-type state target-id)]
      (cond (= type :minion)
            (as-> (update-minion state target-id :damage-taken (+ (get-attack state attacker-id) (get-damage-taken state target-id))) $
              (update-minion $ attacker-id :damage-taken (+ (get-attack state target-id) (get-damage-taken state attacker-id)))
              (let [st $]
                (if (stealth? st attacker-id)
                  (unstealth-minion st attacker-id)
                  st))
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
            (as-> (update-hero state target-id :damage-taken (+ (get-attack state attacker-id) (get-damage-taken state target-id))) $
                (set-minion-attacked $ attacker-id)
                (let [st $]
                  (if (stealth? st attacker-id)
                    (unstealth-minion st attacker-id)
                    st)))

            :else
            (error "Type of the card is unrecognized")))
    (error "Invalid attack")))

(defn play-card
  "Plays a minion card if it is available in the hand"
  {:test (fn []
           (is= (as-> (create-game [{:hand [(create-card "Silver Hand Recruit" :id "shr")]}]) $
                      (play-card $ "p1" "shr" :position 0)
                      (get-in $ [:players "p1" :minions])
                      (map :name $))
                ["Silver Hand Recruit"])
           (is= (as-> (create-game [{:hand [(create-card "Silver Hand Recruit" :id "shr")]}]) $
                      (play-card $ "p1" "shr" :position 0)
                      (get-in $ [:players "p1" :minions])
                      (map :name $))
                ["Silver Hand Recruit"])
           (is= (-> (create-game [{:hand [(create-card "Moroes" :id "m")]}])
                    (play-card "p1" "m" :position 0)
                    (end-turn "p1")
                    (get-minions "p1")
                    (count))
                2)
           (is= (-> (create-game [{:hand [(create-card "Moroes" :id "m1") (create-card "Moroes" :id "m2")]}])
                    (play-card "p1" "m1" :position 0)
                    (play-card "p1" "m2" :position 0)
                    (end-turn "p1")
                    (get-minions "p1")
                    (count))
                4)
           (is= (as-> (create-game [{:hand [(create-card "Moroes" :id "m")]} {:minions [(create-minion "Boulderfist Ogre" :id "bo")]}]) $
                      (play-card $ "p1" "m" :position 0)
                      (end-turn $ "p1")
                      (let [state $
                            minion (get-latest-minion state)]
                        (-> (attack state "p2" "bo" (:id minion))
                            (get-minions "p1")
                            (count))))
                1)
           (is= (-> (create-game [{:hand [(create-card "Moroes" :id "m")]} {:minions [(create-minion "Boulderfist Ogre" :id "bo")]}])
                    (play-card "p1" "m" :position 0)
                    (end-turn "p1")
                    (end-turn "p2")
                    (attack "p1" "m2" "bo")
                    (end-turn "p1")
                    (get-minions "p1")
                    (count))
                1)
           (is= (-> (create-game [{:hand [(create-card "Young Priestess" :id "yp")]
                              :minions [(create-minion "Boulderfist Ogre" :id "bo")]}])
               (play-card "p1" "yp" :position 0)
               (end-turn "p1")
               (get-minion "bo")
               :health)
                8)
           (is= (-> (create-game [{:hand [(create-card "Young Priestess" :id "yp")]
                                   :minions [(create-minion "Young Priestess" :id "yp2")]}])
                    (play-card "p1" "yp" :position 0)
                    (end-turn "p1")
                    (get-minion "yp2")
                    :health)
                2)
           (is= (as-> (create-game [{:hand [(create-card "Faceless Manipulator" :id "fm")] :minions [(create-minion "Boulderfist Ogre" :id "bo")]}]) $
                      (play-card $ "p1" "fm" :position 0 :target-id "bo")
                      (get-in $ [:players "p1" :minions])
                      (map :name $))
                ["Boulderfist Ogre", "Boulderfist Ogre"])
           (is= (-> (create-game [{:hand [(create-card "Alexstrasza" :id "a")] :minions [(create-minion "Boulderfist Ogre" :id "bo")]}])
                    (play-card "p1" "a" :position 0 :target-id "p2")
                    (get-in [:players "p2" :hero :health]))
                15)
           (is= (as-> (create-game [{:hand [(create-card "Nightblade" :id "n")]}]) $
                      (play-card $ "p1" "n" :position 0)
                      (get-in $ [:players "p2" :hero :damage-taken]))
                3)
           (error? (as-> (create-game [{:hand [(create-card "Nightblade" :id "n")]
                                        :mana 0}]) $
                         (play-card $ "p1" "n" 0)
                         )))}
  [state player-id card-id & {position :position target-id :target-id}]
  (let [card (get-card state card-id)]
    (cond (= (:type card) :minion)
          (if (valid-play-minion? state player-id card)
            (-> (pay-mana state player-id (-> (get-definition (:name card))
                                              (:mana-cost)))
                (remove-card-hand player-id card)
                (place-card-board player-id card position)
                (battlecry card :player-id player-id :target-id target-id)
                (combo player-id card))
            (error "Not a valid minion to play"))

          (= (:type card) :spell)
          (if (valid-play-spell? state player-id card)
            (-> (pay-mana state player-id (-> (get-definition (:name card))
                                              (:mana-cost)))
                (remove-card-hand player-id card)
                (play-spell player-id card))


            (error "Not a valid spell to play"))

          :else
          (error "This is not a valid card type"))))

(defn hero-power
  "Uses the hero power of the player's hero"
  {:test (fn []
           (is= (-> (create-game [{:hero "Jaina Proudmoore"} {:minions [(create-minion "Silver Hand Recruit" :id "shr")]}])
                    (hero-power "p1" "shr")
                    (get-minions))
                [])
           (is= (-> (create-game [{:hero "Jaina Proudmoore" :minions [(create-minion "Moroes" :id "m")]}])
                    (hero-power "p1" "m")
                    (get-minions))
                [])
           (is= (-> (create-game [{:hero "Rexxar"} {:hero (create-hero "Jaina Proudmoore" :id "jp")}])
                    (hero-power "p1")
                    (get-character "jp")
                    (get-health))
                28))}
  [state player-id & target-id]
  {:pre [(map? state) (string? player-id)]}
  (if (valid-hero-power-use? state player-id target-id)
    (-> (pay-mana state player-id (:mana-cost (get-hero-power (get-hero-by-player-id state player-id))))
        (use-hero-power player-id (if (empty? target-id)
                                    nil
                                    (first target-id)
                                    ))
        (set-hero-power player-id))
    (-> (error "Can not play hero power")
        state)))
