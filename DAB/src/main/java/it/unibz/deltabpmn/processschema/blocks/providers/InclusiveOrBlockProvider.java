package it.unibz.deltabpmn.processschema.blocks.providers;

import it.unibz.deltabpmn.processschema.blocks.InclusiveOrBlock;

/**
 * An interface for a factory used to generate {@code InclusiveOrBlock} objects.
 */
public interface InclusiveOrBlockProvider {
    InclusiveOrBlock newInclusiveOrBlock(String name);
}
