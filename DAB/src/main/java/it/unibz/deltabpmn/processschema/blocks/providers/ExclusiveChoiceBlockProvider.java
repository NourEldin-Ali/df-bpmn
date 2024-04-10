package it.unibz.deltabpmn.processschema.blocks.providers;

import it.unibz.deltabpmn.processschema.blocks.ExclusiveChoiceBlock;

/**
 * An interface for a factory used to generate {@code ExclusiveChoiceBlock} objects.
 */
public interface ExclusiveChoiceBlockProvider {
    ExclusiveChoiceBlock newExclusiveChoiceBlock(String name);
}
