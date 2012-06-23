(ns arduino-lab.push-button
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

(defn react-to-button
  "Given a board, read from the button"
  [board]
  (while true
    (let [status-button (digital-read board pin-button)]
      (digital-write board pin-led status-button)
      (Thread/sleep 10))))

(comment
  "For the repl - step by step"
  (def device-board "/dev/ttyACM0")
  (System/setProperty "gnu.io.rxtx.SerialPorts" device-board)
  (def board (arduino :firmata device-board))
  (enable-pin board :digital pin-button)
  board
  (pin-mode board pin-led OUTPUT)
  (pin-mode board pin-button INPUT)

  (react-to-button board)

  (close board))

