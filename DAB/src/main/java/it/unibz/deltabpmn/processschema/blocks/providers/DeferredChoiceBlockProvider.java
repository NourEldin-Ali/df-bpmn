package it.unibz.deltabpmn.processschema.blocks.providers;

import it.unibz.deltabpmn.processschema.blocks.DeferredChoiceBlock;

/**
 * An interface for a factory used to generate {@code DeferredChoiceBlock} objects.
 */
public interface DeferredChoiceBlockProvider {
    DeferredChoiceBlock newDeferredChoiceBlock(String name);
}
