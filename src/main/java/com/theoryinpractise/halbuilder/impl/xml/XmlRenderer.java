package com.theoryinpractise.halbuilder.impl.xml;

import com.google.common.base.Optional;
import com.theoryinpractise.halbuilder.api.Link;
import com.theoryinpractise.halbuilder.api.ReadableResource;
import com.theoryinpractise.halbuilder.api.Renderer;
import com.theoryinpractise.halbuilder.api.Resource;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.Text;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;

import static com.theoryinpractise.halbuilder.api.Fields.HREF;
import static com.theoryinpractise.halbuilder.api.Fields.HREFLANG;
import static com.theoryinpractise.halbuilder.api.Fields.LINK;
import static com.theoryinpractise.halbuilder.api.Fields.NAME;
import static com.theoryinpractise.halbuilder.api.Fields.REL;
import static com.theoryinpractise.halbuilder.api.Fields.SELF;
import static com.theoryinpractise.halbuilder.api.Fields.TITLE;


public class XmlRenderer<T> implements Renderer<T> {

    public Optional<T> render(ReadableResource resource, Writer writer) {
        final Element element = renderElement(resource, false);
        final XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
        try {
            outputter.output(element, writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return Optional.absent();
    }

    private Element renderElement(ReadableResource resource, boolean embedded) {

        final Link selfLink = resource.getSelfLink();
        final String href = selfLink.getHref();

        // Create the root element
        final Element resourceElement = new Element("resource");
        resourceElement.setAttribute("href", href);
        if (!selfLink.getRel().equals("self")) {
            resourceElement.setAttribute("rel", selfLink.getRel());
        }

        // Only add namespaces to non-embedded resources
        if (!embedded) {
            for (Map.Entry<String, String> entry : resource.getNamespaces().entrySet()) {
                resourceElement.addNamespaceDeclaration(
                        Namespace.getNamespace(entry.getKey(), entry.getValue()));
            }
        }

        //add a comment
//        resourceElement.addContent(new Comment("Description of a resource"));

        // add links
        List<Link> links = resource.getLinks();
        for (Link link : links) {
            Element linkElement = new Element(LINK);
            if (!link.getRel().contains(SELF)) {
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
                resourceElement.addContent(linkElement);
            }
        }

        // add properties
        for (Map.Entry<String, Object> entry : resource.getProperties().entrySet()) {
            Element propertyElement = new Element(entry.getKey());
            propertyElement.setContent(new Text(entry.getValue().toString()));
            resourceElement.addContent(propertyElement);
        }

        // add subresources
        for (Resource halResource : resource.getResources()) {
            Element subResourceElement = renderElement(halResource, true);
            resourceElement.addContent(subResourceElement);
        }

        return resourceElement;
    }

}
