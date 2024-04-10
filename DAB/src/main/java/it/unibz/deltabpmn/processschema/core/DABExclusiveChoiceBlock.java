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
import it.unibz.deltabpmn.processschema.blocks.ExclusiveChoiceBlock;
import it.unibz.deltabpmn.verification.mcmt.NameManager;

class DABExclusiveChoiceBlock implements ExclusiveChoiceBlock {

    private String name;
    private CaseVariable lifeCycle;
    private Block[] subBlocks;
    private DataSchema dataSchema;
    private ConjunctiveSelectQuery cond;

    public DABExclusiveChoiceBlock(String name, DataSchema schema) {
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
    public void addCondition(ConjunctiveSelectQuery cond) {
        this.cond = cond;
    }

    @Override
    public String getMCMTTranslation() throws InvalidInputException, UnmatchingSortException, EevarOverflowException, EmptyGuardException {
        String result = "";
        IndexGenerator indexGenerator = NameProcessor.getIndexGenerator();


        // first part: itself ENABLED and cond TRUE --> B1 ENABLED and itself ACTIVE
        ConjunctiveSelectQuery firstGuard = new ConjunctiveSelectQuery(this.dataSchema);
        firstGuard.addBinaryCondition(BinaryConditionProvider.equality(this.lifeCycle, State.ENABLED));//check if the block is "enabled"
        InsertTransition firstUpdate = new InsertTransition(this.name + indexGenerator.getNext(), firstGuard, this.dataSchema);
        firstUpdate.addTaskGuard(cond.getMCMTTranslation());//add condition to the guard that will be checked directly in MCMT
        firstUpdate.setControlCaseVariableValue(this.subBlocks[0].getLifeCycleVariable(), State.ENABLED);//change the status of the first sub-block to "enabled"
        firstUpdate.setControlCaseVariableValue(this.lifeCycle, State.ACTIVE);//set the status of the current block to "active"

        // second part: itself ENABLED and cond FALSE --> B2 ENABLED and itself ACTIVE
        ConjunctiveSelectQuery secondGuard = new ConjunctiveSelectQuery(this.dataSchema);
        secondGuard.addBinaryCondition(BinaryConditionProvider.equality(this.lifeCycle, State.ENABLED));
        InsertTransition secondUpdate = new InsertTransition(this.name + indexGenerator.getNext(), secondGuard, this.dataSchema);
        secondUpdate.addTaskGuard(cond.getNegatedMCMT());//add condition to the guard that will be checked directly in MCMT if it's false
        secondUpdate.setControlCaseVariableValue(this.subBlocks[1].getLifeCycleVariable(), State.ENABLED);//change the status of the second sub-block to "enabled"
        secondUpdate.setControlCaseVariableValue(this.lifeCycle, State.ACTIVE);//set the status of the current block to "active"


        // third part: B1 completed --> B1 IDLE and itself COMPLETED
        ConjunctiveSelectQuery thirdGuard = new ConjunctiveSelectQuery(this.dataSchema);
        thirdGuard.addBinaryCondition(BinaryConditionProvider.equality(this.subBlocks[0].getLifeCycleVariable(), State.COMPLETED));//check if the first sub-block is "completed"
        InsertTransition thirdUpdate = new InsertTransition(this.name + indexGenerator.getNext(), thirdGuard, this.dataSchema);
        thirdUpdate.setControlCaseVariableValue(this.subBlocks[0].getLifeCycleVariable(), State.IDLE);//set the status of the first sub-block to "idle"
        thirdUpdate.setControlCaseVariableValue(this.lifeCycle, State.COMPLETED);//set the status of the main block to "completed"


        // fourth part:  B2 completed --> B2 IDLE and itself COMPLETED
        ConjunctiveSelectQuery fourthGuard = new ConjunctiveSelectQuery(this.dataSchema);
        fourthGuard.addBinaryCondition(BinaryConditionProvider.equality(this.subBlocks[1].getLifeCycleVariable(), State.COMPLETED));//check if the second sub-block is "completed"
        InsertTransition fourthUpdate = new InsertTransition(this.name + indexGenerator.getNext(), fourthGuard, this.dataSchema);
        fourthUpdate.setControlCaseVariableValue(this.subBlocks[1].getLifeCycleVariable(), State.IDLE);//set the status of the second sub-block to "idle"
        fourthUpdate.setControlCaseVariableValue(this.lifeCycle, State.COMPLETED);//set the status of the main block to "completed"


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
        if (!(o instanceof DABExclusiveChoiceBlock))
            return false;
        DABExclusiveChoiceBlock obj = (DABExclusiveChoiceBlock) o;
        return name.equals(obj.getName()) && lifeCycle.equals(obj.getLifeCycleVariable());
    }
}
