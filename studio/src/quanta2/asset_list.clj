(ns quanta2.asset-list
  (:require
   [datahike.api :as d]
   [quanta.market.asset.datahike :refer [get-list]]))

(defn only-names [lists]
  (map :lists/name lists))

(defn get-lists [dbconn]
  (-> '[:find [(pull ?id [:lists/name]) ...]
        :in $ ; ?asset-symbol
        :where
        [?id :lists/name _]]
      (d/q @dbconn)
      (only-names)))

(defn show-lists [{:keys [asset-db]}]
  (get-lists asset-db))

(defn assets [{:keys [asset-db]} {:keys [asset list]}]
  (->>  (concat
         (if list (:lists/asset (get-list asset-db list)) [])
         (cond
           (string? asset) [asset]
           (or (seq? asset) (vector? asset)) asset
           :else []))
        (into #{})))

(comment
  (require '[modular.system :refer [system]])
  (def ctx (:ctx system))
  (get-lists (:assetdb ctx))
  (get-list (:assetdb ctx) "")
  (get-list (:assetdb ctx) "flo")
  (assets ctx {:asset "AEE.AX"})
  (assets ctx {:asset ["AEE.AX" "CWI.VI"]})
  (assets ctx {:list "flo"})
  (assets ctx {:list "flo" :asset "AEE.AX"})
  (assets ctx {:list "flo" :asset ["AEE.AX" "CWI.VI"]})

  (map println (assets ctx {:list "flo"}))
 ; 
  )
