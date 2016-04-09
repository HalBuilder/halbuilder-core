# Load nexus rules
load("//:nexus.bzl", "nexus_upload_jar")

# Plugin for Derive4J ADT Processor
java_plugin(
  name="derive4j_plugin",
  deps = ["@derive4j//jar", "@derive4j_annotation//jar", "@derive4j_processor_api//jar", "@javapoet//jar"],
  processor_class = "org.derive4j.processor.DerivingProcessor")

# Plugin for Immutables Processor
java_plugin(
  name="immutables_plugin",
  deps = ["@immutables//jar", "@immutables_processor//jar",
          "@immutables_generator//jar", "@guava//jar"],
  processor_class = "org.immutables.value.processor.Processor")

java_library(
    name = "halbuilder5-core",
    srcs = glob(["src/main/java/com/theoryinpractise/halbuilder5/*.java"]),
    plugins = [":derive4j_plugin"],
    deps = ["@derive4j_annotation//jar",
            "@okio//jar",
            "@javaslang//jar",
            "@jsr305//jar",
            "@guava//jar"])

java_library(
    name = "halbuilder5-json",
    srcs = glob(["src/main/java/com/theoryinpractise/halbuilder5/json/*.java"]),
    deps = [":halbuilder5-core",
            "@okio//jar",
            "@javaslang//jar",
            "@jsr305//jar",
            "@guava//jar",
            "@jackson_core//jar",
            "@jackson_annotations//jar",
            "@jackson_databind//jar"])

nexus_upload_jar(
  name="halbuilder5-nexus",
  pomFile="//:pom.xml",
  file=":halbuilder5",
  user="talios",
  password="...",
  repository="snapshots",
  server="https://oss.sonatype.org/"
  )


java_test(
    name = "testsuite",
    size = "small",
    srcs = glob(["src/test/java/**/*.java"]),
    test_class = "com.theoryinpractise.halbuilder5.AllTests",
    plugins = [":immutables_plugin"],
    deps = [
      "@junit//jar",
      "@truth//jar",
      "@guava//jar",
      "@immutables//jar",
      "@javaslang//jar",
      "@okio//jar",
      "@handry_uri_templates//jar",
      "@gherkin//jar",
      "@cucumber_core//jar",
      "@cucumber_java8//jar",
      "@cucumber_java//jar",
      "@cucumber_junit//jar",
      "//:halbuilder5-core",
      "//:halbuilder5-json"
    ],
)
