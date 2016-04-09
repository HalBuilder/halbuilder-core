package com.theoryinpractise.halbuilder5;

import org.immutables.value.Value;

@Value.Immutable
public abstract class CurriedNamespaceData {
  @Value.Parameter
  public abstract String ns();

  @Value.Parameter
  public abstract String href();

  @Value.Parameter
  public abstract String original();

  @Value.Parameter
  public abstract String curried();
}
