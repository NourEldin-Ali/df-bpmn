package it.unibz.deltabpmn.processschema.blocks;

import it.unibz.deltabpmn.datalogic.ConjunctiveSelectQuery;


public interface InclusiveOrBlock extends Block {


    void addFirstBlock(Block b);

    void addSecondBlock(Block b);

    void addFirstFlowCondition(ConjunctiveSelectQuery cond);

    void addSecondFlowCondition(ConjunctiveSelectQuery cond);

}