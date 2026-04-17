(ns quanta.studio.plot
  (:require
   [tablecloth.api :as tc]
   [rtable.plot.cheetah :refer [cheetah-ds]]))
         
(defn asset-table [assets]
  (let [ds (tc/dataset assets)
        opts {:style {:width "100%"} ; make it as wide as the notebook-viewer
              :columns [{:field :asset/symbol :caption "Symbol" :width 90}
                        {:field :asset/name :caption "Name" :width 220}
                        {:field :asset/category :caption "category" :width 90}
                        {:field :asset/exchange :caption "exchange" :width 90}
                        ]}]
     (cheetah-ds opts ds)))