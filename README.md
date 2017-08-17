Halbuilder is a simple Java API for generating and consuming HAL documents conforming to the
[HAL Specification](http://stateless.co/hal_specification.html).

[![codecov](https://codecov.io/gh/HalBuilder/halbuilder-core/branch/develop/graph/badge.svg)](https://codecov.io/gh/HalBuilder/halbuilder-core)

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
  RepresentationFactory.create("http://example.com/todo-list", todoMeta)
    .withLink("td:search", "/todo-list/search;{searchterm}")
    .withLink("td:description", "/todo-list/description")
    .withRepresentation("td:owner", owner);

JsonRepresentationWriter jsonRepresentationWriter =
  JsonRepresentationWriter.create();

ByteString representation = jsonRepresentationWriter.print(accountRepWithLinks);
System.out.println(representation.utf8());
```

### Reading Local Resources

```java
JsonRepresentationReader jsonRepresentationReader =
  JsonRepresentationReader.create();

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
  <version>5.0.1</version>
</dependency>
```

### Website

More documentation is available from the main website at [gotohal.net](http://www.gotohal.net/).

### Development Forum

Email support and discussion is available on the [development forum](https://groups.google.com/forum/#!forum/halbuilder-dev).
