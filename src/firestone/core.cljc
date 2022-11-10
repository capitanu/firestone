(ns firestone.core
  (:require [ysera.test :refer [is is-not is= error?]]
            [ysera.error :refer [error]]
            [ysera.collections :refer [seq-contains?]]
            [firestone.definitions :refer [get-definition]]
            [firestone.construct :refer [add-cards-to-deck
                                         create-card
                                         create-game
                                         create-hero
                                         create-minion
                                         get-hand
                                         get-heroes
                                         get-minion
                                         get-minions
                                         get-player
                                         update-minion]]))


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
         (< (:attacks-performed-this-turn attacker) 1)
         (not (sleepy? state attacker-id))
         (not= (:owner-id target) (:player-id-in-turn state))
         (not= (:owner-id attacker) (:owner-id target)))))

(defn take-fatigue?
  "Returns truethy or falsey, depending on whether or not the player should get fatigue"
  {:test (fn []
           (is= (-> (create-game)
                    (take-fatigue? "p1"))
                true)
           (is= (-> (create-game)
                    (add-cards-to-deck "p1" ["Nightblade"])
                    (take-fatigue? "p1"))
                nil))}
  [state player-id]
  (if (= (get-in state [:players player-id :deck]) [])
    true
    nil))

(defn draw-card
  ;; TODO: Check if hand is full and destroy card if so
  "Picks up the first card in the deck and puts it in the hand"
  {:test (fn []
           (is= (as-> (create-game [{:deck ["Boulderfist Ogre"]}]) $
                    (draw-card $ "p1")
                    (get-hand $ "p1")
                    (map :name $))
                ["Boulderfist Ogre"])
           (is= (as-> (create-game [{:deck ["Silver Hand Recruit"] :hand ["Boulderfist Ogre", "Boulderfist Ogre", "Boulderfist Ogre","Boulderfist Ogre","Boulderfist Ogre","Boulderfist Ogre","Boulderfist Ogre","Boulderfist Ogre","Boulderfist Ogre","Boulderfist Ogre"]}]) $
                    (draw-card $ "p1")
                    (get-hand $ "p1")
                    (count $))
                10)
           (is= (as-> (create-game [{:deck ["Boulderfist Ogre", "Silver Hand Recruit"]}]) $
                  (draw-card $ "p1")
                  (draw-card $ "p1")
                  (get-hand $ "p1")
                  (map :name $))
                ["Silver Hand Recruit", "Boulderfist Ogre"])
           (error? (-> (create-game)
                    (draw-card "p1"))))}
  
  [state player-id]
  (if (= (get-in state [:players player-id :deck])
         [])
    (error "Player deck is empty"))
  (if (< (-> (count (get-in state [:players player-id :hand])))
           10)
    (let [card (peek (get-in state [:players player-id :deck]))]
      (-> (update-in state [:players player-id :deck] pop)
          (update-in [:players player-id :hand] conj card)))
    (let [card (peek (get-in state [:players player-id :deck]))]
      (update-in state [:players player-id :deck] pop))))

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
  (< (get-health state minion-id)
     0))

(defn remove-minion
  "Removes a minion from the table IF it has negative health"
  {:test (fn []
           (is= (-> (create-game [{:minions [(create-minion "Boulderfist Ogre" :id "bo")]}])
                    (remove-minion "bo")
                    (get-in [:players "p1" :minions])
                    (count))
                0))}
  [state minion-id]
  (let [player-id (-> state
                      (get-character minion-id)
                      (:owner-id))]
    (as-> (filter (fn [x] (not= (:id x) minion-id)) (get-in state [:players player-id :minions])) $
         (update-in state [:players player-id :minions] (constantly $)))))


(defn remove-sleeping-minions
  {:test (fn [] 
           (is= (as-> (create-game) $
                  (update $ :minion-ids-summoned-this-turn (constantly ["bo"]))
                  (remove-sleeping-minions $)
                  (get $ :minion-ids-summoned-this-turn))
                []))}
  [state]
  (update state :minion-ids-summoned-this-turn (constantly [])))


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
  (update-minion state minion-id :attacks-performed-this-turn 1))
