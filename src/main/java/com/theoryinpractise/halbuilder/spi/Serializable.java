package com.theoryinpractise.halbuilder.spi;

/**
 * Implementers of this interface can provide customized "serialization"
 * to a Resource.
 */
public interface Serializable {

    /**
     * "Serializes" data to the given resource.
     * @param resource The resource to serialize into.
     */
    void serializeResource(Resource resource);

}
