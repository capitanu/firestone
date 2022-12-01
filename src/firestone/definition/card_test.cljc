(ns firestone.definition.card-test
  (:require [firestone.construct :refer [create-card
                                         create-game
                                         create-hero]]
            [firestone.core-api :refer [play-minion-card]]
            [ysera.test :refer [is= deftest]]))


;(deftest Antique-Healbot
;         (is= (-> (create-game [{:hero (create-hero "Rexxar" :damage-taken 10)
;                                 :hand [(create-card "Antique Healbot" :id "ah")]}])
;                  (play-minion-card "p1" "ah" 0)
;                  (get-hero "p1"))
;              ))