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

;; duraction of morse signal
(def dit 50)
(def dat (* 3 dit))

;; beyond this threshold, we get a new word
(def threshold 1000)

;; the state of the application to keep:
;; - the word that is been built
;; - the last time  we added a bit
(def ^:dynamic *state* (atom
                        {:time (System/currentTimeMillis) ;; the reading time
                         :word []}))                      ;; the current word that is been read

;; the words that has been read
(def ^:dynamic *words* (atom []))

(defn read-morse
  "Given a suite of bits, return the words"
  [& b]
  (s/join "" (map m/bits-2-letters b)))

(fact "read-morse"
  (read-morse [0 0 0] [1 1 1] [0 0 0]) => "sos"
  (read-morse [0 0 0 0] [0] [0 1 0 0] [0 1 0 0] [1 1 1]) => "hello")

(defn init-state
  "Init the current state of the application"
  ([]
     (reset! *state* {:time (System/currentTimeMillis) ;; the reading time
                      :word  []}))
  ([n]
     (reset! *state* {:time (System/currentTimeMillis) ;; the reading time
                      :word  [n]})))

(fact "init-state - arity 1"
  (binding [*state* (atom {})]
    (init-state) => (contains {:word []})))

(fact "init-state - arity 2"
  (let [*state* (atom {})]
    (init-state :val) => (contains {:word [:val]})))

(defn compute-signal "Compute the signal as 0 or 1 depending on the status of the button and the duration of the pression"
  [duration]
  (cond (<= duration 19) nil
        (<= 20 duration dit) 0
        (< dit duration dat) 1
        :else nil))

(fact "compute-signal"
  (compute-signal 10) => nil
  (compute-signal 20) => 0
  (compute-signal 50) => 0
  (compute-signal 51) => 1
  (compute-signal 149) => 1
  (compute-signal 150) => nil)

(defn read-morse-word
  "Read the word from the global state and update the global list of words read"
  []
  (let [bits (:word @*state*)
        w (apply read-morse bits)]
    (println "bits" bits " -> " w)
    (swap! *words* conj w)))

(fact
  (binding [*state* (atom {:word [[0 0 0] [1 1 1] [0 0 0]]})
            *words* (atom [])]
    (read-morse-word) =>  (contains "sos")))

(defn add-bit
  "Update the state with the new signal read."
  [*state* duration]
  (if-let [signal (compute-signal duration)]
    (swap! *state* (fn [o] (update-in o [:word] conj signal)))))

(fact "add-bit"
  (let [a (atom {:word []})]
    (add-bit a 19) => nil
    (add-bit a 20) => {:word [0]}
    (add-bit a 20) => {:word [0 0]}
    (add-bit a 51) => {:word [0 0 1]}))

;; dispatch on the signal send by the button
(defmulti morse-reading (fn [signal duration] signal))

(defmethod morse-reading HIGH
  [_ duration]
  (if (< threshold duration)
    (do
      (read-morse-word *words* *state*)
      (init-state *state* (compute-signal duration)))
    (add-bit *state* duration)))

(defmethod morse-reading LOW
  [_ duration]
  (when (< threshold duration)
    (do
      (read-morse-word *words* *state*)
      (init-state *state*))))

(defn read-morse-from-button
  "Given a board, read the word the human send with the button"
  [board start-time stop-time]
  (while (< (- (System/currentTimeMillis) start-time) stop-time)
    (let [status-button (digital-read board pin-button)]

      (let [nt (System/currentTimeMillis)
            duration (- nt (:time @*state*))]
        (morse-reading status-button duration))

      (digital-write board pin-led status-button)
      (Thread/sleep 10))))

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
    (clojure.pprint/pprint *words*)

    (close board)))

(comment
  "For the repl - step by step"

  (def device-board "/dev/ttyACM2")
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

  *words*

  (close board))

(comment
  "for the repl - one shot"
  (def device-board "/dev/ttyACM0")
  (System/setProperty "gnu.io.rxtx.SerialPorts" device-board)
  (main device-board))

