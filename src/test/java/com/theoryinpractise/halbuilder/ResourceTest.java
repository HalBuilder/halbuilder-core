package com.theoryinpractise.halbuilder;

import org.testng.annotations.Test;

import static org.fest.assertions.Assertions.assertThat;

public class ResourceTest {

    private ResourceFactory resourceFactory = new ResourceFactory();

    @Test(expectedExceptions = ResourceException.class)
    public void testUndeclaredLinkNamespace() {
        resourceFactory.newHalResource("http://localhost/test")
                .withLink("td:test", "http://localhost/test/2")
                .renderXml();
    }

    @Test(expectedExceptions = ResourceException.class)
    public void testUndeclaredResourceNamespace() {
        resourceFactory.newHalResource("http://localhost/test")
                .withSubresource("td:test", resourceFactory.newHalResource("/"))
                .renderXml();
    }

    @Test(expectedExceptions = ResourceException.class)
    public void testUndeclaredResourceLinkNamespace() {
        resourceFactory.newHalResource("http://localhost/test")
                .withSubresource("test", resourceFactory.newHalResource("/").withLink("td:test", "/"))
                .renderXml();
    }

    @Test(expectedExceptions = ResourceException.class)
    public void testDuplicatePropertyDefinitions() {
        resourceFactory.newHalResource("http://localhost/test")
                .withProperty("name", "Example User")
                .withProperty("name", "Example User")
                .renderXml();
    }

    @Test
    public void testHalResourceHrefShouldBeFullyQualified() {
        String xml = resourceFactory.newHalResource("/test")
                .withBaseHref("http://localhost")
                .renderXml();

        assertThat(xml).contains("http://localhost/test");
    }

    @Test
    public void testRelativeLinksRenderFullyQualified() {
        String xml = resourceFactory.newHalResource("/")
                .withLink("test", "/test")
                .withBaseHref("http://localhost")
                .renderXml();

        assertThat(xml).contains("http://localhost/test");
    }

    @Test
    public void testRelativeResourceRenderFullyQualified() {
        String xml = resourceFactory.newHalResource("/")
                .withSubresource("test", resourceFactory.newHalResource("subresource"))
                .withBaseHref("http://localhost")
                .renderXml();

        assertThat(xml).contains("http://localhost/subresource");
    }

    @Test
    public void testRelativeResourceLinksRenderFullyQualified() {
        String xml = resourceFactory.newHalResource("/")
                .withSubresource("test", resourceFactory
                        .newHalResource("subresource/")
                        .withLink("sub", "/sublink1")
                        .withLink("sub2", "sublink2"))
                .withBaseHref("http://localhost")
                .renderXml();

        assertThat(xml).contains("http://localhost/sublink1");
        assertThat(xml).contains("http://localhost/subresource/sublink2");
    }

}
