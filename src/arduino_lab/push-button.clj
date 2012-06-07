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
  (println "read-morse" board)
  (while true
    (let [status-button (digital-read board pin-button)]
      (println status-button)
      (digital-write board pin-led status-button)
      (Thread/sleep 10))))

(defn main
  "Given a serial device entry:
   - open the board
   - launch the circuit that permits the led to stay lightened as long as the button is pushed
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

    ;; read the morse as we go
    (react-to-button board)

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

  (react-to-button board)

  (close board))

(comment
  "for the repl - one shot"
  (def device-board "/dev/ttyACM0")
  (System/setProperty "gnu.io.rxtx.SerialPorts" device-board)
  (main device-board))

