Halbuilder is a simple Java builder API for generating XML and JSON HAL documents conforming to the
[HAL Specification](http://stateless.co/hal_specification.html).

    HalResource owner = HalResource
      .newHalResource("http://example.com/mike")
      .withLink("td:friend", "http://example.com/mamund")
      .withProperty("name", "Mike")
      .withProperty("age", "36");

    HalResource halResource = HalResource
      .newHalResource("http://example.com/todo-list")
      .withNamespace("td", "http://example.com/todoapp/rels/")
      .withLink("td:search", "/todo-list/search;{searchterm}")
      .withLink("td:description", "/todo-list/description")
      .withProperty("created_at", "2010-01-16")
      .withProperty("updated_at", "2010-02-21")
      .withProperty("summary", "An example list")
      .withSubresource("td:owner", owner);

    String xml = halResource.renderXml();
    String json = halResource.renderJson();
