package com.theoryinpractise.halbuilder;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.util.DefaultPrettyPrinter;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.Text;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;

public class HalResource {

    private String href;
    private Map<String, String> namespaces = new HashMap<String, String>();
    private Map<String, String> links = new HashMap<String, String>();
    private Map<String, String> properties = new HashMap<String, String>();
    private Map<String, HalResource> resources = new HashMap<String, HalResource>();

    private HalResource(String href) {
        this.href = href;
    }

    public static HalResource newHalResource(String href) {
        return new HalResource(href);
    }

    public HalResource withLink(String rel, String url) {
        if (links.containsKey(rel)) {
            throw new HalResourceException(format("Duplicate link '%s' found for resource %s", rel, href));
        }
        links.put(rel, url);
        return this;
    }

    public HalResource withProperty(String name, String value) {
        if (properties.containsKey(name)) {
            throw new HalResourceException(format("Duplicate property '%s' found for resource %s", name, href));
        }
        properties.put(name, value);
        return this;
    }

    public HalResource withNamespace(String namespace, String url) {
        if (namespaces.containsKey(namespace)) {
            throw new HalResourceException(format("Duplicate namespace '%s' found for resource %s", namespace, href));
        }
        namespaces.put(namespace, url);
        return this;
    }

    public HalResource withSubresource(String rel, HalResource resource) {
        if (resources.containsKey(rel)) {
            throw new HalResourceException(format("Duplicate subresource rel '%s' found for resource %s", rel, href));
        }
        resources.put(rel, resource);
        return this;
    }

    private String resolveRelativeHref(String baseHref, String href) {
        try {
            return new URL(new URL(baseHref), href).toExternalForm();
        } catch (MalformedURLException e) {
            throw new HalResourceException(e.getMessage());
        }
    }

    private Element renderElement(String baseHref, HalResource resource) {

        // Create the root element
        Element resourceElement = new Element("resource");
        //add an attribute to the root element
        resourceElement.setAttribute("href", baseHref);
        for (Map.Entry<String, String> entry : resource.namespaces.entrySet()) {
            resourceElement.addNamespaceDeclaration(Namespace.getNamespace(entry.getKey(), entry.getValue()));
        }

        //add a comment
//        resourceElement.addContent(new Comment("Description of a resource"));

        // add links
        for (Map.Entry<String, String> entry : resource.links.entrySet()) {
            Element linkElement = new Element("link");
            linkElement.setAttribute("rel", entry.getKey());
            linkElement.setAttribute("href", resolveRelativeHref(baseHref, entry.getValue()));
            resourceElement.addContent(linkElement);
        }

        // add properties
        for (Map.Entry<String, String> entry : resource.properties.entrySet()) {
            Element propertyElement = new Element(entry.getKey());
            propertyElement.setContent(new Text(entry.getValue()));
            resourceElement.addContent(propertyElement);
        }

        // add subresources
        for (Map.Entry<String, HalResource> entry : resource.resources.entrySet()) {
            String subResourceBaseHref = resolveRelativeHref(baseHref, entry.getValue().href);
            Element subResource = renderElement(subResourceBaseHref, entry.getValue());
            subResource.setAttribute("rel", entry.getKey());
            resourceElement.addContent(subResource);
        }

        return resourceElement;
    }

    public HalResource withBaseHref(String baseHref) {
        try {
            this.href = new URL(new URL(baseHref), this.href).toExternalForm();
        } catch (MalformedURLException e) {
            throw new HalResourceException(e.getMessage());
        }
        return this;
    }

    private void validateNamespaces(HalResource resource) {
        for (String rel : resource.links.keySet()) {
            validateNamespaces(resource.href, rel);
        }
        for (Map.Entry<String, HalResource> entry : resource.resources.entrySet()) {
            validateNamespaces(resource.href, entry.getKey());
            validateNamespaces(entry.getValue());
        }
    }

    private void validateNamespaces(String href, String sourceRel) {
        for (String rel : sourceRel.split(" ")) {
            if (rel.contains(":")) {
                String[] relPart = rel.split(":");
                if (!namespaces.keySet().contains(relPart[0])) {
                    throw new HalResourceException(format("Undeclared namespace in rel %s for resource %s", rel, href));
                }
            }
        }
    }

    public String renderJson() {
        validateNamespaces(this);

        StringWriter sw = new StringWriter();
        JsonFactory f = new JsonFactory();
        f.enable(JsonGenerator.Feature.QUOTE_FIELD_NAMES);

        try {
            JsonGenerator g = f.createJsonGenerator(sw);
            g.setPrettyPrinter(new DefaultPrettyPrinter());

            g.writeStartObject();
            renderJson(href, g, this);
            g.writeEndObject();
            g.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return sw.toString();
    }

    private void renderJson(String baseHref, JsonGenerator g, HalResource resource) throws IOException {

        g.writeStringField("_href", resolveRelativeHref(href, resource.href));

        if (!resource.namespaces.isEmpty()) {
            g.writeObjectFieldStart("_curies");
            for (Map.Entry<String, String> entry : resource.namespaces.entrySet()) {
                g.writeStringField(entry.getKey(), entry.getValue());
            }
            g.writeEndObject();
        }

        if (!resource.links.isEmpty()) {
            g.writeObjectFieldStart("_links");
            for (Map.Entry<String, String> entry : resource.links.entrySet()) {
                g.writeObjectFieldStart(entry.getKey());
                g.writeStringField("_href", resolveRelativeHref(href, entry.getValue()));
                g.writeEndObject();
            }
            g.writeEndObject();
        }

        for (Map.Entry<String, String> entry : resource.properties.entrySet()) {
            g.writeStringField(entry.getKey(), entry.getValue());
        }

        if (!resource.resources.isEmpty()) {
            g.writeObjectFieldStart("_resources");
            for (Map.Entry<String, HalResource> entry : resource.resources.entrySet()) {
                g.writeObjectFieldStart(entry.getKey());
                String subResourceBaseHref = resolveRelativeHref(baseHref, entry.getValue().href);

                renderJson(subResourceBaseHref, g, entry.getValue());
                g.writeEndObject();
            }
        }

    }

    public String renderXml() {
        validateNamespaces(this);
        Element element = renderElement(href, this);
        XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
        return outputter.outputString(element);
    }
}
