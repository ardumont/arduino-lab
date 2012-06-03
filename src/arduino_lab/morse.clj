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
(def short-pulse 250)
(def long-pulse 500)
(def letter-delay 1000)

;; pin number
(def pin-number 13)

;; functions

(defn blink "Given a board and time, make the led blink"
  [board time]
  (digital-write board pin-number HIGH)
  (Thread/sleep time)
  (digital-write board pin-number LOW)
  (Thread/sleep time))

(defn blink-letter "Given a letter, blink accordingly 1 is a long pulse, 0 a short one."
  [board letter]
  (doseq [i letter]
    (if (= i 0)
      (blink board short-pulse)
      (blink board long-pulse)))
  (Thread/sleep letter-delay))

(comment "For the repl"
  (def board (arduino :firmata "/dev/ttyS42"))
  (pin-mode      board pin-number OUTPUT)
  (digital-write board pin-number HIGH)
  (digital-write board pin-number LOW))

(defn morse "Given a word, make the led blink in morse (no upper case, no punctuation)"
  [board word]
  (doseq [l word]
    (blink-letter board (letters l))))

(defn main
  "Given a serial device entry, open the board, make it do some sos light and then close the board"
  [board-serial-port]
  (let [board (arduino :firmata board-serial-port)]
    ;;allow arduino to boot
    (Thread/sleep 5000)
    (pin-mode board pin-number OUTPUT)

    (morse board "hello world")

    (close board)))
