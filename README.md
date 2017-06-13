Halbuilder is a simple Java API for generating and consuming HAL documents conforming to the
[HAL Specification](http://stateless.co/hal_specification.html).

### Generating Local Resources

```java
Map<String,Object> friend = HashMap.of("name", "Mike", "age", 36);
Representation<Map<String,Object>> owner =
  ResourceRepresentation.create("http://example.com/mike", friend)
    .withLink("td:friend", "http://example.com/mamund")

Map<String,Object> todoMeta = HashMap.of(
  "created_at", "2010-01-16", "updated_at", "2017-06-13",
  "summary", "An example list");

Representation<Map<String,Object>> halResource =
  RepresentationFactory.crate("http://example.com/todo-list", todoMeta)
    .withLink("td:search", "/todo-list/search;{searchterm}")
    .withLink("td:description", "/todo-list/description")
    .withRepresentation("td:owner", owner);

JsonRepresentationWriter jsonRepresentationWriter =
  JsonRepresentationWriter.create(new ObjectMapper());

ByteString representation = jsonRepresentationWriter.print(accountRepWithLinks);
System.out.println(representation.utf8());
```

### Reading Local Resources

```java
JsonRepresentationReader jsonRepresentationReader =
  JsonRepresentationReader.create(objectMapper);

Representation<ByteString> representation =
  jsonRepresentationReader.readRepresentation(
    new InputStreamReader(Some.class.getResourceAsStream("/test.json")));

// or as a type

Representation<Person> personRepresentation =
  jsonRepresentationReader.readRepresentation(
    new InputStreamReader(Some.class.getResourceAsStream("/test.json")),
    Person.class);

```

### Apache Maven

HalBuilder is deployed to Apache Maven Central under the following coordinates:

```xml
<dependency>
  <groupId>com.theoryinpractise</groupId>
  <artifactId>halbuilder5</artifactId>
  <version>5.0.1-SNAPSHOT</version>
</dependency>
```

### Website

More documentation is available from the main website at [gotohal.net](http://www.gotohal.net/).

### Development Forum

Email support and discussion is available on the [development forum](https://groups.google.com/forum/#!forum/halbuilder-dev).

curl -v -F r=releases -F hasPom=false -F e=jar -F g=com.test -F a=project -F v=1.0 -F p=jar -F file=@project-1.0.jar -u admin:admin123 http://localhost:8081/nexus/service/local/artifact/maven/content

curl -v -F r=releases -F hasPom=true -F e=jar -F file=@pom.xml -F file=@project-1.0.jar -u admin:admin123 http://localhost:8081/nexus/service/local/artifact/maven/content

#### No pom

    nexus_upload(
      name="sonatype_snapshot",
      extension="jar",
      groupId="com.test",
      artifactId="project",
      version="1.0",
      packaging="jar")

    nexus_upload_jar(
      name="sonatype_snapshot",
      pomFile="halbuilder.pom.xml",
      artifact="//:halbuilder5")
