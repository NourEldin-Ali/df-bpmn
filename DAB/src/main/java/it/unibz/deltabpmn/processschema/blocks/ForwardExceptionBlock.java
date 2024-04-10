package it.unibz.deltabpmn.processschema.blocks;

public interface ForwardExceptionBlock extends Block {

    void addFirstBlock(Block b);

    void addSecondBlock(Block b);

    void addThirdBlock(Block b);

//    void startPropagation(Block handler, InsertTransition transition) throws InvalidInputException, UnmatchingSortException;

//    void propagateError(Block current, InsertTransition transition) throws InvalidInputException, UnmatchingSortException;
}
