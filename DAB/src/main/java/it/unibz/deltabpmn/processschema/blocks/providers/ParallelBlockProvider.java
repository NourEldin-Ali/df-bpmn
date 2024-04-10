package it.unibz.deltabpmn.processschema.blocks.providers;

import it.unibz.deltabpmn.processschema.blocks.ParallelBlock;

/**
 * An interface for a factory used to generate {@code ParallelBlock} objects.
 */
public interface ParallelBlockProvider {
    ParallelBlock newParallelBlock(String name);
}
