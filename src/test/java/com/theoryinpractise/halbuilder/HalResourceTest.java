package com.theoryinpractise.halbuilder;

import org.testng.annotations.Test;

import static org.fest.assertions.Assertions.assertThat;

public class HalResourceTest {

    @Test
    public void testHalBuilder() {

        HalResource owner = HalResource
                .newHalResource("/mike")
                .withLink("td:friend", "/mamund")
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

    @Test
    public void testHalResourceHrefShouldBeFullyQualified() {
        String xml = HalResource.newHalResource("/test")
                .withBaseHref("http://localhost")
                .renderXml();

        System.out.println(xml);

        assertThat(xml).contains("http://localhost/test");
    }

    @Test
    public void testRelativeLinksRenderFullyQualified() {
        String xml = HalResource.newHalResource("/")
                .withLink("test", "/test")
                .withBaseHref("http://localhost")
                .renderXml();

        assertThat(xml).contains("http://localhost/test");
    }

    @Test
    public void testRelativeResourceRenderFullyQualified() {
        String xml = HalResource.newHalResource("/")
                .withSubresource("test", HalResource.newHalResource("subresource"))
                .withBaseHref("http://localhost")
                .renderXml();

        assertThat(xml).contains("http://localhost/subresource");
    }

    @Test
    public void testRelativeResourceLinksRenderFullyQualified() {
        String xml = HalResource.newHalResource("/")
                .withSubresource("test", HalResource
                        .newHalResource("subresource/")
                        .withLink("sub", "/sublink1")
                        .withLink("sub2", "sublink2"))
                .withBaseHref("http://localhost")
                .renderXml();

        assertThat(xml).contains("http://localhost/sublink1");
        assertThat(xml).contains("http://localhost/subresource/sublink2");
    }

}
