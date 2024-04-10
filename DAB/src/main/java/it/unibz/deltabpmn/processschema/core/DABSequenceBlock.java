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
import it.unibz.deltabpmn.processschema.blocks.SequenceBlock;
import it.unibz.deltabpmn.verification.mcmt.NameManager;

class DABSequenceBlock implements SequenceBlock {

    private String name;
    private CaseVariable lifeCycle;
    private Block[] subBlocks;
    private DataSchema dataSchema;

    public DABSequenceBlock(String name, DataSchema schema) {
        this.name = NameManager.normaliseName(name);
        this.subBlocks = new Block[2];
        this.dataSchema = schema;
        this.lifeCycle = this.dataSchema.newCaseVariable("lifecycle" + this.name, SystemSorts.STRING, true);
        this.lifeCycle.setLifeCycle(1);
    }

    @Override
    public String getMCMTTranslation() throws InvalidInputException, UnmatchingSortException, EevarOverflowException, EmptyGuardException {
        String result = "";
        IndexGenerator indexGenerator = NameProcessor.getIndexGenerator();

        // first part: itself ENABLED --> B1 ENABLED and itself ACTIVE
        ConjunctiveSelectQuery firstGuard = new ConjunctiveSelectQuery(this.dataSchema);
        firstGuard.addBinaryCondition(BinaryConditionProvider.equality(this.lifeCycle, State.ENABLED));
        InsertTransition firstUpdate = new InsertTransition(this.name + indexGenerator.getNext(), firstGuard, this.dataSchema);
        firstUpdate.setControlCaseVariableValue(this.lifeCycle, State.ACTIVE);
        firstUpdate.setControlCaseVariableValue(this.subBlocks[0].getLifeCycleVariable(), State.ENABLED);


        // second part: B1 COMPLETED --> B1 IDLE and B2 ENABLED
        ConjunctiveSelectQuery secondGuard = new ConjunctiveSelectQuery(this.dataSchema);
        secondGuard.addBinaryCondition(BinaryConditionProvider.equality(this.subBlocks[0].getLifeCycleVariable(), State.COMPLETED));
        InsertTransition secondUpdate = new InsertTransition(this.name + indexGenerator.getNext(), secondGuard, this.dataSchema);
        secondUpdate.setControlCaseVariableValue(this.subBlocks[0].getLifeCycleVariable(), State.IDLE);
        secondUpdate.setControlCaseVariableValue(this.subBlocks[1].getLifeCycleVariable(), State.ENABLED);

        // third part: B2 COMPLETED --> B2 IDLE and itself COMPLETED
        ConjunctiveSelectQuery thirdGuard = new ConjunctiveSelectQuery(this.dataSchema);
        thirdGuard.addBinaryCondition(BinaryConditionProvider.equality(this.subBlocks[1].getLifeCycleVariable(), State.COMPLETED));
        InsertTransition thirdUpdate = new InsertTransition(this.name + indexGenerator.getNext(), thirdGuard, this.dataSchema);
        thirdUpdate.setControlCaseVariableValue(this.subBlocks[1].getLifeCycleVariable(), State.IDLE);
        thirdUpdate.setControlCaseVariableValue(this.lifeCycle, State.COMPLETED);

        // generate MCMT translation
        result += firstUpdate.getMCMTTranslation() + "\n" + secondUpdate.getMCMTTranslation() + "\n" + thirdUpdate.getMCMTTranslation() + "\n";

        return result;

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
        if (!(o instanceof DABSequenceBlock))
            return false;
        DABSequenceBlock obj = (DABSequenceBlock) o;
        return name.equals(obj.getName()) && lifeCycle.equals(obj.getLifeCycleVariable());
    }
}
