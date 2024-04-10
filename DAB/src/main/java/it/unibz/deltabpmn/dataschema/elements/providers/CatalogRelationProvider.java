package it.unibz.deltabpmn.dataschema.elements.providers;

import it.unibz.deltabpmn.dataschema.elements.CatalogRelation;

/**
 * An interface for a factory used to generate {@code DABCatalogRelation} objects.
 */
public interface CatalogRelationProvider {
    /**
     * A method for creating {@code DABCatalogRelation} objects.
     *
     * @param name A Catalog relation name.
     * @return A {@code DABCatalogRelation} object.
     */
    CatalogRelation newCatalogRelation(String name);
}
