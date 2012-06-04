(ns arduino-lab.morse
  "Morse led"
  (:use :reload-all clodiuno.core)
  (:use :reload-all clodiuno.firmata))

;; 0 represents short signal
;; 1 represents long signal
;; Intermational morse from http://en.wikipedia.org/wiki/Morse_code
(def letters {\a [0 1]
              \b [1 0 0 0]
              \c [1 0 1 0]
              \d [1 0 0]
              \e [0]
              \f [0 0 1 0]
              \g [1 1 0]
              \h [0 0 0 0]
              \i [0 0]
              \j [0 1 1 1]
              \k [1 0 1]
              \l [0 1 0 0]
              \m [1 1]
              \n [1 0]
              \o [1 1 1]
              \p [0 1 1 0]
              \q [1 1 0 1]
              \r [0 1 0]
              \s [0 0 0]
              \t [1]
              \u [0 0 1]
              \v [0 0 0 1]
              \w [0 1 1]
              \x [1 0 0 1]
              \y [1 0 1 1]
              \z [1 1 0 0]})

;; time
(def short-pulse 100)
(def long-pulse 250)
(def letter-delay 500)

;; pin number
(def pin-number 13)

;; functions

(defn blink
  "Given a board and time, make the led blink a given time"
  [board time]
  (digital-write board pin-number HIGH)
  (Thread/sleep time)
  (digital-write board pin-number LOW)
  (Thread/sleep time))

(defn blink-letter
  "Given a letter, blink according to the sequence of 0 (short pulse) and 1 (long pulse)"
  [board letter]
  (doseq [i letter]
    (if (= i 0)
      (blink board short-pulse)
      (blink board long-pulse)))
  (Thread/sleep letter-delay))

(defn morse
  "Given a word, make the led blink in morse for each letter (no upper case, no punctuation)"
  [board word]
  (doseq [l word]
    (blink-letter board (letters l))))

(defn main-morse
  "Given a serial device entry:
   - open the board
   - make the led blink in morse the word word
   - then close the board"
  [board-serial-port word]
  (let [board (arduino :firmata board-serial-port)]
    ;;allow arduino to boot
    (Thread/sleep 5000)
    (pin-mode board pin-number OUTPUT)

    (morse board word)

    (close board)))

(comment
  "For the repl - step by step"
  (def device-board "/dev/ttyACM0")
  (System/setProperty "gnu.io.rxtx.SerialPorts" device-board)
  (def board (arduino :firmata device-board))
  board
  (pin-mode board 13 OUTPUT)
  (morse board "hello world")
  (morse board "sos")
  (close board))

(comment
  "for the repl - one shot"
  (def device-board "/dev/ttyACM0")
  (System/setProperty "gnu.io.rxtx.SerialPorts" device-board)
  (main-morse "device-board" hello world))
