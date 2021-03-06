(ns compound.secondary-indexes.many-to-one
  (:require [compound.custom-key :as cu]
            [compound.secondary-indexes :as csi]
            [clojure.spec.alpha :as s]))

(s/def ::key keyword?)
(s/def ::custom-key keyword?)
(s/def ::id any?)

(defmethod csi/spec :compound/many-to-one
  [_]
  (s/keys :req-un [(or ::key ::custom-key)]
          :opt-un [::id]))

(defmethod csi/empty :compound/many-to-one
  [index-def]
  {})

(defmethod csi/id :compound/many-to-one
  [index-def]
  (or (:id index-def)
      (:custom-key index-def)
      (:key index-def)))

(defmethod csi/add :compound/many-to-one
  [index index-def added]
  (let [{:keys [key custom-key]} index-def
        key-fn (or key (partial cu/custom-key-fn custom-key))
        new-index (reduce (fn add-items [index item]
                            (let [ks (key-fn item)
                                  kvs (reduce (fn [kvs k]
                                                (let [existing-item (get index k)]
                                                  (if existing-item
                                                    (throw (ex-info (str "Duplicate key " k " in secondary-index " (csi/id index-def))
                                                                    {:existing-item existing-item, :new-item item}))
                                                    (conj kvs k item))))
                                              []
                                              ks)]
                              (apply assoc! index kvs)))
                          (transient index)
                          added)]
    (persistent! new-index)))

(defmethod csi/remove :compound/many-to-one
  [index index-def removed]
  (let [{:keys [key custom-key]} index-def
        key-fn (or key (partial cu/custom-key-fn custom-key))
        new-index (reduce (fn remove-items [index item]
                            (let [ks (key-fn item)]
                              (println ks)
                              (apply dissoc! index ks)))
                          (transient index)
                          removed)]
    (persistent! new-index)))
