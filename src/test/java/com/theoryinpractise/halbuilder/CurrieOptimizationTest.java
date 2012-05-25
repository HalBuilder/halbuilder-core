package com.theoryinpractise.halbuilder;

import com.theoryinpractise.halbuilder.spi.Link;
import com.theoryinpractise.halbuilder.spi.Resource;
import org.testng.annotations.Test;

import static org.fest.assertions.api.Assertions.assertThat;

public class CurrieOptimizationTest {

    ResourceFactory resourceFactory = new ResourceFactory().withNamespace("app", "/api/applications/")
                                                           .withNamespace("rel", "/api/rels/");

    Resource resource = resourceFactory.newResource("/api/1")
                                       .withLink("/api/applications/app/1", "/api/rels/foo");


    @Test
    public void testCurrieOptimizationOnFirstLink() {

        Link link = resource.getLinks().get(1);

        assertThat(link.getRel()).isEqualTo("rel:foo");
        assertThat(link.getHref()).isEqualTo("app:app/1");

    }

    @Test
    public void testLinkLookupByCurrieOptimizationRel() {

        Link link2 = resource.getLinksByRel("rel:foo").get(0);

        assertThat(link2.getRel()).isEqualTo("rel:foo");
        assertThat(link2.getHref()).isEqualTo("app:app/1");

    }

    @Test
    public void testLinkLookupByAbsoluteRel() {

        Link link2 = resource.getLinksByRel("/api/rels/foo").get(0);

        assertThat(link2.getRel()).isEqualTo("rel:foo");
        assertThat(link2.getHref()).isEqualTo("app:app/1");

    }

    @Test
    public void testLinkLookupByCanonicalRel() {

        Link link2 = resource.getLinksByRel("http://localhost/api/rels/foo").get(0);

        assertThat(link2.getRel()).isEqualTo("rel:foo");
        assertThat(link2.getHref()).isEqualTo("app:app/1");

    }

}
