package com.theoryinpractise.halbuilder5;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
  CoalesceLinksTest.class,
  LinkTest.class,
  NamespaceManagerTest.class,
  ResourceBasicMethodsTest.class,
  ResourceRepresentationTest.class,
  SingleLinksTest.class,
  ContentTypeTest.class,
})
public class AllTests {}
