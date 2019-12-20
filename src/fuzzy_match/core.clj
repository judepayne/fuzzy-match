(ns fuzzy-match.core
  (:require [clojure.core.memoize :as memo]))

;; Levenshtein
(declare levenshtein)


(defn levenshtein-raw
  "Calculates the levenshtein distane between two strings"
  [a b]
  (let
      [len-a (. (into [] a) length)
       len-b (. (into [] b) length)
       cost (if (= (first a) (first b)) 0 1)]
    (if (or (= len-a 0) (= len-b 0))
      (+ len-a len-b)
      (min
       (+ (levenshtein (rest a) b) 1)
       (+ (levenshtein a (rest b)) 1)
       (+ (levenshtein (rest a) (rest b)) cost)))))



(def default-lu-threshold 24)
;; We memoize the levenshtein function since it is slow.
;; Since this is a library for us in someone else's program, we use
;; clojure.core.memoize rather than the simple memoize function. This allows
;; for the size of the memoization cache to be controlled.
(def ^:dynamic *levenshtein* (memo/lu levenshtein-raw :lu/threshold default-lu-threshold))
;; To alter lu-threshold from another namespace (i.e. when this is a library), use
;; (binding [fuzzy-match.core/*levenshtein* (memo/lu levenshtein-raw :lu/threshold <my-value>)]
;;   (fuzzy-match ... )



;; Permutations
(defn- permutations
  "Returns all permuations of items in the collection."
  [coll]
  (if (= 1 (count coll))
    (list coll)
    (for [head coll
          tail (permutations (disj (set coll) head))]
      (cons head tail))))


;; Utility functions
(defn- strip
  "Removes each of the characters in the chars string from the s string."
 [s chars]
  (apply str (remove #((set chars) %) s)))


(defn- str->re
  "Converts a string to a regular expression."
  [s]
  (re-pattern (str "[" s "]")))


;; Public Clojure api
(defn fuzzy-match
  "Takes two strings and returns the Levenshtein distance between them.
   If ingore-characters, a third string is specified, the characters in that string
   will be removed from the first string, and used to split the second string, then
   removed. After splitting, the parts of the second string will be rotated into all
   permutations, and the minimum Levenshtein distance between the first string and
   each of the rearrangements of the second string returned."
  ([s1 s2]
   (fuzzy-match s1 s2 nil))

  ([s1 s2 ignore-characters]
   (case ignore-characters
     
     nil (levenshtein s1 s2)
     
     (let [src  (strip s1 ignore-characters)
           perms (->> (clojure.string/split s2 (str->re ignore-characters))
                      (filter (complement empty?))  ;; remove empty string parts
                      permutations                 
                      (map #(apply str %)))]        ;; reassemble as strings after permutation

       (apply min (map #(*levenshtein* src %) perms))))))
