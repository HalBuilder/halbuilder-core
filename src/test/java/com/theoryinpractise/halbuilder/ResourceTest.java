package com.theoryinpractise.halbuilder;

import org.testng.annotations.Test;

import static org.fest.assertions.Assertions.assertThat;

public class ResourceTest {

    private ResourceFactory resourceFactory = new ResourceFactory("http://localhost/test");

    @Test(expectedExceptions = ResourceException.class)
    public void testUndeclaredLinkNamespace() {
        resourceFactory.newHalResource("/test")
                       .withLink("http://localhost/test/2", "td:test")
                       .asRenderableResource()
                       .renderXml();
    }

    @Test(expectedExceptions = ResourceException.class)
    public void testUndeclaredResourceNamespace() {
        resourceFactory.newHalResource("http://localhost/test")
                       .withSubresource("td:test", resourceFactory.newHalResource("/"))
                       .asRenderableResource()
                       .renderXml();
    }

    @Test(expectedExceptions = ResourceException.class)
    public void testUndeclaredResourceLinkNamespace() {
        resourceFactory.newHalResource("http://localhost/test")
                       .withSubresource("test", resourceFactory.newHalResource("/").withLink("/", "td:test"))
                       .asRenderableResource()
                       .renderXml();
    }

    @Test(expectedExceptions = ResourceException.class)
    public void testDuplicatePropertyDefinitions() {
        resourceFactory.newHalResource("http://localhost/test")
                       .withProperty("name", "Example User")
                       .withProperty("name", "Example User")
                       .asRenderableResource()
                       .renderXml();
    }

    @Test
    public void testHalResourceHrefShouldBeFullyQualified() {
        String xml = resourceFactory.newHalResource("/test")
                                    .asRenderableResource()
                                    .renderXml();

        assertThat(xml).contains("http://localhost/test");
    }

    @Test
    public void testRelativeLinksRenderFullyQualified() {
        String xml = resourceFactory.newHalResource("/")
                                    .withLink("/test", "test")
                                    .asRenderableResource()
                                    .renderXml();

        assertThat(xml).contains("http://localhost/test");
    }

    @Test
    public void testRelativeResourceRenderFullyQualified() {
        String xml = resourceFactory.newHalResource("/")
                                    .withSubresource("test", resourceFactory.newHalResource("subresource"))
                                    .asRenderableResource()
                                    .renderXml();

        assertThat(xml).contains("http://localhost/subresource");
    }

    @Test
    public void testRelativeResourceLinksRenderFullyQualified() {
        String xml = resourceFactory.newHalResource("/")
                                    .withSubresource("test", resourceFactory
                                            .newHalResource("subresource/")
                                            .withLink("/sublink1", "sub")
                                            .withLink("~/sublink2", "sub2"))
                                    .asRenderableResource()
                                    .renderXml();

        assertThat(xml).contains("http://localhost/sublink1");
        assertThat(xml).contains("http://localhost/subresource/sublink2");
    }

}
