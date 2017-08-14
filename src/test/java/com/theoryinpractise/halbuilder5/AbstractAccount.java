package com.theoryinpractise.halbuilder5;

import org.immutables.value.Value;

@JsonSerializedValue
@Value.Immutable
public abstract class AbstractAccount {
  @Value.Parameter
  public abstract String accountNumber();

  @Value.Parameter
  public abstract String name();
}
