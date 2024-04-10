package it.unibz.deltabpmn.dataschema.elements;

import it.unibz.deltabpmn.verification.mcmt.MCMTDeclarable;

/**
 * An interface that defines a basic type to be extended by more concrete ones like Constant, CaseVariable and Attribute.
 */
public interface Term extends MCMTDeclarable {
    String getName();

    Sort getSort();
}
