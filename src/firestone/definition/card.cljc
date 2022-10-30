(ns firestone.definition.card
  (:require [firestone.definitions :refer [add-definitions!]]
            [firestone.core :refer []]))

(def card-definitions
  {

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

   "Boulderfist Ogre"
   {:name      "Boulderfist Ogre"
    :attack    6
    :health    7
    :mana-cost 6
    :type      :minion
    :set       :basic}

   "Injured Blademaster"
   {:name        "Injured Blademaster"
    :attack      4
    :health      7
    :mana-cost   3
    :type        :minion
    :set         :classic
    :rarity      :rare
    :description "Battlecry: Deal 4 damage to HIMSELF."}

   "Nightblade"
   {:name        "Nightblade"
    :attack      4
    :health      4
    :mana-cost   5
    :type        :minion
    :set         :basic
    :description "Battlecry: Deal 3 damage to the enemy hero."}

   "Silver Hand Recruit"
   {:name      "Silver Hand Recruit"
    :attack    1
    :health    1
    :mana-cost 1
    :class     :paladin
    :type      :minion
    :set       :basic}


   })

(add-definitions! card-definitions)