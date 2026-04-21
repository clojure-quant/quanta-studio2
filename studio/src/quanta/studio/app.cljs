(ns quanta.studio.app
  (:require
   [frontend.css :refer [css-loader]]
   [reitit.frontend.easy :as rfe]
   [shadowx.core :refer [get-resource-path]]))

(defn wrap [page match]
  [:<>
   [:link {:rel "stylesheet"
           :href "/r/quanta-studio-main.css"}]
   [:div
   [css-loader (get-resource-path)]
   [page match]]])

(defn home-page [_]
  [:div.quanta-main-page
   [:div.quanta-main-content
    [:div.quanta-main-grid
     [:h1.quanta-main-title
      "Quanta Studio"]
     [:a.quanta-main-link-box {:href (rfe/href 'reval.page.repl/repl-page)}
      "repl"]
     [:a.quanta-main-link-box {:href "/tradingview"}
      "tradingview"]
     [:a.quanta-main-link-box {:href "/tradingview-events"}
      "tradingview event browser"]]]])

(def routes
  [["/" {:name ::frontpage
         :view home-page}]
   ["/repl" {:name 'reval.page.repl/repl-page}]
   ])

