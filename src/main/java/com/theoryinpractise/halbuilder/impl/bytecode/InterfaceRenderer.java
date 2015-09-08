package com.theoryinpractise.halbuilder.impl.bytecode;

import com.google.common.base.Preconditions;
import com.theoryinpractise.halbuilder.api.Link;
import com.theoryinpractise.halbuilder.api.ReadableRepresentation;
import com.theoryinpractise.halbuilder.api.RepresentationException;
import fj.Ord;
import fj.data.List;
import fj.data.Option;
import fj.data.TreeMap;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

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

  public T render(final TreeMap<String, Option<Object>> map) {
    return render(map, List.nil(), TreeMap.empty(Ord.stringOrd));
  }

  public T render(final TreeMap<String, Option<Object>> properties, final List<Link> links,
                  final TreeMap<String, Collection<? extends ReadableRepresentation>> resources) {
    if (InterfaceContract.newInterfaceContract(anInterface).isSatisfiedBy(properties)) {
      T proxy = (T) Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[]{anInterface}, new InvocationHandler() {
        public Object invoke(Object o, Method method, Object[] objects)
            throws Throwable {

          if (method.getName().equals("getLinks")) {
            return links;
          }

          if (method.getName().equals("getEmbedded")) {
            return resources;
          }

          String propertyName = derivePropertyNameFromMethod(method);

          Option<Object> optionalPropertyValue = properties.get(propertyName)
                                                           .bind(val -> val);

          Class<?> returnType = method.getReturnType();

          Object returnValue = null;

          if (optionalPropertyValue.isSome()) {
            Object propertyValue = optionalPropertyValue.some();
            if (propertyValue instanceof List) {
              List propertyCollection = (List) propertyValue;
              Object propertyHeadValue = propertyCollection.iterator().next();
              ParameterizedType genericReturnType = ((ParameterizedType) method.getGenericReturnType());
              Class<?> collectionType = (Class<?>) genericReturnType.getActualTypeArguments()[0];

              if (collectionType.isInstance(propertyHeadValue)) {
                returnValue = propertyValue;
              } else {
                InterfaceRenderer collectionValueRenderer = new InterfaceRenderer(collectionType);
                returnValue = new ArrayList();
                for (Object item : propertyCollection) {
                  ((ArrayList) returnValue).add(collectionValueRenderer.render((TreeMap) item));
                }
              }
            } else if (returnType.isInstance(propertyValue)) {
              returnValue = propertyValue;
            } else if (Map.class.isInstance(propertyValue)) {

              InterfaceRenderer propertyValueRenderer = new InterfaceRenderer(returnType);
//              returnValue = propertyValueRenderer.render((Map<String, Object>) propertyValue);
              // TODO Work out how to convert a java.util.Map to a fj.data.Map
              returnValue = propertyValueRenderer.render(TreeMap.empty(Ord.stringOrd));
            } else {
              returnValue = returnType.getConstructor(propertyValue.getClass()).newInstance(propertyValue);
            }
          }

          return returnValue;
        }
      });
      return proxy;
    } else {
      throw new RepresentationException("Unable to write representation to " + anInterface.getName());
    }
  }
}
