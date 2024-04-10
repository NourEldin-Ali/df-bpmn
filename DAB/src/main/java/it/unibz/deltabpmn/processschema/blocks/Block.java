package it.unibz.deltabpmn.processschema.blocks;

import it.unibz.deltabpmn.dataschema.elements.CaseVariable;
import it.unibz.deltabpmn.verification.mcmt.MCMTTranslatable;

/**
 * An interface for representing DAB control-flow blocks.
 */
public interface Block extends MCMTTranslatable {
    CaseVariable getLifeCycleVariable();

    String getName();

    Block[] getSubBlocks();
    
}
