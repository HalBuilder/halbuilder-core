package com.theoryinpractise.halbuilder;

import org.testng.annotations.Test;

import static org.fest.assertions.Assertions.assertThat;

public class HalResourceTest {

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
