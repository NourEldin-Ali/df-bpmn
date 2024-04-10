package it.unibz.deltabpmn.processschema.blocks;

import it.unibz.deltabpmn.datalogic.ComplexTransition;

public interface Event extends Block {

    void addTransition(ComplexTransition eff);

    ComplexTransition getTransition();

    boolean hasEffect();
}