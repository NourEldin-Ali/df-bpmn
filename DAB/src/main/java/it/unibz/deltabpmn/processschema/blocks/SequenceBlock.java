package it.unibz.deltabpmn.processschema.blocks;

public interface SequenceBlock extends Block {

    void addFirstBlock(Block b);

    void addSecondBlock(Block b);
}
