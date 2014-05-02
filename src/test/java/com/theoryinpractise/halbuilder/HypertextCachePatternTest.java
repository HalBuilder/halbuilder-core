/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.theoryinpractise.halbuilder;

import com.theoryinpractise.halbuilder.api.Link;
import com.theoryinpractise.halbuilder.api.Representation;
import com.theoryinpractise.halbuilder.api.RepresentationFactory;

import org.testng.annotations.Test;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 *
 * @author pete.johanson
 */
public class HypertextCachePatternTest {
    @Test
    public void testNonHypertextCachePattern() {
        RepresentationFactory f = new DefaultRepresentationFactory();
        
        Representation embed = f.newRepresentation("/foo");
        Representation resource = f.newRepresentation("/bar").withRepresentation("foo", embed);
        
        assertThat(resource.getLinkByRel("foo"))
                .isNull();
    }
    
    @Test
    public void testHypertextCachePattern() {
        RepresentationFactory f = new DefaultRepresentationFactory().withFlag(RepresentationFactory.HYPERTEXT_CACHE_PATTERN);
        
        Representation embed = f.newRepresentation("/foo");
        Representation resource = f.newRepresentation("/bar").withRepresentation("foo", embed);
        
        assertThat(resource.getLinkByRel("foo"))
                .isEqualTo(new Link(f, "foo", "/foo"));
    }
    
    @Test
    public void testHypertextCachePatternWithoutSelfRelInEmbedded() {
        RepresentationFactory f = new DefaultRepresentationFactory().withFlag(RepresentationFactory.HYPERTEXT_CACHE_PATTERN);
        
        Representation embed = f.newRepresentation();
        Representation resource = f.newRepresentation("/bar").withRepresentation("foo", embed);
        
        assertThat(resource.getLinkByRel("foo"))
                .isNull();
    }
}
