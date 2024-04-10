package it.unibz.deltabpmn.processschema.blocks;

import it.unibz.deltabpmn.datalogic.ComplexTransition;

public interface Task extends Block {

    void addTransition(ComplexTransition eff);

    ComplexTransition getTransition();

}


