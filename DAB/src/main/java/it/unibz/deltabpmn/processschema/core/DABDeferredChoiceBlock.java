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
import it.unibz.deltabpmn.processschema.blocks.DeferredChoiceBlock;
import it.unibz.deltabpmn.verification.mcmt.NameManager;

class DABDeferredChoiceBlock implements DeferredChoiceBlock {
    private String name;
    private CaseVariable lifeCycle;
    private Block[] subBlocks;
    private DataSchema dataSchema;

    public DABDeferredChoiceBlock(String name, DataSchema schema) {
        this.name = NameManager.normaliseName(name);
        this.subBlocks = new Block[2];
        this.dataSchema = schema;
        this.lifeCycle = this.dataSchema.newCaseVariable("lifecycle" + this.name, SystemSorts.STRING, true);
        this.lifeCycle.setLifeCycle(1);
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

        // first part: itself ENABLED --> B1 ENABLED and itself ACTIVE
        ConjunctiveSelectQuery firstGuard = new ConjunctiveSelectQuery(this.dataSchema);
        firstGuard.addBinaryCondition(BinaryConditionProvider.equality(this.lifeCycle, State.ENABLED));//check precondition: the block itself must be "enabled"
        InsertTransition firstUpdate = new InsertTransition(this.name + indexGenerator.getNext(), firstGuard, this.dataSchema);
        firstUpdate.setControlCaseVariableValue(this.lifeCycle, State.ACTIVE);//set the status of the main block to "active" (this is done to "disable" the main block as its initial transition won't be able to fire anymore)
        firstUpdate.setControlCaseVariableValue(this.subBlocks[0].getLifeCycleVariable(), State.ENABLED);//make the first sub-block "enabled" (this is meant to happen non-deterministically)

        // second part: itself ENABLED --> B2 ENABLED and itself ACTIVE
        ConjunctiveSelectQuery secondGuard = new ConjunctiveSelectQuery(this.dataSchema);
        secondGuard.addBinaryCondition(BinaryConditionProvider.equality(this.lifeCycle, State.ENABLED));//check precondition: the block itself must be "enabled"
        InsertTransition secondUpdate = new InsertTransition(this.name + indexGenerator.getNext(), secondGuard, this.dataSchema);
        secondUpdate.setControlCaseVariableValue(this.lifeCycle, State.ACTIVE);//set the status of the main block to "active"
        secondUpdate.setControlCaseVariableValue(this.subBlocks[1].getLifeCycleVariable(), State.ENABLED);//make the second sub-block "enabled" (this is meant to happen non-deterministically)


        // third part: B1 completed --> B1 IDLE and itself COMPLETED
        ConjunctiveSelectQuery thirdGuard = new ConjunctiveSelectQuery(this.dataSchema);
        thirdGuard.addBinaryCondition(BinaryConditionProvider.equality(this.subBlocks[0].getLifeCycleVariable(), State.COMPLETED));//check if the first sub-block is "completed"
        InsertTransition thirdUpdate = new InsertTransition(this.name + indexGenerator.getNext(), thirdGuard, this.dataSchema);
        thirdUpdate.setControlCaseVariableValue(this.lifeCycle, State.COMPLETED);//set the status of the whole block to "completed"
        thirdUpdate.setControlCaseVariableValue(this.subBlocks[0].getLifeCycleVariable(), State.IDLE);//set the status of the first sub-block to "idle"

        // fourth part:  B2 completed --> B2 IDLE and itself COMPLETED
        ConjunctiveSelectQuery fourthGuard = new ConjunctiveSelectQuery(this.dataSchema);
        fourthGuard.addBinaryCondition(BinaryConditionProvider.equality(this.subBlocks[1].getLifeCycleVariable(), State.COMPLETED));//check if the second sub-block is "completed"
        InsertTransition fourthUpdate = new InsertTransition(this.name + indexGenerator.getNext(), fourthGuard, this.dataSchema);
        fourthUpdate.setControlCaseVariableValue(this.lifeCycle, State.COMPLETED);//set the status of the whole block to "completed"
        fourthUpdate.setControlCaseVariableValue(this.subBlocks[1].getLifeCycleVariable(), State.IDLE);//set the status of the second sub-block to "idle"

        // generate MCMT translation
        result += firstUpdate.getMCMTTranslation() + "\n" + secondUpdate.getMCMTTranslation() + "\n" + thirdUpdate.getMCMTTranslation() + "\n" + fourthUpdate.getMCMTTranslation() + "\n";

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
        if (!(o instanceof DABDeferredChoiceBlock))
            return false;
        DABDeferredChoiceBlock obj = (DABDeferredChoiceBlock) o;
        return name.equals(obj.getName()) && lifeCycle.equals(obj.getLifeCycleVariable());
    }
}
