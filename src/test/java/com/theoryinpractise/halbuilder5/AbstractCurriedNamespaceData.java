package com.theoryinpractise.halbuilder5;

import org.immutables.value.Value;

@JsonSerializedValue
@Value.Immutable
public abstract class AbstractCurriedNamespaceData {
  @Value.Parameter
  public abstract String ns();

  @Value.Parameter
  public abstract String href();

  @Value.Parameter
  public abstract String original();

  @Value.Parameter
  public abstract String curried();
}
