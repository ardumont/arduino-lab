(ns arduino-lab.read-morse
  "Read morse from the circuit"
  (:use [midje.sweet])
  (:use :reload-all clodiuno.core)
  (:use :reload-all clodiuno.firmata)
  (:require [arduino-lab.morse :as m]
            [clojure.string :as s]
            [clojure.pprint :as p]))

;; circuit: http://arduino.cc/en/Tutorial/Button

;; setup of the pin

(def pin-led    13)
(def pin-button 7)

;; beyond this threshold, we get a new word
(def threshold 1000)

;; the state of the application to keep:
;; - the word that is been built
;; - the last time  we added a bit
(def state (atom
            {:time (System/currentTimeMillis) ;; the reading time
             :word []}))                      ;; the current word that is been read

;; the words that has been read
(def words (atom []))

(defn read-morse
  "Given a suite of bits, return the words"
  [& b]
  (s/join "" (map m/bits-2-letters b)))

(fact "read-morse"
  (read-morse [0 0 0] [1 1 1] [0 0 0]) => "sos"
  (read-morse [0 0 0 0] [0] [0 1 0 0] [0 1 0 0] [1 1 1]) => "hello")

(defn init-state
  "Init the current state of the application"
  [a n]
  (reset! a {:time (System/currentTimeMillis) ;; the reading time
            :word  [n]}))

(defn read-morse-word-and-reinit-word
  "Read the word and then init the state for the new word to come"
  [status-button]
  (let [bits (:word @state)
        w (read-morse bits)]
    (println "bits" bits " -> " w)
    (swap! words conj w)
    (init-state state status-button)))

(defn add-bit
  "Update the state with the new status-button read."
  [state status-button]
  (swap! state (fn [o] (update-in o [:word] conj status-button))))

(defn read-morse-from-button
  "Given a board, read the word the human send with the button"
  [board start-time stop-time]
  (while (< (- (System/currentTimeMillis) start-time) stop-time)
    (let [status-button (digital-read board pin-button)]

      (let [nt (System/currentTimeMillis)
            ot (:time @state)]

        (if (< threshold (- nt ot))
          (read-morse-word-and-reinit-word status-button)
          (add-bit state status-button)))

      (digital-write board pin-led status-button)
      (Thread/sleep 250))))

(defn main
  "Given a serial device entry:
   - open the board
   - launch the listening of the morse code
   - then close the board"
  [board-serial-port]
  (let [board (arduino :firmata board-serial-port)]

    ;; allow arduino to boot
    (Thread/sleep 5000)

    ;; enable-pin for the button
    (enable-pin board :digital pin-button)

    ;; led in output mode
    (pin-mode board pin-led OUTPUT)

    ;; button in input mode
    (pin-mode board pin-button INPUT)

    ;; read the morse as we go - we pass in mode waiting for 60 seconds
    (read-morse-from-button board (System/currentTimeMillis) 60000)

    ;; display the read words
    (clojure.pprint/pprint words)

    (close board)))

(comment
  "For the repl - step by step"

  (def device-board "/dev/ttyACM0")
  (System/setProperty "gnu.io.rxtx.SerialPorts" device-board)
  (def board (arduino :firmata device-board))
  (enable-pin board :digital pin-button)
  board
  (pin-mode board pin-led OUTPUT)
  (pin-mode board pin-button INPUT)

  ;; to check manually some init stuff
  (digital-write board pin-led HIGH)
  (digital-write board pin-led LOW)

  (read-morse-from-button board (System/currentTimeMillis) 60000)

  (close board))

(comment
  "for the repl - one shot"
  (def device-board "/dev/ttyACM0")
  (System/setProperty "gnu.io.rxtx.SerialPorts" device-board)
  (main device-board))

