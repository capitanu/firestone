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
                                         card->minion
                                         create-card
                                         create-game
                                         create-hero
                                         create-minion
                                         count-damaged-minions
                                         get-card
                                         get-card-cost
                                         get-deck
                                         get-fatigue
                                         get-hand
                                         get-hero-by-player-id
                                         get-hero-power-cost
                                         get-hero-power
                                         get-heroes
                                         get-mana
                                         get-minion
                                         get-minions
                                         get-player
                                         get-player-id-in-turn
                                         get-random-minion-excluding-caller
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

(defn deathrattle-astral-tiger
  "Peforms the deathrattle of Astral Tiger"
  {:test (fn []
           (is= (-> (create-game)
                    (deathrattle-astral-tiger "p1")
                    (get-deck "p1")
                    (count))
                1)
           (is= (-> (create-game [{:deck [(create-card "Alexstrasza")]}])
                    (deathrattle-astral-tiger "p1")
                    (get-deck "p1")
                    (count))
                2))}
  [state player-id]
  (if (zero? (count (get-deck state player-id)))
    (add-card-to-deck state player-id (create-card "Astral Tiger"))
    (let [[seed place] (get-random-int 1234 (count (get-deck state player-id)))]
      (as-> (add-card-to-deck state player-id (create-card "Astral Tiger")) $
        (let [st $
              [seed2 shuffled-deck] (shuffle-with-seed 1234 (get-deck st player-id))]
          (update-in st [:players player-id :deck] (constantly shuffled-deck)))))))




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
           (is= (let [card (create-card "Moroes" :id "m")]
             (as-> (create-game [{:hand [card] :minions [(create-minion "Boulderfist Ogre") (create-minion "Boulderfist Ogre")]}]) $
                   (place-card-board $ "p1" card 0)
                   (get-in $ [:players "p1" :end-effect-minions])))
                ["m5"]))}
  [state player-id card position]
  {:pre [(map? state) (string? player-id) (map? card) (int? position)]}
  (let [minion (card->minion card)]
    (as-> (add-minion-to-board state player-id minion position) $
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
                    (if (:end-effect minion)
                      (update-in st [:players player-id :end-effect-minions] (constantly (conj (get st [:players player-id :end-effect-minions]) minion-id)))
                      st)))))))))


(defn get-latest-minion
  "Gets the last minion played on the board"
  {:test (fn []
           (is= (-> (create-game)
                    (add-minion-to-board "p1" (create-minion "Boulderfist Ogre") 0)
                    (add-minion-to-board "p1" (create-minion "Injured Blademaster") 0)
                    (get-latest-minion)
                    (:name))
                "Injured Blademaster"))}
  [state]
  {:pre [(map? state)]}
  (let [all-minions (concat
                     (get-in state [:players "p1" :minions])
                     (get-in state [:players "p2" :minions]))]
    (reduce (fn [x y]
              (if (< (:added-to-board-time-id x)
                     (:added-to-board-time-id y))
                y
                x))
            (first all-minions)
            all-minions)))

(defn battlecry-antique-healbot
  "Performs the battlecry for Antique Healbot"
  {:test (fn []
           (is= (-> (create-game [{:hero (create-hero "Jaina Proudmoore" :damage-taken 10)}])
                    (battlecry-antique-healbot "p1")
                    (get-in [:players "p1" :hero :damage-taken]))
                2))}
  [state player-id]
  {:pre [(map? state) (string? player-id)]}
  (update-in state [:players player-id :hero :damage-taken]
             (constantly (max 0
                              (- (get-in state [:players player-id :hero :damage-taken]) 8)))))


(defn battlecry-injured-blademaster
  "Performs the battlecry for Injured Blademaster"
  {:test (fn []
           (is= (-> (create-game [{:minions [(create-minion "Injured Blademaster" :id "ib")]}])
                    (battlecry-injured-blademaster)
                    (get-health "ib"))
                3))}
  [state]
  {:pre [(map? state)]}
  (let [minion-id (:id (get-latest-minion state))]
    (update-minion state minion-id :damage-taken 4)))


(defn battlecry-nightblade
  "Performs the battlecry for nightblade"
  {:test (fn []
           (is= (-> (create-game [{:hero "Jaina Proudmoore"} {:hero (create-hero "Jaina Proudmoore" :id "jp")}])
                    (battlecry-nightblade "p1")
                    (get-health  "jp"))
                27))}
  [state player-id]
  {:pre [(map? state) (string? player-id)]}
  (let [player-change-fn {"p1" "p2"
                          "p2" "p1"}]
    (let [opponent-id (player-change-fn player-id)]
      (update-in state [:players opponent-id :hero :damage-taken]
                 (constantly (+ (get-in state [:players opponent-id :hero :damage-taken])
                                3))))))

(defn battlecry-alexstrasza
  "Performs the battlecry for alexstrasza"
  {:test (fn []
           (is= (-> (create-game [{:hero "Jaina Proudmoore"} {:hero (create-hero "Jaina Proudmoore" :id "jp")}])
                    (battlecry-alexstrasza "p1" "p2")
                    (get-health  "jp"))
                15))}
  [state player-id target-id]
  {:pre [(map? state) (string? player-id)]}
  (-> (update-in state [:players target-id :hero :damage-taken] (constantly 0))
      (update-in [:players target-id :hero :health] (constantly 15))))

(defn battlecry-barnes
  "Performs the battlecry for barnes"
  {:test (fn []
           (is= (-> (create-game [{:deck [(create-card "Alexstrasza")] :minions [(create-minion "Barnes" :id "b")]}])
                    (battlecry-barnes "p1")
                    (get-minions "p1")
                    (count))
                2))}
  [state player-id]
  {:pre [(map? state) (string? player-id)]}
  (if (< 7 (count (get-in state [:player player-id :minions])))
    state
    (as-> (filter (fn [x] (= (:type x) :minion)) (get-deck state player-id)) $
      (random-nth 1234 $)
      (second $)
      (:name $)
      (add-minion-to-board state player-id (create-minion $ :attack 1 :health 1) 7))))

(defn battlecry-faceless-manipulator
  "Performs the battlecry for Faceless Manipulator"
  {:test (fn []
           (is= (as-> (create-game [{:hero "Jaina Proudmoore" :minions [(create-minion "Boulderfist Ogre" :id "bo")]} {:hero (create-hero "Jaina Proudmoore" :id "jp")}]) $
                    (place-card-board $ "p1" (create-card "Faceless Manipulator") 0)
                    (battlecry-faceless-manipulator $ "p1" "bo")
                    (get-minions $ "p1")
                    (map :name $))
                ["Boulderfist Ogre", "Boulderfist Ogre"]))}
  [state player-id target-id]
  {:pre [(map? state) (string? player-id)]}
  (let [target-name (:name (get-minion state target-id)) minion-id (:id (get-latest-minion state))]
    (update-minion state minion-id :name target-name)))

(defn battlecry
  "Makes the battle-cry happen"
  {:test (fn []
           (is= (-> (create-game [{:hero (create-hero "Jaina Proudmoore" :damage-taken 10)}])
                    (battlecry "p1" (create-card "Antique Healbot"))
                    (get-in [:players "p1" :hero :damage-taken]))
                2))}
  [state player-id card & {target-id :target-id}]
  {:pre [(map? state) (string? player-id) (map? card)]}
  (cond (= (:name card)
           "Antique Healbot")
        (battlecry-antique-healbot state player-id)

        (= (:name card)
           "Nightblade")
        (battlecry-nightblade state player-id)

        (= (:name card)
           "Alexstrasza")
        (battlecry-alexstrasza state player-id target-id)

        (= (:name card)
           "Faceless Manipulator")
        (battlecry-faceless-manipulator state player-id target-id)

        (= (:name card)
           "Barnes")
        (battlecry-barnes state player-id)

        (= (:name card)
           "Injured Blademaster")
        (battlecry-injured-blademaster state)

        :else
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


(defn deathrattle-cairne-bloodhoof
  "Peforms the deathrattle of Cairne Bloodhoof"
  {:test (fn []
           (is= (-> (create-game)
                    (deathrattle-cairne-bloodhoof "p1")
                    (get-minions "p1")
                    (first)
                    (:name))
                "Baine Bloodhoof"))}
  [state player-id]
  (place-card-board state player-id (create-card "Baine Bloodhoof") 7))

(defn deathrattle-loot-hoarder
  "Peforms the deathrattle of Loot Hoarder"
  {:test (fn []
           (is= (-> (create-game)
                    (deathrattle-loot-hoarder "p1")
                    (get-in [:players "p1" :hero :damage-taken]))
                1)
           (is= (-> (create-game [{:deck [(create-card "Alexstrasza")]}])
                    (deathrattle-loot-hoarder "p1")
                    (get-deck "p1")
                    (count))
                0))}
  [state player-id]
  (if (take-fatigue? state player-id)
    (get-fatigue state player-id)
    (draw-card state player-id)))

(defn deathrattle
  "Performs the deathrattle of the given minion."
  {:test (fn []
           (is= (-> true)
                true)
           )}
  [state player-id minion-id]
  (let [minion (get-minion state minion-id)]
    (cond (= (:name minion)
             "Cairne Bloodhoof")
          (deathrattle-cairne-bloodhoof state player-id)

          (= (:name minion)
             "Astral Tiger")
          (deathrattle-astral-tiger state player-id)

          (= (:name minion)
             "Loot Hoarder")
          (deathrattle-loot-hoarder state player-id)

          :else
          state)))

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
        minion (get-random-minion-excluding-caller state minion-id)]
    (if (and minion
             (not= (:id minion)
                   "yp"))
      (increase-health state player-id (:id minion) 1)
      state)))

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
                      (:owner-id))]
    (as-> (into [] (filter (fn [x] (not= (:id x) minion-id)) (get-in state [:players player-id :minions]))) $
      (update-in state [:players player-id :minions] (constantly $))
      (let [st $]
        (-> (update-in st [:players player-id :end-effect-minions] (constantly (filter (fn [x] (not= minion-id x)) (get-in st [:players player-id :end-effect-minions]))))
            (deathrattle player-id minion-id))))))


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
                                "p2" "p1"}]
          (->> (get-in state [:players (player-change-fn player-id) :deck])
               (take-n-random 1234 3)
               (second)
               (map :name)
               (add-cards-to-hand state player-id)))

        (= (:name card) "Battle Rage")
        (let [damaged (count-damaged-minions state player-id)]
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