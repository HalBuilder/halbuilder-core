package com.theoryinpractise.halbuilder.impl.xml;

import com.google.common.base.Strings;
import com.theoryinpractise.halbuilder.api.Link;
import com.theoryinpractise.halbuilder.api.ReadableRepresentation;
import com.theoryinpractise.halbuilder.api.Renderer;
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

    public void render(ReadableRepresentation representation, Writer writer) {
        final Element element = renderElement("self", representation, false);
        final XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
        try {
            outputter.output(element, writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private Element renderElement(String rel, ReadableRepresentation representation, boolean embedded) {

        final Link resourceLink = representation.getResourceLink();

        // Create the root element
        final Element resourceElement = new Element("resource");
        if (resourceLink != null) {
            resourceElement.setAttribute("href", resourceLink.getHref());
        }

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
                if (!Strings.isNullOrEmpty(link.getName())) {
                    linkElement.setAttribute(NAME, link.getName());
                }
                if (!Strings.isNullOrEmpty(link.getTitle())) {
                    linkElement.setAttribute(TITLE, link.getTitle());
                }
                if (!Strings.isNullOrEmpty(link.getHreflang())) {
                    linkElement.setAttribute(HREFLANG, link.getHreflang());
                }
                if (link.hasTemplate()) {
                    linkElement.setAttribute(TEMPLATED, "true");
                }
                resourceElement.addContent(linkElement);
            }
        }

        // add properties
        for (Map.Entry<String, Object> entry : representation.getProperties().entrySet()) {
            Element propertyElement = new Element(entry.getKey());
            if (entry.getValue() != null) {
                propertyElement.setContent(new Text(entry.getValue().toString()));
            } else {
                propertyElement.setAttribute("nil", "true", XSI_NAMESPACE);
            }
            resourceElement.addContent(propertyElement);
        }

        // add subresources
        for (Map.Entry<String, ReadableRepresentation> halResource : representation.getResources()) {
            Element subResourceElement = renderElement(halResource.getKey(), halResource.getValue(), true);
            resourceElement.addContent(subResourceElement);
        }

        return resourceElement;
    }

}
