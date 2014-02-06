(defproject arduino-lab/arduino-lab "1.0.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [clodiuno "0.0.4-SNAPSHOT"]
                 [serial-port "1.1.2"]
                 [midje "1.4.0"]]
  :profiles {:dev
             {:dependencies
              [[native-deps "1.0.5"]]}}
  :min-lein-version "2.0.0"
  :native-dependencies [[org.clojars.samaaron/rxtx "2.2.0.1"]]
  :jvm-opts ["-Djava.library.path=./native/linux/x86/" "-d32"]
  :description "Mess around with arduino from the confort of the repl")
