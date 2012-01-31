package com.theoryinpractise.halbuilder.impl.resources;

import com.google.common.base.Predicate;
import com.theoryinpractise.halbuilder.spi.Link;

import javax.annotation.Nullable;

public class SelfLinkPredicate implements Predicate<Link> {
    public boolean apply(@Nullable Link relatable) {
        return relatable.getRel().contains("self");
    }
}
