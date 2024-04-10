package it.unibz.deltabpmn.processschema.core;

import it.unibz.deltabpmn.datalogic.ComplexTransition;
import it.unibz.deltabpmn.datalogic.ConjunctiveSelectQuery;
import it.unibz.deltabpmn.datalogic.InsertTransition;
import it.unibz.deltabpmn.dataschema.core.DataSchema;
import it.unibz.deltabpmn.processschema.blocks.*;
import it.unibz.deltabpmn.processschema.blocks.providers.*;

public final class ProcessSchema implements TaskProvider, EventProvider, ProcessBlockProvider, SequenceBlockProvider, DeferredChoiceBlockProvider, ExclusiveChoiceBlockProvider, InclusiveOrBlockProvider, LoopBlockProvider, PossibleCompletionProvider, ErrorEventBlockProvider, ForwardExceptionBlockProvider, BackwardExceptionBlockProvider, ParallelBlockProvider {

    private DataSchema dataSchema;

    public ProcessSchema(DataSchema schema) {
        this.dataSchema = schema;
    }

    @Override
    public Task newTask(String name) {
        return new DABTask(name, this.dataSchema);
    }

    @Override
    public Task newTask(String name, ComplexTransition eff) {
        return new DABTask(name, eff, this.dataSchema);
    }

    @Override
    public Event newEvent(String name) {
        return new DABEvent(name, this.dataSchema);
    }

    @Override
    public Event newEvent(String name, ComplexTransition eff) {
        return new DABEvent(name, eff, this.dataSchema);
    }

    @Override
    public ProcessBlock newProcessBlock(String name) {
        return new DABProcessBlock(name, this.dataSchema);
    }

    @Override
    public ProcessBlock newProcessBlock(String name, InsertTransition ins) {
        return new DABProcessBlock(name, ins, this.dataSchema);
    }

    @Override
    public SequenceBlock newSequenceBlock(String name) {
        return new DABSequenceBlock(name, this.dataSchema);
    }

    @Override
    public DeferredChoiceBlock newDeferredChoiceBlock(String name) {
        return new DABDeferredChoiceBlock(name, this.dataSchema);
    }

    @Override
    public ExclusiveChoiceBlock newExclusiveChoiceBlock(String name) {
        return new DABExclusiveChoiceBlock(name, this.dataSchema);
    }

    @Override
    public InclusiveOrBlock newInclusiveOrBlock(String name) {
        return new DABInclusiveOrBlock(name, this.dataSchema);
    }

    @Override
    public LoopBlock newLoopBlock(String name) {
        return new DABLoopBlock(name, this.dataSchema);
    }

    @Override
    public LoopBlock newLoopBlock(String name, ConjunctiveSelectQuery cond) {
        return new DABLoopBlock(name, cond, this.dataSchema);
    }

//    @Override
//    public ErrorBlock newErrorBlock(String name, Block handler) {
//        return new DABErrorBlock(name, handler, this.dataSchema);
//    }
//
//    @Override
//    public ErrorBlock newErrorBlock(String name, Block handler, ConjunctiveSelectQuery cond) {
//        return new DABErrorBlock(name, handler, cond, this.dataSchema);
//    }

    @Override
    public PossibleCompletion newPossibleCompletion(String name) {
        return new DABPossibleCompletionBlock(name, this.dataSchema);
    }

    @Override
    public PossibleCompletion newPossibleCompletion(String name, ConjunctiveSelectQuery cond) {
        return new DABPossibleCompletionBlock(name, cond, this.dataSchema);
    }

    @Override
    public ErrorEventBlock newErrorEventBlock(String name, Block handler) {
        return new DABErrorEventBlock(name, handler, this.dataSchema);
    }

    @Override
    public ForwardExceptionBlock newForwardExceptionBlock(String name) {
        return new DABForwardExceptionBlock(name, this.dataSchema);
    }

    @Override
    public BackwardExceptionBlock newBackwardExceptionBlock(String name) {
        return new DABBackwardExceptionBlock(name, this.dataSchema);
    }

    @Override
    public ParallelBlock newParallelBlock(String name) {
        return new DABParallelBlock(name, this.dataSchema);
    }


}
