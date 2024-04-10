package it.unibz.deltabpmn.processschema.blocks.providers;

import it.unibz.deltabpmn.processschema.blocks.BackwardExceptionBlock;

/**
 * An interface for a factory used to generate {@code BackwardExceptionBlock} objects.
 */
public interface BackwardExceptionBlockProvider {
    BackwardExceptionBlock newBackwardExceptionBlock(String name);
}
