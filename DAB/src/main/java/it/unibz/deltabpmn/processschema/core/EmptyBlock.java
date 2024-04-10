package it.unibz.deltabpmn.processschema.core;

import it.unibz.deltabpmn.datalogic.ComplexTransition;
import it.unibz.deltabpmn.datalogic.EmptyTransition;
import it.unibz.deltabpmn.dataschema.core.DataSchema;
import it.unibz.deltabpmn.dataschema.core.SystemVariables;
import it.unibz.deltabpmn.dataschema.elements.CaseVariable;
import it.unibz.deltabpmn.exception.EevarOverflowException;
import it.unibz.deltabpmn.exception.InvalidInputException;
import it.unibz.deltabpmn.exception.UnmatchingSortException;
import it.unibz.deltabpmn.processschema.blocks.Block;

public class EmptyBlock implements Block {

    private String name;
    private CaseVariable lifeCycle;
    private DataSchema dataSchema;
    private ComplexTransition effect;
    private Block[] subBlocks;


    public EmptyBlock(DataSchema schema) {
        this.name = "arrow";
        this.dataSchema = schema;
        this.lifeCycle = SystemVariables.EMPTY;
        this.effect = new EmptyTransition(this.name + "EmptyBlockTransition", this.dataSchema);
        this.subBlocks = new Block[0];
    }

    @Override
    public CaseVariable getLifeCycleVariable() {
        return this.lifeCycle;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Block[] getSubBlocks() {
        return this.subBlocks;
    }

    @Override
    public String getMCMTTranslation() throws InvalidInputException, UnmatchingSortException, EevarOverflowException {
        String result = "";
        this.effect.addTaskGuard("(= " + this.lifeCycle.getName() + " " + State.ENABLED.getName() + ")");
        this.effect.setControlCaseVariableValue(this.lifeCycle, State.COMPLETED);
        result += this.effect.getMCMTTranslation();
        return result;
    }
}
