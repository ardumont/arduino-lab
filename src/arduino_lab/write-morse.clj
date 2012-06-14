(ns arduino-lab.write-morse
  "Write morse to the led"
  (:use [midje.sweet])
  (:use :reload-all clodiuno.core)
  (:use :reload-all clodiuno.firmata)
  (:require [arduino-lab.morse :as m]))

;; circuit: Just a led on the 13 pin.

;; time
(def short-pulse 100)
(def long-pulse 250)
(def letter-delay 500)

;; pin number
(def pin-led 13)

(defn blink "Given a board and time, make the led blink a given time"
  [board time]
  (digital-write board pin-led HIGH)
  (Thread/sleep time)
  (digital-write board pin-led LOW)
  (Thread/sleep time))

(defmulti blink-letter-morse "Given a bit, blink the led accordingly in morse" identity)
(defmethod blink-letter-morse 0 [_] (blink board short-pulse))
(defmethod blink-letter-morse 1 [_] (blink board long-pulse))

(defn blink-letter "Given a letter in morse representation, make the led blink accordingly (0 short pulse ; 1 long pulse)"
  [board letter-in-morse]
  (doseq [i letter-in-morse] (blink-letter-morse i))
  (Thread/sleep letter-delay))

(defn write-morse "Given a word, make the led blink in morse for each letter (no upper case, no punctuation)"
  [board word]
  (doseq [l word] (blink-letter board (m/letters-2-bits l))))

(comment "Execution code for the repl - step by step"
  (def device-board "/dev/ttyACM0")
  (System/setProperty "gnu.io.rxtx.SerialPorts" device-board)
  (def board (arduino :firmata device-board))
  board
  (pin-mode board pin-led OUTPUT)
  (write-morse board "hello world")
  (write-morse board "sos")
  (close board))
