package com.theoryinpractise.halbuilder;

import com.theoryinpractise.halbuilder.api.Link;
import com.theoryinpractise.halbuilder.api.Representation;
import org.fest.assertions.core.Condition;
import org.testng.annotations.Test;

import java.util.List;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.fail;

public class CollatedLinksTest {

    @Test
    public void testCollatedLinks() {

        Representation resource = new DefaultRepresentationFactory().newRepresentation("/foo")
                .withLink("bar", "/bar")
                .withLink("foo", "/bar");

        assertThat(resource.getLinks())
                .isNotEmpty()
                .has(new ContainsRelCondition("bar"))
                .has(new ContainsRelCondition("foo"));

        assertThat(resource.getLinksByRel("bar"))
                .isNotNull()
                .has(new ContainsRelCondition("bar"))
                .doesNotHave(new ContainsRelCondition("foo"));
    }

    @Test
    public void testSpacedRelsSeparateLinks() {

        Representation representation = new DefaultRepresentationFactory().newRepresentation("/foo");

        try {
            representation.withLink("bar foo", "/bar");
            fail("We should fail to add a space separated link rel.");
        } catch (IllegalArgumentException e) {
            // expected
        }

    }

    @Test
    public void testMultiSpacedRelsSeparateLinks() {

        Representation representation = new DefaultRepresentationFactory().newRepresentation("/foo");
        try {
            Representation resource = representation.withLink("bar                  foo", "/bar");
            fail("We should fail to add a space separated link rel.");
        } catch (IllegalArgumentException e) {
            // expected
        }

    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testRelLookupsWithNullFail() {
        Representation resource = new DefaultRepresentationFactory().newRepresentation("/foo")
                .withLink("bar foo", "/bar");

        resource.getLinkByRel(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testRelLookupsWithEmptyRelFail() {
        Representation resource = new DefaultRepresentationFactory().newRepresentation("/foo")
                .withLink("bar", "/bar");

        resource.getLinkByRel("");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testRelLookupsWithSpacesFail() {
        Representation resource = new DefaultRepresentationFactory().newRepresentation("/foo")
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
            boolean hasMatch = false;
            for (Object object : objects) {
                Link link = (Link) object;
                if (link.getRel().equals(rel)) {
                    hasMatch = true;
                }
            }
            return hasMatch;
        }
    }
}
