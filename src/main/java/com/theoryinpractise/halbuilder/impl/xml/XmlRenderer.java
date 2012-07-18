package com.theoryinpractise.halbuilder.impl.xml;

import com.google.common.base.Optional;
import com.theoryinpractise.halbuilder.spi.Link;
import com.theoryinpractise.halbuilder.spi.ReadableRepresentation;
import com.theoryinpractise.halbuilder.spi.Renderer;
import com.theoryinpractise.halbuilder.spi.Representation;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.Text;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;

import static com.theoryinpractise.halbuilder.impl.api.Support.HREF;
import static com.theoryinpractise.halbuilder.impl.api.Support.HREFLANG;
import static com.theoryinpractise.halbuilder.impl.api.Support.LINK;
import static com.theoryinpractise.halbuilder.impl.api.Support.NAME;
import static com.theoryinpractise.halbuilder.impl.api.Support.REL;
import static com.theoryinpractise.halbuilder.impl.api.Support.SELF;
import static com.theoryinpractise.halbuilder.impl.api.Support.TEMPLATED;
import static com.theoryinpractise.halbuilder.impl.api.Support.TITLE;
import static com.theoryinpractise.halbuilder.impl.api.Support.XSI_NAMESPACE;


public class XmlRenderer<T> implements Renderer<T> {

    public Optional<T> render(ReadableRepresentation representation, Writer writer) {
        final Element element = renderElement("self", representation, false);
        final XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
        try {
            outputter.output(element, writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return Optional.absent();
    }

    private Element renderElement(String rel, ReadableRepresentation representation, boolean embedded) {

        final Link resourceLink = representation.getResourceLink();
        final String href = resourceLink.getHref();

        // Create the root element
        final Element resourceElement = new Element("resource");
        resourceElement.setAttribute("href", href);
        if (!rel.equals("self")) {
            resourceElement.setAttribute("rel", rel);
        }

        // Only add namespaces to non-embedded resources
        if (!embedded) {
            for (Map.Entry<String, String> entry : representation.getNamespaces().entrySet()) {
                resourceElement.addNamespaceDeclaration(
                        Namespace.getNamespace(entry.getKey(), entry.getValue()));
            }
            // Add the instance namespace if there are null properties on this
            // representation or on any embedded resources.
            if (representation.hasNullProperties()) {
                resourceElement.addNamespaceDeclaration(XSI_NAMESPACE);
            }
        }

        //add a comment
//        resourceElement.addContent(new Comment("Description of a representation"));

        // add links
        List<Link> links = representation.getLinks();
        for (Link link : links) {
            Element linkElement = new Element(LINK);
            if (!link.getRel().equals(SELF)) {
                linkElement.setAttribute(REL, link.getRel());
                linkElement.setAttribute(HREF, link.getHref());
                if (link.getName().isPresent()) {
                    linkElement.setAttribute(NAME, link.getName().get());
                }
                if (link.getTitle().isPresent()) {
                    linkElement.setAttribute(TITLE, link.getTitle().get());
                }
                if (link.getHreflang().isPresent()) {
                    linkElement.setAttribute(HREFLANG, link.getHreflang().get());
                }
                if (link.hasTemplate()) {
                    linkElement.setAttribute(TEMPLATED, "true");
                }
                resourceElement.addContent(linkElement);
            }
        }

        // add properties
        for (Map.Entry<String, Optional<Object>> entry : representation.getProperties().entrySet()) {
            Element propertyElement = new Element(entry.getKey());
            if (entry.getValue().isPresent()) {
                propertyElement.setContent(new Text(entry.getValue().get().toString()));
            } else {
                propertyElement.setAttribute("nil", "true", XSI_NAMESPACE);
            }
            resourceElement.addContent(propertyElement);
        }

        // add subresources
        for (Map.Entry<String, Representation> halResource : representation.getResources().entries()) {
            Element subResourceElement = renderElement(halResource.getKey(), halResource.getValue(), true);
            resourceElement.addContent(subResourceElement);
        }

        return resourceElement;
    }

}
