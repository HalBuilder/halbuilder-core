package com.theoryinpractise.halbuilder.impl.xml;

import com.theoryinpractise.halbuilder.ResourceFactory;
import com.theoryinpractise.halbuilder.impl.api.ResourceReader;
import com.theoryinpractise.halbuilder.impl.resources.MutableResource;
import com.theoryinpractise.halbuilder.spi.ReadableResource;
import com.theoryinpractise.halbuilder.spi.RenderableResource;
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

public class XmlResourceReader implements ResourceReader {
    private ResourceFactory resourceFactory;

    public XmlResourceReader(ResourceFactory resourceFactory) {
        this.resourceFactory = resourceFactory;
    }

    public RenderableResource read(Reader reader) {
        try {
            Document d = new SAXBuilder().build(reader);
            Element root = d.getRootElement();
            return readResource(root).asRenderableResource();
        } catch (JDOMException e) {
            throw new ResourceException(e);
        } catch (IOException e) {
            throw new ResourceException(e);
        }
    }

    private Resource readResource(Element root) {
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
            resource.withLink(link.getAttributeValue("href"), link.getAttributeValue("rel"));
        }

    }

    private void readProperties(Resource resource, Element element) {
        List<Element> properties = element.getChildren();
        for (Element property : properties) {
            if (!property.getName().matches("(link|resource)")) {
                resource.withProperty(property.getName(), property.getValue());
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
