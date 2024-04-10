package it.unibz.deltabpmn.processschema.blocks;


public interface DeferredChoiceBlock extends Block {

    void addFirstBlock(Block b);

    void addSecondBlock(Block b);
}
