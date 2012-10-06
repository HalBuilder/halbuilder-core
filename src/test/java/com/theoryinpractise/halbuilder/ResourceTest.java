package com.theoryinpractise.halbuilder;

import com.theoryinpractise.halbuilder.api.RepresentationException;
import com.theoryinpractise.halbuilder.api.RepresentationFactory;
import org.testng.annotations.Test;

import static org.fest.assertions.api.Assertions.assertThat;

public class ResourceTest {

    private RepresentationFactory representationFactory = new DefaultRepresentationFactory("http://localhost/test");

    @Test(expectedExceptions = RepresentationException.class)
    public void testUndeclaredLinkNamespace() {
        representationFactory.newRepresentation("/test")
                .withLink("td:test", "http://localhost/test/2")
                .toString(RepresentationFactory.HAL_XML);
    }

    @Test(expectedExceptions = RepresentationException.class)
    public void testUndeclaredResourceNamespace() {
        representationFactory.newRepresentation("http://localhost/test")
                .withRepresentation("td:test", representationFactory.newRepresentation("/"))
                .toString(RepresentationFactory.HAL_XML);
    }

    @Test(expectedExceptions = RepresentationException.class)
    public void testUndeclaredResourceLinkNamespace() {
        representationFactory.newRepresentation("http://localhost/test")
                .withRepresentation("test", representationFactory.newRepresentation("/").withLink("td:test", "/"))
                .toString(RepresentationFactory.HAL_XML);
    }

    @Test(expectedExceptions = RepresentationException.class)
    public void testDuplicatePropertyDefinitions() {
        representationFactory.newRepresentation("http://localhost/test")
                .withProperty("name", "Example User")
                .withProperty("name", "Example User")
                .toString(RepresentationFactory.HAL_XML);
    }

    @Test
    public void testHalResourceHrefShouldBeFullyQualified() {
        String xml = representationFactory.newRepresentation("/test")
                .toString(RepresentationFactory.HAL_XML);

        assertThat(xml).contains("http://localhost/test");
    }

    @Test
    public void testRelativeLinksRenderFullyQualified() {
        String xml = representationFactory.newRepresentation("/")
                .withLink("test", "/test")
                .toString(RepresentationFactory.HAL_XML);

        assertThat(xml).contains("http://localhost/test");
    }

    @Test
    public void testRelativeResourceRenderFullyQualified() {
        String xml = representationFactory.newRepresentation("/")
                .withRepresentation("test", representationFactory.newRepresentation("subresource"))
                .toString(RepresentationFactory.HAL_XML);

        assertThat(xml).contains("http://localhost/subresource");
    }

    @Test
    public void testRelativeResourceLinksRenderFullyQualified() {
        String xml = representationFactory.newRepresentation("/")
                .withRepresentation("test", representationFactory
                        .newRepresentation("subresource/")
                        .withLink("sub", "/sublink1")
                        .withLink("sub2", "~/sublink2"))
                .toString(RepresentationFactory.HAL_XML);

        assertThat(xml).contains("http://localhost/sublink1");
        assertThat(xml).contains("http://localhost/subresource/sublink2");
    }

}
