package it.unibz.deltabpmn.processschema.core;

import it.unibz.deltabpmn.datalogic.BinaryConditionProvider;
import it.unibz.deltabpmn.datalogic.ConjunctiveSelectQuery;
import it.unibz.deltabpmn.datalogic.InsertTransition;
import it.unibz.deltabpmn.dataschema.core.SystemSorts;
import it.unibz.deltabpmn.dataschema.elements.CaseVariable;
import it.unibz.deltabpmn.dataschema.elements.Constant;
import it.unibz.deltabpmn.dataschema.core.DataSchema;
import it.unibz.deltabpmn.exception.EevarOverflowException;
import it.unibz.deltabpmn.exception.EmptyGuardException;
import it.unibz.deltabpmn.exception.InvalidInputException;
import it.unibz.deltabpmn.exception.UnmatchingSortException;
import it.unibz.deltabpmn.processschema.blocks.BackwardExceptionBlock;
import it.unibz.deltabpmn.processschema.blocks.Block;
import it.unibz.deltabpmn.processschema.blocks.Task;
import it.unibz.deltabpmn.verification.mcmt.NameManager;



class DABBackwardExceptionBlock implements BackwardExceptionBlock {

    private String name;
    private CaseVariable lifeCycle;
    private Block[] subBlocks;
    private DataSchema dataSchema;
    private Constant error;

    public DABBackwardExceptionBlock(String name, DataSchema dataSchema) {
        this.name = NameManager.normaliseName(name);
        this.subBlocks = new Block[2];
        this.dataSchema = dataSchema;
        this.lifeCycle = this.dataSchema.newCaseVariable("lifecycle" + this.name, SystemSorts.STRING, true);
        this.lifeCycle.setLifeCycle(1);
    }

    //ToDo: add proper block definition: Block 1 is a block dealing with an exception
    @Override
    public void addFirstBlock(Block b) {
        this.subBlocks[0] = b;
        // add constant error
        this.error = this.dataSchema.newConstant("Error" + b.getName(), SystemSorts.STRING);
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
        firstGuard.addBinaryCondition(BinaryConditionProvider.equality(this.lifeCycle, State.ENABLED));//check if the first block is enabled
        InsertTransition firstUpdate = new InsertTransition(this.name + indexGenerator.getNext(), firstGuard, this.dataSchema);
        firstUpdate.setControlCaseVariableValue(this.subBlocks[0].getLifeCycleVariable(), State.ENABLED);//the state of the first sub-block becomes "enabled"
        firstUpdate.setControlCaseVariableValue(this.lifeCycle, State.ACTIVE);//the state of the current block becomes "active"

        // second part: B1 completed --> B1 IDLE and itself COMPLETED
        ConjunctiveSelectQuery secondGuard = new ConjunctiveSelectQuery(this.dataSchema);
        secondGuard.addBinaryCondition(BinaryConditionProvider.equality(this.subBlocks[0].getLifeCycleVariable(), State.COMPLETED)); //check if the first sub-block has finished working and now its state is set to "completed"
        InsertTransition secondUpdate = new InsertTransition(this.name + indexGenerator.getNext(), secondGuard, this.dataSchema);
        secondUpdate.setControlCaseVariableValue(this.subBlocks[0].getLifeCycleVariable(), State.IDLE);//the first sub-block B1 changes its state to "idle"
        secondUpdate.setControlCaseVariableValue(this.lifeCycle, State.COMPLETED);//the state of the current block becomes "completed"

        // third part: B1 error --> B1 IDLE and B2 ENABLED
        ConjunctiveSelectQuery thirdGuard = new ConjunctiveSelectQuery(this.dataSchema);
        thirdGuard.addBinaryCondition(BinaryConditionProvider.equality(this.subBlocks[0].getLifeCycleVariable(), this.error));//check if the first sub-block threw an error (i.e., its state is "error")
        InsertTransition thirdUpdate = new InsertTransition(this.name + indexGenerator.getNext(), thirdGuard, this.dataSchema);
        thirdUpdate.setControlCaseVariableValue(this.subBlocks[0].getLifeCycleVariable(), State.IDLE);//the first sub-block goes to "idle"
        thirdUpdate.setControlCaseVariableValue(this.subBlocks[1].getLifeCycleVariable(), State.ENABLED);//the second sub-block, used for error-handling, becomes "enabled"

        // fourth part:  B2 completed --> B1 enabled B2 idle
        ConjunctiveSelectQuery fourthGuard = new ConjunctiveSelectQuery(this.dataSchema);
        fourthGuard.addBinaryCondition(BinaryConditionProvider.equality(this.subBlocks[1].getLifeCycleVariable(), State.COMPLETED));//check if the error-handling sub-block B2 has finished working (i.e., its state is "completed")
        InsertTransition fourthUpdate = new InsertTransition(this.name + indexGenerator.getNext(), fourthGuard, this.dataSchema);
        fourthUpdate.setControlCaseVariableValue(this.subBlocks[0].getLifeCycleVariable(), State.ENABLED);//we enable again the first sub-block (as the pattern goes in the loop thanks to a XOR gateway)
        fourthUpdate.setControlCaseVariableValue(this.subBlocks[1].getLifeCycleVariable(), State.IDLE);//the second error-handling sub-block changes its state to "idle"

        // fifth part, non deterministic error (contains T_{err_1} only for a state Enabled)
        //ToDo: add T_{err_3} from translation, as "active" and "waiting" in this code are the same
        //fifth part: non-deterministic case in which B1 is set to ERROR (in that case the third part can be enabled)
        ConjunctiveSelectQuery fifthGuard = new ConjunctiveSelectQuery(this.dataSchema);
        fifthGuard.addBinaryCondition(BinaryConditionProvider.equality(this.subBlocks[0].getLifeCycleVariable(), State.ENABLED));//check if the first sub-block is "enabled"
        InsertTransition nondeterministicUpdate = new InsertTransition(this.name + indexGenerator.getNext(), fifthGuard, this.dataSchema);
        nondeterministicUpdate.setControlCaseVariableValue(this.subBlocks[0].getLifeCycleVariable(), this.error);//non-deterministically generate an error for the first sub-block
        startPropagation(this.subBlocks[0], nondeterministicUpdate);//start the propagation by putting to "idle" all the sub-blocks


        // generate MCMT translation
        result += firstUpdate.getMCMTTranslation() + "\n" + secondUpdate.getMCMTTranslation() + "\n" + thirdUpdate.getMCMTTranslation() + "\n" + fourthUpdate.getMCMTTranslation() + "\n" + nondeterministicUpdate.getMCMTTranslation() + "\n";
        nondeterministicUpdate.setGuard("(= " + this.subBlocks[0].getLifeCycleVariable().getName() + " "+State.ACTIVE.getName()+")");
        nondeterministicUpdate.setName(this.name + indexGenerator.getNext());
        result += nondeterministicUpdate.getMCMTTranslation() + "\n";

        return result;

    }

    private void startPropagation(Block handler, InsertTransition transition) throws InvalidInputException, UnmatchingSortException {
        for (Block block : handler.getSubBlocks())
            propagateError(block, transition);
    }

    private void propagateError(Block current, InsertTransition transition) throws InvalidInputException, UnmatchingSortException {
        if (current instanceof Task) {
            transition.setControlCaseVariableValue(current.getLifeCycleVariable(), State.IDLE);
            return;
        }
        transition.setControlCaseVariableValue(current.getLifeCycleVariable(), State.IDLE);
        for (Block block : current.getSubBlocks())
            propagateError(block, transition);
    }

//ToDo: do we need to override equals etc. for blocks?

//    @Override
//    public boolean equals(Object o) {
//        if (this == o)
//            return true;
//        if (!(o instanceof DABBackwardExceptionBlock))
//            return false;
//        DABBackwardExceptionBlock obj = (DABBackwardExceptionBlock) o;
//    }

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
        if (!(o instanceof DABBackwardExceptionBlock))
            return false;
        DABBackwardExceptionBlock obj = (DABBackwardExceptionBlock) o;
        return name.equals(obj.getName()) && lifeCycle.equals(obj.getLifeCycleVariable());
    }
}