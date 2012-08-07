package com.theoryinpractise.halbuilder;

import com.theoryinpractise.halbuilder.spi.Link;
import com.theoryinpractise.halbuilder.spi.Representation;
import org.fest.assertions.core.Condition;
import org.testng.annotations.Test;

import java.util.List;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.fail;

public class CollatedLinksTest {

    @Test
    public void testCollatedLinks() {

        Representation resource = new RepresentationFactory().newRepresentation("/foo")
                .withLink("bar", "/bar")
                .withLink("foo", "/bar");

        List<Link> collatedLinks = resource.getLinks();

        assertThat(collatedLinks)
                .isNotEmpty()
                .has(new ContainsRelCondition("bar foo"));

        assertThat(resource.getLinksByRel("bar"))
                .isNotNull()
                .has(new ContainsRelCondition("bar foo"));


    }

    @Test
    public void testSpacedRelsSeparateLinks() {

        Representation representation = new RepresentationFactory().newRepresentation("/foo");

        try {
            Representation resource = representation.withLink("bar foo", "/bar");
            fail("We should fail to add a space separated link rel.");
        } catch (IllegalArgumentException e) {
            // expected
        }

    }

    @Test
    public void testMultiSpacedRelsSeparateLinks() {

        Representation representation = new RepresentationFactory().newRepresentation("/foo");
        try {
            Representation resource = representation.withLink("bar                  foo", "/bar");
            fail("We should fail to add a space separated link rel.");
        } catch (IllegalArgumentException e) {
            // expected
        }

    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testRelLookupsWithNullFail() {
        Representation resource = new RepresentationFactory().newRepresentation("/foo")
                .withLink("bar foo", "/bar");

        resource.getLinkByRel(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testRelLookupsWithEmptyRelFail() {
        Representation resource = new RepresentationFactory().newRepresentation("/foo")
                .withLink("bar", "/bar");

        resource.getLinkByRel("");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testRelLookupsWithSpacesFail() {
        Representation resource = new RepresentationFactory().newRepresentation("/foo")
                .withLink("bar", "/bar");

        resource.getLinkByRel("test fail");
    }

    private static class ContainsRelCondition extends Condition<List<?>> {

        private final String rel;

        public ContainsRelCondition(final String rel) {
            this.rel = rel;
        }

        @Override
        public boolean matches(List<?> objects) {
            for (Object object : objects) {
                Link link = (Link) object;
                if (link.getRel().equals(rel)) {
                    return true;
                }
            }
            return false;
        }
    }
}
