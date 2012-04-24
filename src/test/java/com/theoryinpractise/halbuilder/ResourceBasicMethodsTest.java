package com.theoryinpractise.halbuilder;

import static org.fest.assertions.Assertions.assertThat;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.theoryinpractise.halbuilder.ResourceFactory;
import com.theoryinpractise.halbuilder.impl.resources.MutableResource;
import com.theoryinpractise.halbuilder.spi.Resource;

public class ResourceBasicMethodsTest {

    private ResourceFactory resourceFactory = new ResourceFactory("http://localhost/");

    private Resource resource;
    private Resource otherResource;
    private int resourceHashCode;
    
    @BeforeMethod
    public void setUpResources() {
        resource = createDefaultResource();
        otherResource = createDefaultResource();
        resourceHashCode = resource.hashCode();
    }
    

    private Resource createDefaultResource() {
        return resourceFactory.newResource("/test")
                .withNamespace("testns", "http://example.com/test")
                .withLink("http://example.com/link", "testlink")
                .withProperty("testprop", "value")
                .withSubresource("testsub", resourceFactory.newResource("/subtest"));
    }
    
    @Test
    public void equalResourcesHaveEqualHashCodes() {
        assertThat(resource.hashCode()).isEqualTo(otherResource.hashCode());
    }
    
    @Test
    public void testHashCodeIsDependentOnNamespaces() {
        resource.withNamespace("testns2", "http://example.com/test2");
        assertThat(resource.hashCode()).isNotEqualTo(resourceHashCode);
    }

    @Test
    public void testHashCodeIsDependentOnLinks() {
        resource.withLink("http://example.com/link2", "testlink2");
        assertThat(resource.hashCode()).isNotEqualTo(resourceHashCode);
    }
    
    @Test
    public void testHashCodeIsDependentOnProperties() {
        resource.withProperty("proptest2", "value2");
        assertThat(resource.hashCode()).isNotEqualTo(resourceHashCode);
    }
    
    @Test
    public void testHashCodeIsDependentOnResources() {
        resource.withSubresource("testsub2", resourceFactory.newResource("/subtest2"));
        assertThat(resource.hashCode()).isNotEqualTo(resourceHashCode);
    }
    
    @Test
    public void testEqualsIsDependentOnNamespaces() {
        resource.withNamespace("testns2", "http://example.com/test2");
        assertThat(resource).isNotEqualTo(otherResource);
    }
    
    @Test
    public void testEqualsIsDependentOnLinks() {
        resource.withLink("http://example.com/link2", "testlink2");
        assertThat(resource).isNotEqualTo(otherResource);
    }
    
    @Test
    public void testEqualsIsDependentOnProperties() {
        resource.withProperty("proptest2", "value2");
        assertThat(resource).isNotEqualTo(otherResource);
    }
    
    @Test
    public void testEqualsIsDependentOnResources() {
        resource.withSubresource("testsub2", resourceFactory.newResource("/subtest2"));
        assertThat(resource).isNotEqualTo(otherResource);
    }
    
    @Test
    public void testToStringRendersSelfHref() {
        String toString = new MutableResource(resourceFactory, "/test").toString();
        assertThat(toString).isEqualTo("<Resource: http://localhost/test>");
    }
    
    @Test
    public void testToStringRendersHashCode() {
        String toString = new MutableResource(resourceFactory).toString();
        assertThat(toString).matches("<Resource: @[0-9a-f]+>");
    }
}
