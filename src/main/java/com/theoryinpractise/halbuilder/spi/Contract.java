package com.theoryinpractise.halbuilder.spi;

/**
 * A Contract is used to assert that a given ReadableRepresentation conforms
 * to a specific set of requirements.
 *
 * Implementing this interface allows an object to assert the validatity of a resources
 * structure (required fields), business data (duplicate data) or any other contractual
 * concept.
 */
public interface Contract {

    /**
     * Returns whether the representation is satisfied by this contract.
     * @param representation The representation needing satisfaction
     * @return A boolean.
     */
    boolean isSatisfiedBy(ReadableRepresentation representation);
}
