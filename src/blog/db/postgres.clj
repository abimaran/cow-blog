(ns blog.db.postgres
  (:require (clojure.contrib [sql :as sql])
            (oyako [core :as oyako])
            (blog [db :as db])))

(defn init-db-postgres []
  (db/with-db
    (let [id [:id "bigserial primary key"]
          varchar "varchar(255) not null"
          nullchar "varchar(255)"
          text "text not null"
          desc [:title varchar]
          url [:url  varchar]
          timestamp [:date_created "timestamp default localtimestamp"]
          tables [[:categories id desc url
                   [:num_posts "bigint default 0"]]
                  [:tags id desc url
                   [:num_posts "bigint default 0"]]
                  [:users id
                   [:username varchar]
                   [:password varchar]
                   [:salt varchar]]
                  [:posts id url timestamp
                   [:user_id "bigint default 1 references users(id) on delete set default"]
                   [:category_id "bigint default 1 references categories (id) on delete set default"]
                   [:status varchar]
                   [:type varchar]
                   [:title varchar]
                   [:parent_id "bigint references posts (id)"]
                   [:markdown text]
                   [:html text]
                   [:num_comments "bigint default 0"]]
                  [:comments id timestamp
                   [:post_id "bigint references posts (id) on delete cascade"]
                   [:status varchar]
                   [:author varchar]
                   [:email nullchar]
                   [:homepage nullchar]
                   [:ip varchar]
                   [:markdown text]
                   [:html text]]
                  [:post_tags id
                   [:post_id "bigint not null references posts (id) on delete cascade"]
                   [:tag_id "bigint not null references tags (id) on delete cascade"]]]]
      (doseq [[table & _] (reverse tables)]
        (sql/do-commands (str "drop table if exists " (name table)))
        (println "Dropped" table "(if it existed)."))
      (doseq [[table & specs] tables]
        (apply sql/create-table table specs)
        (println "Created" table)))
    (doseq [table [:posts :categories :tags]]
      (sql/do-commands (str "alter table " (name table) " add constraint " (name table) "_url unique (url)")))
    (doseq [[table & cols] [[:posts :category_id]
                            [:post_tags :post_id :tag_id]
                            [:comments :post_id]]
            col cols]
      (sql/do-commands (str "create index " (name table) "_" (name col)
                            " on " (name table) "(" (name col) ")")))
    (println "Added indices")
    (sql/insert-records :users {:username "Nobody" :password "" :salt ""})
    (sql/insert-records :categories {:title "Uncategorized" :url "uncategorized"})
    (println "Initialized" :categories)))

