package it.unibz.deltabpmn.processschema.blocks.providers;

import it.unibz.deltabpmn.datalogic.InsertTransition;
import it.unibz.deltabpmn.processschema.blocks.ProcessBlock;

/**
 * An interface for a factory used to generate {@code ProcessBlock} objects.
 */
public interface ProcessBlockProvider {

    ProcessBlock newProcessBlock(String name);

    ProcessBlock newProcessBlock(String name, InsertTransition ins);
}
