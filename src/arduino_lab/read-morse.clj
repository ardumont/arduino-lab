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
(def threshold (* 2 dat))

;; the state of the application to keep:
;; - the word that is been built
;; - the last time  we added a bit
(def ^:dynamic *state* (atom
                        {:time (System/currentTimeMillis) ;; the reading time
                         :word []}))                      ;; the letters that are been read

(defn init-state "Initialize the state of the program"
  []
  (reset! *state* {:time (System/currentTimeMillis)
                   :word []}))

(fact
  (binding [*state* (atom {:word [[0 0 0]]})]
    (init-state) => (contains {:word []})
    @*state*      => (contains {:word []})))

(defn read-morse
  "Given a suite of bits, return the words"
  [& b]
  (map m/bits-2-letters b))

(fact "read-morse"
  (read-morse [0 0 0] [1 1 1] [0 0 0]) => [\s \o \s]
  (read-morse [0 0 0 0] [0] [0 1 0 0] [0 1 0 0] [1 1 1]) => [\h \e \l \l \o]
  (read-morse [0 0 0 0] [0] [0 1 0 0] [0 1 0 0] [1 1 1] [] [0 0 0] [1 1 1] [0 0 0]) => [\h \e \l \l \o nil \s \o \s]
  (read-morse [0 0 0 0 0]) => [nil])

(defn init-new-word
  "Init the current state of the application"
  ([]
     (swap! *state* (fn [o] {:time (System/currentTimeMillis)
                            :word  (conj (o :word) [])})))
  ([n]
     (swap! *state* (fn [o] {:time (System/currentTimeMillis)
                            :word  (conj (o :word) [n])}))))

(fact
  (binding [*state* (atom {:word [[:some-previous-val]]})]
    (init-new-word) => (contains {:word [[:some-previous-val] []]})))

(fact
  (binding [*state* (atom {:word [[:some-previous-val]]})]
    (init-new-word :val) => (contains {:word [[:some-previous-val] [:val]]})))

(defn read-morse-word
  "Read the word from the global state and update the global list of words read"
  []
  (let [bits (:word @*state*)]
    (apply read-morse bits)))

(fact
  (binding [*state* (atom {:word [[0 0 0] [1 1 1] [0 0 0]]})]
    (read-morse-word) => [\s \o \s]))

(defn compute-bit "Given a duration, compute the bit as 0 or 1"
  [duration]
  (cond (<= duration 19) nil
        (<= 20 duration dit) 0
        (< dit duration dat) 1
        :else nil))

(fact "compute-bit"
  (compute-bit 10) => nil
  (compute-bit 20) => 0
  (compute-bit 50) => 0
  (compute-bit 51) => 1
  (compute-bit 149) => 1
  (compute-bit 150) => nil)

(defn add-bit
  "Update the state with the newly read signal."
  [duration]
  (if-let [signal (compute-bit duration)]
    (swap! *state* (fn [o]
                     (let [i (dec (count (:word o)))]
                       (update-in o [:word i] conj signal))))))

(fact "add-bit"
  (binding [*state* (atom {:word [[]]})]
    (add-bit 19) => nil
    (add-bit 20) => {:word [[0]]}
    (add-bit 20) => {:word [[0 0]]}
    (add-bit 51) => {:word [[0 0 1]]}))

(def morse-reading nil)

(defn beyond-threshold? "Given a duration, compute if the threshold is reached or not."
  [d]
  (<= threshold d))

(fact
  (beyond-threshold? 50) => false
  (beyond-threshold? 1000) => true)

;; dispatch on the signal send by the button
(defmulti morse-reading (fn [signal duration]
                          [signal (if (beyond-threshold? duration) :new-word :same-word)]))

(defmethod morse-reading [LOW :same-word]
  [_ _])

(defmethod morse-reading [HIGH :new-word]
  [_ duration]
  (if-let [cs (compute-bit (- duration threshold))]
    (init-new-word cs)
    (init-new-word)))

(fact "morse-reading - new word"
  (binding [*state* (atom {:word [[]]})]
    (morse-reading HIGH threshold) => (contains {:word [[] []]})
    (morse-reading HIGH (+ threshold dit)) => (contains {:word [[] [] [0]]})
    (morse-reading HIGH (+ threshold (dec dat))) => (contains {:word [[] [] [0] [1]]})))

(defmethod morse-reading [HIGH :same-word]
  [_ duration]
  (add-bit duration))

(fact "morse-reading - same word"
  (binding [*state* (atom {:word [[]]})]
    (morse-reading HIGH 50) => {:word [[0]]}
    (morse-reading HIGH 50) => {:word [[0 0]]}
    (morse-reading HIGH 140) => {:word [[0 0 1]]}))

(defmethod morse-reading [LOW :new-word]
  [_ _]
  (init-new-word))

(fact "morse-reading - new word"
  (binding [*state* (atom {:word [[1 1 1]]})]
    (morse-reading LOW 1000) => (contains {:word [[1 1 1] []]})
    (morse-reading LOW 1100) => (contains {:word [[1 1 1] [] []]})))

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

  (def device-board "/dev/ttyACM0")
  (System/setProperty "gnu.io.rxtx.SerialPorts" device-board)
  (def board (arduino :firmata device-board))
  board
  (enable-pin board :digital pin-button)
  (pin-mode board pin-led OUTPUT)
  (pin-mode board pin-button INPUT)

  ;; to check manually some init stuff
  (digital-write board pin-led HIGH)
  (digital-write board pin-led LOW)

  (init-state)
  (read-morse-from-button board (System/currentTimeMillis) 60000)

  (p/pprint (read-morse-word))

  (close board))
