(ns firestone.definition.hero
  (:require [firestone.definitions :refer [add-definitions!]]))

(def hero-definitions
  {

   "Jaina Proudmoore"
   {:name       "Jaina Proudmoore"
    :health     30
    :class      :mage
    :type       :hero
    :hero-power "Fireblast"}

   "Rexxar"
   {:name       "Rexxar"
    :health     30
    :class      :hunter
    :type       :hero
    :hero-power "Ballista Shot"}

   })

(add-definitions! hero-definitions)