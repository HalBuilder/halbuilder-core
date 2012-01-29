package com.theoryinpractise.halbuilder.links;

import com.theoryinpractise.halbuilder.Link;
import com.theoryinpractise.halbuilder.Resource;
import com.theoryinpractise.halbuilder.ResourceFactory;
import org.fest.assertions.Condition;
import org.testng.annotations.Test;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class CollatedLinks {

    @Test
    public void testCollatedLinks() {

        Resource resource = new ResourceFactory().newHalResource("/foo")
                                                 .withLink("/bar", "bar")
                                                 .withLink("/bar", "foo");

        List<Link> collatedLinks = resource.getLinks();

        assertThat(collatedLinks)
                .isNotEmpty()
                .satisfies(new ContainsRelCondition("bar foo"));

        assertThat(resource.getLinksByRel("bar"))
                .isNotNull()
                .satisfies(new ContainsRelCondition("bar foo"));


    }

    @Test
    public void testSpacedRelsSeparateLinks() {

        Resource resource = new ResourceFactory().newHalResource("/foo")
                                                 .withLink("/bar", "bar foo");

        assertThat(resource.getCanonicalLinks())
                .isNotEmpty()
                .hasSize(3)
                .satisfies(new ContainsRelCondition("bar"))
                .satisfies(new ContainsRelCondition("foo"));



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
