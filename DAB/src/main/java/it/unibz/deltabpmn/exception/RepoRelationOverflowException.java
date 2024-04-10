package it.unibz.deltabpmn.exception;

/**
 * Exception used for cases when the maximum number of allowed repository relations has been reached.
 */
public class RepoRelationOverflowException extends Exception {

    public RepoRelationOverflowException(String message) {
        super(message);
    }
}
