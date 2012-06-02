(defproject arduino-lab "1.0.0-SNAPSHOT"
  :description "FIXME: write description"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [clodiuno "0.0.3-SNAPSHOT"]
                 [serial-port "1.1.2"]]
  :native-dependencies [[org.clojars.samaaron/rxtx "2.2.0.1"]]
  :dev-dependencies [[native-deps "1.0.5"]]
  :jvm-opts ["-Djava.library.path=./native/linux/x86/"
             "-d32"])

