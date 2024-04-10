package it.unibz.deltabpmn.exception;

/**
 * Class that extends Exception and represents an exception thrown if there is an improper input.
 * @author DavideCremonini
 * @version 1.0
 * @since 18/07/2019
 */
public class InvalidInputException extends Exception{

    /**
     * Constructor of the exception which calls the constructor of the parent class and
     * sends a message.
     * @param message the message which explains the exception.
     */
    public InvalidInputException (String message){
        super(message);
    }
}