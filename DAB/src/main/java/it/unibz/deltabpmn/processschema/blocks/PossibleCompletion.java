package it.unibz.deltabpmn.processschema.blocks;

import it.unibz.deltabpmn.datalogic.ConjunctiveSelectQuery;
import it.unibz.deltabpmn.dataschema.elements.CaseVariable;

public interface PossibleCompletion extends Block {


    void addMainProcessLifecycleVariable(CaseVariable global);

    void addCondition(ConjunctiveSelectQuery cond);

//    void startPropagation(Block handler, InsertTransition transition) throws InvalidInputException, UnmatchingSortException;

//    void propagateError(Block current, InsertTransition transition) throws InvalidInputException, UnmatchingSortException;
}
