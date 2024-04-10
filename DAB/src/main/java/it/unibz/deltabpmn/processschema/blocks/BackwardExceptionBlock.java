package it.unibz.deltabpmn.processschema.blocks;

public interface BackwardExceptionBlock extends Block {

    void addFirstBlock(Block b);

    void addSecondBlock(Block b);

    //void startPropagation(Block handler, InsertTransition transition) throws InvalidInputException, UnmatchingSortException;

    //void propagateError(Block current, InsertTransition transition) throws InvalidInputException, UnmatchingSortException;
}

