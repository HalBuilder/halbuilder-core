package com.theoryinpractise.halbuilder.spi;

/**
 * A Contract is used to assert that a given ReadableResource conforms
 * to a specific set of requirements.
 *
 * Implementing this interface allows an object to assert the validatity of a resources
 * structure (required fields), business data (duplicate data) or any other contractual
 * concept.
 */
public interface Contract {

    /**
     * Returns whether the resource is satisfied by this contract.
     * @param resource The resource needing satisfaction
     * @return A boolean.
     */
    boolean isSatisfiedBy(ReadableResource resource);
}
