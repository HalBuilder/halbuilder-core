package com.theoryinpractise.halbuilder.xml;

import com.google.common.base.Optional;
import com.theoryinpractise.halbuilder.Link;
import com.theoryinpractise.halbuilder.ReadableResource;
import com.theoryinpractise.halbuilder.Renderer;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.Text;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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
            Element linkElement = new Element("link");
            if (!link.getRel().contains("self")) {
                linkElement.setAttribute("rel", link.getRel());
                linkElement.setAttribute("href", link.getHref());
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
        for (Map.Entry<String, Collection<ReadableResource>> resourceEntry : resource.getResources().asMap().entrySet()) {
            for (ReadableResource halResource : resourceEntry.getValue()) {
                Element subResourceElement = renderElement(halResource, true);
//                subResourceElement.setAttribute("rel", resourceEntry.getKey());
                resourceElement.addContent(subResourceElement);
            }
        }

        return resourceElement;
    }

}
