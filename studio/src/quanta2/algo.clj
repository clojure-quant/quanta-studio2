(ns quanta2.algo
  (:require 
   [clojure.string :as str]
   [quanta2.protocol]))

(defonce algo-registry (atom {}))

(defn- kw->RecordSym
  "Turn :my-algo-name into MyAlgoNameAlgo"
  [k]
  (let [s (name k) ; keyword -> \"my-algo-name\"
        pascal (->> (str/split s #"-")
                    (remove str/blank?)
                    (map str/capitalize)
                    (apply str))]
    (symbol (str pascal "Algo"))))



(defmacro register-algo
  "Usage:
    (register-algo :algo-name
      (calc [_ opts] ...))

  Also accepts a map:
    (register-algo {:name :algo-name}
      (calc [_ opts] ...))

  Defines a defrecord <AlgoName>Algo implementing Algo, instantiates it,
  calls (name instance), and registers {name -> ->RecordCtor} in algo-registry."
  [spec & impl-body]
  (let [k (cond
            (keyword? spec) spec
            (map? spec)     (:name spec)
            :else           nil)]
    (when-not (keyword? k)
      (throw (ex-info "register-algo expects a keyword (e.g. :my-algo) or a map {:name :my-algo}"
                      {:got spec})))
    (let [rec-sym (kw->RecordSym k)
          algo-name-str (name k)
          ns-name (ns-name *ns*)
          ctor-sym (symbol (name ns-name) (str "->" rec-sym))
          map-ctor-sym (symbol (name ns-name) (str "map->" rec-sym))
          c (symbol (str rec-sym "."))
          has-calc? (some #(and (seq? %) (= 'calculate (first %))) impl-body)]
      (when-not has-calc?
        (throw (ex-info "register-algo body must include a (calc [this opts] ...) implementation"
                        {:name k :body impl-body})))
      `(do
         (defrecord ~rec-sym []
           quanta2.protocol/barstudy
           (~'algoname [~'_] ~algo-name-str)
           ~@impl-body)
         ; register algo
         (swap! algo-registry assoc ~k (~c))))))

(defn get-algo [algo-kw]
  (get @algo-registry algo-kw))

(defn available-algos []
  (keys @algo-registry))

(comment
  (kw->RecordSym :flo)

  (defrecord lingus []
    quanta2.protocol/barstudy
    (algoname [this] (println "this: " this) "lingus")
    (calculate [_ opts asset] {:asset asset :add (+ 1 3)}))

  (quanta2.protocol/calculate (lingus.) {} :ds)

  (macroexpand '(register-algo
                 :flo-test
                 (calculate [_ opts ds]
                            {:result 42
                             :opts opts})))

  (register-algo
   :hi
   (calculate [_ opts ds]
              {:result 42
               :opts opts}))


  (HiAlgo.)
  (map->HiAlgo nil)

  (quanta2.protocol/calculate (HiAlgo.) {} :ds)

  (quanta2.protocol/calculate  (get-algo :hi) {} :ds)

  ;
  )

