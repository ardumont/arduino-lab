(ns arduino-lab.utils
  "some utility functions"
  (:use [midje.sweet]))

 (defn reverse-map
   "Reverse the keys/values of a map"
   [m]
   (into {} (map (fn [[k v]] [v k]) m)))

(fact
  (reverse-map {:a [0 0]
                :b [1 1]}) => {[0 0] :a
                               [1 1] :b})
