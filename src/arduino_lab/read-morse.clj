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
  (map m/bits-2-letters b))

(fact "read-morse"
  (read-morse [0 0 0] [1 1 1] [0 0 0]) => [\s \o \s]
  (read-morse [0 0 0 0] [0] [0 1 0 0] [0 1 0 0] [1 1 1]) => [\h \e \l \l \o]
  (read-morse [0 0 0 0] [0] [0 1 0 0] [0 1 0 0] [1 1 1] [] [0 0 0] [1 1 1] [0 0 0]) => [\h \e \l \l \o nil \s \o \s])

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
  [duration]
  (if-let [signal (compute-signal duration)]
    (swap! *state* update-in [:word] conj signal)))

(fact "add-bit"
  (binding [*state* (atom {:word []})]
    (add-bit 19) => nil
    (add-bit 20) => {:word [0]}
    (add-bit 20) => {:word [0 0]}
    (add-bit 51) => {:word [0 0 1]}))

;; dispatch on the signal send by the button
(defmulti morse-reading (fn [signal duration] signal))

(defmethod morse-reading HIGH
  [_ duration]
  (if (< threshold duration)
    (do
      (read-morse-word)
      (init-state (compute-signal duration)))
    (add-bit duration)))

(defmethod morse-reading LOW
  [_ duration]
  (when (< threshold duration)
    (do
      (read-morse-word)
      (init-state))))

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


