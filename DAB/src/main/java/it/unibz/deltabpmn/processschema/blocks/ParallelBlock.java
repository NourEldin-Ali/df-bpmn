package it.unibz.deltabpmn.processschema.blocks;

public interface ParallelBlock extends Block {
    void addFirstBlock(Block b);

    void addSecondBlock(Block b);
}
