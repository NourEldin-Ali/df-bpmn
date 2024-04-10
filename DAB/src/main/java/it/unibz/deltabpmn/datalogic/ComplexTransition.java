package it.unibz.deltabpmn.datalogic;

import it.unibz.deltabpmn.dataschema.elements.Attribute;
import it.unibz.deltabpmn.dataschema.elements.CaseVariable;
import it.unibz.deltabpmn.dataschema.elements.Constant;
import it.unibz.deltabpmn.exception.EevarOverflowException;
import it.unibz.deltabpmn.exception.InvalidInputException;
import it.unibz.deltabpmn.exception.UnmatchingSortException;

/**
 * An interface defining a transition that can update case variables using dedicated setters
 * (that is, expressions in the {@code SET x_1=v_1,...,x_n=v_n} parts of {@code INSERT} and {@code DELETE} transitions).
 */
public interface ComplexTransition extends Transition {

    void setControlCaseVariableValue(CaseVariable variable, Constant newValue) throws InvalidInputException, UnmatchingSortException;

    void setControlCaseVariableValue(CaseVariable variable, Attribute attr) throws InvalidInputException, UnmatchingSortException;

    /**
     * This is a method that should be used only when we have an expression #v1 = v2, where v2 is defined in the same update statement (i.e., it's a new variable)
     *
     * @param variable
     * @param newVariable
     * @throws InvalidInputException
     * @throws UnmatchingSortException
     */
    void setControlCaseVariableValue(CaseVariable variable, CaseVariable newVariable) throws InvalidInputException, UnmatchingSortException, EevarOverflowException;

}
