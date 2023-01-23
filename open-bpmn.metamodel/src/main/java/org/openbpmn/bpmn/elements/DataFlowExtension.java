package org.openbpmn.bpmn.elements;

import org.openbpmn.bpmn.BPMNModel;
import org.openbpmn.bpmn.elements.core.BPMNElementEdge;
import org.w3c.dom.Element;

public class DataFlowExtension extends BPMNElementEdge {
    protected BPMNProcess bpmnProcess = null;
    protected Activity activity = null;
    public DataFlowExtension(BPMNModel model, Element node, String _type, BPMNProcess _bpmnProcess,Activity activity) {
        super(model, node, _type);
        this.bpmnProcess=_bpmnProcess;
        this.activity = activity;
    }


    /**
     * Returns the BPMN Process this element belongs to.
     * 
     * @return
     */
    public BPMNProcess getProcess() {
        return bpmnProcess;
    }

}