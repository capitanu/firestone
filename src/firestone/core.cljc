(ns firestone.core
  (:require [ysera.test :refer [is is-not is= error?]]
            [ysera.error :refer [error]]
            [ysera.random :refer [get-random-int
                                  shuffle-with-seed
                                  take-n-random
                                  random-nth]]
            [ysera.collections :refer [seq-contains?]]
            [firestone.definitions :refer [get-definition]]
            [firestone.construct :refer [add-cards-to-deck
                                         add-card-to-deck
                                         add-cards-to-hand
                                         add-minion-to-board
                                         apply-random-fn
                                         card->minion
                                         create-card
                                         create-game
                                         create-hero
                                         create-minion
                                         count-damaged-characters
                                         damage-hero
                                         draw-card
                                         get-card
                                         get-card-cost
                                         get-deck
                                         get-fatigue
                                         get-hand
                                         get-hero-by-player-id
                                         get-hero-power-cost
                                         get-hero-power
                                         get-heroes
                                         get-latest-minion
                                         get-mana
                                         get-minion
                                         get-minions
                                         get-minion-names
                                         get-player
                                         get-player-id-in-turn
                                         get-random-minion-excluding-caller
                                         take-fatigue?
                                         increase-health
                                         update-minion
                                         update-hero
                                         update-player-mana]]))

(defn get-character
  "Returns the character with the given id from the state."
  {:test (fn []
           (is= (-> (create-game [{:hero (create-hero "Jaina Proudmoore" :id "h1")}])
                    (get-character "h1")
                    (:name))
                "Jaina Proudmoore")
           (is= (-> (create-game [{:minions [(create-minion "Nightblade" :id "n")]}])
                    (get-character "n")
                    (:name))
                "Nightblade"))}
  [state id]
  (or (some (fn [m] (when (= (:id m) id) m))
            (get-minions state))
      (some (fn [h] (when (= (:id h) id) h))
            (get-heroes state))))

(defn get-entity-type
  "Returns the type of the given entity: either minion or hero."
  {:test (fn []
           (is= (-> (create-game [{:hero (create-hero "Jaina Proudmoore" :id "h1")}])
                    (get-entity-type "h1"))
                :hero)
           (is= (-> (create-game [{:minions [(create-minion "Nightblade" :id "n")]}])
                    (get-entity-type "n"))
                :minion))}
  [state id]
  (-> (get-character state id)
      (:name)
      (get-definition)
      (:type)))

(defn get-health
  "Returns the health of the character."
  {:test (fn []
           ;; Uninjured minion
           (is= (-> (create-minion "Nightblade")
                    (get-health))
                4)
           ;; Injured minion
           (is= (-> (create-minion "Nightblade" :damage-taken 1)
                    (get-health))
                3)
           ;; Minion in a state
           (is= (-> (create-game [{:minions [(create-minion "Nightblade" :id "n")]}])
                    (get-health "n"))
                4)
           ;; Uninjured hero
           (is= (-> (create-hero "Jaina Proudmoore")
                    (get-health))
                30)
           ;; Injured hero
           (is= (-> (create-hero "Jaina Proudmoore" :damage-taken 2)
                    (get-health))
                28)
           ;; Hero in a state
           (is= (-> (create-game [{:hero (create-hero "Jaina Proudmoore" :id "h1")}])
                    (get-health "h1"))
                30))}
  ([character]
   {:pre [(map? character) (contains? character :damage-taken)]}
   (let [definition (get-definition character)]
     (- (:health character) (:damage-taken character))))
  ([state id]
   (get-health (get-character state id))))

(defn get-attack
  "Returns the attack of the minion with the given id."
  {:test (fn []
           (is= (-> (create-game [{:minions [(create-minion "Nightblade" :id "n")]}])
                    (get-attack "n"))
                4))}
  [state id]
  (let [minion (get-minion state id)
        definition (get-definition minion)]
    (:attack definition)))




(defn sleepy?
  "Checks if the minion with given id is sleepy."
  {:test (fn []
           (is (-> (create-game [{:minions [(create-minion "Nightblade" :id "n")]}]
                                :minion-ids-summoned-this-turn ["n"])
                   (sleepy? "n")))
           (is-not (-> (create-game [{:minions [(create-minion "Nightblade" :id "n")]}])
                       (sleepy? "n"))))}
  [state id]
  (seq-contains? (:minion-ids-summoned-this-turn state) id))

(defn stealth?
  "Returns true if the minion is stealthy"
  {:test (fn []
           (is= (-> (create-game [{:minions [(create-minion "Moroes" :id "m")]}])
                    (stealth? "m"))
                true))}
  [state id]
  (-> (get-minion state id)
      (:stealth)))

(defn unstealth-minion
  "Removes the stealth mark of a minion"
  {:test (fn []
           (is= (-> (create-game [{:minions [(create-minion "Moroes" :id "m")]}])
                    (unstealth-minion "m")
                    (stealth? "m"))
                nil))}
  [state id]
  (update-minion state id :stealth nil))

(defn valid-attack?
  "Checks if the attack is valid"
  {:test (fn []
           ;; Should be able to attack an enemy minion
           (is (-> (create-game [{:minions [(create-minion "Nightblade" :id "n")]}
                                 {:minions [(create-minion "Boulderfist Ogre" :id "bo")]}])
                   (valid-attack? "p1" "n" "bo")))
           ;; Should be able to attack an enemy hero
           (is (-> (create-game [{:minions [(create-minion "Nightblade" :id "n")]}])
                   (valid-attack? "p1" "n" "h2")))
           ;; Should not be able to attack your own minions
           (is-not (-> (create-game [{:minions [(create-minion "Nightblade" :id "n")
                                                (create-minion "Boulderfist Ogre" :id "bo")]}])
                       (valid-attack? "p1" "n" "bo")))
           ;; Should not be able to attack if it is not your turn
           (is-not (-> (create-game [{:minions [(create-minion "Nightblade" :id "n")]}
                                     {:minions [(create-minion "Boulderfist Ogre" :id "bo")]}]
                                    :player-id-in-turn "p2")
                       (valid-attack? "p1" "n" "bo")))
           ;; Should not be able to attack if you are sleepy
           (is-not (-> (create-game [{:minions [(create-minion "Nightblade" :id "n")]}
                                     {:minions [(create-minion "Boulderfist Ogre" :id "bo")]}]
                                    :minion-ids-summoned-this-turn ["n"])
                       (valid-attack? "p1" "n" "bo")))
           ;; Should not be able to attack if you already attacked this turn
           (is-not (-> (create-game [{:minions [(create-minion "Nightblade" :id "n" :attacks-performed-this-turn 1)]}
                                     {:minions [(create-minion "Boulderfist Ogre" :id "bo")]}])
                       (valid-attack? "p1" "n" "bo"))))}
  [state player-id attacker-id target-id]
  (let [attacker (get-minion state attacker-id)
        target (get-character state target-id)]
    (and attacker
         target
         (= (:player-id-in-turn state) player-id)
         (not= (stealth? state target-id) true)
         (< (:attacks-performed-this-turn attacker) 1)
         (not (sleepy? state attacker-id))
         (not= (:owner-id target) (:player-id-in-turn state))
         (not= (:owner-id attacker) (:owner-id target)))))


(defn reset-minions-attack
  "Resets all current player minion attacked to 0"
  {:test (fn []
           (is= (-> (create-game [{:minions [(create-minion "Silver Hand Recruit" :id "shr" :attacks-performed-this-turn 1)]}
                                  {:hero (create-hero "Jaina Proudmoore" :id "jp")}])
                    (reset-minions-attack "p1")
                    (get-minion "shr")
                    :attacks-performed-this-turn)
                0))}
  [state player-id]
  {:pre [(map? state) (string? player-id)]}
  (as-> (get-in state [:players player-id :minions]) $
    (mapv (fn [x] (update x :attacks-performed-this-turn (constantly 0))) $)
    (update-in state [:players player-id :minions] (constantly $))))


(defn remove-minion?
  "Removes a minion from the table IF it has negative health"
  {:test (fn []
           (is= (-> (create-game [{:minions [(create-minion "Nightblade" :id "n" :damage-taken 5)]}])
                    (remove-minion? "n"))
                true)
           (is= (-> (create-game [{:minions [(create-minion "Nightblade" :id "n" :damage-taken 3)]}])
                    (remove-minion? "n"))
                false))}
  [state minion-id]
  {:pre [(map? state) (string? minion-id)]}
  (<= (get-health state minion-id)
      0))

(defn unlicensed-apothecary-effect
  "Deals 5 damage to your own hero when you summon a minion"
  {:test (fn []
           (is= (-> (create-game [{:hero (create-hero "Jaina Proudmoore" :id "jp")
                                   :minions [(create-minion "Unlicensed Apothecary" :id "ua")]}])
                    (unlicensed-apothecary-effect)
                    (get-health "jp"))
                25))}
  [state]
  (damage-hero state (get-in state [:players (get-player-id-in-turn state) :id]) 5))

(defn minion-attacked?
  "Truthy if minion attacked, falsey otherwise"
  {:test (fn []
           (is= (-> (create-game [{:minions [(create-minion "Silver Hand Recruit" :id "shr")]}])
                    (minion-attacked? "shr"))
                false)
           (is= (-> (create-game [{:minions [(create-minion "Silver Hand Recruit" :id "shr" :attacks-performed-this-turn 1)]}])
                    (minion-attacked? "shr"))
                true))}
  [state minion-id]
  {:pre [(map? state) (string? minion-id)]}
  (if (= 1
         (-> (get-character state minion-id)
             (:attacks-performed-this-turn)))
    true
    false))

(defn set-minion-attacked
  "Sets the minions attack to 1"
  {:test (fn []
           (is= (-> (create-game [{:minions [(create-minion "Silver Hand Recruit" :id "shr")]}])
                    (set-minion-attacked "shr")
                    (minion-attacked? "shr"))
                true)
           (is= (-> (create-game [{:minions [(create-minion "Silver Hand Recruit" :id "shr")]}])
                    (minion-attacked? "shr"))
                false))}
  [state minion-id]
  {:pre [(map? state) (string? minion-id)]}
  (update-minion state minion-id :attacks-performed-this-turn 1))

(defn valid-play-minion?
  {:test (fn []
           (is= (-> (create-game [{:mana 5}])
                    (valid-play-minion? "p1" (create-card "Boulderfist Ogre")))
                false)
           (is= (-> (create-game [{:mana 6}])
                    (valid-play-minion? "p1" (create-card "Boulderfist Ogre")))
                true)
           (is= (-> (create-game [{:mana 7}])
                    (valid-play-minion? "p1" (create-card "Boulderfist Ogre")))
                true))}
  [state player-id card]
  {:pre [(map? state) (string? player-id) (map? card)]}
  (and (= (:player-id-in-turn state) player-id)
       (< (count (get-in state [:players player-id :minions])) 7)
       (<= (get-card-cost card)
           (get-mana state player-id))))

(defn pay-mana
  {:test (fn []
           (is= (-> (create-game [{:mana 7}])
                    (pay-mana "p1" 6)
                    (get-mana "p1"))
                1))
   }
  [state player-id mana-cost]
  {:pre [(map? state) (string? player-id) (int? mana-cost)]}
  (let [player-mana (get-mana state player-id)]
    (update-player-mana state player-id (- player-mana mana-cost))))

(defn remove-card-hand
  "Removes a card from the hand"
  {:test (fn []
           (is= (let [card (create-card "Boulderfist Ogre" :id "bo")]
                  (-> (create-game [{:hand [card (create-card "Nightblade" :id "n" )]}])
                      (remove-card-hand "p1" card)
                      (get-in [:players "p1" :hand])
                      (first)
                      (:id)))
                "n"))}
  [state player-id card]
  {:pre [(map? state) (string? player-id) (map? card)]}
  (as-> (remove (fn [x] (= (:id x) (:id card))) (get-hand state player-id)) $
    (update-in state [:players player-id :hand] (constantly $)))
  )

(defn reaction-effect
  "Performs an effect on minion summon"
  {:test (fn []
           (is= (-> (create-game [{:minions [(create-minion "Unlicensed Apothecary" :id "ua")]}])
                    (reaction-effect "ua")
                    (get-in [:players "p1" :hero :damage-taken]))
                5))}
  [state minion-id]
  (cond (= (:name (get-minion state minion-id))
           "Unlicensed Apothecary")
        (unlicensed-apothecary-effect state)
        :else
        state))

(-> (create-game [{:minions [(create-minion "Unlicensed Apothecary" :id "ua")]}])
    (reaction-effect "ua"))

(defn trigger-reaction-effects
  "Triggers each reaction effect one by one"
  [state player-id]
  (let [minions (get-in state [:players player-id :reaction-effect-minions])]
    (reduce (fn [state minion-id]
              (reaction-effect state minion-id))
            state
            minions)))

(defn place-card-board
  "Creates a minion and places it on the board"
  {:test (fn []
           (is= (let [card (create-card "Boulderfist Ogre" :id "bo")]
                  (-> (create-game [{:hand [card]}])
                      (place-card-board "p1" card 1)
                      (get-in [:players "p1" :minions])
                      (first)
                      (:name)))
                "Boulderfist Ogre")
           (is= (let [card (create-card "Boulderfist Ogre" :id "bo")]
                  (-> (create-game [{:hand [card]}])
                      (place-card-board "p1" card 1)
                      (get :minion-ids-summoned-this-turn)
                      (count)))
                1)
           (is= (let [card (create-card "Nightblade" :id "n")]
                  (as-> (create-game [{:hand [card] :minions [(create-minion "Boulderfist Ogre") (create-minion "Boulderfist Ogre")]}]) $
                    (place-card-board $ "p1" card 0)
                    (get-in $ [:players "p1" :minions])
                    (map :name $)))
                ["Boulderfist Ogre" "Boulderfist Ogre" "Nightblade"])
           (is= (let [card1 (create-card "Moroes" :id "m1")
                      card2 (create-card "Moroes" :id "m2")]
                  (-> (create-game [{:hand [card1 card2]}])
                      (place-card-board "p1" card1 0)
                      (place-card-board "p1" card2 0)
                      (get-in [:players "p1" :end-effect-minions])
                      (count)))
                2)
           (is= (let [card (create-card "Moroes" :id "m")]
             (as-> (create-game [{:hand [card] :minions [(create-minion "Boulderfist Ogre") (create-minion "Boulderfist Ogre")]}]) $
                   (place-card-board $ "p1" card 0)
                   (get-in $ [:players "p1" :end-effect-minions])))
                ["m5"])
           (is= (let [card (create-card "Unlicensed Apothecary" :id "ua")]
                  (as-> (create-game [{:hand [card] :minions [(create-minion "Boulderfist Ogre") (create-minion "Boulderfist Ogre")]}]) $
                        (place-card-board $ "p1" card 0)
                        (get-in $ [:players "p1" :reaction-effect-minions])))
                ["m5"])
           (is= (let [card (create-card "Unlicensed Apothecary" :id "ua")]
                  (let [card2 (create-card "Boulderfist Ogre" :id "bo")]
                    (as-> (create-game [{:hand [card] :minions [(create-minion "Boulderfist Ogre") (create-minion "Boulderfist Ogre")]}]) $
                          (place-card-board $ "p1" card 0)
                          (place-card-board $ "p1" card2 1)
                          (get-health $ "h1"))))
                25)
           (is= (let [card (create-card "Unlicensed Apothecary")]
                   (let [card2 (create-card "Unlicensed Apothecary")]
                     (let [card3 (create-card "Boulderfist Ogre" :id "bo")]
                       (as-> (create-game [{:hand [card] :minions [(create-minion "Boulderfist Ogre") (create-minion "Boulderfist Ogre")]}]) $
                             (place-card-board $ "p1" card 0)
                             (place-card-board $ "p1" card2 1)
                             (place-card-board $ "p1" card3 2)
                             (get-health $ "h1")))))
                15))}
  [state player-id card position]
  {:pre [(map? state) (string? player-id) (map? card) (int? position)]}
  (let [minion (card->minion card)]
    (as-> (add-minion-to-board state player-id minion position) $
          (trigger-reaction-effects $ player-id)
      (let [state-temp $]
        (let [minions (get-in $ [:players player-id :minions])]
          (let [minion-id (-> (filter
                               (fn [x] (= (:added-to-board-time-id x)
                                         (reduce max (map (fn [y] (:added-to-board-time-id y)) minions))))
                               minions)
                              (first)
                              (:id))]
            (as-> (update-in state-temp [:minion-ids-summoned-this-turn] (constantly (conj (get state-temp :minion-ids-summoned-this-turn) minion-id))) x
                  (let [st x]
                    (as-> (if (:end-effect minion)
                      (update-in st [:players player-id :end-effect-minions] (constantly (conj (get-in st [:players player-id :end-effect-minions]) minion-id)))
                      st) y
                    (if (:reaction-effect minion)
                      (update-in y [:players player-id :reaction-effect-minions] (constantly (conj (get-in y [:players player-id :reaction-effect-minions]) minion-id)))
                      y)
                    )))))))))

(defn battlecry
  "Makes the battle-cry happen"
  {:test (fn []
           (is= (let [card (create-card "Faceless Manipulator")]
                  (as-> (create-game [{:hero "Jaina Proudmoore" :minions [(create-minion "Boulderfist Ogre" :id "bo")]} {:hero (create-hero "Jaina Proudmoore" :id "jp")}]) $
                    (place-card-board $ "p1" card 0)
                    (battlecry $ card :player-id "p1" :target-id "bo")
                    (get-minions $ "p1")
                    (map :name $)))
                ["Boulderfist Ogre", "Boulderfist Ogre"])
           (is= (-> (create-game [{:minions [(create-minion "Injured Blademaster" :id "ib")]}])
                    (battlecry (create-card "Injured Blademaster") :player-id "p1")
                    (get-health "ib"))
                3)
           (is= (-> (create-game [{:hero "Jaina Proudmoore"} {:hero (create-hero "Jaina Proudmoore" :id "jp")}])
                    (battlecry (create-card "Nightblade") :player-id "p1")
                    (get-health  "jp"))
                27)
           (is= (-> (create-game [{:hero "Jaina Proudmoore"} {:hero (create-hero "Jaina Proudmoore" :id "jp")}])
                    (battlecry (create-card "Alexstrasza") :player-id "p1" :target-id "p2")
                    (get-health  "jp"))
                15)
           (is= (-> (create-game [{:deck [(create-card "Alexstrasza")] :minions [(create-minion "Barnes" :id "b")]}])
                    (battlecry (create-card "Barnes") :player-id "p1")
                    (get-minions "p1")
                    (count))
                2)
           (is= (-> (create-game [{:hero (create-hero "Jaina Proudmoore" :damage-taken 10)}])
                    (battlecry (create-card "Antique Healbot") :player-id "p1")
                    (get-in [:players "p1" :hero :damage-taken]))
                2))}
  [state card & {player-id :player-id target-id :target-id}]
  {:pre [(map? state) (string? player-id) (map? card)]}
  (if (:battlecry (get-definition (:name card)))
    ((:battlecry (get-definition (:name card))) state :player-id player-id :target-id target-id)
    state))

(defn get-owner
  "Return the id of the owner"
  {:test (fn []
           (is= (-> (create-game [{:hero (create-hero "Jaina Proudmoore" :id "jp")}])
                    (get-owner "jp"))
                "p1")
           (is= (-> (create-game [{:minions [(create-minion "Boulderfist Ogre" :id "bo")]}])
                    (get-owner "bo"))
                "p1")
           )}
  [state id]
  {:pre [(map? state) (string? id)]}
  (cond (= (get-entity-type state id)
           :minion)
        (-> (get-minion state id)
            (:owner-id))

        (= (get-entity-type state id)
           :hero)
        (cond (= (get-in state [:players "p1" :hero :id]) id)
              "p1"

              (= (get-in state [:players "p2" :hero :id]) id)
              "p2"

              :else
              (error "Invalid hero id"))))

(defn get-damage-taken
  "Return the damage taken by the entity"
  {:test (fn []
           (is= (-> (create-game [{:hero (create-hero "Jaina Proudmoore" :id "jp" :damage-taken 25)}])
                    (get-damage-taken "jp"))
                25)
           (is= (-> (create-game [{:minions [(create-minion "Boulderfist Ogre" :id "bo" :damage-taken 2)]}])
                    (get-damage-taken "bo"))
                2))}
  [state id]
  {:pre [(map? state) (string? id)]}
  (cond (= (get-entity-type state id)
           :minion)
        (-> (filter (fn [x] (= (:id x) id)) (get-in state [:players (get-owner state id) :minions]))
            (first)
            (:damage-taken))

        (= (get-entity-type state id)
           :hero)
        (get-in state [:players (get-owner state id) :hero :damage-taken])))

(defn valid-hero-power-use?
  {:test (fn []
           (is= (-> (create-game [{:mana 1} {:minions [(create-minion "Silver Hand Recruit" :id "shr")]}])
                    (valid-hero-power-use? "p1" "shr"))
                false)
           (is= (-> (create-game [{:mana 2} {:minions [(create-minion "Silver Hand Recruit" :id "shr")]}])
                    (valid-hero-power-use? "p1" "shr"))
                true)
           (is= (-> (create-game [{:mana 3} {:minions [(create-minion "Silver Hand Recruit" :id "shr")]}])
                    (valid-hero-power-use? "p1"))
                false)
           (is= (-> (create-game [{:mana 3 :hero (create-hero "Rexxar")} {:minions [(create-minion "Silver Hand Recruit" :id "shr")]}])
                    (valid-hero-power-use? "p1"))
                true))}
  [state player-id & target-id]
  (and (= (:player-id-in-turn state) player-id)
       (= (get-in state [:players player-id :hero :hero-power-used]) false)
       (or (and (= (:name (get-hero-by-player-id state player-id))
                   "Jaina Proudmoore")
                target-id)
           (= (:name (get-hero-by-player-id state player-id))
              "Rexxar"))
       (<= (get-hero-power-cost (get-hero-by-player-id state player-id))
           (get-mana state player-id))))

(defn deathrattle
  "Performs the deathrattle of the given minion."
  {:test (fn []
           (is= (-> (create-game [{:minions [(create-minion "Loot Hoarder" :id "lh")]}])
                    (deathrattle "Loot Hoarder" :player-id "p1")
                    (get-in [:players "p1" :hero :damage-taken]))
                1)
           (is= (-> (create-game [{:minions [(create-minion "Cairne Bloodhoof" :id "cb")]}])
                    (deathrattle "Cairne Bloodhoof" :player-id "p1")
                    (get-minions "p1")
                    (second)
                    (:name))
                "Baine Bloodhoof")
           (is= (-> (create-game [{:minions [(create-minion "Astral Tiger" :id "at")]}])
                    (deathrattle "Astral Tiger" :player-id "p1")
                    (get-deck "p1")
                    (count))
                1)
           (is= (-> (create-game [{:minions [(create-minion "Astral Tiger" :id "at")] :deck [(create-card "Alexstrasza")]}])
                    (deathrattle "Astral Tiger" :player-id "p1")
                    (get-deck "p1")
                    (count))
                2)
           (is= (-> (create-game [{:minions [(create-minion "Loot Hoarder" :id "lh")] :deck [(create-card "Alexstrasza")]}])
                    (deathrattle "Loot Hoarder" :player-id "p1")
                    (get-deck "p1")
                    (count))
                0))}
  [state minion-name & {player-id :player-id}]
  {:pre [(map? state)]}
  (if (:deathrattle (get-definition minion-name))
    ((:deathrattle (get-definition minion-name)) state :player-id player-id)
    state))

(defn end-of-turn-yp
  "Increases the health of a random friendly minion by 1 that is not itself"
  {:test (fn []
           (is= (-> (create-game [{:minions [(create-minion "Silver Hand Recruit" :id "shr")]}])
                    (end-of-turn-yp "p1" "yp")
                    (get-minions "p1")
                    (first)
                    (:health))
                2)
           (is= (-> (create-game [{:minions [(create-minion "Young Priestess" :id "yp")]}])
                    (end-of-turn-yp "p1" "yp")
                    (get-minions "p1")
                    (first)
                    (:health))
                1)
           (is= (-> (create-game [{:minions [(create-minion "Young Priestess" :id "yp")
                                             (create-minion "Silver Hand Recruit" :id "shr")]}])
                    (end-of-turn-yp "p1" "yp")
                    (get-minion "shr")
                    (:health))
                2))}
  [state player-id minion-id]
  (let [minions (get-minions state player-id)
        result (get-random-minion-excluding-caller state minion-id player-id)
        st (first result)
        minion (second result)]
    (if (and minion
             (not= (:id minion)
                   "yp"))
      (increase-health st player-id (:id minion) 1)
      st)))

(defn remove-minion
  "Removes a minion from the table IF it has negative health"
  {:test (fn []
           (is= (-> (create-game [{:minions [(create-minion "Boulderfist Ogre" :id "bo")]}])
                    (remove-minion "bo")
                    (get-in [:players "p1" :minions])
                    (count))
                0))}
  [state minion-id]
  {:pre [(map? state) (string? minion-id)]}
  (let [player-id (-> state
                      (get-character minion-id)
                      (:owner-id))
        minion-name (:name (get-minion state minion-id))]
    (as-> (into [] (filter (fn [x] (not= (:id x) minion-id)) (get-in state [:players player-id :minions]))) $
      (update-in state [:players player-id :minions] (constantly $))
        (let [st $]
          (as-> (update-in st [:players player-id :end-effect-minions] (constantly (filter (fn [x] (not= minion-id x)) (get-in st [:players player-id :end-effect-minions])))) y
            (update-in y [:players player-id :reaction-effect-minions] (constantly (filter (fn [x] (not= minion-id x)) (get-in y [:players player-id :reaction-effect-minions]))))
            (deathrattle y minion-name :player-id player-id))))))

(defn remove-sleeping-minions
  {:test (fn [] 
           (is= (as-> (create-game) $
                  (update $ :minion-ids-summoned-this-turn (constantly ["bo"]))
                  (remove-sleeping-minions $)
                  (get $ :minion-ids-summoned-this-turn))
                []))}
  [state]
  {:pre [(map? state)]}
  (update state :minion-ids-summoned-this-turn (constantly [])))


(defn valid-play-spell?
  {:test (fn []
           (is= (-> (create-game [{:mana 1}])
                    (valid-play-spell? "p1" (create-card "Battle Rage")))
                false)
           (is= (-> (create-game [{:mana 5}])
                    (valid-play-spell? "p1" (create-card "Devour Mind")))
                true)
           (is= (-> (create-game [{:mana 7}])
                    (valid-play-spell? "p1" (create-card "Devour Mind")))
                true))}
  [state player-id card]
  {:pre [(map? state) (string? player-id) (map? card)]}
  (and (= (:player-id-in-turn state) player-id)
       (<= (get-card-cost card)
           (get-mana state player-id))))

(defn play-spell
  {:test (fn []
           (is= (-> (create-game [{} {:deck [(create-card "Alexstrasza") (create-card "Alexstrasza") (create-card "Alexstrasza")]}])
                    (play-spell "p1" (create-card "Devour Mind"))
                    (get-in [:players "p1" :hand])
                    (count))
                3)
           (is= (-> (create-game [{:minions [(create-minion "Boulderfist Ogre" :damage-taken 1)] :deck [(create-card "Alexstrasza") (create-card "Alexstrasza") (create-card "Alexstrasza")]}])
                    (play-spell "p1" (create-card "Battle Rage"))
                    (get-in [:players "p1" :hand])
                    (count))
                1)
           (is= (-> (create-game [{:minions [(create-minion "Boulderfist Ogre" :damage-taken 1) (create-minion "Alexstrasza" :damage-taken 3)] :deck [(create-card "Alexstrasza") (create-card "Alexstrasza") (create-card "Alexstrasza")]}])
                    (play-spell "p1" (create-card "Battle Rage"))
                    (get-in [:players "p1" :hand])
                    (count))
                2)
           (is= (-> (create-game [{:minions [(create-minion "Boulderfist Ogre" :damage-taken 1) (create-minion "Alexstrasza" :damage-taken 3)] :deck [(create-card "Alexstrasza")]}])
                    (play-spell "p1" (create-card "Battle Rage"))
                    (get-in [:players "p1" :hero :damage-taken]))
                1)
           )}
  [state player-id card]
  (cond (= (:name card) "Devour Mind")
        (let [player-change-fn {"p1" "p2"
                                "p2" "p1"}
              deck (get-in state [:players (player-change-fn player-id) :deck])
              seed (or (get state :seed) 1234)]
          (as->
              (take-n-random seed 3 deck) $
            (let [result $]
              (as-> (map :name (second result)) x
                (add-cards-to-hand state player-id x)
                (update x :seed (constantly (first result)))))))

        (= (:name card) "Battle Rage")
        (let [damaged (count-damaged-characters state player-id)]
          (loop [damaged damaged
                 state state]
            (if (zero? damaged)
              state
              (recur (dec damaged)
                     (if (take-fatigue? state player-id)
                       (get-fatigue state player-id)
                       (draw-card state player-id))))))

        :else
        (error "Not a valid spell")))

(defn draw-first-minion
  "Draws the first minion in the deck"
  {:test (fn []
           (is= (-> (create-game [{:deck [(create-card "Battle Rage") (create-card "Boulderfist Ogre") (create-card "Battle Rage")]}])
                    (draw-first-minion "p1")
                    (get-in [:players "p1" :hand])
                    (first)
                    (:name))
                "Boulderfist Ogre")
           (is= (-> (create-game [{:deck [(create-card "Battle Rage") (create-card "Battle Rage")]}])
                    (draw-first-minion "p1")
                    (get-in [:players "p1" :hand])
                    (first))
                nil))}
  [state player-id]
  (let [first-minion (as-> (get-deck state player-id) $
                  (filter (fn [x] (= (:type x) :minion)) $)
                  (first $))]
    (if first-minion
      (if (< (-> (count (get-in state [:players player-id :hand])))
             10)
        (as-> (filter (fn [x] (not= (:id x) (:id first-minion))) (get-deck state player-id)) $
          (update-in state [:players player-id :deck] (constantly $))
          (update-in $ [:players player-id :hand] conj first-minion))
        (as-> (filter (fn [x] (not= (:id x) (:id first-minion))) (get-deck state player-id)) $
          (update-in state [:players player-id :deck] (constantly $))))
      state)))


(defn combo
  "Triggers the combo"
  {:test (fn []
           (is= (as-> (create-game []) $
                    (place-card-board $ "p1" (create-card "Boulderfist Ogre") 0)
                    (place-card-board $ "p1" (create-card "Shado-Pan Rider") 0)
                    (combo $ "p1" (create-card "Shado-Pan Rider"))
                    (let [state $]
                      (-> (get-minion state (:id (get-latest-minion state)))
                          (:attack))))
                6)
           (is= (-> (create-game [{:deck [(create-card "Boulderfist Ogre")]}])
                    (place-card-board "p1" (create-card "Boulderfist Ogre") 0)
                    (place-card-board "p1" (create-card "Shado-Pan Rider") 0)
                    (combo "p1" (create-card "Elven Minstrel"))
                    (get-in [:players "p1" :hand])
                    (first)
                    (:name))
                "Boulderfist Ogre")
           (is= (as-> (create-game [{:deck [(create-card "Boulderfist Ogre") (create-card "Alexstrasza")]}]) $
                  (place-card-board $ "p1" (create-card "Boulderfist Ogre") 0)
                  (place-card-board $ "p1" (create-card "Shado-Pan Rider") 0)
                  (combo $ "p1" (create-card "Elven Minstrel"))
                  (get-in $ [:players "p1" :hand])
                  (map :name $))
                ["Boulderfist Ogre" "Alexstrasza"])
           )}
  [state player-id card]
  {:pre [(map? state) (string? player-id) (map? card)]}
  (if (>= 1 (count (get state :minion-ids-summoned-this-turn)))
    state
    (cond (= (:name card) "Elven Minstrel")
          (-> (draw-first-minion state player-id)
              (draw-first-minion player-id))

          (= (:name card) "Shado-Pan Rider")
          (update-minion state (:id (get-latest-minion state)) :attack (+ 3 (get-attack state (:id (get-latest-minion state)))))
          
          :else
          state)))

(defn use-hero-power
  "Uses the hero power of the current"
  {:test (fn []
           (is= (-> (create-game [{} {:minions [(create-minion "Silver Hand Recruit" :id "shr")]}])
                    (use-hero-power "p1" "shr")
                    (get-minions "p1"))
                [])
           )}
  [state player-id & target-id]
  (let [hero-power (get-hero-power (get-hero-by-player-id state player-id))]
    (cond (= (:name hero-power)
             "Fireblast")
          (let [tg-id (first target-id) type (get-entity-type state tg-id)]
            (cond (= type :minion)
                  (as-> (update-minion state tg-id :damage-taken (+ 1 (get-damage-taken state tg-id))) $
                    (let [st $]
                      (if (remove-minion? st tg-id)
                        (remove-minion st tg-id)
                        st)))

                  (= type :hero)
                  (update-hero state tg-id :damage-taken (+ 1 (get-damage-taken state tg-id)))

                  :else
                  (error "Type of the target is unrecognized")))

          (= (:name hero-power)
             "Ballista Shot")
          (let [player-change-fn {"p1" "p2"
                                  "p2" "p1"}]
            (update-hero state (:id (get-hero-by-player-id state (player-change-fn player-id))) :damage-taken (+ 2 (get-damage-taken state (:id (get-hero-by-player-id state (player-change-fn player-id)))))))

          :else
          state)))

(defn end-effect
  "Triggers one end effect"
  {:test (fn []
           (is= (-> (create-game [{:minions [(create-minion "Moroes" :id "m")]}])
                    (end-effect "m")
                    (get-minions)
                    (second)
                    (:name))
                "Steward")
           (is= (-> (create-game [{:minions [(create-minion "Silver Hand Recruit" :id "shr")]}])
                    (end-of-turn-yp "p1" "yp")
                    (get-minions "p1")
                    (first)
                    (:health))
                2))}
  [state minion-id]
  (cond (= (:name (get-minion state minion-id))
           "Moroes")
        (if (> 7 (count (get-in state [:player (get-player-id-in-turn state) :minions])))
          (add-minion-to-board state (get-player-id-in-turn state) (create-minion "Steward") 7)
          state)
        
        (= (:name (get-minion state minion-id))
           "Young Priestess")
        (end-of-turn-yp state (get-in state [:players (get-player-id-in-turn state) :id] )minion-id)
        :else
        state))

(defn trigger-end-turn-effects
  "Triggers each end of turn effect one by one"
  {:test (fn []
           (is= (-> (create-game)
                    (place-card-board "p1" (create-card "Moroes") 0)
                    (trigger-end-turn-effects "p1")
                    (get-minions "p1")
                    (count))
                2))}
  [state player-id]
  (let [minions (get-in state [:players player-id :end-effect-minions])]
    (reduce (fn [state minion-id]
              (end-effect state minion-id))
            state
            minions)))

