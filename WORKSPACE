# Add Sonatype SNAPSHOT repository
maven_server(name = "oss_snapshots", url = "https://oss.sonatype.org/content/repositories/snapshots/")

# Derive4j ADT processor dependencies
maven_jar(name = "derive4j",               artifact = "org.derive4j:derive4j:0.7")
maven_jar(name = "derive4j_annotation",    artifact = "org.derive4j:derive4j-annotation:0.7")
maven_jar(name = "derive4j_processor_api", artifact = "org.derive4j:derive4j-processor-api:0.7")
maven_jar(name = "javapoet",               artifact = "com.squareup:javapoet:1.6.1")

# Immutables processor dependencies
maven_jar(name = "immutables",             artifact = "org.immutables:value:2.1.18")
maven_jar(name = "immutables_processor",   artifact = "org.immutables:value-processor:2.1.18")
maven_jar(name = "immutables_generator",   artifact = "org.immutables:generator:2.1.18")

# Actual HalBuilder 5 dependencies
maven_jar(name = "guava",                  artifact = "com.google.guava:guava:19.0")
maven_jar(name = "javaslang",              artifact = "io.javaslang:javaslang:2.1.0-SNAPSHOT", server="oss_snapshots")
maven_jar(name = "jsr305",                 artifact = "com.google.code.findbugs:jsr305:3.0.1")
maven_jar(name = "okio",                   artifact = "com.squareup.okio:okio:1.7.0")

# HalBuilder 5 JSON Dependencies
maven_jar(name = "jackson_core",           artifact = "com.fasterxml.jackson.core:jackson-core:2.7.3")
maven_jar(name = "jackson_annotations",    artifact = "com.fasterxml.jackson.core:jackson-annotations:2.7.3")
maven_jar(name = "jackson_databind",       artifact = "com.fasterxml.jackson.core:jackson-databind:2.7.3")

# Test Dependencies
maven_jar(name = "junit",                  artifact = "junit:junit:4.12")
maven_jar(name = "truth",                  artifact = "com.google.truth:truth:0.28")
maven_jar(name = "handry_uri_templates",   artifact = "com.damnhandy:handy-uri-templates:2.1.5")

maven_jar(name = "gherkin",         artifact = "info.cukes:gherkin:2.12.2")
maven_jar(name = "cucumber_core",         artifact = "info.cukes:cucumber-core:1.2.4")
maven_jar(name = "cucumber_java8",         artifact = "info.cukes:cucumber-java8:1.2.4")
maven_jar(name = "cucumber_java",          artifact = "info.cukes:cucumber-java:1.2.4")
maven_jar(name = "cucumber_junit",          artifact = "info.cukes:cucumber-junit:1.2.4")
