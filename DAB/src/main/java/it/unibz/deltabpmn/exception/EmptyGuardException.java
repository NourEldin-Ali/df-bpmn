package it.unibz.deltabpmn.exception;

/**
 * An exception to be thrown when a query used in a guard is empty
 */
public class EmptyGuardException extends Exception {

    public EmptyGuardException (String message){
        super(message);
    }

}
