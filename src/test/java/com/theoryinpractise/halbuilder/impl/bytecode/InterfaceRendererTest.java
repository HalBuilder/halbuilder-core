package com.theoryinpractise.halbuilder.impl.bytecode;

import com.theoryinpractise.halbuilder.DefaultRepresentationFactory;
import com.theoryinpractise.halbuilder.api.Link;
import com.theoryinpractise.halbuilder.api.ReadableRepresentation;
import com.theoryinpractise.halbuilder.api.Representation;
import com.theoryinpractise.halbuilder.api.RepresentationFactory;
import fj.Ord;
import fj.data.List;
import fj.data.Option;
import fj.data.Set;
import fj.data.TreeMap;
import org.testng.annotations.Test;

import java.util.Collection;
import java.util.Map;

import static fj.data.Option.some;
import static org.fest.assertions.api.Assertions.assertThat;

public class InterfaceRendererTest {

  @Test
  public void testRendering() {

    RepresentationFactory representationFactory = new DefaultRepresentationFactory();
    TreeMap<String, Option<Object>> properties = TreeMap.<String, Option<Object>>empty(Ord.stringOrd)
                                                     .set("name", some("Joe Smith"))
                                                     .set("id", some(1))
                                                     .set("expired", some(false))
                                                     .set("age", some(40));

    List<Link> links = List.list(new Link(representationFactory, "self", "/123/456"));

    final Set<Representation> representations = Set.set(Ord.hashOrd(), representationFactory.newRepresentation());

    final Collection<? extends ReadableRepresentation> coll = representations.toList().toCollection();

    TreeMap<String, Collection<? extends ReadableRepresentation>> embedded = TreeMap.empty(Ord.stringOrd);
    embedded = embedded.set("user", coll);

    InterfaceRenderer<IPerson> renderer = InterfaceRenderer.newInterfaceRenderer(IPerson.class);
    IPerson person = renderer.render(properties, links, embedded);
    assertThat(person).isNotNull();
    assertThat(person.getName()).isNotEmpty();
    assertThat(person.getLinks()).isNotEmpty();
  }

  public interface IPerson {
    Integer getAge();

    Boolean getExpired();

    Integer getId();

    String getName();

    List<Link> getLinks();

    Map<String, Collection<ReadableRepresentation>> getEmbedded();
  }

}
