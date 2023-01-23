package org.openbpmn.bpmn.elements;

import java.util.Set;

import org.openbpmn.bpmn.BPMNModel;
import org.openbpmn.bpmn.elements.core.BPMNElementNode;
import org.openbpmn.bpmn.exceptions.BPMNModelException;
import org.w3c.dom.Element;

/**
 * An Data object is work within an Activity.  * 
 * @author Ali Nour Eldin
 *
 */
public class DataObjectAttributeExtension extends BPMNElementNode {

    public final static double DEFAULT_WIDTH = 110.0;
    public final static double DEFAULT_HEIGHT = 50.0;
    @SuppressWarnings("unused")
	private DataInputObjectExtension dataInputObject=null;
    
    @SuppressWarnings("unused")
	private DataOutputObjectExtension dataOutputObject=null;
    
    protected DataObjectAttributeExtension(BPMNModel model, Element node, String type, BPMNProcess bpmnProcess,DataInputObjectExtension dataObject) throws BPMNModelException {
        super(model, node, type,bpmnProcess);
        this.dataInputObject = dataObject;
    }

    protected DataObjectAttributeExtension(BPMNModel model, Element node, String type, BPMNProcess bpmnProcess,DataOutputObjectExtension dataObject) throws BPMNModelException {
        super(model, node, type,bpmnProcess);
        this.dataOutputObject = dataObject;
    }
    
    public Set<DataObjectAttributeExtension> getAttributesObject() {
    if ( dataInputObject!=null) {
    	return dataInputObject.getDataAttributes();
    }
    if ( dataOutputObject!=null) {
    	return dataOutputObject.getDataAttributes();
    }
    return null;
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
