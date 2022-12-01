;(ns firestone.definition.advanced-tests
;  (:require [ysera.test :refer [is=]]
;            [clojure.test :refer [deftest]]
;            [firestone.construct :refer :all]
;            [firestone.core-api :refer :all]))
;
;
;(deftest Unearthed-Raptor-test
;  (is= (as-> (create-game [{:hand  [(create-card "Unearthed Raptor" :id "ur")]
;                            :minions [(create-minion "Malorne" :id "m1")]}
;                           {:minions [(create-minion "Malorne" :id "m2")]}]) $
;             ; Getting the deathrattle of Malorne
;             (play-minion-card $ "p1" "ur" 0 "m1")
;             (end-turn $ "p1")
;             ; Kill the Unearthed Raptor
;             (let [unearthed-raptor (get-minion-by-name $ "p1" "Unearthed Raptor")]
;               (attack $ "p2" "m2" (:id unearthed-raptor)))
;             ; The Unearthed Raptor should appear in the deck
;             (get-deck $ "p1")
;             (map :name $))
;       ["Unearthed Raptor"]))
;
;
;(deftest Edwin-VanCleef-test
;  (as-> (create-game [{:minions [(create-minion "Edwin VanCleef" :id "ev")]
;                       :hand  [(create-card "Silver Hand Recruit" :id "shr")
;                               (create-card "Elixir of Hope" :id "eoh")]}
;                      {:minions [(create-minion "Boulderfist Ogre" :id "bo")]}]) $
;        (play-minion-card $ "p1" "shr" 0)
;        ; Give Edwin VanCleef Elixir of Hope deathrattle
;        (play-spell-card $ "p1" "eoh" "ev")
;        ; Kill Edwin
;        (attack $ "p1" "ev" "bo")
;        ; Play the returned Edwin
;        (let [edwin-card (->> (get-hand $ "p1")
;                              (first))]
;          (play-minion-card $ "p1" (:id edwin-card) 0))
;        (let [edwin (get-minion-by-name $ "p1" "Edwin VanCleef")]
;          (is= (get-attack $ edwin) 6)
;          (is= (get-health $ edwin) 6))))
;
;
;(deftest Sneaky-Devil-test
;  (as-> (create-game [{:minions [(create-minion "Sneaky Devil" :id "sd")
;                               (create-minion "Moroes" :id "m")]}]) $
;        (end-turn $ "p1")
;        (do (is= (get-attack $ (get-minion $ "sd")) 2)
;            (is= (get-attack $ (get-minion $ "m")) 2)
;            (is= (get-attack $ (get-minion-by-name $ "p1" "Steward")) 2))))
;
;
;(deftest Spellbreaker-test
;  (is= (as-> (create-game [{:minions [(create-minion "Moroes" :id "m")]
;                            :hand  [(create-card "Spellbreaker" :id "s")]}]) $
;             (play-minion-card $ "p1" "s" 0 "m")
;             (end-turn $ "p1")
;             (get-minions $ "p1")
;             (map :name $)
;             (set $))
;       #{"Moroes" "Spellbreaker"}))
;
;
;
;
;
;
;
