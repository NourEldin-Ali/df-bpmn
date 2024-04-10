package it.unibz.deltabpmn.processschema.blocks.providers;

import it.unibz.deltabpmn.datalogic.ConjunctiveSelectQuery;
import it.unibz.deltabpmn.processschema.blocks.LoopBlock;

/**
 * An interface for a factory used to generate {@code LoopBlock} objects.
 */
public interface LoopBlockProvider {
    LoopBlock newLoopBlock(String name);

    LoopBlock newLoopBlock(String name, ConjunctiveSelectQuery cond);
}
