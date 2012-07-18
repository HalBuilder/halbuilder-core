package com.theoryinpractise.halbuilder;

import com.theoryinpractise.halbuilder.spi.Link;
import com.theoryinpractise.halbuilder.spi.Representation;
import org.fest.assertions.core.Condition;
import org.testng.annotations.Test;

import java.util.List;

import static org.fest.assertions.api.Assertions.assertThat;

public class CollatedLinksTest {

    @Test
    public void testCollatedLinks() {

        Representation resource = new RepresentationFactory().newResource("/foo")
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

        Representation resource = new RepresentationFactory().newResource("/foo")
                                                 .withLink("bar foo", "/bar");

        assertThat(resource.getCanonicalLinks())
                .isNotEmpty()
                .hasSize(3)
                .has(new ContainsRelCondition("bar"))
                .has(new ContainsRelCondition("foo"));

    }

    @Test
    public void testMultiSpacedRelsSeparateLinks() {

        Representation resource = new RepresentationFactory().newResource("/foo")
                                                 .withLink("bar                  foo", "/bar");

        assertThat(resource.getCanonicalLinks())
                .isNotEmpty()
                .hasSize(3)
                .has(new ContainsRelCondition("bar"))
                .has(new ContainsRelCondition("foo"));

    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testRelLookupsWithNullFail() {
        Representation resource = new RepresentationFactory().newResource("/foo")
                                                 .withLink("bar foo", "/bar");

        resource.getLinkByRel(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testRelLookupsWithEmptyRelFail() {
        Representation resource = new RepresentationFactory().newResource("/foo")
                                                 .withLink("bar foo", "/bar");

        resource.getLinkByRel("");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testRelLookupsWithSpacesFail() {
        Representation resource = new RepresentationFactory().newResource("/foo")
                                                 .withLink("bar foo", "/bar");

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
                if (link.getRel().equals(rel)) return true;
            }
            return false;
        }
    }
}
