package it.unibz.deltabpmn.exception;

/**
 * Appears when a newly declared variable has been alredy declared in one of the tasks.
 */
public class DuplicateDeclarationException extends Exception{

    public DuplicateDeclarationException (String message){
        super(message);
    }
}