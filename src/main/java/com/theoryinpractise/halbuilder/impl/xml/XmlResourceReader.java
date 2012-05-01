package com.theoryinpractise.halbuilder.impl.xml;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.theoryinpractise.halbuilder.ResourceFactory;
import com.theoryinpractise.halbuilder.impl.api.ResourceReader;
import com.theoryinpractise.halbuilder.impl.resources.MutableResource;
import com.theoryinpractise.halbuilder.spi.ReadableResource;
import com.theoryinpractise.halbuilder.spi.Resource;
import com.theoryinpractise.halbuilder.spi.ResourceException;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;

import java.io.IOException;
import java.io.Reader;
import java.util.List;

import static com.theoryinpractise.halbuilder.impl.api.Support.HREFLANG;
import static com.theoryinpractise.halbuilder.impl.api.Support.NAME;
import static com.theoryinpractise.halbuilder.impl.api.Support.TITLE;
import static com.theoryinpractise.halbuilder.impl.api.Support.XSI_NAMESPACE;

public class XmlResourceReader implements ResourceReader {
    private ResourceFactory resourceFactory;

    public XmlResourceReader(ResourceFactory resourceFactory) {
        this.resourceFactory = resourceFactory;
    }

    public ReadableResource read(Reader reader) {
        try {
            Document d = new SAXBuilder().build(reader);
            Element root = d.getRootElement();
            return readResource(root).toImmutableResource();
        } catch (JDOMException e) {
            throw new ResourceException(e);
        } catch (IOException e) {
            throw new ResourceException(e);
        }
    }

    private MutableResource readResource(Element root) {
        String href = root.getAttributeValue("href");
        MutableResource resource = new MutableResource(resourceFactory, href);

        readNamespaces(resource, root);
        readLinks(resource, root);
        readProperties(resource, root);
        readResources(resource, root);

        return resource;
    }

    private void readNamespaces(Resource resource, Element element) {
        List<Namespace> namespaces = element.getAdditionalNamespaces();
        for (Namespace ns : namespaces) {
            resource.withNamespace(ns.getPrefix(), ns.getURI());
        }
    }

    private void readLinks(Resource resource, Element element) {

        List<Element> links = element.getChildren("link");
        for (Element link : links) {
            String rel = link.getAttributeValue("rel");
            String href = link.getAttributeValue("href");
            Optional<String> name = optionalElementValueAsText(link, NAME);
            Optional<String> title = optionalElementValueAsText(link, TITLE);
            Optional<String> hreflang = optionalElementValueAsText(link, HREFLANG);
            Optional<Predicate<ReadableResource>> predicate = Optional.<Predicate<ReadableResource>>absent();

            resource.withLink(href, rel, predicate, name, title, hreflang);
        }

    }

    Optional<String> optionalElementValueAsText(Element node, String key) {
        String value = node.getAttributeValue(key);
        return value != null ? Optional.of(value) : Optional.<String>absent();
    }

    private void readProperties(Resource resource, Element element) {
        List<Element> properties = element.getChildren();
        for (Element property : properties) {
            if (!property.getName().matches("(link|resource)")) {
                if (property.getAttribute("nil", XSI_NAMESPACE) != null) {
                    resource.withProperty(property.getName(), null);
                } else {
                resource.withProperty(property.getName(), property.getValue());
            }
        }
    }
    }

    private void readResources(Resource halResource, Element element) {
        List<Element> resources = element.getChildren("resource");
        for (Element resource : resources) {
            String rel = resource.getAttributeValue("rel");
            Resource subResource = readResource(resource);
            halResource.withSubresource(rel, subResource);
        }
    }
}
