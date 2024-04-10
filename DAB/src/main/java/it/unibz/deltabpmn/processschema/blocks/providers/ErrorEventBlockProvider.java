package it.unibz.deltabpmn.processschema.blocks.providers;

import it.unibz.deltabpmn.processschema.blocks.Block;
import it.unibz.deltabpmn.processschema.blocks.ErrorEventBlock;

/**
 * An interface for a factory used to generate {@code ErrorEventBlock} objects.
 */
public interface ErrorEventBlockProvider {
    ErrorEventBlock newErrorEventBlock(String name, Block handler);
}
