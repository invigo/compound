(ns compound.secondary-indexes.one-to-many-nested
  (:require [compound.core :as c]
            [compound.secondary-indexes :as csi]
            [clojure.spec.alpha :as s]
            [clojure.string :as string]))

(s/def ::keys (s/coll-of keyword? :kind vector?))
(s/def ::custom-key keyword?)
(s/def ::id any?)

(defmethod csi/spec :compound/one-to-many-nested
  [_]
  (s/keys :req-un [(or ::keys ::custom-key)]
          :opt-un [::id]))

(defmethod csi/empty :compound/one-to-many-nested
  [index-def]
  {})

(defmethod csi/id :compound/one-to-many-nested
  [index-def]
  (or (:id index-def)
      (:custom-key index-def)
      (:keys index-def)))

(defmethod csi/add :compound/one-to-many-nested
  [index index-def added]
  (let [{:keys [keys custom-key]} index-def
        key-fn (if keys (apply juxt keys) (partial c/custom-key-fn custom-key))
        new-index (reduce (fn add-items [index item]
                            (let [ks (key-fn item)]
                              (update-in index ks (fnil conj #{}) item)))
                          index
                          added)]
    new-index))

(defmethod csi/remove :compound/one-to-many-nested
  [index index-def removed]
  (let [{:keys [keys custom-key]} index-def
        key-fn (if keys (apply juxt keys) (partial c/custom-key-fn custom-key))
        new-index (reduce (fn remove-items [index item]
                            (let [ks (key-fn item)]
                              (update-in index ks disj item)))
                          index
                          removed)]
    new-index))
