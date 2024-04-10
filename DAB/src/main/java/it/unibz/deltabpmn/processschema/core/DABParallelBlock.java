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
import it.unibz.deltabpmn.processschema.blocks.ParallelBlock;
import it.unibz.deltabpmn.verification.mcmt.NameManager;

class DABParallelBlock implements ParallelBlock {

    private String name;
    private CaseVariable lifeCycle;
    private Block[] subBlocks;
    private DataSchema dataSchema;

    public DABParallelBlock(String name, DataSchema dataSchema) {
        this.name = NameManager.normaliseName(name);
        this.subBlocks = new Block[2];
        this.lifeCycle = this.dataSchema.newCaseVariable("lifecycle" + this.name, SystemSorts.STRING, true);
        this.lifeCycle.setLifeCycle(1);
        this.dataSchema = dataSchema;
    }

    @Override
    public void addFirstBlock(Block b) {
        this.subBlocks[0] = b;
    }

    @Override
    public void addSecondBlock(Block b) {
        this.subBlocks[1] = b;
    }


    @Override
    public String getMCMTTranslation() throws InvalidInputException, UnmatchingSortException, EevarOverflowException, EmptyGuardException {
        String result = "";

        IndexGenerator indexGenerator = NameProcessor.getIndexGenerator();

        // first part: itself ENABLED --> B1 ENABLED, B2 enabled and itself ACTIVE
        ConjunctiveSelectQuery firstGuard = new ConjunctiveSelectQuery(this.dataSchema);
        firstGuard.addBinaryCondition(BinaryConditionProvider.equality(this.lifeCycle, State.ENABLED));//check if the main block is "enabled"
        InsertTransition firstUpdate = new InsertTransition(this.name + indexGenerator.getNext(), firstGuard, this.dataSchema);
        firstUpdate.setControlCaseVariableValue(this.lifeCycle, State.ACTIVE);//change the main block's state to "active"
        firstUpdate.setControlCaseVariableValue(this.subBlocks[0].getLifeCycleVariable(), State.ENABLED);//make the first sub-block "enabled"
        firstUpdate.setControlCaseVariableValue(this.subBlocks[1].getLifeCycleVariable(), State.ENABLED);//make the second sub-block "enabled"


        // second part: B1 COMPLETED and B2 COMPLETED --> B1 IDLE, B2 IDLE and itself COMPLETED
        ConjunctiveSelectQuery secondGuard = new ConjunctiveSelectQuery(this.dataSchema);
        secondGuard.addBinaryCondition(BinaryConditionProvider.equality(this.subBlocks[0].getLifeCycleVariable(), State.COMPLETED));//check if the first sub-block is "completed"
        secondGuard.addBinaryCondition(BinaryConditionProvider.equality(this.subBlocks[1].getLifeCycleVariable(), State.COMPLETED));//check if the second sub-block is "completed"
        InsertTransition secondUpdate = new InsertTransition(this.name + indexGenerator.getNext(), secondGuard, this.dataSchema);
        secondUpdate.setControlCaseVariableValue(this.subBlocks[0].getLifeCycleVariable(), State.IDLE);//change the state of the first sub-block to "idle"
        secondUpdate.setControlCaseVariableValue(this.subBlocks[1].getLifeCycleVariable(), State.IDLE);//change the state of the second sub-block to "idle"
        secondUpdate.setControlCaseVariableValue(this.lifeCycle, State.COMPLETED);//set the state variable of the main block to "completed"

        // generate MCMT translation
        result += firstUpdate.getMCMTTranslation() + "\n" + secondUpdate.getMCMTTranslation() + "\n";

        return result;

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
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof DABParallelBlock))
            return false;
        DABParallelBlock obj = (DABParallelBlock) o;
        return name.equals(obj.getName()) && lifeCycle.equals(obj.getLifeCycleVariable());
    }

}