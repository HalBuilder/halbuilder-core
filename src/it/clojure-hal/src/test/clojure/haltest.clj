(ns haltest
  (:use [clojure.test]))

(deftest haltest
    (let [representation-factory (com.theoryinpractise.halbuilder.DefaultRepresentationFactory.)
          representation (doto (.newRepresentation representation-factory "/foo")
                         (.withProperty "name" "Mark")
                         (.withLink "home" "/home"))]
         (println (.renderContent representation "application/hal+xml"))) )
