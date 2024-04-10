package it.unibz.deltabpmn.processschema.blocks.providers;

import it.unibz.deltabpmn.datalogic.ComplexTransition;
import it.unibz.deltabpmn.processschema.blocks.Task;

/**
 * An interface for a factory used to generate {@code Task} objects.
 */
public interface TaskProvider {

    Task newTask(String name);

    Task newTask(String name, ComplexTransition eff);

}
