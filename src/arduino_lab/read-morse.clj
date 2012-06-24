(ns arduino-lab.read-morse
  "Read morse from the circuit"
  (:use [midje.sweet])
  (:use :reload-all clodiuno.core)
  (:use :reload-all clodiuno.firmata)
  (:require [arduino-lab.morse :as m]
            [clojure.string :as s]
            [clojure.pprint :as p]))

;; reading morse: http://wwwhome.cs.utwente.nl/~ptdeboer/ham/rscw/algorithm.html
;; circuit: http://arduino.cc/en/Tutorial/Button

;; setup of the pin

(def pin-led    13)
(def pin-button 7)

;; duration of morse signal
(def dot 50)
(def dash (* 3 dot))

;; beyond this threshold, we get a new letter
(def threshold-new-letter (* 5 dot))

;; beyond this threshold, we get a new word
(def threshold-new-word   (* 7 dot))

;; the state of the application to keep:
;; - the word that is been built
;; - the last time  we added a bit
(def ^:dynamic *state* (atom
                        {:time (System/currentTimeMillis) ;; the reading time
                         :word [[]]}))                    ;; the letters that are been read

(defn init-state "Initialize the state of the program"
  []
  (reset! *state* {:time (System/currentTimeMillis)
                   :word [[]]}))

(fact
  (binding [*state* (atom {:word [[0 0 0]]})]
    (init-state) => (contains {:word [[]]})
    @*state*     => (contains {:word [[]]})))

(defn read-morse
  "Given a suite of bits, return the words"
  [& b]
  (map m/bits-2-letters b))

(fact "read-morse"
  (read-morse [0 0 0] [1 1 1] [0 0 0]) => [\s \o \s]
  (read-morse [0 0 0 0] [0] [0 1 0 0] [0 1 0 0] [1 1 1]) => [\h \e \l \l \o]
  (read-morse [0 0 0 0] [0] [0 1 0 0] [0 1 0 0] [1 1 1] [] [0 0 0] [1 1 1] [0 0 0]) => [\h \e \l \l \o nil \s \o \s]
  (read-morse [0 0 0 0 0]) => [nil])

(defn read-morse-word "Reading the state at the keyword :word and translate it into seq of letter"
  []
  (map (partial apply read-morse) (:word @*state*)))

(fact "read-morse-word"
  (binding [*state* (atom {:word [[[0 0 0] [1 1 1] [0 0 0]] [[1 1 1] [0 0 0] [0 0 0]]]})]
    (read-morse-word)) => [[\s \o \s] [\o \s \s]])

(defn init-new-letter
  "Init the current state of the application"
  ([]
     (init-new-letter nil))
  ([n]
     (swap! *state* (fn [o]
                      (let [i (dec (count (:word o)))
                            nw (update-in o [:word i] conj (if n [n] []))]
                        {:time (System/currentTimeMillis)
                         :word (:word nw)})))))

(fact "init-new-letter without init value"
  (binding [*state* (atom {:word [[[:some-previous-val]]]})]
    (init-new-letter) => (contains {:word [[[:some-previous-val] []]]})))

(fact "init-new-letter with init value"
  (binding [*state* (atom {:word [[[:some-previous-val]]]})]
    (init-new-letter :val) => (contains {:word [[[:some-previous-val] [:val]]]})))

(defn init-new-word
  "Init the current state of the application"
  ([]
     (init-new-word nil))
  ([n]
     (swap! *state* (fn [o]
                      {:time (System/currentTimeMillis)
                       :word (conj (:word o) [(if n [n] [])])}))))

(fact "init-new-letter without init value"
  (binding [*state* (atom {:word [[[:some-previous-val]]]})]
    (init-new-word) => (contains {:word [[[:some-previous-val]] [[]]]})))

(fact "init-new-letter with init value"
  (binding [*state* (atom {:word [[[:some-previous-val]]]})]
    (init-new-word :val) => (contains {:word [[[:some-previous-val]] [[:val]]]})))

(defn compute-bit "Given a duration, compute the bit as 0 or 1"
  [duration]
  (cond (< duration dot)                              nil
        (<= dot duration (dec dash))                  0
        (<= dash duration (dec threshold-new-letter)) 1))

(fact "compute-bit"
  (compute-bit (dec dot))                  => nil
  (compute-bit dot)                        => 0
  (compute-bit (dec dash))                 => 0
  (compute-bit dash)                       => 1
  (compute-bit (dec threshold-new-letter)) => 1)

(defn add-bit
  "Update the state with the newly read signal."
  [duration]
  (if-let [signal (compute-bit duration)]
    (swap! *state* (fn [o]
                     (let [i (dec (count (:word o)))
                           j (dec (count (last (:word o))))]
                       (update-in o [:word i j] conj signal))))))

(fact "add-bit"
  (binding [*state* (atom {:word [:some-previous-letter [[]]]})]
    (add-bit (dec dot))                  => nil
    (add-bit dot)                        => {:word [:some-previous-letter [[0]]]}
    (add-bit (dec dash))                 => {:word [:some-previous-letter [[0 0]]]}
    (add-bit dash)                       => {:word [:some-previous-letter [[0 0 1]]]}
    (add-bit (dec threshold-new-letter)) => {:word [:some-previous-letter [[0 0 1 1]]]}))

(defn beyond-threshold? "Given a duration, compute if the threshold is reached or not."
  [d]
  (cond (< d threshold-new-letter) :same-letter
        (< d threshold-new-word)   :new-letter
        :else :new-word))

(fact "beyond-threshold"
  (beyond-threshold? dot)                  => :same-letter
  (beyond-threshold? dash)                 => :same-letter
  (beyond-threshold? threshold-new-letter) => :new-letter
  (beyond-threshold? threshold-new-word)   => :new-word)

;; hack for the multi-method definition to be recomputed each time chanes occur
(def morse-reading nil)

;; dispatch on the signal send by the button
(defmulti morse-reading (fn [signal duration] [signal (beyond-threshold? duration)]))

(defmethod morse-reading [HIGH :same-letter]
  [_ duration]
  (add-bit duration))

(fact "morse-reading HIGH :same-letter"
  (binding [*state* (atom {:word [[[]]]})]
    (morse-reading HIGH dot) => (contains {:word [[[0]]]})
    (morse-reading HIGH dot) => (contains {:word [[[0 0]]]})
    (morse-reading HIGH dash) => (contains {:word [[[0 0 1]]]})))

(defmethod morse-reading [HIGH :new-letter]
  [_ duration]
  (if-let [cs (compute-bit (- duration threshold-new-word))]
    (init-new-letter cs)
    (init-new-letter)))

(fact "morse-reading HIGH :new-letter"
  (binding [*state* (atom {:word [[[]]]})]
    (morse-reading HIGH threshold-new-letter) => (contains {:word [[[] []]]})))

(defmethod morse-reading [HIGH :new-word]
  [_ duration]
  (if-let [cs (compute-bit (- duration threshold-new-word))]
    (init-new-word cs)
    (init-new-word)))

(fact "morse-reading HIGH :new-word"
  (binding [*state* (atom {:word [[:previous-word]]})]
    (morse-reading HIGH threshold-new-word) => (contains {:word [[:previous-word] [[]]]})
    (morse-reading HIGH threshold-new-word) => (contains {:word [[:previous-word] [[]] [[]]]})))

(defmethod morse-reading [LOW :same-letter]
  [_ _])

(fact "morse-reading LOW :same-letter"
  (morse-reading LOW dot) => nil)

(defmethod morse-reading [LOW :new-letter]
  [_ _]
  (init-new-letter))

(fact "morse-reading LOW :new-letter"
  (binding [*state* (atom {:word [[[1 1 1]]]})]
    (morse-reading LOW threshold-new-letter)         => (contains {:word [[[1 1 1] []]]})
    (morse-reading LOW (+ threshold-new-letter dot)) => (contains {:word [[[1 1 1] [] []]]})))

(defmethod morse-reading [LOW :new-word]
  [_ _]
  (init-new-word))

(fact "morse-reading LOW :new-word"
  (binding [*state* (atom {:word [[:previous-word]]})]
    (morse-reading LOW threshold-new-word)) => (contains {:word [[:previous-word] [[]]]}))

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
