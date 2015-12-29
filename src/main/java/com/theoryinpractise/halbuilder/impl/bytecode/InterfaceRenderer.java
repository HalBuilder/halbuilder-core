package com.theoryinpractise.halbuilder.impl.bytecode;

import com.google.common.base.Preconditions;
import com.theoryinpractise.halbuilder.api.Link;
import com.theoryinpractise.halbuilder.api.ReadableRepresentation;
import com.theoryinpractise.halbuilder.api.RepresentationException;
import javaslang.collection.List;
import javaslang.collection.Map;
import javaslang.collection.TreeMap;
import javaslang.control.Option;

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
    return render(representation.getProperties(), representation.getLinks(), representation.getResourceMap());
  }

  public T render(final Map<String, Option<Object>> map) {
    return render(map, List.empty(), TreeMap.empty(Comparator.naturalOrder()));
  }

  private TreeMap<String, Option<Object>> fromJavaMap(java.util.Map<String, Object> map) {
    TreeMap<String, Option<Object>> returnMap = TreeMap.empty(Comparator.naturalOrder());
    for (java.util.Map.Entry<String, Object> entry : map.entrySet()) {
      returnMap = returnMap.put(entry.getKey(), Option.of(entry.getValue()));
    }
    return returnMap;
  }

  @SuppressWarnings("unchecked")
  public T render(final Map<String, Option<Object>> properties, final List<Link> links,
                  final Map<String, List<? extends ReadableRepresentation>> resources) {
    if (InterfaceContract.newInterfaceContract(anInterface).isSatisfiedBy(properties)) {
      return (T) Proxy.newProxyInstance(this.getClass().getClassLoader(),
          new Class[]{anInterface},
          makeInterfaceRendererHandler(properties, links, resources));
    } else {
      throw new RepresentationException("Unable to write representation to " + anInterface.getName());
    }
  }

  public InvocationHandler makeInterfaceRendererHandler(final Map<String, Option<Object>> properties, final List<Link> links,
                                                        final Map<String, List<? extends ReadableRepresentation>> resources) {
    return (o, method, objects) -> {

      if (method.getName().equals("getLinks")) {
        return links;
      }

      if (method.getName().equals("getEmbedded")) {
        return resources;
      }

      String propertyName = derivePropertyNameFromMethod(method);

      Option<Object> optionalPropertyValue = properties.get(propertyName)
                                                       .flatMap(val -> val);

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
          returnValue = returnType.getConstructor(
              propertyValue.getClass()).newInstance(propertyValue);
        }
      }

      return returnValue;
    };
  }
}
