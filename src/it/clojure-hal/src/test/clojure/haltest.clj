(ns haltest
  (:use [clojure.test]))

(deftest haltest
    (let [resource-factory (com.theoryinpractise.halbuilder.ResourceFactory.)
          resource (doto (.newResource resource-factory "/foo")
                         (.withProperty "name" "Mark")
                         (.withLink "/home" "home"))]
         (println (.renderContent resource "application/hal+xml"))) )
