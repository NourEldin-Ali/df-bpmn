package it.unibz.deltabpmn.processschema.blocks.providers;

import it.unibz.deltabpmn.datalogic.ConjunctiveSelectQuery;
import it.unibz.deltabpmn.processschema.blocks.PossibleCompletion;

/**
 * An interface for a factory used to generate {@code ErrorBlock} objects.
 */
public interface PossibleCompletionProvider {

    //    ErrorBlock newErrorBlock(String name, Block handler);
    //
    //    ErrorBlock newErrorBlock(String name, Block handler, ConjunctiveSelectQuery cond);

    PossibleCompletion newPossibleCompletion(String name);

    PossibleCompletion newPossibleCompletion(String name, ConjunctiveSelectQuery cond);

}
