(ns firestone.construct
  (:require [ysera.test :refer [is is-not is= error?]]
            [ysera.random :refer [random-nth]]
            [ysera.error :refer [error]]
            [firestone.definitions :refer [get-definition]]))

(defn create-hero
  "Creates a hero from its definition by the given hero name. The additional key-values will override the default values."
  {:test (fn []
           (is= (create-hero "Jaina Proudmoore")
                {:name         "Jaina Proudmoore"
                 :entity-type  :hero
                 :hero-power-used false
                 :health       30
                 :damage-taken 0})
           (is= (create-hero "Jaina Proudmoore" :damage-taken 10)
                {:name         "Jaina Proudmoore"
                 :hero-power-used false
                 :entity-type  :hero
                 :health       30
                 :damage-taken 10}))}
  ;; Variadic functions [https://clojure.org/guides/learn/functions#_variadic_functions]
  [name & kvs]
  (let [definition (get-definition name)
        hero {:name         name
              :entity-type  :hero
              :health       (:health definition)
              :hero-power-used false
              :damage-taken 0}]
    (if (empty? kvs)
      hero
      ;; use apply when the arguments are in a sequence
      (apply assoc hero kvs))))


(defn create-card
  "Creates a card from its definition by the given card name. The additional key-values will override the default values."
  {:test (fn []
           (is= (-> (create-card "Boulderfist Ogre" :id "bo")
                    (:id))
                "bo"))}
  [name & kvs]
  (let [definition (get-definition name)
        card {:name        name
              :type        (:type definition)
              :mana-cost   (:mana-cost definition)
              :original-mana-cost   (:mana-cost definition)
              :playable    false
              :valid-target-ids []
              :attack      (:attack definition)
              :original-attack      (:attack definition)
              :health      (:health definition)
              :original-health      (:health definition)
              :description (or (:description definition) "")
              :entity-type :card}]
    (as-> (if (:stealth definition)
            (apply assoc card [:stealth (:stealth definition)])
            card) $
      (if (:end-effect definition)
        (apply assoc $ [:end-effect (:end-effect definition)])
        $)
      (if (:reaction-effect definition)
        (apply assoc $ [:reaction-effect (:reaction-effect definition)])
        $)
      (let [c $]
        (if (empty? kvs)
          c
          (apply assoc c kvs))))))

(defn create-minion
  "Creates a minion from its definition by the given minion name. The additional key-values will override the default values."
  {:test (fn []
           (is= (create-minion "Nightblade"
                               :id "n"
                               :attacks-performed-this-turn 1)
                {:description "Battlecry: Deal 3 damage to the enemy hero.",
		         :can-attack false,
		         :entity-type :minion,
		         :states (),
		         :name "Nightblade",
		         :max-health 4,
		         :attacks-performed-this-turn 1,
		         :damage-taken 0,
		         :mana-cost 5,
		         :id "n",
		         :original-attack 4,
		         :sleepy true,
		         :health 4,
		         :original-health 4,
		         :set :basic,
		         :attack 4,
		         :valid-attack-ids {}}))}
  [name & kvs]
  (let [definition (get-definition name)                    ;; Will be used later
        minion {:damage-taken                0
                :entity-type                 :minion
                :attack                      (:attack definition)
                :health                      (:health definition)
                :name                        name
                :attacks-performed-this-turn 0
                :description (or (:description definition) "")
                :can-attack false
                :mana-cost (:mana-cost definition)
                :max-health (:health definition)
                :original-attack (:attack definition)
                :original-health (:health definition)
                :set (:set definition)
                :sleepy true
                :states (filter (fn [el] (not= el nil)) [(when (:stealth definition) :stealth) (when (:deathrattle definition) :deathrattle)])
                :valid-attack-ids {}}]
    (as-> (if (:stealth definition)
            (apply assoc minion [:stealth (:stealth definition)])
            minion) $
      (if (:end-effect definition)
        (apply assoc $ [:end-effect (:end-effect definition)])
        $)
      (if (:reaction-effect definition)
        (apply assoc $ [:reaction-effect (:reaction-effect definition)])
        $)
      (let [m $]
        (if (empty? kvs)
          m
          (apply assoc m kvs))))))

(defn create-empty-state
  "Creates an empty state with the given heroes."
  {:test (fn []
           ;; Jaina Proudmoore will be the default hero
           (is= (create-empty-state [(create-hero "Jaina Proudmoore")
                                     (create-hero "Jaina Proudmoore")])
                (create-empty-state))

           (is= (create-empty-state [(create-hero "Jaina Proudmoore" :id "r")
                                     (create-hero "Rexxar")])
                {:player-id-in-turn             "p1"
                 :players                       {"p1" {:id       "p1"
                                                       :deck     []
                                                       :end-effect-minions []
                                                       :reaction-effect-minions []
                                                       :hand     []
                                                       :minions  []
                                                       :hero     {:name         "Jaina Proudmoore"
                                                                  :id           "r"
                                                                  :health       30
                                                                  :hero-power-used false
                                                                  :damage-taken 0
                                                                  :entity-type  :hero}
                                                       :fatigue  1
                                                       :mana     10
                                                       :max-mana 10}
                                                 "p2" {:id       "p2"
                                                       :deck     []
                                                       :end-effect-minions []
                                                       :reaction-effect-minions []
                                                       :hand     []
                                                       :minions  []
                                                       :hero     {:name         "Rexxar"
                                                                  :id           "h2"
                                                                  :health       30
                                                                  :hero-power-used false
                                                                  :damage-taken 0
                                                                  :entity-type  :hero}
                                                       :fatigue  1
                                                       :mana     10
                                                       :max-mana 10}}
                 :counter                       1
                 :minion-ids-summoned-this-turn []}))}
  ;; Multiple arity of a function [https://clojure.org/guides/learn/functions#_multi_arity_functions]
  ([]
   (create-empty-state []))
  ([heroes]
   ;; Creates Jaina Proudmoore heroes if heroes are missing.
   (let [heroes (->> (concat heroes [(create-hero "Jaina Proudmoore")
                                     (create-hero "Jaina Proudmoore")])
                     (take 2))]
     {:player-id-in-turn             "p1"
      :players                       (->> heroes
                                          (map-indexed (fn [index hero]
                                                         {:id      (str "p" (inc index))
                                                          :deck    []
                                                          :hand    []
                                                          :end-effect-minions []
                                                          :reaction-effect-minions []
                                                          :minions []
                                                          :hero    (if (contains? hero :id)
                                                                     hero
                                                                     (assoc hero :id (str "h" (inc index))))
                                                          :fatigue  1
                                                          :mana     10
                                                          :max-mana 10}))
                                          (reduce (fn [a v]
                                                    (assoc a (:id v) v))
                                                  {}))
      :counter                       1
      :minion-ids-summoned-this-turn []})))

(defn get-player
  "Returns the player with the given id."
  {:test (fn []
           (is= (-> (create-empty-state)
                    (get-player "p1")
                    (:id))
                "p1"))}
  [state player-id]
  (get-in state [:players player-id]))


(defn get-player-id-in-turn
  {:test (fn []
           (is= (-> (create-empty-state)
                    (get-player-id-in-turn))
                "p1"))}
  [state]
  (:player-id-in-turn state))


(defn get-minions
  "Returns the minions on the board for the given player-id or for both players."
  {:test (fn []
           ;; Getting minions is also tested in add-minion-to-board.
           (is= (-> (create-empty-state)
                    (get-minions "p1"))
                [])
           (is= (-> (create-empty-state)
                    (get-minions))
                [])
           (is= (as-> (create-empty-state) $
                  (assoc-in $ [:players "p1" :minions] [(create-minion "Nightblade")])
                  (get-minions $ "p1")
                  (map :name $))
                ["Nightblade"]))}
  ([state player-id]
   (:minions (get-player state player-id)))
  ([state]
   (->> (:players state)
        (vals)
        (map :minions)
        (apply concat))))

(defn get-minion-names
  "Gets the names of the minions on the board for the given player-id or for both players."
  {:test (fn []
           (is= (-> (create-empty-state)
                    (get-minion-names "p1"))
                [])
           (is= (-> (create-empty-state)
                    (get-minion-names))
                [])
           (is= (as-> (create-empty-state) $
                  (assoc-in $ [:players "p1" :minions] [(create-minion "Nightblade")])
                  (get-minion-names $ "p1"))
                ["Nightblade"]))}
  ([state player-id]
   (->> (get-minions state player-id)
        (map :name)))
  ([state]
   (->> (get-minions state)
        (map :name))))

(defn get-deck
  {:test (fn []
           (is= (-> (create-empty-state)
                    (get-deck "p1"))
                []))}
  [state player-id]
  (get-in state [:players player-id :deck]))


(defn get-hand
  {:test (fn []
           (is= (-> (create-empty-state)
                    (get-hand "p1"))
                []))}
  [state player-id]
  (get-in state [:players player-id :hand]))

(defn- generate-id
  "Generates an id and returns a tuple with the new state and the generated id."
  {:test (fn []
           (is= (generate-id {:counter 6})
                [{:counter 7} 6]))}
  [state]
  {:pre [(contains? state :counter)]}
  [(update state :counter inc) (:counter state)])


(defn- generate-time-id
  "Generates a number and returns a tuple with the new state and the generated number."
  {:test (fn []
           (is= (generate-time-id {:counter 6})
                [{:counter 7} 6]))}
  [state]
  {:pre [(contains? state :counter)]}
  [(update state :counter inc) (:counter state)])

(defn add-minion-to-board
  "Adds a minion with a given position to a player's minions and updates the other minions' positions."
  {:test (fn []
           ;; Adding a minion to an empty board
           (is= (as-> (create-empty-state) $
                  (add-minion-to-board $ "p1" (create-minion "Injured Blademaster" :id "ib") 0)
                  (get-minions $ "p1")
                  (map (fn [m] {:id (:id m) :name (:name m)}) $))
                [{:id "ib" :name "Injured Blademaster"}])
           ;; Adding a minion and update positions
           (let [minions (-> (create-empty-state)
                             (add-minion-to-board "p1" (create-minion "Injured Blademaster" :id "ib1") 0)
                             (add-minion-to-board "p1" (create-minion "Injured Blademaster" :id "ib2") 0)
                             (add-minion-to-board "p1" (create-minion "Injured Blademaster" :id "ib3") 1)
                             (get-minions "p1"))]
             (is= (map :id minions) ["ib1" "ib2" "ib3"])
             (is= (map :position minions) [2 0 1]))
           ;; Generating an id for the new minion
           (let [state (-> (create-empty-state)
                           (add-minion-to-board "p1" (create-minion "Injured Blademaster") 0))]
             (is= (-> (get-minions state "p1")
                      (first)
                      (:name))
                  "Injured Blademaster")
             (is= (:counter state) 3)))}
  [state player-id minion position]
  {:pre [(map? state) (string? player-id) (map? minion) (number? position)]}
  (let [[state id] (if (contains? minion :id)
                     [state (:id minion)]
                     (let [[state value] (generate-id state)]
                       [state (str "m" value)]))
        [state time-id] (generate-time-id state)
        ready-minion (assoc minion :position position
                            :owner-id player-id
                            :id id
                            :added-to-board-time-id time-id)]
    (as-> (update-in state
               [:players player-id :minions]
               (fn [minions]
                 (conj (->> minions
                            (mapv (fn [m]
                                    (if (< (:position m) position)
                                      m
                                      (update m :position inc)))))
                       ready-minion))) $
      (let [st $]
        (update-in st [:players player-id :minions] (constantly (sort-by :position (get-in st [:players player-id :minions]))))))))


(defn add-minions-to-board
  {:test (fn []
           (is= (as-> (create-empty-state) $
                  (add-minions-to-board $ "p1" [(create-minion "Nightblade")
                                                "Injured Blademaster"
                                                (create-minion "Silver Hand Recruit")])
                  (get-minions $ "p1")
                  (map :name $))
                ["Nightblade" "Injured Blademaster" "Silver Hand Recruit"]))}
  [state player-id minions]
  (->> minions
       (reduce-kv (fn [state index minion]
                    (add-minion-to-board state
                                         player-id
                                         (if (string? minion)
                                           (create-minion minion)
                                           minion)
                                         index))
                  state)))


(defn- add-card-to
  "Adds a card to either the hand or the deck."
  {:test (fn []
           ;; Adding cards to deck
           (is= (as-> (create-empty-state) $
                  (add-card-to $ "p1" "Nightblade" :deck)
                  (add-card-to $ "p1" "Silver Hand Recruit" :deck)
                  (get-deck $ "p1")
                  (map :name $))
                ["Nightblade" "Silver Hand Recruit"])
           ;; Adding cards to hand
           (is= (as-> (create-empty-state) $
                  (add-card-to $ "p1" "Nightblade" :hand)
                  (add-card-to $ "p1" "Silver Hand Recruit" :hand)
                  (get-hand $ "p1")
                  (map :name $))
                ["Nightblade" "Silver Hand Recruit"]))}
  [state player-id card-or-name place]
  (let [card (if (string? card-or-name)
               (create-card card-or-name)
               card-or-name)
        [state id] (if (contains? card :id)
                     [state (:id card)]
                     (let [[state value] (generate-id state)]
                       [state (str "c" value)]))
        ready-card (assoc card :owner-id player-id
                          :id id)]
    (update-in state [:players player-id place] conj ready-card)))


(defn add-card-to-deck
  {:test (fn []
           (is= (as-> (create-empty-state) $
                  (add-card-to-deck $ "p1" "Nightblade")
                  (get-deck $ "p1")
                  (map :name $))
                ["Nightblade"]))}
  [state player-id card]
  (add-card-to state player-id card :deck))


(defn add-card-to-hand
  {:test (fn []
           (is= (as-> (create-empty-state) $
                  (add-card-to-hand $ "p1" "Nightblade")
                  (get-hand $ "p1")
                  (map :name $))
                ["Nightblade"]))}
  [state player-id card]
  (add-card-to state player-id card :hand))


(defn add-cards-to-deck
  {:test (fn []
           (is= (as-> (create-empty-state) $
                  (add-cards-to-deck $ "p1" ["Nightblade" "Silver Hand Recruit"])
                  (get-deck $ "p1")
                  (map :name $))
                ["Nightblade" "Silver Hand Recruit"]))}
  [state player-id cards]
  (reduce (fn [state card]
            (add-card-to-deck state player-id card))
          state
          cards))


(defn add-cards-to-hand
  {:test (fn []
           (is= (as-> (create-empty-state) $
                  (add-cards-to-hand $ "p1" ["Nightblade" "Silver Hand Recruit"])
                  (get-hand $ "p1")
                  (map :name $))
                ["Nightblade" "Silver Hand Recruit"]))}
  [state player-id cards]
  (reduce (fn [state card]
            (add-card-to-hand state player-id card))
          state
          cards))

(defn update-player-mana
  {:test (fn []
           (is= (as-> (create-empty-state) $
                  (update-player-mana $ "p1" 5)
                  (get-player $ "p1")
                  (:mana $))
                5))}
  [state player-id mana]
  (if mana
    (update-in state [:players player-id :mana] (constantly mana))
    state))

(defn update-player-max-mana
  {:test (fn []
           (is= (as-> (create-empty-state) $
                  (update-player-max-mana $ "p1" 5)
                  (get-player $ "p1")
                  (:max-mana $))
                5))}
  [state player-id max-mana]
  (if max-mana
    (update-in state [:players player-id :max-mana] (constantly max-mana))
    state))

(defn update-player-fatigue
  {:test (fn []
           (is= (as-> (create-empty-state) $
                  (update-player-fatigue $ "p1" 5)
                  (get-player $ "p1")
                  (:fatigue $))
                5))}
  [state player-id fatigue]
  (if fatigue
    (update-in state [:players player-id :fatigue] (constantly fatigue))
    state))

(defn update-reaction-effect-minions
  "Updates the collection reaction-effect-minions"
  {:test (fn []
           (is= (as-> (create-empty-state) $
                  (update-reaction-effect-minions $ "p1" ["Nightblade" "Silver Hand Recruit"])
                  (get-player $ "p1")
                  (:reaction-effect-minions $))
                ["Nightblade" "Silver Hand Recruit"]))}
  [state player-id minions]
  (update-in state [:players player-id :reaction-effect-minions] (constantly minions)))

(defn create-game
  "Creates a game with the given deck, hand, minions (placed on the board), and heroes."
  {:test (fn []
           (is= (create-game) (create-empty-state))

           (is= (create-game [{:hero (create-hero "Rexxar")}])
                (create-game [{:hero "Rexxar"}]))

           (is= (create-game [{:minions [(create-minion "Nightblade")]}])
                (create-game [{:minions ["Nightblade"]}]))

           ;; This test is showing the state structure - otherwise avoid large assertions
           (comment 
             (is= (create-game [{:minions ["Nightblade"]
                                 :deck    ["Silver Hand Recruit"]
                                 :hand    ["Boulderfist Ogre"]}
                                {:hero "Rexxar"}]
                               :player-id-in-turn "p2")
                  {:player-id-in-turn             "p2"
                   :players                       {"p1" {:id      "p1"
                                                         :end-effect-minions []
                                                         :reaction-effect-minions []
                                                         :deck    [{:entity-type :card
                                                                    :id          "c3"
                                                                    :name        "Silver Hand Recruit"
                                                                    :type        :minion
                                                                    :owner-id    "p1"}]
                                                         :hand    [{:entity-type :card
                                                                    :id          "c4"
                                                                    :name        "Boulderfist Ogre"
                                                                    :type        :minion
                                                                    :owner-id    "p1"}]
                                                         :minions [{:damage-taken                0
                                                                    :attacks-performed-this-turn 0
                                                                    :added-to-board-time-id      2
                                                                    :entity-type                 :minion
                                                                    :attack                      4
                                                                    :health                      4
                                                                    :name                        "Nightblade"
                                                                    :id                          "m1"
                                                                    :position                    0
                                                                    :owner-id                    "p1"}]
                                                         :hero    {:name         "Jaina Proudmoore"
                                                                   :id           "h1"
                                                                   :health       30
                                                                   :entity-type  :hero
                                                                   :hero-power-used false
                                                                   :damage-taken 0}
                                                         :fatigue  1
                                                         :mana     10
                                                         :max-mana 10}
                                                   "p2" {:id       "p2"
                                                         :end-effect-minions []
                                                         :reaction-effect-minions []
                                                         :deck     []
                                                         :hand     []
                                                         :minions  []
                                                         :hero     {:name         "Rexxar"
                                                                    :id           "h2"
                                                                    :health       30
                                                                    :hero-power-used false
                                                                    :entity-type  :hero
                                                                    :damage-taken 0}
                                                         :fatigue  1
                                                         :mana     10
                                                         :max-mana 10}}
                   :counter                       5
                   :minion-ids-summoned-this-turn []})))}
  ([data & kvs]
   (let [players-data (map-indexed (fn [index player-data]
                                     (assoc player-data :player-id (str "p" (inc index))))
                                   data)
         state (as-> (create-empty-state (map (fn [player-data]
                                                (cond (nil? (:hero player-data))
                                                      (create-hero "Jaina Proudmoore")

                                                      (string? (:hero player-data))
                                                      (create-hero (:hero player-data))

                                                      :else
                                                      (:hero player-data)))
                                              data)) $
                 (reduce (fn [state {player-id :player-id
                                    minions   :minions
                                    deck      :deck
                                    mana      :mana
                                    max-mana  :max-mana
                                    fatigue   :fatigue
                                    hand      :hand}]
                           (-> state
                               (add-minions-to-board player-id minions)
                               (add-cards-to-deck player-id deck)
                               (update-player-mana player-id mana)
                               (update-player-max-mana player-id max-mana)
                               (update-player-fatigue player-id fatigue)
                               (add-cards-to-hand player-id hand)))
                         $
                         players-data))]
     (if (empty? kvs)
       state
       (apply assoc state kvs))))
  ([]
   (create-game [])))


(defn get-minion
  "Returns the minion with the given id."
  {:test (fn []
           (is= (-> (create-game [{:minions [(create-minion "Nightblade" :id "n")]}])
                    (get-minion "n")
                    (:name))
                "Nightblade"))}
  [state id]
  (->> (get-minions state)
       (filter (fn [m] (= (:id m) id)))
       (first)))


(defn get-players
  {:test (fn []
           (is= (->> (create-game)
                     (get-players)
                     (map :id))
                ["p1" "p2"]))}
  [state]
  (->> (:players state)
       (vals)))


(defn get-heroes
  {:test (fn []
           (is= (->> (create-game [{:hero "Rexxar"}])
                     (get-heroes)
                     (map :name))
                ["Rexxar" "Jaina Proudmoore"]))}
  [state]
  (->> (get-players state)
       (map :hero)))

(defn get-hero-by-player-id
  "Gets the hero associated with the given player"
  {:test (fn []
           (is= (-> (create-game [{:hero "Rexxar"}])
                    (get-hero-by-player-id "p1")
                    (:name))
                "Rexxar"))}
  [state id]
  (get-in state [:players id :hero])

  )

(defn get-hero
  "Get the hero map from the id"
  {:test (fn []
           (is= (-> (create-game [{:hero (create-hero "Rexxar" :id "h1")}])
                    (get-hero "h1")
                    (:name))
                "Rexxar"))}
  [state id]
  (->> (get-heroes state)
       (filter (fn [h] (= (:id h) id)))
       (first)))

(defn replace-minion
  "Replaces a minion with the same id by the given new-minion."
  {:test (fn []
           (is= (-> (create-game [{:minions [(create-minion "Nightblade" :id "m")]}])
                    (replace-minion (create-minion "Silver Hand Recruit" :id "m"))
                    (get-minion "m")
                    (:name))
                "Silver Hand Recruit"))}
  [state new-minion]
  (let [owner-id (or (:owner-id new-minion)
                     (:owner-id (get-minion state (:id new-minion))))]
    (update-in state
               [:players owner-id :minions]
               (fn [minions]
                 (map (fn [m]
                        (if (= (:id m) (:id new-minion))
                          new-minion
                          m))
                      minions)))))

(defn replace-card
  [state new-card]
  (let [owner-id (or (:owner-id new-card)
                     (:owner-id (get-card state (:id new-card))))]
    (update-in state
               [:players owner-id :hand]
               (fn [cards]
                 (map (fn [m]
                        (if (= (:id m) (:id new-card))
                          new-card
                          m))
                      cards)))))



(defn update-minion
  "Updates the value of the given key for the minion with the given id. If function-or-value is a value it will be the
   new value, else if it is a function it will be applied on the existing value to produce the new value."
  {:test (fn []
           (is= (-> (create-game [{:minions [(create-minion "Nightblade" :id "n")]}])
                    (update-minion "n" :damage-taken inc)
                    (get-minion "n")
                    (:damage-taken))
                1)
           (is= (-> (create-game [{:minions [(create-minion "Nightblade" :id "n")]}])
                    (update-minion "n" :damage-taken 2)
                    (get-minion "n")
                    (:damage-taken))
                2))}
  [state id key function-or-value]
  (let [minion (get-minion state id)]
    (replace-minion state (if (fn? function-or-value)
                            (update minion key function-or-value)
                            (assoc minion key function-or-value)))))


(defn update-card
  [state id key function-or-value]
  (let [card (get-card state id)]
    (replace-card state (if (fn? function-or-value)
                          (update card key function-or-value)
                          (assoc card key function-or-value)))))


(defn replace-hero
  "Replaces a hero with the same id by the given new hero"
  {:test (fn []
           (is= (-> (create-game [{:hero (create-hero "Jaina Proudmoore" :id "h1" :damage-taken 0)}])
                    (replace-hero (create-hero "Jaina Proudmoore" :id "h1" :damage-taken 1))
                    (get-hero "h1")
                    (:damage-taken))
                1))}
  [state new-hero]
  (let [player-id (or (if (= (:id (get-hero-by-player-id state "p1"))
                             (:id new-hero))
                        "p1")
                      (if (= (:id (get-hero-by-player-id state "p2"))
                             (:id new-hero))
                        "p2"))]
    (update-in state [:players player-id :hero] (constantly new-hero))))

(defn update-hero
  "Updates the value of the given key for the hero with the given id. If function-or-value is a value it will be the
   new value, else if it is a function it will be applied on the existing value to produce the new value."
  {:test (fn []
           (is= (-> (create-game [{:hero (create-hero "Jaina Proudmoore" :id "h1")}])
                    (update-hero "h1" :damage-taken inc)
                    (get-hero "h1")
                    (:damage-taken))
                1))}
  [state id key function-or-value]
  (let [hero (get-hero state id)]
    (replace-hero state (if (fn? function-or-value)
                          (update hero key function-or-value)
                          (assoc hero key function-or-value)))))

(defn remove-minion
  "Removes a minion with the given id from the state."
  {:test (fn []
           (is= (-> (create-game [{:minions [(create-minion "Nightblade" :id "n")]}])
                    (remove-minion "n")
                    (get-minions))
                [])
           (is= (-> (create-game [{:minions [(create-minion "Nightblade" :id "n")]}])
                    (remove-minion "ob")
                    (get-minion "n")
                    (:id))
                "n"))}
  [state id]
  (let [owner-id (:owner-id (get-minion state id))]
    (update-in state
               [:players owner-id :minions]
               (fn [minions]
                 (remove (fn [m] (= (:id m) id)) minions)))))




(defn remove-minions
  "Removes the minions with the given ids from the state."
  {:test (fn []
           (is= (as-> (create-game [{:minions [(create-minion "Nightblade" :id "n1")
                                               (create-minion "Nightblade" :id "n2")]}
                                    {:minions [(create-minion "Nightblade" :id "n3")
                                               (create-minion "Nightblade" :id "n4")]}]) $
                  (remove-minions $ "n1" "n4")
                  (get-minions $)
                  (map :id $))
                ["n2" "n3"]))}
  [state & ids]
  (reduce remove-minion state ids))

(defn get-mana
  "Gets the current mana left"
  {:test (fn []
           (is= (-> (create-game)
                    (get-mana "p1"))
                10)
           (is= (-> (create-game [{:mana 5}])
                    (get-mana "p1"))
                5))}
  [state player-id]
  {:pre [(map? state) (string? player-id)]}
  (get-in state [:players player-id :mana]))

(defn get-max-mana
  "Gets the maximum allowed mana"
  {:test (fn []
           (is= (-> (create-game [{:mana 5}])
                    (get-max-mana "p1"))
                10)
           (is= (-> (create-game [{:max-mana 5}])
                    (get-max-mana "p1"))
                5))}
  [state player-id]
  {:pre [(map? state) (string? player-id)]}
  (get-in state [:players player-id :max-mana]))

(defn reset-mana
  "Increases max-mana and resets current mana to max-mana"
  {:test (fn []
           (is= (-> (create-game [{:mana 5 :max-mana 8}])
                    (reset-mana "p1")
                    (get-mana "p1"))
                9)
           (is= (-> (create-game [{:mana 5 :max-mana 10}])
                    (reset-mana "p1")
                    (get-mana "p1"))
                10)
           (is= (-> (create-game [{:mana 10 :max-mana 10}])
                    (reset-mana "p1")
                    (get-mana "p1"))
                10)
           (is= (-> (create-game [{:mana 5 :max-mana 8}])
                    (reset-mana "p1")
                    (get-max-mana "p1"))
                9))}
  
  [state player-id]
  {:pre [(map? state) (string? player-id)]}
  (let []
    (if-not (= (get-max-mana state player-id) 10)
      (-> (update-player-max-mana state player-id (do (inc (get-max-mana state player-id))))
          (update-player-mana player-id (do (inc (get-max-mana state player-id)))))
      (update-player-mana state player-id (get-max-mana state player-id)))))


(defn get-fatigue
  {:test (fn []
           (is= (as-> (create-game) $
                  (get-fatigue $ "p1")
                  (get-player $ "p1")
                  (:fatigue $))
                2)
           (is= (as-> (create-game) $
                  (get-fatigue $ "p1")
                  (get-fatigue $ "p1")
                  (get-player $ "p1")
                  (:fatigue $))
                3))}
  [state player-id]
  {:pre [(map? state) (string? player-id)]}
  (-> (update-in state [:players player-id :hero :damage-taken]
                 + (get-in state [:players player-id :fatigue]))
      (update-in [:players player-id :fatigue] + 1)))

(defn get-card-cost
  {:test (fn []
           (is= (-> (get-card-cost (create-card "Boulderfist Ogre")))
                6))}
  [card]
  {:pre [(map? card)]}
  (-> (get-definition card)
      (:mana-cost)))

(defn get-card
  {:test (fn []
           (is= (-> (create-game [{:hand [(create-card "Boulderfist Ogre" :id "bo")]}])
                    (get-card "bo")
                    (:name))
                "Boulderfist Ogre"))}
  [state card-id]
  {:pre [(map? state) (string? card-id)]}
  (-> (filter (fn [x] (= (:id x) card-id)) (into [] (concat (get-in state [:players "p1" :hand]) (get-in state [:players "p2" :hand]))))
      (first)))

(defn card->minion
  "Creates a minion out of a card"
  {:test (fn []
           (is= (-> (card->minion (create-card "Boulderfist Ogre"))
                    (:damage-taken))
                0)
           (is= (-> (card->minion (create-card "Boulderfist Ogre"))
                    (:entity-type))
                :minion))}
  [card]
  {:pre [(map? card)]}
  (as-> (get-definition (:name card)) $
    (let [card-def $ stealth (:stealth card-def) name (:name card-def)]
      (create-minion name :description (or (:description card-def) "") :can-attack false :mana-cost (:mana-cost card-def) :max-health (:health card-def) :original-attack (:attack card-def) :original-health (:health card-def) :set (:set card-def) :sleepy true :states (filter (fn [el] (not= el nil)) [(when (:stealth card-def) :stealth) (when (:deathrattle card-def) :deathrattle)]) :valid-attack-ids {}))))

(defn get-minion-owner
  "Returns the owner id"
  {:test (fn []
           (is= (-> (create-game [{:minions [(create-minion "Boulderfist Ogre" :id "bo" :owner-id "p1")]}])
                    (get-minion-owner "bo"))
                "p1"))}
  [state id]
  {:pre [(map? state) (string? id)]}
  (-> (get-minion state id)
      (:owner-id)))

(defn get-hero-power-cost
  "Returns the cost of the hero power, given a hero"
  {:test (fn []
           (is= (-> (create-hero "Jaina Proudmoore")
                    (get-hero-power-cost))
                2))}
  [hero]
  (-> (get-definition (:name hero))
      (:hero-power)
      (get-definition)
      (:mana-cost)))

(defn get-hero-power
  "Returns the cost of the hero power, given a hero"
  {:test (fn []
           (is= (-> (create-hero "Jaina Proudmoore")
                    (get-hero-power)
                    (:name))
                "Fireblast"))}
  [hero]
  (-> (get-definition (:name hero))
      (:hero-power)
      (get-definition)))

(defn reset-hero-power
  "Resets the use of the hero power"
  {:test (fn []
           (is= (-> (create-game [{:hero (create-hero "Jaina Proudmoore" :hero-power-used true)}])
                    (reset-hero-power "p1")
                    (get-in [:players "p1" :hero :hero-power-used]))
                false))}
  [state player-id]
  (update-in state [:players player-id :hero :hero-power-used] (constantly false)))

(defn set-hero-power
  "Sets the hero power to used."
  {:test (fn []
           (is= (-> (create-game [{:hero (create-hero "Jaina Proudmoore" :hero-power-used false)}])
                    (set-hero-power "p1")
                    (get-in [:players "p1" :hero :hero-power-used]))
                true))}
  [state player-id]
  (update-in state [:players player-id :hero :hero-power-used] (constantly true)))

(defn increase-health
  "Increases health of a given minion by a given amount"
  {:test (fn []
           (is= (-> (create-game [{:minions [(create-minion "Boulderfist Ogre" :id "bo" :health 5)]}])
                    (increase-health "p1" "bo" 5)
                    (get-minions "p1")
                    (first)
                    (:health))
                10))}
  [state player-id minion-id amount]
  (update-in state [:players player-id :minions]
             (fn [minions]
               (map (fn [minion]
                      (if (= (:id minion)
                             minion-id)
                        (assoc minion :health (+ (:health minion) amount))
                        minion))
                    minions))))

(defn damage-hero
  "Damages the hero of the given player."
  {:test (fn []
           (is= (-> (create-game [{:hero (create-hero "Rexxar" :id "h1")}])
                    (damage-hero "p1" 1)
                    (get-in [:players "p1" :hero :damage-taken]))
                1)
           (is= (-> (create-game [{:hero (create-hero "Rexxar" :id "h1")}])
                    (damage-hero "p1" 30)
                    (get-in [:players "p1" :hero :damage-taken]))
                30))}
  [state player-id damage]
  (update-hero state (get-in state [:players player-id :hero :id]) :damage-taken (+ damage (get-in state [:players player-id :hero :damage-taken]))))


(defn apply-random-fn
  [state random-fn element]
  (let [result (if (get state :seed)
                 (random-fn (get state :seed) element)
                 (random-fn 1234 element))]
    [(update state :seed (constantly (first result))) (second result)]))

(defn get-random-minion-excluding-caller
  "Gets a random minion that is not the calling minion"
  {:test (fn []
           (is= (-> (create-game [{:minions [(create-minion "Young Priestess" :id "yp")
                                             (create-minion "Silver Hand Recruit" :id "shr")]}])
                    (get-random-minion-excluding-caller "yp" "p1")
                    (second)
                    (:id))
                "shr"))}
  [state minion-id player-id]
  {:pre [(map? state) (string? minion-id)]}
  (->> (get-minions state player-id)
       (filter (fn [x] (not= (:id x) minion-id)))
       (apply-random-fn state random-nth)))

(defn count-damaged-characters
  "Returns the number of damaged minions for a player"
  {:test (fn []
           (is= (-> (create-game [{:minions [(create-minion "Boulderfist Ogre") (create-minion "Boulderfist Ogre" :damage-taken 3) (create-minion "Boulderfist Ogre" :damage-taken 2)]}])
                    (count-damaged-characters "p1"))
                2)
           (is= (-> (create-game [{:hero (create-hero "Jaina Proudmoore" :damage-taken 3) :minions [(create-minion "Boulderfist Ogre") (create-minion "Boulderfist Ogre" :damage-taken 3) (create-minion "Boulderfist Ogre" :damage-taken 2)]}])
                    (count-damaged-characters "p1"))
                3))}
  [state player-id]
  (->> (get-in state [:players "p1" :minions])
       (filter (fn [x] (not= (:damage-taken x) 0)))
       (count)
       (+ (if (not= (:damage-taken (get-hero-by-player-id state player-id))
                    0)
            1
            0))))

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
  (if (= (count (get-in state [:players player-id :deck])) 0)
    true
    nil))

(defn draw-card
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
          (update-in [:players player-id :hand] reverse)
          (update-in [:players player-id :hand] conj card)
          (update-in [:players player-id :hand] reverse)))
    (let [card (peek (get-in state [:players player-id :deck]))]
      (update-in state [:players player-id :deck] pop))))

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
          (update-in $ [:players player-id :hand] reverse)
          (update-in $ [:players player-id :hand] conj first-minion)
          (update-in $ [:players player-id :hand] reverse))
        (as-> (filter (fn [x] (not= (:id x) (:id first-minion))) (get-deck state player-id)) $
          (update-in state [:players player-id :deck] (constantly $))))
      state)))

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


