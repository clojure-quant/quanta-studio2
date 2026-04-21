(ns quanta2.protocol
  (:require [clojure.string :as str]))


(defprotocol barstudy
  (algoname [_])
  ; calculate
  (calculate [_ opts ds])
  (select-signal [_ opts ds])
    
  ;(preload-n [_ opts])
  )

(defprotocol barsignal
  (signal-fn [_]))




(comment 
  (defrecord longus []
    quanta2.protocol/barstudy
    (algoname [this] (println "this: " this) "longus")
    (calculate [_ opts asset] {:asset asset :add (+ 1 3)}))
(algoname (longus.))
(calculate (longus.) :opts :asset)  
  ;
  
  )


