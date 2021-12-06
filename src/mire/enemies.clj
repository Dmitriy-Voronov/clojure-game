(ns mire.enemies)

(def enemies (ref {}))

(defn load-enemy [enemies file]
  (let [enemy (read-string (slurp (.getAbsolutePath file)))]
    (conj enemies
          {(keyword (.getName file))
           {:name (:name enemy)
            :baseHp (:baseHp enemy)
            :baseDamage (:baseDamage enemy)
            :drops (ref (or (:drops enemy) #{}))
           }}
          )
    )
  )

(defn load-enemies
  "Given a dir, return a map with an entry corresponding to each file
  in it. Files should be maps containing room data."
  [enemies dir]
  (dosync
    (reduce load-enemy enemies
            (.listFiles (java.io.File. dir)))))

(defn add-enemies
  "Look through all the files in a dir for files describing rooms and add
  them to the mire.rooms/rooms map."
  [dir]
  (dosync
    (alter enemies load-enemies dir)))

