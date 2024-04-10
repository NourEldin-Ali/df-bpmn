package it.unibz.deltabpmn.datalogic;


import it.unibz.deltabpmn.exception.InvalidInputException;


/**
 * An interface representing an MCMT transition.
 */
public interface Transition {
    String getMCMTTranslation() throws InvalidInputException;

    void addTaskGuard(String guard);


}
