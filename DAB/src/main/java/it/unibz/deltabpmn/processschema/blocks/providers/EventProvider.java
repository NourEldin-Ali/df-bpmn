package it.unibz.deltabpmn.processschema.blocks.providers;

import it.unibz.deltabpmn.datalogic.ComplexTransition;
import it.unibz.deltabpmn.processschema.blocks.Event;

/**
 * An interface for a factory used to generate {@code Event} objects.
 */
public interface EventProvider {
    Event newEvent(String name);

    Event newEvent(String name, ComplexTransition eff);
}
