package com.theoryinpractise.halbuilder.impl.bytecode;

import com.google.common.base.Preconditions;
import com.theoryinpractise.halbuilder.api.Contract;
import com.theoryinpractise.halbuilder.api.ReadableRepresentation;
import javaslang.Tuple;
import javaslang.collection.Map;

import java.lang.reflect.Method;

import static com.theoryinpractise.halbuilder.impl.bytecode.InterfaceSupport.derivePropertyNameFromMethod;

/**
 * A Java Interface matching contract.
 */
public class InterfaceContract<T>
    implements Contract {

  private Class<T> anInterface;

  private InterfaceContract(Class<T> anInterface) {
    Preconditions.checkArgument(anInterface.isInterface(), "Contract class MUST be an interface.");
    this.anInterface = anInterface;
  }

  public static <T> InterfaceContract<T> newInterfaceContract(Class<T> anInterface) {
    return new InterfaceContract<T>(anInterface);
  }

  public boolean isSatisfiedBy(ReadableRepresentation representation) {
    final Map<String, Object> map = representation.getProperties()
                                                  .map((k, v) -> Tuple.of(k, v.orElse(null)));
    return isSatisfiedBy(map);

  }

  public boolean isSatisfiedBy(Map<String, ?> properties) {
    for (Method method : anInterface.getDeclaredMethods()) {
      String propertyName = derivePropertyNameFromMethod(method);
      if (!"class".equals(propertyName)
          && !"links".equals(propertyName)
          && !"embedded".equals(propertyName)
          && !properties.containsKey(propertyName)) {
        return false;
      }
    }

    return true;
  }
}
