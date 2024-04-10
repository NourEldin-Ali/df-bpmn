package it.unibz.deltabpmn.dataschema.elements;

import it.unibz.deltabpmn.verification.mcmt.MCMTDeclarable;

/**
 * A sort represents a type of an attribute or a variable. A sort should be defined only "symbolically",
 * that is, it should not carry any information about allowed operations on sorted objects.
 */
public interface Sort extends MCMTDeclarable {

    /**
     * @return The name of the sort.
     */
    String getSortName();
}
