package com.theoryinpractise.halbuilder.spi;

import com.google.common.base.Optional;
import com.theoryinpractise.halbuilder.ResourceFactory;

/**
 * A Link to an exteral resource.
 */
public class Link {
    private ResourceFactory resourceFactory;

    private String href;
    private String rel;
    private Optional<String> name = Optional.absent();
    private Optional<String> title = Optional.absent();
    private Optional<String> hreflang = Optional.absent();

    public Link(ResourceFactory resourceFactory, String href, String rel) {
        this.resourceFactory = resourceFactory;
        this.href = href;
        this.rel = rel;
    }

    public Link(ResourceFactory resourceFactory, String href, String rel, Optional<String> name, Optional<String> title, Optional<String> hreflang) {
        this.resourceFactory = resourceFactory;
        this.href = href;
        this.rel = rel;
        this.name = name;
        this.title = title;
        this.hreflang = hreflang;
    }

    public String getHref() {
        return href;
    }

    public String getRel() {
        return rel;
    }

    public Optional<String> getName() {
        return name;
    }

    public Optional<String> getTitle() {
        return title;
    }

    public Optional<String> getHreflang() {
        return hreflang;
    }
    
    @Override
    public int hashCode() {
        int h = href.hashCode();
        h += rel.hashCode();
        h += name.hashCode();
        h += title.hashCode();
        h += hreflang.hashCode();
        return h;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Link)) {
            return false;
        }
        Link that = (Link) obj;
        boolean e = this.href.equals(that.href);
        e &= this.rel.equals(that.rel);
        e &= this.name.equals(that.name);
        e &= this.title.equals(that.title);
        e &= this.hreflang.equals(that.hreflang);
        return e;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("<link rel=\"").append(rel).append("\" href=\"").append(href).append("\"");
        if (name.isPresent()) {
            sb.append(" name=\"").append(name.get()).append("\"");
        }
        if (title.isPresent()) {
            sb.append(" title=\"").append(title.get()).append("\"");
        }
        if (hreflang.isPresent()) {
            sb.append(" hreflang=\"").append(hreflang.get()).append("\"");
        }
        sb.append("/>");

        return sb.toString();
    }
}
