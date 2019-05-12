package com.theoryinpractise.halbuilder.impl.bytecode;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.theoryinpractise.halbuilder.api.Link;
import com.theoryinpractise.halbuilder.api.ReadableRepresentation;
import com.theoryinpractise.halbuilder.api.RepresentationException;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.theoryinpractise.halbuilder.impl.bytecode.InterfaceSupport.derivePropertyNameFromMethod;

/** Java Interface based "renderer", this will write the resource as a Proxy to a Java interface. */
public class InterfaceRenderer<T> {

  private Class<T> anInterface;

  public static <I> InterfaceRenderer<I> newInterfaceRenderer(Class<I> anInterface) {
    return new InterfaceRenderer<I>(anInterface);
  }

  private InterfaceRenderer(Class<T> anInterface) {
    Preconditions.checkArgument(anInterface.isInterface(), "Provided class MUST be an interface.");
    this.anInterface = anInterface;
  }

  public T render(final ReadableRepresentation representation) {
    return render(
        representation.getProperties(), representation.getLinks(), representation.getResourceMap());
  }

  public T render(final Map<String, Object> map) {
    return render(
        map,
        ImmutableList.<Link>of(),
        ImmutableMap.<String, Collection<ReadableRepresentation>>of());
  }

  public T render(
      final Map<String, Object> properties,
      final List<Link> links,
      final Map<String, Collection<ReadableRepresentation>> resources) {
    if (InterfaceContract.newInterfaceContract(anInterface).isSatisfiedBy(properties)) {
      T proxy =
          (T)
              Proxy.newProxyInstance(
                  this.getClass().getClassLoader(),
                  new Class[] {anInterface},
                  new InvocationHandler() {
                    public Object invoke(Object o, Method method, Object[] objects)
                        throws Throwable {

                      if (method.getName().equals("getLinks")) {
                        return links;
                      }

                      if (method.getName().equals("getEmbedded")) {
                        return resources;
                      }

                      String propertyName = derivePropertyNameFromMethod(method);

                      Object propertyValue = properties.get(propertyName);

                      Class<?> returnType = method.getReturnType();

                      Object returnValue = null;

                      if (propertyValue != null) {

                        if (propertyValue instanceof List) {
                          List propertyCollection = (List) propertyValue;
                          Object propertyHeadValue = propertyCollection.iterator().next();
                          ParameterizedType genericReturnType =
                              ((ParameterizedType) method.getGenericReturnType());
                          Class<?> collectionType =
                              (Class<?>) genericReturnType.getActualTypeArguments()[0];

                          if (collectionType.isInstance(propertyHeadValue)) {
                            returnValue = propertyValue;
                          } else {
                            InterfaceRenderer collectionValueRenderer =
                                new InterfaceRenderer(collectionType);
                            returnValue = new ArrayList();
                            for (Object item : propertyCollection) {
                              ((List) returnValue).add(collectionValueRenderer.render((Map) item));
                            }
                          }
                        } else if (returnType.isInstance(propertyValue)) {
                          returnValue = propertyValue;
                        } else if (Map.class.isInstance(propertyValue)) {
                          InterfaceRenderer propertyValueRenderer =
                              new InterfaceRenderer(returnType);
                          returnValue =
                              propertyValueRenderer.render((Map<String, Object>) propertyValue);
                        } else {
                          returnValue =
                              returnType
                                  .getConstructor(propertyValue.getClass())
                                  .newInstance(propertyValue);
                        }
                      }

                      return returnValue;
                    }
                  });
      return proxy;
    } else {
      throw new RepresentationException(
          "Unable to write representation to " + anInterface.getName());
    }
  }
}
