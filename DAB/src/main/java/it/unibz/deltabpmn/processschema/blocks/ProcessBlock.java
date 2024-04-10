package it.unibz.deltabpmn.processschema.blocks;

import it.unibz.deltabpmn.datalogic.InsertTransition;

public interface ProcessBlock extends Block {

    void addBlock(Block b);

    void setEventTransition(InsertTransition ins);

}