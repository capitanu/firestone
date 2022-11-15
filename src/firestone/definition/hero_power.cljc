(ns firestone.definition.hero-power
  (:require [firestone.definitions :refer [add-definitions!]]))

(def hero-power-definitions
  {

   "Ballista Shot"
   {:name        "Ballista Shot"
    :mana-cost   2
    :type        :hero-power
    :description "Deal 2 damage to the enemy hero."}

   "Fireblast"
   {:name         "Fireblast"
    :mana-cost    2
    :type         :hero-power
    :sub-type     :basic
    :class        :mage
    :description  "Deal 1 damage."}

   })

(add-definitions! hero-power-definitions)
