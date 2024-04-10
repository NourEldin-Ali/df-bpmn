package it.unibz.deltabpmn.processschema.blocks.providers;

import it.unibz.deltabpmn.processschema.blocks.SequenceBlock;

/**
 * An interface for a factory used to generate {@code SequenceBlock} objects.
 */
public interface SequenceBlockProvider {
    SequenceBlock newSequenceBlock(String name);
}
