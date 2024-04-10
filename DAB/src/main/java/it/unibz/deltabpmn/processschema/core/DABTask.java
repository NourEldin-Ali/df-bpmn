package it.unibz.deltabpmn.processschema.core;

import it.unibz.deltabpmn.datalogic.ComplexTransition;
import it.unibz.deltabpmn.datalogic.EmptyTransition;
import it.unibz.deltabpmn.dataschema.core.DataSchema;
import it.unibz.deltabpmn.dataschema.core.SystemSorts;
import it.unibz.deltabpmn.dataschema.elements.CaseVariable;
import it.unibz.deltabpmn.exception.InvalidInputException;
import it.unibz.deltabpmn.exception.UnmatchingSortException;
import it.unibz.deltabpmn.processschema.blocks.Block;
import it.unibz.deltabpmn.processschema.blocks.Task;
import it.unibz.deltabpmn.verification.mcmt.NameManager;

class DABTask implements Task {
    private ComplexTransition effect;
    private String name;
    private CaseVariable lifeCycle;
    private Block[] subBlocks;
    private DataSchema dataSchema;

    /**
     * @param name
     */
    public DABTask(String name, DataSchema dataSchema) {
        this.name = NameManager.normaliseName(name);
        this.subBlocks = new Block[0];
        this.dataSchema = dataSchema;
        this.lifeCycle = this.dataSchema.newCaseVariable("lifecycle" + this.name, SystemSorts.STRING, true);
        this.lifeCycle.setLifeCycle(1);
        this.effect = new EmptyTransition(this.name + "TaskTransition", this.dataSchema);
    }

    /**
     * @param name
     * @param eff
     */
    public DABTask(String name, ComplexTransition eff, DataSchema schema) {
        this.name = NameManager.normaliseName(name);
        this.dataSchema = schema;
        this.effect = eff;
        this.subBlocks = new Block[0];
        this.lifeCycle = this.dataSchema.newCaseVariable("lifecycle" + this.name, SystemSorts.STRING, true);
        this.lifeCycle.setLifeCycle(1);
    }

    @Override
    public void addTransition(ComplexTransition eff) {
        this.effect = eff;
    }

    @Override
    public ComplexTransition getTransition() {
        return this.effect;
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
    public String getMCMTTranslation() throws InvalidInputException, UnmatchingSortException {
        String result = "";
        this.effect.addTaskGuard("(= " + this.lifeCycle.getName() + " " + State.ENABLED.getName() + ")");
        //handle lifecycle variable updates for bulk update transitions
//        if (this.effect instanceof BulkUpdate)
//            ((BulkUpdate) this.effect).defineLifecycleVariable(this.lifeCycle);
        //else
        this.effect.setControlCaseVariableValue(this.lifeCycle, State.COMPLETED);
        result += this.effect.getMCMTTranslation();
        return result;
    }

    @Override
    public String toString() {
        return this.name;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + lifeCycle.hashCode();
        for (Block b : subBlocks)
            result = 31 * result + b.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof DABTask))
            return false;
        DABTask obj = (DABTask) o;
        return name.equals(obj.getName()) && lifeCycle.equals(obj.getLifeCycleVariable());
    }
}


