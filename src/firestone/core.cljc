(ns firestone.core
  (:require [ysera.test :refer [is is-not is= error?]]
            [ysera.error :refer [error]]
            [ysera.collections :refer [seq-contains?]]
            [firestone.definitions :refer [get-definition]]
            [firestone.construct :refer [create-card
                                         create-game
                                         create-hero
                                         create-minion
                                         get-heroes
                                         get-minion
                                         get-minions]]))


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


(defn get-health
  "Returns the health of the character."
  {:test (fn []
           ; Uninjured minion
           (is= (-> (create-minion "Nightblade")
                    (get-health))
                4)
           ; Injured minion
           (is= (-> (create-minion "Nightblade" :damage-taken 1)
                    (get-health))
                3)
           ; Minion in a state
           (is= (-> (create-game [{:minions [(create-minion "Nightblade" :id "n")]}])
                    (get-health "n"))
                4)
           ; Uninjured hero
           (is= (-> (create-hero "Jaina Proudmoore")
                    (get-health))
                30)
           ; Injured hero
           (is= (-> (create-hero "Jaina Proudmoore" :damage-taken 2)
                    (get-health))
                28)
           ; Hero in a state
           (is= (-> (create-game [{:hero (create-hero "Jaina Proudmoore" :id "h1")}])
                    (get-health "h1"))
                30))}
  ([character]
   {:pre [(map? character) (contains? character :damage-taken)]}
   (let [definition (get-definition character)]
     (- (:health definition) (:damage-taken character))))
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


(defn valid-attack?
  "Checks if the attack is valid"
  {:test (fn []
           ; Should be able to attack an enemy minion
           (is (-> (create-game [{:minions [(create-minion "Nightblade" :id "n")]}
                                 {:minions [(create-minion "Boulderfist Ogre" :id "bo")]}])
                   (valid-attack? "p1" "n" "bo")))
           ; Should be able to attack an enemy hero
           (is (-> (create-game [{:minions [(create-minion "Nightblade" :id "n")]}])
                   (valid-attack? "p1" "n" "h2")))
           ; Should not be able to attack your own minions
           (is-not (-> (create-game [{:minions [(create-minion "Nightblade" :id "n")
                                                (create-minion "Boulderfist Ogre" :id "bo")]}])
                       (valid-attack? "p1" "n" "bo")))
           ; Should not be able to attack if it is not your turn
           (is-not (-> (create-game [{:minions [(create-minion "Nightblade" :id "n")]}
                                     {:minions [(create-minion "Boulderfist Ogre" :id "bo")]}]
                                    :player-id-in-turn "p2")
                       (valid-attack? "p1" "n" "bo")))
           ; Should not be able to attack if you are sleepy
           (is-not (-> (create-game [{:minions [(create-minion "Nightblade" :id "n")]}
                                     {:minions [(create-minion "Boulderfist Ogre" :id "bo")]}]
                                    :minion-ids-summoned-this-turn ["n"])
                       (valid-attack? "p1" "n" "bo")))
           ; Should not be able to attack if you already attacked this turn
           (is-not (-> (create-game [{:minions [(create-minion "Nightblade" :id "n" :attacks-performed-this-turn 1)]}
                                     {:minions [(create-minion "Boulderfist Ogre" :id "bo")]}])
                       (valid-attack? "p1" "n" "bo"))))}
  [state player-id attacker-id target-id]
  (let [attacker (get-minion state attacker-id)
        target (get-character state target-id)]
    (and attacker
         target
         (= (:player-id-in-turn state) player-id)
         (< (:attacks-performed-this-turn attacker) 1)
         (not (sleepy? state attacker-id))
         (not= (:owner-id attacker) (:owner-id target)))))
