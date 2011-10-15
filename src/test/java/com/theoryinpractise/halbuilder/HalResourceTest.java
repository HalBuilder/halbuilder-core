package com.theoryinpractise.halbuilder;

import org.testng.annotations.Test;

public class HalResourceTest {

    @Test
    public void testHalBuilder() {

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

        System.out.println(halResource.renderJson());
        System.out.println(halResource.renderXml());


    }

    @Test(expectedExceptions = HalResourceException.class)
    public void testUndeclaredLinkNamespace() {
        HalResource.newHalResource("http://localhost/test")
                .withLink("td:test", "http://localhost/test/2")
                .renderXml();

    }

    @Test(expectedExceptions = HalResourceException.class)
    public void testUndeclaredResourceNamespace() {
        HalResource.newHalResource("http://localhost/test")
                .withSubresource("td:test", HalResource.newHalResource("/"))
                .renderXml();

    }

    @Test(expectedExceptions = HalResourceException.class)
    public void testUndeclaredResourceLinkNamespace() {
        HalResource.newHalResource("http://localhost/test")
                .withSubresource("test", HalResource.newHalResource("/").withLink("td:test", "/"))
                .renderXml();

    }

    @Test(expectedExceptions = HalResourceException.class)
    public void testDuplicatePropertyDefinitions() {
        HalResource.newHalResource("http://localhost/test")
                .withProperty("name", "Example User")
                .withProperty("name", "Example User")
                .renderXml();

    }

}
