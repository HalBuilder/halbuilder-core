package com.theoryinpractise.halbuilder.spi;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;

import java.util.List;
import java.util.Map;

/**
 * A ReadableResource is a read-only, immutable HAL Resource object.
 */
public interface ReadableResource {

    /**
     * Returns a Link with a rel of "self".
     * @return A Link
     */
    Link getResourceLink();

    /**
     * Returns an ImmutableMap of the currently defined resource namespaces
     * @return A Map
     */
    Map<String, String> getNamespaces();

    /**
     * Returns an ImmutableList of individual Link instances on this resource.
     * @return A List of Links
     */
    List<Link> getCanonicalLinks();

    /**
     * Returns an ImmutableList of collated Link instances on this resource.
     *
     * Multiple links to the same resolved HREF are collated into a single Link
     * instance with a space separated combined rel attribute.
     * @return A List of Links
     */
    List<Link> getLinks();

    /**
     * Returns the first link matching the given rel by searching this, then
     * any embedded resource instance.
     * @param rel The rel type to search for.
     * @return A Guava Optional Link
     */
    Optional<Link> getLinkByRel(String rel);

    /**
     * Returns all links matching the given rel by searching this, then
     * any embedded resource instance.
     * @param rel The rel type to search for.
     * @return An Immutable List of Links
     */
    List<Link> getLinksByRel(String rel);

    /**
     * Returns all embedded resources matching the given rel by searching this, then
     * any embedded resource instance.
     * @param rel The rel type to search for.
     * @return An Immutable List of Resources
     */
    List<? extends ReadableResource> getResourcesByRel(String rel);

    /**
     * Returns a finds all embedded resources from the Resource
     * that match the predicate
     * @param findPredicate The predicate to check against in the embedded resources
     * @return A List of matching objects (properties, links, resource)
     */
    List<? extends ReadableResource> findResources(Predicate<Resource> findPredicate);

    /**
     * Returns a property from the Resource.
     * @param name The property to return
     * @return A Guava Optional for the property
     */
    Optional<Object> get(String name);

    /**
     * Returns a property from the Resource
     * @param name The property to return
     * @return An Object of the property value, or null if absent
     */
    Object getValue(String name);

    /**
     * Returns a property from the Resource
     * @param name The property to return
     * @return An Object of the property value, or a user supplied default value
     */
    Object getValue(String name, Object defaultValue);

    /**
     * Returns an ImmutableMap of the resources properties.
     * @return A Map
     */
    Map<String, Optional<Object>> getProperties();

    /**
     * Return an indication of whether this resource, or subresources of this
     * resource, contain null properties.
     * @return True if this resource, or subresources of this resource,
     * contain null properties.  False if not.
     */
    boolean hasNullProperties();

    /**
     * Returns an ImmutableList of the resources currently embedded resources.
     * @return A Map
     */
    List<Resource> getResources();

    /**
     * Returns whether this resource is satisfied by the provided Contact.
     * @param contract The contract to check.
     * @return A boolean
     */
    boolean isSatisfiedBy(Contract contract);

    /**
     * Returns an optional result of applying the provided function if the resource
     * is satisified by the provided interface.
     *
     * Interface satisfaction is determined by all interface methods being resolvable to
     * properties on the resource.
     *
     * @param anInterface The interface to check
     * @param function The function to apply
     * @param <T> The resource "interface"
     * @param <V> A return value
     * @return A Guava Optional of the functions return value.
     */
    <T, V> Optional<V> ifSatisfiedBy(Class<T> anInterface, Function<T, V> function);

    /**
     * Returns an optional proxy to the given interface mirroring the resource.
     *
     * @param anInterface An interface to mirror
     * @return A Guava Optional Resource Proxy
     */
    <T> Optional<T> renderClass(Class<T> anInterface);

    /**
     * Returns the resource in the request content-type.
     *
     * application/hal+xml and application/hal+json are provided by default,
     * additional Renderers can be added to a ResourceFactory.
     *
     * @param contentType The content type requested
     * @return A String
     */
    String renderContent(String contentType);

}
