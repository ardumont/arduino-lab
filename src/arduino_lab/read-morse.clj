(ns arduino-lab.read-morse
  "Read morse from the circuit"
  (:use [midje.sweet])
  (:require [arduino-lab.morse :as m]
            [clojure.string :as s]))

(defn read-morse
  "Given a suite of bits, return the words"
  [& b]
  (s/join "" (map m/bits-2-letters b)))

(fact "read-morse"
  (read-morse [0 0 0] [1 1 1] [0 0 0]) => "sos"
  (read-morse [0 0 0 0] [0] [0 1 0 0] [0 1 0 0] [1 1 1]) => "hello")
