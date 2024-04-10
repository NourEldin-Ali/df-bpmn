package it.unibz.deltabpmn.dataschema.elements.providers;

import it.unibz.deltabpmn.dataschema.elements.Constant;
import it.unibz.deltabpmn.dataschema.elements.Sort;

/**
 * An interface for a factory used to generate {@code Constant} objects.
 */
public interface ConstantProvider {
    /**
     * A method for creating {@code Constant} objects.
     * @param name A constant name.
     * @param sort A constant sort.
     * @return A {@code Constant} object.
     */
    Constant newConstant(String name, Sort sort);
}
