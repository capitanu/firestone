(ns firestone.definition.card
  (:require [firestone.definitions :refer [add-definitions!]]
            [firestone.construct :refer [get-minion
                                         get-deck
                                         add-card-to-deck
                                         add-minion-to-board
                                         draw-first-minion
                                         draw-card
                                         create-card
                                         get-attack
                                         create-minion
                                         get-fatigue
                                         get-latest-minion
                                         get-minions
                                         add-minion-to-board
                                         take-fatigue?
                                         update-minion]]
            [ysera.random :refer [random-nth
                                  shuffle-with-seed
                                  get-random-int]]))

(def card-definitions
  {

   ;; Implemented
   "Antique Healbot"
   {:name        "Antique Healbot"
    :attack      3
    :health      3
    :mana-cost   5
    :type        :minion
    :set         :goblins-vs-gnomes
    :race        :mech
    :rarity      :common
    :description "Battlecry: Restore 8 Health to your hero."
    :battlecry (fn [state & {player-id :player-id}]
                 (update-in state [:players player-id :hero :damage-taken]
                            (constantly (max 0
                                             (- (get-in state [:players player-id :hero :damage-taken]) 8)))))}

   ;; Implemented
   "Boulderfist Ogre"
   {:name      "Boulderfist Ogre"
    :attack    6
    :health    7
    :mana-cost 6
    :type      :minion
    :set       :basic}

   ;; Implemented
   "Injured Blademaster"
   {:name        "Injured Blademaster"
    :attack      4
    :health      7
    :mana-cost   3
    :type        :minion
    :set         :classic
    :rarity      :rare
    :description "Battlecry: Deal 4 damage to HIMSELF."
    :battlecry   (fn [state & {}]
                   (let [minion-id (:id (get-latest-minion state))]
                     (-> (update-minion state minion-id :damage-taken 4)
                         (update-minion minion-id :health 3))))}

   ;; Implemented
   "Nightblade"
   {:name        "Nightblade"
    :attack      4
    :health      4
    :mana-cost   5
    :type        :minion
    :set         :basic
    :description "Battlecry: Deal 3 damage to the enemy hero."
    :battlecry   (fn [state & {player-id :player-id}]
                   (let [player-change-fn {"p1" "p2"
                                           "p2" "p1"}]
                     (let [opponent-id (player-change-fn player-id)]
                       (update-in state [:players opponent-id :hero :damage-taken]
                                  (constantly (+ (get-in state [:players opponent-id :hero :damage-taken])
                                                 3))))))}

   ;; Implemented
   "Silver Hand Recruit"
   {:name      "Silver Hand Recruit"
    :attack    1
    :health    1
    :mana-cost 1
    :class     :paladin
    :type      :minion
    :set       :basic}

   ;; Implemented
   "Baine Bloodhoof"
   {:name      "Baine Bloodhoof"
    :attack    4
    :health    5
    :mana-cost 4
    :type      :minion
    :set       :classic
    :rarity    :legendary}

   ;; Implemented
   "Cairne Bloodhoof"
   {:name        "Cairne Bloodhoof"
    :attack      4
    :health      5
    :mana-cost   6
    :type        :minion
    :set         :classic
    :rarity      :legendary
    :description "Deathrattle: Summon a 4/5 Baine Bloodhoof."
    ;; Check with view
    :deathrattle (fn [state & {player-id :player-id}]
                   (add-minion-to-board state player-id (create-card "Baine Bloodhoof") 7))}

   ;; Implemented
   "Elven Minstrel"
   {:name        "Elven Minstrel"
    :attack      3
    :health      2
    :mana-cost   4
    :type        :minion
    :class       :rogue
    :rarity      :rare
    :set         :kobolds-and-catacombs
    :description "Combo: Draw 2 minions from your deck."
    :combo       (fn [state & {player-id :player-id}]
                   (-> (draw-first-minion state player-id)
                       (draw-first-minion player-id)))}

   ;; Implemented
   "Shado-Pan Rider"
   {:name        "Shado-Pan Rider"
    :attack      3
    :health      7
    :mana-cost   5
    :type        :minion
    :class       :rogue
    :set         :the-grand-tournament
    :rarity      :common
    :description "Combo: Gain +3 Attack."
    :combo       (fn [state & {}]
                   (update-minion state (:id (get-latest-minion state)) :attack (+ 3 (get-attack state (:id (get-latest-minion state))))))}

   ;; Implemented
   "Alexstrasza"
   {:name         "Alexstrasza"
    :attack       8
    :health       8
    :mana-cost    9
    :type         :minion
    :rarity       :legendary
    :set          :classic
    :race         :dragon
    :description  "Battlecry: Set a hero's remaining Health to 15."
    ; Fix this with the view
    :battlecry    (fn [state & {player-id :player-id target-id :target-id}]
                    (-> (update-in state [:players target-id :hero :damage-taken] (constantly 0))
                        (update-in [:players target-id :hero :health] (constantly 15))))}

   ;; Implemented
   "Faceless Manipulator"
   {:name         "Faceless Manipulator"
    :attack       3
    :health       3
    :mana-cost    5
    :set          :classic
    :rarity       :epic
    :type         :minion
    :description  "Battlecry: Choose a minion and become a copy of it."
    ; Fix this with the view
    :battlecry    (fn [state & {player-id :player-id target-id :target-id}]
                    (let [target-name (:name (get-minion state target-id))
                          minion-id (:id (get-latest-minion state))]
                      (reduce (fn [st kv] (update-minion st minion-id (first kv) (second kv))) state (select-keys (get-minion state target-id) [:name :damage-taken :health :attack :owner-id]))))}

   ;; Implemented
   "Barnes"
   {:name        "Barnes"
    :attack      3
    :health      4
    :mana-cost   4
    :type        :minion
    :set         :one-night-in-karazhan
    :rarity      :legendary
    :description "Battlecry: Summon a 1/1 copy of a random minion in your deck."
    :battlecry   (fn [state & {player-id :player-id}]
                   (if (< 7 (count (get-in state [:player player-id :minions])))
                     state
                     (let [seed (or (get state :seed) 1234)
                           minions (filter (fn [x] (= (:type x) :minion)) (get-deck state player-id))
                           result (random-nth seed minions)
                           new-seed (first result)
                           minion (:name (second result))]
                       (-> (add-minion-to-board state player-id (create-minion minion :attack 1 :health 1 :original-health 1 :max-health 1 :original-attack 1) (+ 1 (reduce (fn [max curr] (if (< max (:position curr)) (:position curr) max)) 0 (get-minions state (get state :player-id-in-turn)))))
                           (update :seed (constantly new-seed))))))}

   ;; Implemented
   "Astral Tiger"
   {:name        "Astral Tiger"
    :attack      3
    :health      5
    :mana-cost   4
    :type        :minion
    :class       :druid
    :rarity      :epic
    :set         :kobolds-and-catacombs
    :description "Deathrattle: Shuffle a copy of this minion into your deck."
    ; Check with view
    :deathrattle (fn [state & {player-id :player-id}]
                   (if (zero? (count (get-deck state player-id)))
                     (add-card-to-deck state player-id (create-card "Astral Tiger"))
                     (let [[seed place] (get-random-int (or (get state :seed) 1234) (count (get-deck state player-id)))]
                       (as-> (add-card-to-deck state player-id (create-card "Astral Tiger")) $
                         (let [st $
                               [seed2 shuffled-deck] (shuffle-with-seed seed (get-deck st player-id))]
                           (-> (update-in st [:players player-id :deck] (constantly shuffled-deck))
                               (update :seed (constantly seed2))))))))}

   ;; Implemented
   "Loot Hoarder"
   {:name        "Loot Hoarder"
    :attack      2
    :health      1
    :mana-cost   2
    :type        :minion
    :set         :classic
    :rarity      :common
    :description "Deathrattle: Draw a card."
    ; Check with view
    :deathrattle (fn [state & {player-id :player-id}]
                   (if (take-fatigue? state player-id)
                     (get-fatigue state player-id)
                     (draw-card state player-id)))}

   ;; Implemented
   "Battle Rage"
   {:name         "Battle Rage"
    :mana-cost    2
    :type         :spell
    :class        :warrior
    :set          :classic
    :rarity       :common
    :description  "Draw a card for each damaged friendly character."}

   ;; Implemented
   "Young Priestess"
   {:name        "Young Priestess"
    :attack      2
    :health      1
    :mana-cost   1
    :type        :minion
    :set         :classic
    :rarity      :rare
    :end-effect  :true
    ; Needs fix with view
    :description "At the end of your turn give another random friendly minion +1 Health."}

   ;; Implemented
   "Moroes"
   {:name        "Moroes"
    :attack      1
    :health      1
    :mana-cost   3
    :type        :minion
    :set         :one-night-in-karazhan
    :rarity      :legendary
    :stealth     true
    :end-effect  true
    :description "Stealth. At the end of your turn, summon a 1/1 Steward."}
   
   ;; Implemented
   "Steward"
   {:name          "Steward"
    :attack        1
    :health        1
    :mana-cost     1
    :type          :minion
    :set           :one-night-in-karazhan}

   ;; Implemented
   "Unlicensed Apothecary"
   {:name               "Unlicensed Apothecary"
    :attack             5
    :health             5
    :mana-cost          3
    :race               :demon
    :type               :minion
    :rarity             :epic
    :class              :warlock
    :set                :mean-streets-of-gadgetzan
    :reaction-effect    :true
    ; Fix for minions that spawn more minions
    :description        "Whenever you summon a minion deal 5 damage to your Hero."}

   ;; Implemented
   "Devour Mind"
   {:name         "Devour Mind"
    :mana-cost    5
    :type         :spell
    :class        :priest
    :set          :knights-of-the-frozen-throne
    :rarity       :rare
    ; Fix with view
    :description  "Copy 3 cards in your opponent's deck and add them to your hand."}


   ;; TODO
   "Dire Wolf Alpha"
   {:name        "Dire Wolf Alpha"
    :attack      2
    :health      2
    :mana-cost   2
    :type        :minion
    :rarity      :common
    :set         :classic
    :description "Adjacent minions have +1 Attack."}

   ;; TODO
   "Edwin VanCleef"
   {:name        "Edwin VanCleef"
    :attack      2
    :health      2
    :mana-cost   3
    :type        :minion
    :class       :rogue
    :set         :classic
    :rarity      :legendary
    :description "Combo: Gain +2/+2 for each card played earlier this turn."}

   ;; TODO
   "Elixir of Hope"
   {:name        "Elixir of Hope"
    :mana-cost   3
    :type        :spell
    :class       :priest
    :rarity      :common
    :set         :kobolds-and-catacombs
    :description "Give a minion +2/+2 and \"Deathrattle: Return this minion to your hand.\""}

   ;; TODO
   "Frothing Berserker"
   {:name        "Frothing Berserker"
    :attack      2
    :health      4
    :mana-cost   3
    :type        :minion
    :class       :warrior
    :set         :classic
    :rarity      :rare
    :description "Whenever a minion takes damage, gain +1 Attack."}

   ;; TODO
   "Malorne"
   {:name        "Malorne"
    :attack      9
    :health      7
    :mana-cost   7
    :type        :minion
    :class       :druid
    :set         :goblins-vs-gnomes
    :rarity      :legendary
    :description "Deathrattle: Shuffle this minion into your deck."}

   ;; TODO
   "Sneaky Devil"
   {:name        "Sneaky Devil"
    :attack      2
    :health      2
    :mana-cost   4
    :type        :minion
    :rarity      :common
    :set         :kobolds-and-catacombs
    :description "Stealth. Your other minions have +1 Attack."}

   ;; TODO
   "Spellbreaker"
   {:name        "Spellbreaker"
    :attack      4
    :health      3
    :mana-cost   4
    :rarity      :common
    :set         :classic
    :type        :minion
    :description "Battlecry: Silence a minion."}

   ;; TODO
   "Unearthed Raptor"
   {:name        "Unearthed Raptor"
    :attack      3
    :health      4
    :mana-cost   3
    :type        :minion
    :class       :rogue
    :set         :the-league-of-explorers
    :rarity      :rare
    :description "Battlecry: Choose a friendly minion. Gain a copy of its Deathrattle effect."}

   })

(add-definitions! card-definitions)

