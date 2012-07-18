Halbuilder is a simple Java API for generating and consuming HAL documents conforming to the
[HAL Specification](http://stateless.co/hal_specification.html).

### Generating Local Resources

    ResourceFactory representationFactory = new ResourceFactory();

    Resource owner = representationFactory.newResource("http://example.com/mike")
      .withLink("td:friend", "http://example.com/mamund")
      .withProperty("name", "Mike")
      .withProperty("age", "36");

    Resource halResource = representationFactory.newResource("http://example.com/todo-list")
      .withNamespace("td", "http://example.com/todoapp/rels/")
      .withLink("/todo-list/search;{searchterm}", "td:search")
      .withLink("/todo-list/description", "td:description")
      .withProperty("created_at", "2010-01-16")
      .withProperty("updated_at", "2010-02-21")
      .withProperty("summary", "An example list")
      .withSubresource("td:owner", owner);

    String xml = halResource.renderContent(ResourceFactory.HAL_XML);
    String json = halResource.renderContent(ResourceFactory.HAL_JSON);

### Reading Local Resources

    ResourceFactory representationFactory = new ResourceFactory();

    Resource representation = representationFactory.readResource(new InputStreamReader(Some.class.getResourceAsStream("/test.xml")));

### Apache Maven

HalBuilder is deployed to Apache Maven Central under the following coordinates:

    <dependency>
      <groupId>com.theoryinpractise</groupId>
      <artifactId>halbuilder</artifactId>
      <version>1.0.3</version>
    </dependency>

### Development Forum

Email support and discussion is available on the [development forum](https://groups.google.com/forum/#!forum/halbuilder-dev).
