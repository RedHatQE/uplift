(ns uplift.utils.algos)

(defn lmap
  "Sometimes you don't want to iterate through every item in a sequence
  This function will walk through the sequence applying f to each item
  and then applying checkfn to the result of f.  If/when checkfn returns
  a falsey value iteration will stop"
  [f checkfn seq]
  (lazy-seq
    (let [h (first seq)
          t (rest seq)
          result (f h)
          continue (checkfn result)]
      (if (and continue (not-empty t))
        (cons result (lmap f checkfn t))
        nil))))