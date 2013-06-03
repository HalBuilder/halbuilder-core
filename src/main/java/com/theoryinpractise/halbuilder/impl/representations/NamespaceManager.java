package com.theoryinpractise.halbuilder.impl.representations;

import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.theoryinpractise.halbuilder.api.RepresentationException;

import java.util.Map;
import java.util.TreeMap;

import static com.theoryinpractise.halbuilder.impl.api.Support.WHITESPACE_SPLITTER;
import static java.lang.String.format;

public class NamespaceManager {

    private TreeMap<String, String> namespaces = Maps.newTreeMap(Ordering.usingToString());

    public TreeMap<String, String> getNamespaces() {
        return namespaces;
    }

    public NamespaceManager withNamespace(String namespace, String href) {
        if (namespaces.containsKey(namespace)) {
            throw new RepresentationException(format("Duplicate namespace '%s' found for representation factory", namespace));
        }
        if (!href.contains("{rel}")) {
            throw new RepresentationException(format("Namespace '%s' does not include {rel} URI template argument.", namespace));
        }
        namespaces.put(namespace, href);
        return this;
    }


    public void validateNamespaces(String sourceRel) {
        for (String rel : WHITESPACE_SPLITTER.split(sourceRel)) {
            if (!rel.contains("://") && rel.contains(":")) {
                String[] relPart = rel.split(":");
                if (!namespaces.keySet().contains(relPart[0])) {
                    throw new RepresentationException(format("Undeclared namespace in rel %s for resource", rel));
                }
            }
        }
    }

    public String currieHref(String href) {
        for (Map.Entry<String, String> entry : namespaces.entrySet()) {

            String nsRef = entry.getValue();
            int startIndex = nsRef.indexOf("{rel}");
            int endIndex = startIndex + 5;

            String left = nsRef.substring(0, startIndex);
            String right = nsRef.substring(endIndex);

            if (href.startsWith(left) && href.endsWith(right)) {
                return entry.getKey() + ":" + href.substring(startIndex, endIndex - 2);
            }
        }
        return href;
    }

    public String resolve(String ns) {
        if (!ns.contains(":")) {
            throw new RepresentationException("Namespaced value does not include : - not namespaced?");
        }
        String[] parts = ns.split(":");
        if (namespaces.containsKey(parts[0])) {
            return namespaces.get(parts[0]).replace("{rel}", parts[1]);
        } else {
            throw new RepresentationException("Unknown namespace key: " + parts[0]);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NamespaceManager that = (NamespaceManager) o;

        if (namespaces != null ? !namespaces.equals(that.namespaces) : that.namespaces != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return namespaces != null ? namespaces.hashCode() : 0;
    }
}
