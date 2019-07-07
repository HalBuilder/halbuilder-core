package com.theoryinpractise.halbuilder.impl.bytecode;

import static com.theoryinpractise.halbuilder.impl.bytecode.InterfaceSupport.derivePropertyNameFromMethod;

import com.google.common.base.Preconditions;
import com.theoryinpractise.halbuilder.api.Contract;
import com.theoryinpractise.halbuilder.api.ReadableRepresentation;
import java.lang.reflect.Method;
import java.util.Map;

/** A Java Interface matching contract */
public class InterfaceContract<T> implements Contract {

  private Class<T> anInterface;

  public static <T> InterfaceContract<T> newInterfaceContract(Class<T> anInterface) {
    return new InterfaceContract<T>(anInterface);
  }

  private InterfaceContract(Class<T> anInterface) {
    Preconditions.checkArgument(anInterface.isInterface(), "Contract class MUST be an interface.");
    this.anInterface = anInterface;
  }

  @Override
  public boolean isSatisfiedBy(ReadableRepresentation representation) {
    return isSatisfiedBy(representation.getProperties());
  }

  public boolean isSatisfiedBy(Map<String, Object> properties) {
    for (Method method : anInterface.getDeclaredMethods()) {
      String propertyName = derivePropertyNameFromMethod(method);
      if (!"class".equals(propertyName) && !"links".equals(propertyName) && !"embedded".equals(propertyName) && !properties.containsKey(propertyName)) {
        return false;
      }
    }

    return true;
  }
}
