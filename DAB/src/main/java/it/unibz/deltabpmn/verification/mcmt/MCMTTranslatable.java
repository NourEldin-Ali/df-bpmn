package it.unibz.deltabpmn.verification.mcmt;

import it.unibz.deltabpmn.exception.*;

/**
 * Interface specifying basic methods used for translating DAB components into MCMT code.
 */
public interface MCMTTranslatable {
    
    String getMCMTTranslation() throws InvalidInputException, UnmatchingSortException, EevarOverflowException, EmptyGuardException, UninitializedLifecyleVariableException;

}
