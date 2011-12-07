package com.theoryinpractise.halbuilder.reader;

import com.theoryinpractise.halbuilder.HalReader;
import com.theoryinpractise.halbuilder.HalResource;
import com.theoryinpractise.halbuilder.HalResourceException;
import org.jdom.*;
import org.jdom.input.SAXBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;

public class XmlHalReader implements HalReader {
    public HalResource read(String source) {
        try {
            Document d = new SAXBuilder().build(new StringReader(source));
            Element root = d.getRootElement();
            return readResource(root);
        } catch (JDOMException e) {
            throw new HalResourceException(e);
        } catch (IOException e) {
            throw new HalResourceException(e);
        }
    }

    private HalResource readResource(Element root) {
        HalResource halResource = HalResource.newHalResource("/");

        halResource.withHref(root.getAttributeValue("href"));
        readNamespaces(halResource, root);
        readLinks(halResource, root);
        readProperties(halResource, root);
        readResources(halResource, root);

        return halResource;
    }

    private void readNamespaces(HalResource halResource, Element element) {
        List<Namespace> namespaces = element.getAdditionalNamespaces();
        for (Namespace ns : namespaces) {
            halResource.withNamespace(ns.getPrefix(), ns.getURI());
        }
    }

    private void readLinks(HalResource halResource, Element element) {

        List<Element> links = element.getChildren("link");
        for (Element link : links) {
            halResource.withLink(link.getAttributeValue("rel"), link.getAttributeValue("href"));
        }

    }

    private void readProperties(HalResource halResource, Element element) {
        List<Element> properties = element.getChildren();
        for (Element property : properties) {
            if (!property.getName().matches("(link|resource)")) {
                halResource.withProperty(property.getName(), property.getValue());
            }
        }
    }

    private void readResources(HalResource halResource, Element element) {
        List<Element> resources = element.getChildren("resource");
        for (Element resource : resources) {
            String rel = resource.getAttributeValue("rel");
            HalResource subHalResource = readResource(resource);
            halResource.withSubresource(rel, subHalResource);
        }
    }
}
