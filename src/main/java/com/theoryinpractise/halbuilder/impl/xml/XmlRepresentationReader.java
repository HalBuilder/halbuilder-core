package com.theoryinpractise.halbuilder.impl.xml;

import com.theoryinpractise.halbuilder.api.ReadableRepresentation;
import com.theoryinpractise.halbuilder.api.Representation;
import com.theoryinpractise.halbuilder.api.RepresentationException;
import com.theoryinpractise.halbuilder.api.RepresentationFactory;
import com.theoryinpractise.halbuilder.impl.api.RepresentationReader;
import com.theoryinpractise.halbuilder.impl.representations.MutableRepresentation;
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

public class XmlRepresentationReader implements RepresentationReader {
    private RepresentationFactory representationFactory;

    public XmlRepresentationReader(RepresentationFactory representationFactory) {
        this.representationFactory = representationFactory;
    }

    public ReadableRepresentation read(Reader reader) {
        try {
            Document d = new SAXBuilder().build(reader);
            Element root = d.getRootElement();
            return readRepresentation(root).toImmutableResource();
        } catch (JDOMException e) {
            throw new RepresentationException(e);
        } catch (IOException e) {
            throw new RepresentationException(e);
        }
    }

    private MutableRepresentation readRepresentation(Element root) {
        String href = root.getAttributeValue("href");
        MutableRepresentation resource = new MutableRepresentation(representationFactory, href);

        readNamespaces(resource, root);
        readLinks(resource, root);
        readProperties(resource, root);
        readResources(resource, root);

        return resource;
    }

    private void readNamespaces(Representation resource, Element element) {
        List<Namespace> namespaces = element.getAdditionalNamespaces();
        for (Namespace ns : namespaces) {
            resource.withNamespace(ns.getPrefix(), ns.getURI());
        }
    }

    private void readLinks(Representation resource, Element element) {

        List<Element> links = element.getChildren("link");
        for (Element link : links) {
            String rel = link.getAttributeValue("rel");
            String href = link.getAttributeValue("href");
            String name = link.getAttributeValue( NAME);
            String title = link.getAttributeValue( TITLE);
            String hreflang = link.getAttributeValue( HREFLANG);

            resource.withLink(rel, href, name, title, hreflang);
        }

    }

    private void readProperties(Representation resource, Element element) {
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

    private void readResources(Representation halResource, Element element) {
        List<Element> resources = element.getChildren("resource");
        for (Element resource : resources) {
            String rel = resource.getAttributeValue("rel");
            Representation subResource = readRepresentation(resource);
            halResource.withRepresentation(rel, subResource);
        }
    }
}
