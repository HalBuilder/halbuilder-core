package com.theoryinpractise.halbuilder5;

import org.junit.Test;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import static com.google.common.truth.Truth.assertThat;

@RunWith(Theories.class)
public class NamespaceManagerTest {

  @Test
  public void testNamespaceNeedsRel() throws Exception {

    NamespaceManager ns = NamespaceManager.EMPTY;
    try {
      ns.withNamespace("tst", "http://localhost/test/rel");
      throw new AssertionError("Should not allow us to add a namespace href without {rel}");
    } catch (RepresentationException e) {
      //
    }
    try {
      ns.withNamespace("tst", "http://localhost/test/{rel}");
    } catch (RepresentationException e) {
      throw new AssertionError("Should not fail when adding a valid namespace href");
    }
  }

  @DataPoint
  public static CurriedNamespaceData NS1 =
      ImmutableCurriedNamespaceData.of("tst", "http://localhost/test/{rel}", "http://localhost/test/foo", "tst:foo");

  @DataPoint
  public static CurriedNamespaceData NS2 =
      ImmutableCurriedNamespaceData.of("tst", "http://localhost/test/{rel}/spec", "http://localhost/test/foo/spec", "tst:foo");

  @Theory
  public void testCurrieHref(CurriedNamespaceData data) throws Exception {
    NamespaceManager namespaceManager = NamespaceManager.EMPTY;
    NamespaceManager updatedNamespaceManager = namespaceManager.withNamespace(data.ns(), data.href());
    assertThat(namespaceManager.currieHref(data.original())).isEqualTo(data.original());
    assertThat(updatedNamespaceManager.currieHref(data.original())).isEqualTo(data.curried());
  }

  @Theory
  public void testUnCurrieHref(CurriedNamespaceData data) throws Exception {
    NamespaceManager namespaceManager = NamespaceManager.EMPTY.withNamespace(data.ns(), data.href());

    assertThat(namespaceManager.resolve(data.curried()).isRight()).isTrue();
    assertThat(namespaceManager.resolve(data.curried()).get()).isEqualTo(data.original());
  }
}
