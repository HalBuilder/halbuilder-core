package com.theoryinpractise.halbuilder;

import static org.fest.assertions.api.Assertions.assertThat;

import com.theoryinpractise.halbuilder.api.Representation;
import com.theoryinpractise.halbuilder.impl.representations.MutableRepresentation;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ResourceBasicMethodsTest {

  private DefaultRepresentationFactory representationFactory = new DefaultRepresentationFactory();

  private Representation resource;
  private Representation otherResource;
  private int resourceHashCode;

  @BeforeMethod
  public void setUpResources() {
    resource = createDefaultResource();
    otherResource = createDefaultResource();
    resourceHashCode = resource.hashCode();
  }

  public static class TestValue {
    private String name;

    public TestValue(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
  }

  private Representation createDefaultResource() {
    return representationFactory
        .newRepresentation("http://localhost/test")
        .withNamespace("testns", "http://example.com/test/{rel}")
        .withLink("testlink", "http://example.com/link")
        .withProperty("testprop", "value")
        .withProperty("nullprop", null)
        .withRepresentation("testsub", representationFactory.newRepresentation("/subtest"));
  }

  @Test
  public void equalResourcesHaveEqualHashCodes() {
    assertThat(resource.hashCode()).isEqualTo(otherResource.hashCode());
  }

  @Test
  public void testWithBeanProperties() {
    resource.withBean(new TestValue("Test"));
    assertThat(resource.getValue("name")).isEqualTo("Test");
  }

  @Test
  public void testHashCodeIsDependentOnNamespaces() {
    resource.withNamespace("testns2", "http://example.com/test2/{rel}");
    assertThat(resource.hashCode()).isNotEqualTo(resourceHashCode);
  }

  @Test
  public void testHashCodeIsDependentOnLinks() {
    resource.withLink("testlink2", "http://example.com/link2");
    assertThat(resource.hashCode()).isNotEqualTo(resourceHashCode);
  }

  @Test
  public void testHashCodeIsDependentOnProperties() {
    resource.withProperty("proptest2", "value2");
    assertThat(resource.hashCode()).isNotEqualTo(resourceHashCode);
  }

  @Test
  public void testHashCodeIsDependentOnNullProperties() {
    resource.withProperty("othernullprop", null);
    assertThat(resource.hashCode()).isNotEqualTo(resourceHashCode);
  }

  @Test
  public void testHashCodeIsDependentOnResources() {
    resource.withRepresentation("testsub2", representationFactory.newRepresentation("/subtest2"));
    assertThat(resource.hashCode()).isNotEqualTo(resourceHashCode);
  }

  @Test
  public void testEqualsIsDependentOnNamespaces() {
    resource.withNamespace("testns2", "http://example.com/test2/{rel}");
    assertThat(resource).isNotEqualTo(otherResource);
  }

  @Test
  public void testEqualsIsDependentOnLinks() {
    resource.withLink("testlink2", "http://example.com/link2");
    assertThat(resource).isNotEqualTo(otherResource);
  }

  @Test
  public void testEqualsIsDependentOnProperties() {
    resource.withProperty("proptest2", "value2");
    assertThat(resource).isNotEqualTo(otherResource);
  }

  @Test
  public void testEqualsIsDependentOnNullProperties() {
    resource.withProperty("othernullprop", null);
    assertThat(resource).isNotEqualTo(otherResource);
  }

  @Test
  public void testEqualsIsDependentOnResources() {
    resource.withRepresentation("testsub2", representationFactory.newRepresentation("/subtest2"));
    assertThat(resource).isNotEqualTo(otherResource);
  }

  @Test
  public void testToStringRendersSelfHref() {
    String toString = new MutableRepresentation(representationFactory, "http://localhost/test").toString();
    assertThat(toString).isEqualTo("<Representation: http://localhost/test>");
  }

  @Test
  public void testToStringRendersHashCode() {
    String toString = new MutableRepresentation(representationFactory).toString();
    assertThat(toString).matches("<Representation: @[0-9a-f]+>");
  }
}
