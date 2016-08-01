package com.theoryinpractise.halbuilder5;

import javaslang.collection.TreeMap;
import javaslang.control.Option;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static com.google.common.truth.Truth.assertThat;
import static javaslang.control.Option.none;
import static javaslang.control.Option.some;

public class ResourceBasicMethodsTest {

  private ResourceRepresentation<TreeMap<String, Option<String>>> resource;
  private ResourceRepresentation<TreeMap<String, Option<String>>> otherResource;
  private int resourceHashCode;

  @BeforeMethod
  public void setUpResources() {
    resource = createDefaultResource();
    otherResource = createDefaultResource();
    resourceHashCode = resource.hashCode();
  }

  private ResourceRepresentation<TreeMap<String, Option<String>>> createDefaultResource() {
    TreeMap<String, Option<String>> properties =
        TreeMap.of("testprop", some("value"), "nullprop", none());
    return ResourceRepresentation.create("http://localhost/test")
        .withNamespace("testns", "http://example.com/test/{rel}")
        .withLink("testlink", "http://example.com/link")
        .withValue(properties)
        .withRepresentation("testsub", ResourceRepresentation.empty("/subtest"));
  }

  @Test
  public void equalResourcesHaveEqualHashCodes() {
    assertThat(resource.hashCode()).isEqualTo(otherResource.hashCode());
  }

  @Test
  public void testHashCodeIsDependentOnNamespaces() {
    ResourceRepresentation<TreeMap<String, Option<String>>> updatedResource =
        resource.withNamespace("testns2", "http://example.com/test2/{rel}");
    assertThat(resource).isEqualTo(otherResource);
    assertThat(updatedResource.hashCode()).isNotEqualTo(resourceHashCode);
  }

  @Test
  public void testHashCodeIsDependentOnLinks() {
    ResourceRepresentation<TreeMap<String, Option<String>>> updatedResource =
        resource.withLink("testlink2", "http://example.com/link2");
    assertThat(resource).isEqualTo(otherResource);
    assertThat(updatedResource.hashCode()).isNotEqualTo(resourceHashCode);
  }

  @Test
  public void testHashCodeIsDependentOnProperties() {
    ResourceRepresentation<TreeMap<String, Option<String>>> updatedResource =
        resource.map(values -> values.put("proptest2", some("value2")));
    assertThat(resource).isEqualTo(otherResource);
    assertThat(updatedResource).isNotEqualTo(resource);
    assertThat(updatedResource).isNotEqualTo(otherResource);
    assertThat(updatedResource.hashCode()).isNotEqualTo(resourceHashCode);
  }

  @Test
  public void testHashCodeIsDependentOnNullProperties() {
    ResourceRepresentation<TreeMap<String, Option<String>>> updatedResource =
        resource.map(values -> values.put("othernullprop", none()));
    assertThat(resource).isEqualTo(otherResource);
    assertThat(updatedResource.hashCode()).isNotEqualTo(resourceHashCode);
  }

  @Test
  public void testHashCodeIsDependentOnResources() {
    ResourceRepresentation<TreeMap<String, Option<String>>> updatedResource =
        resource.withRepresentation("testsub2", ResourceRepresentation.empty("/subtest2"));
    assertThat(resource).isEqualTo(otherResource);
    assertThat(updatedResource.hashCode()).isNotEqualTo(resourceHashCode);
  }

  @Test
  public void testEqualsIsDependentOnNamespaces() {
    ResourceRepresentation<TreeMap<String, Option<String>>> updatedResource =
        resource.withNamespace("testns2", "http://example.com/test2/{rel}");
    assertThat(resource).isEqualTo(otherResource);
    assertThat(updatedResource).isNotEqualTo(otherResource);
  }

  @Test
  public void testEqualsIsDependentOnLinks() {
    ResourceRepresentation<TreeMap<String, Option<String>>> updatedResource =
        resource.withLink("testlink2", "http://example.com/link2");
    assertThat(resource).isEqualTo(otherResource);
    assertThat(updatedResource).isNotEqualTo(otherResource);
  }

  @Test
  public void testEqualsIsDependentOnProperties() {
    ResourceRepresentation<TreeMap<String, Option<String>>> updatedResource =
        resource.map(values -> values.put("proptest2", some("value2")));
    assertThat(resource).isEqualTo(otherResource);
    assertThat(updatedResource).isNotEqualTo(otherResource);
  }

  @Test
  public void testEqualsIsDependentOnNullProperties() {
    ResourceRepresentation<TreeMap<String, Option<String>>> updatedResource =
        resource.map(values -> values.put("othernullprop", none()));
    assertThat(resource).isEqualTo(otherResource);
    assertThat(updatedResource).isNotEqualTo(otherResource);
  }

  @Test
  public void testEqualsIsDependentOnResources() {
    ResourceRepresentation<TreeMap<String, Option<String>>> updatedResource =
        resource.withRepresentation("testsub2", ResourceRepresentation.empty("/subtest2"));
    assertThat(resource).isEqualTo(otherResource);
    assertThat(updatedResource).isNotEqualTo(otherResource);
  }

  @Test
  public void testToStringRendersSelfHref() {
    String toString =
        ResourceRepresentation.empty().withLink("self", "http://localhost/test").toString();
    assertThat(toString).isEqualTo("<Representation: http://localhost/test>");
  }

  @Test
  public void testToStringRendersHashCode() {
    String toString = ResourceRepresentation.empty().toString();
    assertThat(toString).matches("<Representation: @[0-9a-f]+>");
  }
}
