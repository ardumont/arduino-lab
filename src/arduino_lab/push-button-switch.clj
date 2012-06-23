(ns arduino-lab.push-button-switch
  "Push button - http://arduino.cc/en/Tutorial/Button"
  (:use [midje.sweet])
  (:use :reload-all clodiuno.core)
  (:use :reload-all clodiuno.firmata)
  (:require [arduino-lab.morse :as m]
            [clojure.string :as s]
            [clojure.pprint :as p]))

;; circuit: http://arduino.cc/en/Tutorial/Button

(def pin-led    13)
(def pin-button 7)

(def state (atom 0))
(def old-state (atom 0))

(defn rev "Reverse the state"
  [cv]
  (- 1 cv))

(fact
  (rev 0) => 1
  (rev 1) => 0)

(defn switch-button
  "Given a board, read from the button"
  [board]
  (while true
    (let [status-button (digital-read board pin-button)]
      ;; deal with state
      (if (and (= HIGH status-button) (not= @old-state (rev @state)))
        (do
          ;; reverse the current state
          (swap! state rev)
          ;; for stabilisation
          (Thread/sleep 100)))

      (reset! old-state @state)

      ;; let the light be... or not
      (digital-write board pin-led @state))))

(comment
  "For the repl - step by step"
  (def device-board "/dev/ttyACM0")
  (System/setProperty "gnu.io.rxtx.SerialPorts" device-board)
  (def board (arduino :firmata device-board))
  board
  (enable-pin board :digital pin-button)
  (pin-mode board pin-led OUTPUT)
  (pin-mode board pin-button INPUT)

  (switch-button board)
  (close board))

