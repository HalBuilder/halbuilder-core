package com.theoryinpractise.halbuilder.xml;

import com.google.common.base.Optional;
import com.theoryinpractise.halbuilder.Renderer;
import com.theoryinpractise.halbuilder.ReadableResource;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.Text;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.Map;

import static com.theoryinpractise.halbuilder.MutableResource.resolveRelativeHref;

public class XmlRenderer<T> implements Renderer<T> {

    public Optional<T> render(ReadableResource resource, Writer writer) {
        Element element = renderElement(resource.getHref(), resource, false);
        XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
        try {
            outputter.output(element, writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return Optional.absent();
    }

    private Element renderElement(String baseHref, ReadableResource resource, boolean embedded) {

        // Create the root element
        Element resourceElement = new Element("resource");
        //add an attribute to the root element
        resourceElement.setAttribute("href", baseHref);


        // Only add namespaces to non-embedded resources
        if (!embedded) {
            for (Map.Entry<String, String> entry : resource.getNamespaces().entrySet()) {
                resourceElement.addNamespaceDeclaration(
                        Namespace.getNamespace(entry.getKey(), resolveRelativeHref(baseHref, entry.getValue())));
            }
        }

        //add a comment
//        resourceElement.addContent(new Comment("Description of a resource"));

        // add links
        for (Map.Entry<String, Collection<String>> linkEntry : resource.getLinks().asMap().entrySet()) {
            for (String url : linkEntry.getValue()) {
                Element linkElement = new Element("link");
                linkElement.setAttribute("rel", linkEntry.getKey());
                linkElement.setAttribute("href", resolveRelativeHref(baseHref, url));
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
                String subResourceBaseHref = resolveRelativeHref(baseHref, halResource.getHref());
                Element subResourceElement = renderElement(subResourceBaseHref, halResource, true);
                subResourceElement.setAttribute("rel", resourceEntry.getKey());
                resourceElement.addContent(subResourceElement);
            }
        }

        return resourceElement;
    }

}
