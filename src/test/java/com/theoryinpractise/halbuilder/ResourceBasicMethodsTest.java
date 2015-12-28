package com.theoryinpractise.halbuilder;

import com.theoryinpractise.halbuilder.api.Representation;
import com.theoryinpractise.halbuilder.impl.representations.PersistentRepresentation;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static com.google.common.truth.Truth.assertThat;

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

  private Representation createDefaultResource() {
    return representationFactory.newRepresentation("http://localhost/test")
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
  public void testHashCodeIsDependentOnNamespaces() {
    final Representation updatedResource = resource.withNamespace("testns2", "http://example.com/test2/{rel}");
    assertThat(resource).isEqualTo(otherResource);
    assertThat(updatedResource.hashCode()).isNotEqualTo(resourceHashCode);
  }

  @Test
  public void testHashCodeIsDependentOnLinks() {
    final Representation updatedResource = resource.withLink("testlink2", "http://example.com/link2");
    assertThat(resource).isEqualTo(otherResource);
    assertThat(updatedResource.hashCode()).isNotEqualTo(resourceHashCode);
  }

  @Test
  public void testHashCodeIsDependentOnProperties() {
    final Representation updatedResource = resource.withProperty("proptest2", "value2");
    assertThat(resource).isEqualTo(otherResource);
    assertThat(updatedResource.hashCode()).isNotEqualTo(resourceHashCode);
  }

  @Test
  public void testHashCodeIsDependentOnNullProperties() {
    final Representation updatedResource = resource.withProperty("othernullprop", null);
    assertThat(resource).isEqualTo(otherResource);
    assertThat(updatedResource.hashCode()).isNotEqualTo(resourceHashCode);
  }

  @Test
  public void testHashCodeIsDependentOnResources() {
    final Representation updatedResource = resource.withRepresentation("testsub2",
                                                                       representationFactory.newRepresentation("/subtest2"));
    assertThat(resource).isEqualTo(otherResource);
    assertThat(updatedResource.hashCode()).isNotEqualTo(resourceHashCode);
  }

  @Test
  public void testEqualsIsDependentOnNamespaces() {
    final Representation updatedResource = resource.withNamespace("testns2", "http://example.com/test2/{rel}");
    assertThat(resource).isEqualTo(otherResource);
    assertThat(updatedResource).isNotEqualTo(otherResource);
  }

  @Test
  public void testEqualsIsDependentOnLinks() {
    final Representation updatedResource = resource.withLink("testlink2", "http://example.com/link2");
    assertThat(resource).isEqualTo(otherResource);
    assertThat(updatedResource).isNotEqualTo(otherResource);
  }

  @Test
  public void testEqualsIsDependentOnProperties() {
    final Representation updatedResource = resource.withProperty("proptest2", "value2");
    assertThat(resource).isEqualTo(otherResource);
    assertThat(updatedResource).isNotEqualTo(otherResource);
  }

  @Test
  public void testEqualsIsDependentOnNullProperties() {
    final Representation updatedResource = resource.withProperty("othernullprop", null);
    assertThat(resource).isEqualTo(otherResource);
    assertThat(updatedResource).isNotEqualTo(otherResource);
  }

  @Test
  public void testEqualsIsDependentOnResources() {
    final Representation updatedResource = resource.withRepresentation("testsub2",
                                                                       representationFactory.newRepresentation("/subtest2"));
    assertThat(resource).isEqualTo(otherResource);
    assertThat(updatedResource).isNotEqualTo(otherResource);
  }

  @Test
  public void testToStringRendersSelfHref() {
    String toString = new PersistentRepresentation(representationFactory, "http://localhost/test").toString();
    assertThat(toString).isEqualTo("<Representation: http://localhost/test>");
  }

  @Test
  public void testToStringRendersHashCode() {
    String toString = new PersistentRepresentation(representationFactory).toString();
    assertThat(toString).matches("<Representation: @[0-9a-f]+>");
  }

}
