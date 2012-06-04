(ns arduino-lab.morse
  "Morse"
  (:require [arduino-lab.utils :as u]))

;; 0 represents short signal
;; 1 represents long signal
;; Intermational morse from http://en.wikipedia.org/wiki/Morse_code
(def letters-2-bits {\a [0 1]
                     \b [1 0 0 0]
                     \c [1 0 1 0]
                     \d [1 0 0]
                     \e [0]
                     \f [0 0 1 0]
                     \g [1 1 0]
                     \h [0 0 0 0]
                     \i [0 0]
                     \j [0 1 1 1]
                     \k [1 0 1]
                     \l [0 1 0 0]
                     \m [1 1]
                     \n [1 0]
                     \o [1 1 1]
                     \p [0 1 1 0]
                     \q [1 1 0 1]
                     \r [0 1 0]
                     \s [0 0 0]
                     \t [1]
                     \u [0 0 1]
                     \v [0 0 0 1]
                     \w [0 1 1]
                     \x [1 0 0 1]
                     \y [1 0 1 1]
                     \z [1 1 0 0]})

(def bits-2-letters (u/reverse-map letters-2-bits))
