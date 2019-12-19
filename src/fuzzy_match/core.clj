(ns fuzzy-match.core)

;; Levenshtein
(declare levenshtein)


(defn- levenshtein-raw
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


(def ^:private levenshtein (memoize levenshtein-raw))


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
   If optional :ignore-chars is provided, those characters will be removed from both
   strings, the latter string will be split at any occurrence of each of those chars
   and the parts permuted. The Levenshtein distance returned is the minimum between
   the stripped first string and the permutations of the stripped, split latter string."
  [s1 s2 & {:keys  [ignore-chars] :or {ignore-chars nil}}]

  (case ignore-chars
    
    nil (levenshtein s1 s2)
    
    (let [src  (strip s1 ignore-chars)
          perms (-> (clojure.string/split s2 (str->re ignore-chars))
                    permutations)
          tgts (distinct (map #(apply str %) perms))]
      (apply min (map #(levenshtein src %) tgts)))))
