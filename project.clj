(defproject arduino-lab "1.0.0-SNAPSHOT"
  :description "Mess around with arduino from the comfort of the repl"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [clodiuno "0.0.3-SNAPSHOT"]
                 [serial-port "1.1.2"]
                 [midje "1.3.1"]]
  :native-dependencies [[org.clojars.samaaron/rxtx "2.2.0.1"]]
  :dev-dependencies [[native-deps "1.0.5"]
                     [com.intelie/lazytest "1.0.0-SNAPSHOT" :exclusions [swank-clojure]]]
  :jvm-opts ["-Djava.library.path=./native/linux/x86/"
             "-d32"])

