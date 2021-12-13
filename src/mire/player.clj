(ns mire.player)

(def ^:dynamic *current-room*)
(def ^:dynamic *inventory*)
(def ^:dynamic *name*)
(def ^:dynamic *group*)

(def prompt "> ")
(def streams (ref {}))
(def groups (ref {}))
(defn carrying? [thing]
  (some #{(keyword thing)} @*inventory*))
