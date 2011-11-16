package com.theoryinpractise.halbuilder;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.theoryinpractise.halbuilder.renderer.JsonHalRenderer;
import com.theoryinpractise.halbuilder.renderer.XmlHalRenderer;

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

    public String getHref() {
        return href;
    }

    public Map<String, String> getNamespaces() {
        return namespaces;
    }

    public Multimap<String, String> getLinks() {
        return links;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public Multimap<String, HalResource> getResources() {
        return resources;
    }

    public static String resolveRelativeHref(String baseHref, String href) {
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
        return renderJson(new JsonHalRenderer());
    }

    public String renderXml() {
        return renderJson(new XmlHalRenderer());
    }

    private String renderJson(final HalRenderer renderer) {
        validateNamespaces(this);

        StringWriter sw = new StringWriter();
        renderer.render(this, sw);
        return sw.toString();
    }
}
