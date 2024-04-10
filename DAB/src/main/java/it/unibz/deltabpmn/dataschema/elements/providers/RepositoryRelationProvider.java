package it.unibz.deltabpmn.dataschema.elements.providers;

import it.unibz.deltabpmn.dataschema.elements.RepositoryRelation;

/**
 * An interface for a factory used to generate {@code DABRepositoryRelation} objects.
 */
public interface RepositoryRelationProvider {
    /**
     * A method for creating {@code DABRepositoryRelation} objects.
     *
     * @param name A Repository relation name.
     * @return A {@code DABRepositoryRelation} object.
     */
    public RepositoryRelation newRepositoryRelation(String name);
}

