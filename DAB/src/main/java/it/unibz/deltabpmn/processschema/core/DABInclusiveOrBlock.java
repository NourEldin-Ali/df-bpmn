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
import it.unibz.deltabpmn.processschema.blocks.InclusiveOrBlock;
import it.unibz.deltabpmn.verification.mcmt.NameManager;

class DABInclusiveOrBlock implements InclusiveOrBlock {
    private String name;
    private CaseVariable lifeCycle;
    private Block[] subBlocks;
    private DataSchema dataSchema;
    private ConjunctiveSelectQuery cond1;
    private ConjunctiveSelectQuery cond2;


    public DABInclusiveOrBlock(String name, DataSchema schema) {
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
    public void addFirstFlowCondition(ConjunctiveSelectQuery cond) {
        this.cond1 = cond;
    }

    @Override
    public void addSecondFlowCondition(ConjunctiveSelectQuery cond) {
        this.cond2 = cond;
    }

    @Override
    public String getMCMTTranslation() throws InvalidInputException, UnmatchingSortException, EevarOverflowException, EmptyGuardException {
        String result = "";

        IndexGenerator indexGenerator = NameProcessor.getIndexGenerator();

        // first part: itself ENABLED and cond1 and NOT cond2 --> B1 ENABLED and itself ACTIVE1
        ConjunctiveSelectQuery firstGuard = new ConjunctiveSelectQuery(this.dataSchema);
        firstGuard.addBinaryCondition(BinaryConditionProvider.equality(this.lifeCycle, State.ENABLED));
        InsertTransition firstUpdate = new InsertTransition(this.name + indexGenerator.getNext(), firstGuard, this.dataSchema);
        firstUpdate.addTaskGuard(cond1.getMCMTTranslation());
        firstUpdate.addTaskGuard(cond2.getNegatedMCMT());
        firstUpdate.setControlCaseVariableValue(this.lifeCycle, State.ACTIVE_SINGLE);//"active1" is used for a situation in which you have only one path
        firstUpdate.setControlCaseVariableValue(this.subBlocks[0].getLifeCycleVariable(), State.ENABLED);

        // second part: itself ENABLED and NOT cond1 and cond2 --> B2 ENABLED and itself ACTIVE1
        ConjunctiveSelectQuery secondGuard = new ConjunctiveSelectQuery(this.dataSchema);
        secondGuard.addBinaryCondition(BinaryConditionProvider.equality(this.lifeCycle, State.ENABLED));
        InsertTransition secondUpdate = new InsertTransition(this.name + indexGenerator.getNext(), secondGuard, this.dataSchema);
        secondUpdate.addTaskGuard(cond2.getMCMTTranslation());
        secondUpdate.addTaskGuard(cond1.getNegatedMCMT());
        secondUpdate.setControlCaseVariableValue(this.lifeCycle, State.ACTIVE_SINGLE);//"active1" is used for a situation in which you have only one path
        secondUpdate.setControlCaseVariableValue(this.subBlocks[1].getLifeCycleVariable(), State.ENABLED);


        // third part: itself ENABLED and BOTH cond1 and cond2 --> B1 ENABLED, B2 ENABLED and itself ACTIVE2
        ConjunctiveSelectQuery thirdGuard = new ConjunctiveSelectQuery(this.dataSchema);
        thirdGuard.addBinaryCondition(BinaryConditionProvider.equality(lifeCycle, State.ENABLED));
        InsertTransition thirdUpdate = new InsertTransition(this.name + indexGenerator.getNext(), thirdGuard, this.dataSchema);
        thirdUpdate.addTaskGuard(cond1.getMCMTTranslation());
        thirdUpdate.addTaskGuard(cond2.getMCMTTranslation());
        thirdUpdate.setControlCaseVariableValue(this.lifeCycle, State.ACTIVE_ALL);//"active2" is used for a situation in which you have both paths enabled
        thirdUpdate.setControlCaseVariableValue(this.subBlocks[0].getLifeCycleVariable(), State.ENABLED);
        thirdUpdate.setControlCaseVariableValue(this.subBlocks[1].getLifeCycleVariable(), State.ENABLED);

        // fourth part: B1 completed and itself ACTIVE1 --> B1 idle and itself completed
        ConjunctiveSelectQuery fourthGuard = new ConjunctiveSelectQuery(this.dataSchema);
        fourthGuard.addBinaryCondition(BinaryConditionProvider.equality(this.subBlocks[0].getLifeCycleVariable(), State.COMPLETED));
        fourthGuard.addBinaryCondition(BinaryConditionProvider.equality(this.lifeCycle, State.ACTIVE_SINGLE));
        InsertTransition fourthUpdate = new InsertTransition(this.name + indexGenerator.getNext(), fourthGuard, this.dataSchema);
        fourthUpdate.setControlCaseVariableValue(this.subBlocks[0].getLifeCycleVariable(), State.IDLE);
        fourthUpdate.setControlCaseVariableValue(this.lifeCycle, State.COMPLETED);

        // fifth part: B2 completed and itself ACTIVE1 --> B2 idle and itself completed
        ConjunctiveSelectQuery fifthGuard = new ConjunctiveSelectQuery(this.dataSchema);
        fifthGuard.addBinaryCondition(BinaryConditionProvider.equality(this.subBlocks[1].getLifeCycleVariable(), State.COMPLETED));
        fifthGuard.addBinaryCondition(BinaryConditionProvider.equality(this.lifeCycle, State.ACTIVE_SINGLE));
        InsertTransition fifthUpdate = new InsertTransition(this.name + indexGenerator.getNext(), fifthGuard, this.dataSchema);
        fifthUpdate.setControlCaseVariableValue(this.subBlocks[1].getLifeCycleVariable(), State.IDLE);
        fifthUpdate.setControlCaseVariableValue(this.lifeCycle, State.COMPLETED);

        // sixth part: B1 and B2 completed and itself ACTIVE2 --> B1 and B2 idle and itself completed
        ConjunctiveSelectQuery sixthGuard = new ConjunctiveSelectQuery(this.dataSchema);
        sixthGuard.addBinaryCondition(BinaryConditionProvider.equality(this.subBlocks[0].getLifeCycleVariable(), State.COMPLETED));
        sixthGuard.addBinaryCondition(BinaryConditionProvider.equality(this.subBlocks[1].getLifeCycleVariable(), State.COMPLETED));
        sixthGuard.addBinaryCondition(BinaryConditionProvider.equality(this.lifeCycle, State.ACTIVE_ALL));
        InsertTransition sixthUpdate = new InsertTransition(this.name + indexGenerator.getNext(), sixthGuard, this.dataSchema);
        sixthUpdate.setControlCaseVariableValue(this.subBlocks[0].getLifeCycleVariable(), State.IDLE);
        sixthUpdate.setControlCaseVariableValue(this.subBlocks[1].getLifeCycleVariable(), State.IDLE);
        sixthUpdate.setControlCaseVariableValue(this.lifeCycle, State.COMPLETED);

        // generate MCMT translation
        result += firstUpdate.getMCMTTranslation() + "\n" + secondUpdate.getMCMTTranslation() + "\n" + thirdUpdate.getMCMTTranslation() + "\n" + fourthUpdate.getMCMTTranslation() + "\n" + fifthUpdate.getMCMTTranslation() + "\n" + sixthUpdate.getMCMTTranslation() + "\n";

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
        if (!(o instanceof DABInclusiveOrBlock))
            return false;
        DABInclusiveOrBlock obj = (DABInclusiveOrBlock) o;
        return name.equals(obj.getName()) && lifeCycle.equals(obj.getLifeCycleVariable());
    }
}
