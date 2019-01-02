(ns xyz.homebrew.app.strategy.core
  (:gen-class
    :extends xyz.homebrew.core.AbstractTrader
    :name xyz.homebrew.app.strategy.ClojureTrader)
  (:import (xyz.homebrew.core Market)))

(defn -init [this]
  (println "clojure init"))

(defn -onMarketDepthUpdate [this symbol ^Market market]
  (println "clojure found")
  (-> market .offers .getVolume println)
  (-> market .bids .getVolume println))
