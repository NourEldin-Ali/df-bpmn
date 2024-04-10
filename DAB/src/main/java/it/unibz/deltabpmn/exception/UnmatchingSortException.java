package it.unibz.deltabpmn.exception;

/**
 * Class that extends Exception and represents an exception thrown if there is an improper use of sorts.
 * @author DavideCremonini
 * @version 1.0
 * @since 18/07/2019
 */
public class UnmatchingSortException extends Exception{

    /**
     * Constructor of the exception which calls the constructor of the parent class and
     * sends a message.
     * @param message the message which explains the exception.
     */
    public UnmatchingSortException (String message){
        super(message);
    }
}
