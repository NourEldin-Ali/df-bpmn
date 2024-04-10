package it.unibz.deltabpmn.dataschema.elements.providers;

import it.unibz.deltabpmn.dataschema.elements.CaseVariable;
import it.unibz.deltabpmn.dataschema.elements.Sort;

/**
 * An interface for a factory used to generate {@code DABCaseVariable} objects.
 */
public interface CaseVariableProvider {

    /**
     * A method for creating {@code DABCaseVariable} objects.
     *
     * @param name A case variable name.
     * @param sort A case variable sort.
     * @param type {@code true} is the case variable is used in the one-case setting. {@code false} otherwise.
     * @return A {@CaseVariable} object.
     */
    CaseVariable newCaseVariable(String name, Sort sort, boolean type);
}
