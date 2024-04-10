package it.unibz.deltabpmn.dataschema.elements.providers;

import it.unibz.deltabpmn.dataschema.elements.Sort;

/**
 * An interface for a factory used to generate {@code Sort} objects.
 */
public interface SortProvider {
    /**
     * A method for creating {@code Sort} objects.
     * @param name A sort name.
     * @return A {@code Sort} object.
     */
    Sort newSort(String name);
}
