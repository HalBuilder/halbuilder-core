package com.theoryinpractise.halbuilder.impl.bytecode;

import com.theoryinpractise.halbuilder.DefaultRepresentationFactory;
import com.theoryinpractise.halbuilder.api.Link;
import com.theoryinpractise.halbuilder.api.ReadableRepresentation;
import com.theoryinpractise.halbuilder.api.RepresentationFactory;
import org.testng.annotations.Test;

import java.util.*;

import static org.fest.assertions.api.Assertions.assertThat;

public class InterfaceRendererTest {
    private static final RepresentationFactory representationFactory = new DefaultRepresentationFactory();

    public static interface IPerson {
        Integer getAge();

        Boolean getExpired();

        Integer getId();

        String getName();

        List<Link> getLinks();

        Map<String, Collection<ReadableRepresentation>> getEmbedded();
    }

    private static final Map<String, Object> properties = new HashMap<String, Object>();
    private static final List<Link> links = new ArrayList<Link>();
    private static final Map<String, Collection<ReadableRepresentation>> embedded = new HashMap<String, Collection<ReadableRepresentation>>();

    static {
        properties.put("name","Joe Smith");
        properties.put("id",1);
        properties.put("expired",false);
        properties.put("age",40);
        links.add(new Link(representationFactory, "self", "/123/456"));
        Set<ReadableRepresentation> embeddedResources = new HashSet<ReadableRepresentation>();
        embeddedResources.add(representationFactory.newRepresentation());
        embedded.put("user", embeddedResources);
    }

    @Test
    public void testRendering() {
        InterfaceRenderer<IPerson> renderer = InterfaceRenderer.newInterfaceRenderer(IPerson.class);
        IPerson person = renderer.render(properties, links, embedded);
        assertThat(person).isNotNull();
        assertThat(person.getName()).isNotEmpty();
        assertThat(person.getLinks()).isNotEmpty();
    }
}
