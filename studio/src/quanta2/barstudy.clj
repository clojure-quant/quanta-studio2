(ns quanta2.barstudy
  (:require
   [taoensso.timbre :as timbre :refer [debug info warn error]]
   [tick.core :as t]
   [tablecloth.api :as tc]
   [missionary.core :as m]
   [quanta.bar.protocol :as b]
   [quanta.calendar.window :as w]
   [quanta.missionary :refer [run-parallel]]
   [quanta2.asset-list :refer [assets]]
   [quanta2.protocol]
   [quanta2.algo :refer [get-algo]]))

(defn bar-all->map [bar-all-ds]
  (->> (tc/group-by bar-all-ds :asset {:result-type :as-map})
       (map (fn [[asset ds]]
              [asset (tc/order-by ds :date)]))
       (into {})))


(defn run-algo [algo algo-opts assets bar-dict]
  (let [make-task (fn [asset]
                    (let [bar-ds (get bar-dict asset)]
                      ;(m/sp 
                      (m/via m/cpu
                             (try
                               {:data (quanta2.protocol/calculate algo algo-opts bar-ds)}
                               (catch Exception ex
                                 {:error asset})))))
        calc-tasks (map make-task assets)]
    (run-parallel calc-tasks 8)))

(defn concat-dss [dss]
  ; this style of concatenating datasets is more robust than apply tc/concat because it handles errors gracefully.
  ; exceptions at this level can appear because of lazy-evaluation.
  (let [empty-ds (tc/dataset {})]
    (reduce (fn [acc ds]
              (try
                (tc/concat acc ds)
                (catch Exception ex
                  (error "error concatenating datasets: " (ex-message ex))
                  acc)))
            empty-ds
            dss)))


(defn get-signals [algo algo-opts results]
  ;(doall (map (fn [r] (info "result:\r\n" r)) results))
  (info "concatenating signals.."   )
  (let [all-ds (concat-dss results)]
    (info "concatenating signals..done.")
    (quanta2.protocol/select-signal algo algo-opts all-ds)))

(defn result-ok? [result-ds]
  (try
    (> (tc/row-count result-ds) 0)
    (catch Exception ex
      (error "error checking result: " (ex-message ex))
      false)))


(defn run-algos [env {:keys [algo asset list algo-opts calendar window] :as opts}]
  (m/sp
   (let [algo-kw algo
         algo (get-algo algo-kw)
         _ (assert algo (str "run-algo algo " algo-kw " not available. "))
         assets (->> (assets env (select-keys opts [:list :asset]))
                     (into []))
         _ (info "running on assets: " (count assets))
         bar-all-ds (m/? (b/get-bars (:bar-db env) {:asset assets :calendar calendar}
                                     (select-keys window [:start :end])))
         bar-dict (bar-all->map bar-all-ds)
         _ (info "bars loaded for assets" (count (keys bar-dict)))
         result-seq (m/? (run-algo algo algo-opts assets bar-dict))
         results (->>  result-seq
                       (remove (fn [r] (:error r)))
                       (map :data))
         err-seq (->> result-seq
                      (filter (fn [r] (:error r)))
                      (map :error))]
     (info "run finished. success: " (count results) " errors: " (count err-seq))
     ;bar-all-ds
     ;bar-dict
     {:errors err-seq
      :results results})))

(defn combine-signals [{:keys [algo algo-opts] :as opts} {:keys [results errors]}]
  (let [algo-kw algo
        algo (get-algo algo-kw)
        _ (assert algo (str "run-algo algo [" algo-kw "] not available. "))
        results-ok (filter result-ok? results)]
    (get-signals algo algo-opts results-ok)))




#_(defn calculate-window
    "calculates an algo over a window. 
   will pre-load bars and cut them after calculation is finished."
    [env algo algo-opts window]
    (m/sp
     (let [window-original-range (w/window->close-range window)
           target-start-date (:start window-original-range)
           bar-db (:bar-db env)
           skip-n (preload-n algo algo-opts)
           asset (:asset algo-opts)
         ;_ (println "calc-window " (w/window->close-range window))
           window-extended (w/window-extend-left window skip-n)
           _ (println "calculate window original: " window-original-range
                      "skip-n: " skip-n
                      " extended: " (w/window->close-range window-extended))
           bar-ds (m/? (b/get-bars bar-db {:asset asset} window-extended))
           _ (println "bars loaded: " (tc/row-count bar-ds))
           bar-ds (clone-ds bar-ds) ; clone to fix duckdb transit issue.
           ]
       (if (= 0 (tc/row-count bar-ds))
         bar-ds
         (let [algo-ds (calculate algo algo-opts bar-ds)
             ; just removing the preload-count rows does reduce the original 
             ; window in case in the preload-window were missing bars.
             ; _ (println "skipping preload: " skip-n "ds-full: " (tc/row-count algo-ds))
             ;algo-ds  (tc/select-rows algo-ds (range skip-n (tc/row-count algo-ds)))
               algo-ds  (if (> skip-n 0)
                          (tc/select-rows algo-ds
                                          (fn [row]
                                            (t/>= (:date row) target-start-date)))
                          algo-ds)]
         ;(println "algo-opts: " algo-opts)
           algo-ds)))))


#_(defn calculate-signals [env algo algo-opts window]
    (m/sp
     (let [bar-ds (m/? (calculate-window env algo algo-opts window))]
       (tc/select-rows bar-ds (signal-fn algo)))))

#_(defn screen-signals [env algo algo-opts window assets]
    (m/sp
     (let [calculate-asset (fn [asset]
                             (let [algo-opts (assoc algo-opts :asset asset)]
                               (println "asset: " asset " algo-opts: " algo-opts)
                               (calculate-signals env algo algo-opts window)))
           tasks (map calculate-asset assets)
           ds (m/? (apply m/join
                          (fn [& dss]
                            (apply tc/concat dss))
                          tasks))]
       (-> ds
           (tc/order-by :date :desc)
           (tc/add-column :idx (range (tc/row-count ds)))))))

#_(defn calculate-assets [env algo window assets]
    (m/sp
     (let [tasks (map #(calculate-window env algo % window) assets)
           ds (m/? (apply m/join
                          (fn [& dss]
                            (apply tc/concat dss))
                          tasks))]
       ds)))

(comment
  


  (->> (get-window-trailing [:forex :h] 10)
       w/window->close-range)

  (require '[modular.system])
  (def env (modular.system/system :env))
  env

  (def w (get-window-trailing [:forex :h] 100))
  w
  (m/? calculate-window env)

;  
  )