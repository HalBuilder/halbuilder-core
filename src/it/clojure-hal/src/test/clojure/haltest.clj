(ns haltest
  (:use [clojure.test]))

(deftest haltest
    (let [resource-factory (com.theoryinpractise.halbuilder.ResourceFactory.)
          resource (doto (.newHalResource resource-factory "/foo")
                         (.withProperty "name" "Mark")
                         (.withLink "/home" "home"))]
         (println (.renderContent (.asRenderableResource resource) "application/hal+xml"))) )
