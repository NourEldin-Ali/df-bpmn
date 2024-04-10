package it.unibz.deltabpmn.processschema.core;

import it.unibz.deltabpmn.datalogic.BinaryConditionProvider;
import it.unibz.deltabpmn.datalogic.ConjunctiveSelectQuery;
import it.unibz.deltabpmn.datalogic.InsertTransition;
import it.unibz.deltabpmn.dataschema.core.DataSchema;
import it.unibz.deltabpmn.dataschema.core.SystemSorts;
import it.unibz.deltabpmn.dataschema.elements.CaseVariable;
import it.unibz.deltabpmn.exception.*;
import it.unibz.deltabpmn.processschema.blocks.Block;
import it.unibz.deltabpmn.processschema.blocks.PossibleCompletion;
import it.unibz.deltabpmn.verification.mcmt.NameManager;


public class DABPossibleCompletionBlock implements PossibleCompletion {

    private String name;
    private CaseVariable lifeCycle;
    private Block[] subBlocks;
    private DataSchema dataSchema;
    private ConjunctiveSelectQuery cond;
    private CaseVariable global;

    public DABPossibleCompletionBlock(String name, DataSchema schema) {
        this.name = NameManager.normaliseName(name);
        this.cond = new ConjunctiveSelectQuery(this.dataSchema);
        this.subBlocks = new Block[0];
        this.dataSchema = schema;
        this.lifeCycle = this.dataSchema.newCaseVariable("lifecycle" + this.name, SystemSorts.STRING, true);
        this.lifeCycle.setLifeCycle(1);
        this.global = null;
    }

    public DABPossibleCompletionBlock(String name, ConjunctiveSelectQuery cond, DataSchema schema) {
        this.name = NameManager.normaliseName(name);
        this.cond = cond;
        this.subBlocks = new Block[0];
        this.dataSchema = schema;
        this.lifeCycle = this.dataSchema.newCaseVariable("lifecycle" + this.name, SystemSorts.STRING, true);
        this.lifeCycle.setLifeCycle(1);
        this.global = null;
    }


    @Override
    public void addMainProcessLifecycleVariable(CaseVariable global) {
        this.global = global;
    }

    @Override
    public void addCondition(ConjunctiveSelectQuery cond) {
        this.cond = cond;
    }

    @Override
    public String getMCMTTranslation() throws InvalidInputException, UnmatchingSortException, EevarOverflowException, EmptyGuardException, UninitializedLifecyleVariableException {

        if (this.global == null)
            throw new UninitializedLifecyleVariableException("The global lifecycle variable has not been specified in the possible completion block " + this.name);
        String result = "";

        IndexGenerator indexGenerator = NameProcessor.getIndexGenerator();

        // first execution step via an MCMT transition:  cond TRUE --> itself COMPLETED
        //check if the sub-block's state is set to "completed"
        ConjunctiveSelectQuery firstGuard = new ConjunctiveSelectQuery(this.dataSchema);
        firstGuard.addBinaryCondition(BinaryConditionProvider.equality(this.lifeCycle, State.ENABLED));
        InsertTransition firstUpdate = new InsertTransition(this.name + indexGenerator.getNext(), firstGuard, this.dataSchema);
        firstUpdate.addTaskGuard(this.cond.getMCMTTranslation());//add condition to the guard that will be checked directly in MCMT
        firstUpdate.setControlCaseVariableValue(this.lifeCycle, State.COMPLETED);

        // third execution step via an MCMT transition:  cond FALSE --> the main process should become COMPLETED and itself ERROR
        ConjunctiveSelectQuery secondGuard = new ConjunctiveSelectQuery(this.dataSchema);
        secondGuard.addBinaryCondition(BinaryConditionProvider.equality(this.lifeCycle, State.ENABLED));
        InsertTransition secondUpdate = new InsertTransition(this.name + indexGenerator.getNext(), secondGuard, this.dataSchema);
        secondUpdate.addTaskGuard(this.cond.getNegatedMCMT());// adds a guard that guarantees that the process will progress along a path associated to a condition that is FALSE
        secondUpdate.setControlCaseVariableValue(this.global, State.COMPLETED); //ToDo: check whether this approach works
        secondUpdate.setControlCaseVariableValue(this.lifeCycle, State.ERROR);


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
        if (!(o instanceof DABPossibleCompletionBlock))
            return false;
        DABPossibleCompletionBlock obj = (DABPossibleCompletionBlock) o;
        return name.equals(obj.getName()) && lifeCycle.equals(obj.getLifeCycleVariable());
    }
}
