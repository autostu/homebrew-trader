(ns xyz.homebrew.app.strategy.core
  (:gen-class
    :extends xyz.homebrew.core.AbstractTrader
    :name xyz.homebrew.app.strategy.ClojureTrader))

(defn -init [this]
  (println "clojure init"))

(defn -spotted [this market]
  true)

(defn -execute [this]
  (println "clojure executed"))