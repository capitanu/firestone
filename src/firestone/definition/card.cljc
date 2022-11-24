(ns firestone.definition.card
  (:require [firestone.definitions :refer [add-definitions!]]
            [firestone.core :refer []]))

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
    :description "Battlecry: Restore 8 Health to your hero."}

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
    :description "Battlecry: Deal 4 damage to HIMSELF."}

   ;; Implemented
   "Nightblade"
   {:name        "Nightblade"
    :attack      4
    :health      4
    :mana-cost   5
    :type        :minion
    :set         :basic
    :description "Battlecry: Deal 3 damage to the enemy hero."}

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
    :description "Deathrattle: Summon a 4/5 Baine Bloodhoof."}

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
    :description "Combo: Draw 2 minions from your deck."}

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
    :description "Combo: Gain +3 Attack."}

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
    :description  "Battlecry: Set a hero's remaining Health to 15."}

   ;; Implemented
   "Faceless Manipulator"
   {:name         "Faceless Manipulator"
    :attack       3
    :health       3
    :mana-cost    5
    :set          :classic
    :rarity       :epic
    :type         :minion
    :description  "Battlecry: Choose a minion and become a copy of it."}

   ;; Implemented
   "Barnes"
   {:name        "Barnes"
    :attack      3
    :health      4
    :mana-cost   4
    :type        :minion
    :set         :one-night-in-karazhan
    :rarity      :legendary
    :description "Battlecry: Summon a 1/1 copy of a random minion in your deck."}

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
    :description "Deathrattle: Shuffle a copy of this minion into your deck."}

   ;; Implemented
   "Loot Hoarder"
   {:name        "Loot Hoarder"
    :attack      2
    :health      1
    :mana-cost   2
    :type        :minion
    :set         :classic
    :rarity      :common
    :description "Deathrattle: Draw a card."}

   ;; Implemented
   "Battle Rage"
   {:name         "Battle Rage"
    :mana-cost    2
    :type         :spell
    :class        :warrior
    :set          :classic
    :rarity       :common
    :description  "Draw a card for each damaged friendly character."}

   "Young Priestess"
   {:name        "Young Priestess"
    :attack      2
    :health      1
    :mana-cost   1
    :type        :minion
    :set         :classic
    :rarity      :rare
    :end-effect  :true
    :description "At the end of your turn give another random friendly minion +1 Health."}

   "Moroes"
   {:name        "Moroes"
    :attack      1
    :health      1
    :mana-cost   3
    :type        :minion
    :set         :one-night-in-karazhan
    :rarity      :legendary
    :end-effect  :true
    :stealth     true
    :description "Stealth. At the end of your turn, summon a 1/1 Steward."}

   "Steward"
   {:name          "Steward"
    :attack        1
    :health        1
    :mana-cost     1
    :type          :minion
    :set           :one-night-in-karazhan}

   ;; TODO
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
    :description        "Whenever you summon a minion deal 5 damage to your Hero."}

   ;; Implemented
   "Devour Mind"
   {:name         "Devour Mind"
    :mana-cost    5
    :type         :spell
    :class        :priest
    :set          :knights-of-the-frozen-throne
    :rarity       :rare
    :description  "Copy 3 cards in your opponent's deck and add them to your hand."}

   })

(add-definitions! card-definitions)
