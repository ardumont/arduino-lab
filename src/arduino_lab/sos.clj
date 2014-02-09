(ns arduino-lab.sos
  "Blinking sos led."
  (:use :reload-all clodiuno.core)
  (:use :reload-all clodiuno.firmata))

(def short-pulse 250)
(def long-pulse 500)
(def letter-delay 1000)

(def letters {:s  [0 0 0]
              :o  [1 1 1]})

(defn blink "Given a board and time, make the led blink"
  [board time]
  (digital-write board 13 HIGH)
  (Thread/sleep time)
  (digital-write board 13 LOW)
  (Thread/sleep time))

(defn blink-letter
  [board letter]
  (doseq [i letter]
    (if (= i 0)
      (blink board short-pulse)
      (blink board long-pulse)))
  (Thread/sleep letter-delay))

(defn sos "The main algorithm to make the led from the board light the sos"
  [board]
  (doseq [_ (range 3)]
    (blink-letter board (:s letters))
    (blink-letter board (:o letters))
    (blink-letter board (:s letters))))

(defn main
  "Given a serial device entry, open the board, make it do some sos light and then close the board"
  [board-serial-port]
  (let [board (arduino :firmata board-serial-port)]
    ;;allow arduino to boot
    (Thread/sleep 5000)
    (pin-mode board 13 OUTPUT)

    (sos board)

    (close board)))

(comment "For the repl"
  (def board (arduino :firmata "/dev/ttyACM0"))
  (pin-mode board 13 OUTPUT)
  (digital-write board 13 HIGH)
  (digital-write board 13 LOW)
  (sos board)
  (close board))
