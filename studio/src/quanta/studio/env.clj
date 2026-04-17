(ns quanta.studio.env
  (:require
   [modular.system :refer [system]]))


(def env (:ctx system))

(keys env)


(def env
  {:assetdb (system :assetdb)
   :bar-db (:barsource (system :bardb-sa))
   ;:bar-db (system :bardb)
   
   :tradingview  (:tradingview (system :config))
   :default {:client-id "tradingview.com"
             :user-id "user-444"}
   
   
   })