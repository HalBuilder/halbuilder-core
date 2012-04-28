package com.theoryinpractise.halbuilder.impl.resources;

import com.google.common.base.Optional;
import com.theoryinpractise.halbuilder.ResourceFactory;
import com.theoryinpractise.halbuilder.impl.bytecode.InterfaceContract;
import com.theoryinpractise.halbuilder.impl.bytecode.InterfaceRenderer;
import com.theoryinpractise.halbuilder.spi.Link;
import com.theoryinpractise.halbuilder.spi.Renderer;
import com.theoryinpractise.halbuilder.spi.Resource;

import java.io.StringWriter;
import java.util.List;
import java.util.Map;

public class ImmutableResource extends BaseResource {

    private final Link resourceLink;

    public ImmutableResource(ResourceFactory resourceFactory,
                             Map<String, String> namespaces, List<Link> links, Map<String, Optional<Object>> properties, List<Resource> resources, boolean hasNullProperties) {
        super(resourceFactory);
        this.namespaces = namespaces;
        this.links = links;
        this.properties = properties;
        this.resources = resources;

        this.resourceLink = super.getResourceLink();
        
        this.hasNullProperties = hasNullProperties;
    }

    public Link getResourceLink() {
        return resourceLink;
    }

    /**
     * Renders the current Resource as a proxy to the provider interface
     *
     * @param anInterface The interface we wish to proxy the resource as
     * @return A Guava Optional of the rendered class, this will be absent if the interface doesn't satisfy the interface
     */
    public <T> Optional<T> renderClass(Class<T> anInterface) {
        if (InterfaceContract.newInterfaceContract(anInterface).isSatisfiedBy(this)) {
            return InterfaceRenderer.newInterfaceRenderer(anInterface).render(this, null);
        } else {
            return Optional.absent();
        }
    }

    public String renderContent(String contentType) {
        Renderer<String> renderer = resourceFactory.lookupRenderer(contentType);
        return renderAsString(renderer);
    }

    private String renderAsString(final Renderer renderer) {
        validateNamespaces(this);
        StringWriter sw = new StringWriter();
        renderer.render(this, sw);
        return sw.toString();
    }

}
