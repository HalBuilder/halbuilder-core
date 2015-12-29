package com.theoryinpractise.halbuilder.impl.bytecode;

import com.google.common.base.Preconditions;
import com.theoryinpractise.halbuilder.api.Link;
import com.theoryinpractise.halbuilder.api.ReadableRepresentation;
import com.theoryinpractise.halbuilder.api.RepresentationException;
import javaslang.Tuple;
import javaslang.collection.List;
import javaslang.collection.Map;
import javaslang.collection.TreeMap;
import javaslang.control.None;
import javaslang.control.Option;
import javaslang.control.Some;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Comparator;

import static com.theoryinpractise.halbuilder.impl.bytecode.InterfaceSupport.derivePropertyNameFromMethod;

/**
 * Java Interface based "renderer", this will write the resource as a Proxy to a Java interface.
 */
public class InterfaceRenderer<T> {

  private Class<T> anInterface;

  private InterfaceRenderer(Class<T> anInterface) {
    Preconditions.checkArgument(anInterface.isInterface(), "Provided class MUST be an interface.");
    this.anInterface = anInterface;
  }

  public static <I> InterfaceRenderer<I> newInterfaceRenderer(Class<I> anInterface) {
    return new InterfaceRenderer<I>(anInterface);
  }

  public T render(final ReadableRepresentation representation) {
    return render(representation.getProperties().map((k, v) -> Tuple.of(k, maybeUnwrap(v))),
                  representation.getLinks(), representation.getResourceMap());
  }

  private T render(final Map<String, Object> map) {
    return render(map, List.empty(), TreeMap.empty(Comparator.naturalOrder()));
  }

  @SuppressWarnings("unchecked")
  T render(final Map<String, Object> properties, final List<Link> links,
                  final Map<String, List<? extends ReadableRepresentation>> resources) {
    if (InterfaceContract.newInterfaceContract(anInterface).isSatisfiedBy(properties)) {
      return (T) Proxy.newProxyInstance(this.getClass().getClassLoader(),
          new Class[]{anInterface},
          makeInterfaceRendererHandler(properties, links, resources));
    } else {
      throw new RepresentationException("Unable to write representation to " + anInterface.getName());
    }
  }

  public InvocationHandler makeInterfaceRendererHandler(final Map<String, ?> properties, final List<Link> links,
                                                        final Map<String, List<? extends ReadableRepresentation>> resources) {
    return (o, method, objects) -> {

      if (method.getName().equals("getLinks")) {
        return links;
      }

      if (method.getName().equals("getEmbedded")) {
        return resources;
      }

      String propertyName = derivePropertyNameFromMethod(method);

      Option<?> optionalPropertyValue = properties.get(propertyName);

      Class<?> returnType = method.getReturnType();

      Object returnValue = null;

      if (optionalPropertyValue.isDefined()) {
        Object propertyValue = optionalPropertyValue.get();
        if (propertyValue instanceof java.util.List) {
          java.util.List propertyCollection = (java.util.List) propertyValue;
          Object propertyHeadValue = propertyCollection.iterator().next();
          ParameterizedType genericReturnType = ((ParameterizedType) method.getGenericReturnType());
          Class<?> collectionType = (Class<?>) genericReturnType.getActualTypeArguments()[0];

          if (collectionType.isInstance(propertyHeadValue)) {
            returnValue = propertyValue;
          } else {
            InterfaceRenderer collectionValueRenderer = new InterfaceRenderer(collectionType);
            returnValue = new ArrayList();
            for (Object item : propertyCollection) {
              ((ArrayList) returnValue).add(
                  collectionValueRenderer.render((Map) item));
            }
          }
        } else if (returnType.isInstance(propertyValue)) {
          returnValue = propertyValue;
        } else if (Map.class.isInstance(propertyValue)) {
          InterfaceRenderer propertyValueRenderer = new InterfaceRenderer(returnType);
          returnValue = propertyValueRenderer.render((Map) propertyValue);
        } else {
          returnValue = propertyValue == null
                        ? null
                        : returnType.getConstructor(propertyValue.getClass()).newInstance(propertyValue);
        }
      }

      return returnValue;
    };
  }

  private Object maybeUnwrap(Object value) {
    if (value instanceof Some) {
      return ((Some) value).get();
    } else if (value instanceof None) {
      return null;
    } else {
      return value;
    }
  }

}
