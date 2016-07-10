package com.theoryinpractise.halbuilder5;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static com.google.common.truth.Truth.assertThat;

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

  @DataProvider
  public static Object[][] provideNamespaces() {
    return new Object[][] {
      {CurriedNamespaceData.of("tst", "http://localhost/test/{rel}", "http://localhost/test/foo", "tst:foo")},
      {CurriedNamespaceData.of("tst", "http://localhost/test/{rel}/spec", "http://localhost/test/foo/spec", "tst:foo")}
    };
  }

  @Test(dataProvider = "provideNamespaces")
  public void testCurrieHref(CurriedNamespaceData data) throws Exception {
    NamespaceManager namespaceManager = NamespaceManager.EMPTY;
    NamespaceManager updatedNamespaceManager = namespaceManager.withNamespace(data.ns(), data.href());

    assertThat(namespaceManager.currieHref(data.original())).isEqualTo(data.original());
    assertThat(updatedNamespaceManager.currieHref(data.original())).isEqualTo(data.curried());
  }

  @Test(dataProvider = "provideNamespaces")
  public void testUnCurrieHref(CurriedNamespaceData data) throws Exception {
    NamespaceManager namespaceManager = NamespaceManager.EMPTY.withNamespace(data.ns(), data.href());

    assertThat(namespaceManager.resolve(data.curried()).isRight()).isTrue();
    assertThat(namespaceManager.resolve(data.curried()).get()).isEqualTo(data.original());
  }
}
