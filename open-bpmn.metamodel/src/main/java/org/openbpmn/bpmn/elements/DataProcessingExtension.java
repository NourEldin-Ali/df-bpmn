package org.openbpmn.bpmn.elements;

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
    public double getDefaultHeigth() {
        return DEFAULT_HEIGHT;
    }
}
