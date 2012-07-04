package com.theoryinpractise.halbuilder.impl.api;

import com.google.common.base.Splitter;
import org.jdom.Namespace;

public class Support {

    public static final Splitter WHITESPACE_SPLITTER = Splitter.onPattern("\\s")
                                                               .omitEmptyStrings();


    public static final String REL = "rel";
    public static final String SELF = "self";
    public static final String LINK = "link";
    public static final String HREF = "href";
    public static final String LINKS = "_links";
    public static final String EMBEDDED = "_embedded";
    public static final String NAME = "name";
    public static final String CURIE = "curie";
    public static final String TITLE = "title";
    public static final String HREFLANG = "hreflang";
    public static final String TEMPLATED = "templated";

    /** 
     * Define the XML schema instance namespace, so we can use it when
     * rendering nil elements.
     */
    public static final Namespace XSI_NAMESPACE = Namespace.getNamespace(
            "xsi", "http://www.w3.org/2001/XMLSchema-instance");

}
