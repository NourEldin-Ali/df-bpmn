package it.unibz.deltabpmn.processschema.blocks.providers;

import it.unibz.deltabpmn.processschema.blocks.ForwardExceptionBlock;

/**
 * An interface for a factory used to generate {@code ForwardExceptionBlock} objects.
 */
public interface ForwardExceptionBlockProvider {
    ForwardExceptionBlock newForwardExceptionBlock(String name);
}
