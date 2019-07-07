package com.theoryinpractise.halbuilder;

import static org.fest.assertions.api.Assertions.assertThat;

import com.theoryinpractise.halbuilder.api.Link;
import com.theoryinpractise.halbuilder.api.Representation;
import com.theoryinpractise.halbuilder.api.RepresentationFactory;
import org.testng.annotations.Test;

public class CurrieOptimizationTest {

  RepresentationFactory representationFactory =
      new DefaultRepresentationFactory()
          .withNamespace("app", "http://localhost/api/applications/{rel}")
          .withNamespace("rel", "http://localhost/api/rels/{rel}");

  Representation resource =
      representationFactory.newRepresentation("/api/1").withLink("http://localhost/api/rels/foo", "http://localhost/api/applications/app/1");

  @Test
  public void testCurrieOptimizationOnFirstLink() {

    Link link = resource.getLinks().get(1);

    assertThat(link.getRel()).isEqualTo("rel:foo");
    assertThat(link.getHref()).isEqualTo("http://localhost/api/applications/app/1");
  }

  @Test
  public void testLinkLookupByCurrieOptimizationRel() {

    Link link2 = resource.getLinksByRel("rel:foo").get(0);

    assertThat(link2.getRel()).isEqualTo("rel:foo");
    assertThat(link2.getHref()).isEqualTo("http://localhost/api/applications/app/1");
  }

  @Test
  public void testLinkLookupByAbsoluteRel() {

    Link link2 = resource.getLinksByRel("http://localhost/api/rels/foo").get(0);

    assertThat(link2.getRel()).isEqualTo("rel:foo");
    assertThat(link2.getHref()).isEqualTo("http://localhost/api/applications/app/1");
  }

  @Test
  public void testLinkLookupByCanonicalRel() {

    Link link2 = resource.getLinksByRel("http://localhost/api/rels/foo").get(0);

    assertThat(link2.getRel()).isEqualTo("rel:foo");
    assertThat(link2.getHref()).isEqualTo("http://localhost/api/applications/app/1");
  }
}
