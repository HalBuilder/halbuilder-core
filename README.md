Halbuilder is a simple Java API for generating and consuming HAL documents conforming to the
[HAL Specification](http://stateless.co/hal_specification.html).

### Generating Local Resources

```java
RepresentationFactory representationFactory = new StandardRepresentationFactory();

Representation owner = representationFactory.newRepresentation("http://example.com/mike")
  .withLink("td:friend", "http://example.com/mamund")
  .withProperty("name", "Mike")
  .withProperty("age", "36");

Representation halResource = representationFactory.newRepresentation("http://example.com/todo-list")
  .withNamespace("td", "http://example.com/todoapp/rels/{rel}")
  .withLink("/todo-list/search;{searchterm}", "td:search")
  .withLink("/todo-list/description", "td:description")
  .withProperty("created_at", "2010-01-16")
  .withProperty("updated_at", "2010-02-21")
  .withProperty("summary", "An example list")
  .withRepresentation("td:owner", owner);

String xml = halResource.toString(RepresentationFactory.HAL_XML);
String json = halResource.toString(RepresentationFactory.HAL_JSON);
```

### Reading Local Resources

```java
RepresentationFactory representationFactory = new RepresentationFactory();

Representation representation = representationFactory.readRepresentation(
                  new InputStreamReader(Some.class.getResourceAsStream("/test.xml")));
```

### Apache Maven

HalBuilder is deployed to Apache Maven Central under the following coordinates:

```xml
<dependency>
  <groupId>com.theoryinpractise</groupId>
  <artifactId>halbuilder-standard</artifactId>
  <version>3.0.1</version>
</dependency>
```

### Website

More documentation is available from the main website at [gotohal.net](http://gotohal.net/).

### Development Forum

Email support and discussion is available on the [development forum](https://groups.google.com/forum/#!forum/halbuilder-dev).
