package org.openbpmn.bpmn.elements;

import java.util.Set;
import java.util.stream.Collectors;

import org.openbpmn.bpmn.BPMNModel;
import org.openbpmn.bpmn.elements.core.BPMNElementNode;
import org.openbpmn.bpmn.exceptions.BPMNModelException;
import org.w3c.dom.Element;

public class DataProcessingExtension extends BPMNElementNode {

    public final static double DEFAULT_WIDTH = 50.0;
    public final static double DEFAULT_HEIGHT = 50.0;
    public static final double LABEL_OFFSET = 0;
    public Activity activity = null;
    
    public DataProcessingExtension(BPMNModel model, Element node, String type, BPMNProcess bpmnProcess,Activity activity) throws BPMNModelException {
        super(model, node, type,bpmnProcess);
        this.activity = activity;
    }

    @Override
    public double getDefaultWidth() {
        return DEFAULT_WIDTH;
    }

    @Override
    public double getDefaultHeight() {
        return DEFAULT_HEIGHT;
    }
    
    
    /**
     * Returns a List of all ingoing SequenceFlows associated with this element
     * 
     * @return
     */
    public Set<DataFlowExtension> getIncomingDataFlows() {
        // filter all sequenceFlows with a target to this elementNode
        Set<DataFlowExtension> result = this.activity.getDataFlows()
                .stream()
                .filter(c -> c.getTargetRef().equals(this.getId()))
                .collect(Collectors.toSet());
        return result;

    }
    
    /**
     * Returns a List of all outgoing SequenceFlows associated with this element
     * 
     * @return
     */
    public Set<DataFlowExtension> getOutgoingDataFlows() {
        // filter all sequenceFlows with a sourceRef to this elementNode
        Set<DataFlowExtension> result = this.activity.getDataFlows()
                .stream()
                .filter(c -> c.getSourceRef().equals(this.getId()))
                .collect(Collectors.toSet());
        return result;

    }
    
}
