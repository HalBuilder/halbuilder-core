package com.theoryinpractise.halbuilder;

import com.theoryinpractise.halbuilder.spi.ResourceException;
import org.testng.annotations.Test;

import static org.fest.assertions.Assertions.assertThat;

public class ResourceTest {

    private ResourceFactory resourceFactory = new ResourceFactory("http://localhost/test");

    @Test(expectedExceptions = ResourceException.class)
    public void testUndeclaredLinkNamespace() {
        resourceFactory.newResource("/test")
                       .withLink("http://localhost/test/2", "td:test")
                       .asRenderableResource()
                       .renderContent(ResourceFactory.HALXML);
    }

    @Test(expectedExceptions = ResourceException.class)
    public void testUndeclaredResourceNamespace() {
        resourceFactory.newResource("http://localhost/test")
                       .withSubresource("td:test", resourceFactory.newResource("/"))
                       .asRenderableResource()
                       .renderContent(ResourceFactory.HALXML);
    }

    @Test(expectedExceptions = ResourceException.class)
    public void testUndeclaredResourceLinkNamespace() {
        resourceFactory.newResource("http://localhost/test")
                       .withSubresource("test", resourceFactory.newResource("/").withLink("/", "td:test"))
                       .asRenderableResource()
                       .renderContent(ResourceFactory.HALXML);
    }

    @Test(expectedExceptions = ResourceException.class)
    public void testDuplicatePropertyDefinitions() {
        resourceFactory.newResource("http://localhost/test")
                       .withProperty("name", "Example User")
                       .withProperty("name", "Example User")
                       .asRenderableResource()
                       .renderContent(ResourceFactory.HALXML);
    }

    @Test
    public void testHalResourceHrefShouldBeFullyQualified() {
        String xml = resourceFactory.newResource("/test")
                                    .asRenderableResource()
                                    .renderContent(ResourceFactory.HALXML);

        assertThat(xml).contains("http://localhost/test");
    }

    @Test
    public void testRelativeLinksRenderFullyQualified() {
        String xml = resourceFactory.newResource("/")
                                    .withLink("/test", "test")
                                    .asRenderableResource()
                                    .renderContent(ResourceFactory.HALXML);

        assertThat(xml).contains("http://localhost/test");
    }

    @Test
    public void testRelativeResourceRenderFullyQualified() {
        String xml = resourceFactory.newResource("/")
                                    .withSubresource("test", resourceFactory.newResource("subresource"))
                                    .asRenderableResource()
                                    .renderContent(ResourceFactory.HALXML);

        assertThat(xml).contains("http://localhost/subresource");
    }

    @Test
    public void testRelativeResourceLinksRenderFullyQualified() {
        String xml = resourceFactory.newResource("/")
                                    .withSubresource("test", resourceFactory
                                            .newResource("subresource/")
                                            .withLink("/sublink1", "sub")
                                            .withLink("~/sublink2", "sub2"))
                                    .asRenderableResource()
                                    .renderContent(ResourceFactory.HALXML);

        assertThat(xml).contains("http://localhost/sublink1");
        assertThat(xml).contains("http://localhost/subresource/sublink2");
    }

}
