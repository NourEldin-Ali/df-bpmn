package it.unibz.deltabpmn.processschema.core;

import it.unibz.deltabpmn.datalogic.BinaryConditionProvider;
import it.unibz.deltabpmn.datalogic.ConjunctiveSelectQuery;
import it.unibz.deltabpmn.datalogic.InsertTransition;
import it.unibz.deltabpmn.dataschema.core.DataSchema;
import it.unibz.deltabpmn.dataschema.core.SystemSorts;
import it.unibz.deltabpmn.dataschema.elements.CaseVariable;
import it.unibz.deltabpmn.exception.EevarOverflowException;
import it.unibz.deltabpmn.exception.EmptyGuardException;
import it.unibz.deltabpmn.exception.InvalidInputException;
import it.unibz.deltabpmn.exception.UnmatchingSortException;
import it.unibz.deltabpmn.processschema.blocks.Block;
import it.unibz.deltabpmn.processschema.blocks.ProcessBlock;
import it.unibz.deltabpmn.verification.mcmt.NameManager;

class DABProcessBlock implements ProcessBlock {

    private InsertTransition setTrans = null; // possibility of having a start event with set update
    private String name;
    private CaseVariable lifeCycle;
    private Block[] subBlocks;
    //private InsertTransition effect = null;
    private DataSchema dataSchema;

    public DABProcessBlock(String name, DataSchema schema) {
        this.name = NameManager.normaliseName(name);
        this.subBlocks = new Block[1];
        this.dataSchema = schema;
        this.lifeCycle = this.dataSchema.newCaseVariable("lifecycle" + this.name, SystemSorts.STRING, true);
        this.lifeCycle.setLifeCycle(1);
    }

    public DABProcessBlock(String name, InsertTransition ins, DataSchema schema) {
        this.name = NameManager.normaliseName(name);
        this.setTrans = ins;
        this.subBlocks = new Block[1];
        this.lifeCycle = this.dataSchema.newCaseVariable("lifecycle" + this.name, SystemSorts.STRING, true);
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
    public void addBlock(Block b) {
        this.subBlocks[0] = b;
    }

    @Override
    public void setEventTransition(InsertTransition ins) {
        this.setTrans = ins;
    }

    @Override
    public String getMCMTTranslation() throws InvalidInputException, UnmatchingSortException, EevarOverflowException, EmptyGuardException {
        String result = "";

        IndexGenerator indexGenerator = NameProcessor.getIndexGenerator();

        // first part: itself ENABLED --> B1 ENABLED and itself ACTIVE
        ConjunctiveSelectQuery firstGuard = new ConjunctiveSelectQuery(this.dataSchema);
        firstGuard.addBinaryCondition(BinaryConditionProvider.equality(this.lifeCycle, State.ENABLED));
        InsertTransition firstUpdate = new InsertTransition(this.name + indexGenerator.getNext(), firstGuard, this.dataSchema);
        firstUpdate.setControlCaseVariableValue(this.subBlocks[0].getLifeCycleVariable(), State.ENABLED);
        firstUpdate.setControlCaseVariableValue(this.lifeCycle, State.ACTIVE);


        // second part: B1 COMPLETED --> B1 IDLE and itself completed
        ConjunctiveSelectQuery secondGuard = new ConjunctiveSelectQuery(this.dataSchema);
        secondGuard.addBinaryCondition(BinaryConditionProvider.equality(this.subBlocks[0].getLifeCycleVariable(), State.COMPLETED));
        InsertTransition secondUpdate = new InsertTransition(this.name + indexGenerator.getNext(), secondGuard, this.dataSchema);
        secondUpdate.setControlCaseVariableValue(this.subBlocks[0].getLifeCycleVariable(), State.IDLE);
        secondUpdate.setControlCaseVariableValue(this.lifeCycle, State.COMPLETED);


        // generate MCMT translation
        // check if there is a set update in the start event
        if (setTrans != null) {
            result += setTrans.getMCMTTranslation() + "\n" + firstUpdate.getMCMTTranslation() + "\n" + secondUpdate.getMCMTTranslation() + "\n";
        } else
            result += firstUpdate.getMCMTTranslation() + "\n" + secondUpdate.getMCMTTranslation() + "\n";

        return result;

    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof DABProcessBlock))
            return false;
        DABProcessBlock obj = (DABProcessBlock) o;
        return name.equals(obj.getName()) && lifeCycle.equals(obj.getLifeCycleVariable());
    }
}
