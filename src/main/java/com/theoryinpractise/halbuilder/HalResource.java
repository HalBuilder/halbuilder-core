package com.theoryinpractise.halbuilder;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;

public class HalResource {

    private String href;
    private Map<String, String> namespaces = new HashMap<String, String>();
    private Multimap<String, String> links = ArrayListMultimap.create();
    private Map<String, Object> properties = new HashMap<String, Object>();
    private Multimap<String, HalResource> resources = ArrayListMultimap.create();

    private HalResource(String href) {
        this.href = href;
    }

    public static HalResource newHalResource(String href) {
        return new HalResource(href);
    }

    public HalResource withLink(String rel, String url) {
        links.put(rel, url);
        return this;
    }

    public HalResource withProperty(String name, Object value) {
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
        resources.put(rel, resource);
        return this;
    }

    private String resolveRelativeHref(String baseHref, String href) {
        try {
            if (href.startsWith("?")) {
                return new URL(baseHref + href).toExternalForm();
            } else if (href.startsWith("~/")) {
                return new URL(baseHref + href.substring(1)).toExternalForm();
            } else {
                return new URL(new URL(baseHref), href).toExternalForm();
            }
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
            resourceElement.addNamespaceDeclaration(Namespace.getNamespace(entry.getKey(), resolveRelativeHref(baseHref, entry.getValue())));
        }

        //add a comment
//        resourceElement.addContent(new Comment("Description of a resource"));

        // add links
        for (Map.Entry<String, Collection<String>> linkEntry : resource.links.asMap().entrySet()) {
            for (String url : linkEntry.getValue()) {
                Element linkElement = new Element("link");
                linkElement.setAttribute("rel", linkEntry.getKey());
                linkElement.setAttribute("href", resolveRelativeHref(baseHref, url));
                resourceElement.addContent(linkElement);
            }
        }

        // add properties
        for (Map.Entry<String, Object> entry : resource.properties.entrySet()) {
            Element propertyElement = new Element(entry.getKey());
            propertyElement.setContent(new Text(entry.getValue().toString()));
            resourceElement.addContent(propertyElement);
        }

        // add subresources
        for (Map.Entry<String, Collection<HalResource>> resourceEntry : resource.resources.asMap().entrySet()) {
            for (HalResource halResource : resourceEntry.getValue()) {
                String subResourceBaseHref = resolveRelativeHref(baseHref, halResource.href);
                Element subResourceElement = renderElement(subResourceBaseHref, halResource);
                subResourceElement.setAttribute("rel", resourceEntry.getKey());
                resourceElement.addContent(subResourceElement);
            }
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
        for (Map.Entry<String, Collection<HalResource>> entry : resource.resources.asMap().entrySet()) {
            for (HalResource halResource : entry.getValue()) {
                validateNamespaces(resource.href, entry.getKey());
                validateNamespaces(halResource);
            }
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
                g.writeStringField(entry.getKey(), resolveRelativeHref(baseHref, entry.getValue()));
            }
            g.writeEndObject();
        }

        if (!resource.links.isEmpty()) {
            g.writeObjectFieldStart("_links");
            for (Map.Entry<String, Collection<String>> linkEntry : resource.links.asMap().entrySet()) {
                if (linkEntry.getValue().size() == 1) {
                    g.writeObjectFieldStart(linkEntry.getKey());
                    g.writeStringField("_href", resolveRelativeHref(href, linkEntry.getValue().iterator().next()));
                    g.writeEndObject();
                } else {
                    g.writeArrayFieldStart(linkEntry.getKey());
                    for (String url : linkEntry.getValue()) {
                        g.writeStartObject();
                        g.writeStringField("_href", resolveRelativeHref(href, url));
                        g.writeEndObject();
                    }
                    g.writeEndArray();
                }
            }
            g.writeEndObject();
        }

        for (Map.Entry<String, Object> entry : resource.properties.entrySet()) {
            g.writeObjectField(entry.getKey(), entry.getValue());
        }

        if (!resource.resources.isEmpty()) {
            g.writeObjectFieldStart("_resources");
            for (Map.Entry<String, Collection<HalResource>> resourceEntry : resource.resources.asMap().entrySet()) {
                if (resourceEntry.getValue().size() == 1) {
                    g.writeObjectFieldStart(resourceEntry.getKey());
                    HalResource subResource = resourceEntry.getValue().iterator().next();
                    String subResourceBaseHref = resolveRelativeHref(baseHref, subResource.href);
                    renderJson(subResourceBaseHref, g, subResource);
                    g.writeEndObject();
                } else {
                    g.writeArrayFieldStart(resourceEntry.getKey());
                    for (HalResource halResource : resourceEntry.getValue()) {
                        g.writeStartObject();
                        HalResource subResource = resourceEntry.getValue().iterator().next();
                        String subResourceBaseHref = resolveRelativeHref(baseHref, subResource.href);
                        renderJson(subResourceBaseHref, g, subResource);
                        g.writeEndObject();

                    }
                }
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
