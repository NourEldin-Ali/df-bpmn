package it.unibz.deltabpmn.exception;

/**
 * Exception used for cases when lifecycle (case) variables in blocks are (partially) initialised externally
 */
public class UninitializedLifecyleVariableException extends Exception {
    public UninitializedLifecyleVariableException(String message) {
        super(message);
    }

}
