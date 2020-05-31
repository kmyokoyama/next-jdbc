;; copyright (c) 2019-2020 Sean Corfield, all rights reserved

(ns next.jdbc.datafy-test
  "Tests for the datafy extensions over JDBC types."
  (:require [clojure.core.protocols :as core-p]
            [clojure.set :as set]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [next.jdbc :as jdbc]
            [next.jdbc.datafy]
            [next.jdbc.test-fixtures :refer [with-test-db db ds
                                              derby?
                                              mssql?]]
            [next.jdbc.specs :as specs])
  (:import (java.sql ResultSet)))

(set! *warn-on-reflection* true)

(use-fixtures :once with-test-db)

(specs/instrument)

(def ^:private basic-connection-keys
  "Generic JDBC Connection fields."
  #{:autoCommit :catalog :clientInfo :holdability :metaData
    :networkTimeout :schema :transactionIsolation :typeMap :warnings
    ;; boolean properties
    :closed :readOnly
    ;; added by bean itself
    :class})

(deftest connection-datafy-tests
  (testing "basic datafication"
    (if (derby?)
      (is (= #{:exception :cause} ; at least one property not supported
             (set (keys (core-p/datafy (jdbc/get-connection (ds)))))))
      (let [data (set (keys (core-p/datafy (jdbc/get-connection (ds)))))]
        (when-let [diff (seq (set/difference data basic-connection-keys))]
          (println (:dbtype (db)) (sort diff)))
        (is (= basic-connection-keys
               (set/intersection basic-connection-keys data))))))
  (testing "nav to metadata yields object"
    (when-not (derby?)
      (is (instance? java.sql.DatabaseMetaData
                     (core-p/nav (core-p/datafy (jdbc/get-connection (ds)))
                                 :metaData
                                 nil))))))
