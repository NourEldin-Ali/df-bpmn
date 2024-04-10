package it.unibz.deltabpmn.processschema.blocks;

public interface ErrorEventBlock extends Block {

    void addFirstBlock(Event b);

    void addSecondBlock(Event b);

//    void startPropagation(Block handler, InsertTransition transition) throws InvalidInputException, UnmatchingSortException;

//    void propagateError(Block current, InsertTransition transition) throws InvalidInputException, UnmatchingSortException;
}