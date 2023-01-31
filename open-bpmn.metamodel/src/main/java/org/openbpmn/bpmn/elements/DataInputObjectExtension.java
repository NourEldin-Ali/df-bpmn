package org.openbpmn.bpmn.elements;

import java.util.LinkedHashSet;
import java.util.Set;

import org.openbpmn.bpmn.BPMNModel;
import org.openbpmn.bpmn.BPMNNS;
import org.openbpmn.bpmn.BPMNTypes;
import org.openbpmn.bpmn.elements.core.BPMNElement;
import org.openbpmn.bpmn.elements.core.BPMNElementNode;
import org.openbpmn.bpmn.exceptions.BPMNInvalidTypeException;
import org.openbpmn.bpmn.exceptions.BPMNMissingElementException;
import org.openbpmn.bpmn.exceptions.BPMNModelException;
import org.w3c.dom.Element;

/**
 * An Data object is work within an Activity.  * 
 * @author Ali Nour Eldin
 *
 */
public class DataInputObjectExtension extends BPMNElementNode {

    public final static double DEFAULT_WIDTH = 130.0;
    public final static double DEFAULT_HEIGHT = 50.0;
	public Activity activity = null;
    
	
    protected DataInputObjectExtension(BPMNModel model, Element node, String type, BPMNProcess bpmnProcess, Activity activity) throws BPMNModelException {
        super(model, node, type,bpmnProcess);
        this.activity = activity;
        this.dataAttributes = new LinkedHashSet<DataObjectAttributeExtension>();
    }

    @Override
    public double getDefaultWidth() {
        return DEFAULT_WIDTH;
    }


    @Override
    public double getDefaultHeigth() {
        return DEFAULT_HEIGHT;
    }
    
    private Set<DataObjectAttributeExtension> dataAttributes = null;
    
    
    
    
	public Set<DataObjectAttributeExtension> getDataAttributes() {
		return dataAttributes;
	}

	public void setDataAttributes(Set<DataObjectAttributeExtension> dataAttributes) {
		dataAttributes = dataAttributes;
	}
	
	
	public DataObjectAttributeExtension addAttributeObject( String attName, String attType)
			throws BPMNModelException {
		if (this.getElementNode() == null) {
			throw new BPMNMissingElementException("Missing ElementNode!");
		}

		if (this.getElementNode().getTagName().contains(BPMNTypes.DATA_INPUT_OBJECT_LOCAL)) {
			throw new BPMNInvalidTypeException("Local type can't be contains attributes");
		}
			
		
		if (attName.isEmpty() || attType.isEmpty()) {
			throw new BPMNInvalidTypeException("Attribute name and attribute type should be filled");
		}
//		for (DataObjectAttributeExtension dataObjectAttributeExtension : DataAttributes) {
//			if(dataObjectAttributeExtension.getAttribute("name").contentEquals(attName)) {
//				throw new BPMNInvalidTypeException("Attribute name already used");
//			}
//		}
		Element dataObject = model.createElement(BPMNNS.BPMN2, BPMNTypes.DATA_OBJECT_ATTRIBUTE);
		dataObject.setAttribute("id",  BPMNModel.generateShortID(BPMNTypes.DATA_OBJECT_ATTRIBUTE_Extension));
		dataObject.setAttribute("name", attName);
		dataObject.setAttribute("type", attType);
		
		this.getElementNode().appendChild(dataObject);
		
		DataObjectAttributeExtension data = this.createDataAttributObjectByNode(dataObject);
		return data;
	}

	protected DataObjectAttributeExtension createDataAttributObjectByNode(Element element) throws BPMNModelException {
		DataObjectAttributeExtension dataObject = null;
		dataObject = new DataObjectAttributeExtension(model, element, element.getLocalName(), this.getBpmnProcess(), this);
		getDataAttributes().add(dataObject);
		return dataObject;
	}

	public BPMNElement findElementById(String id) {
   		for (DataObjectAttributeExtension element : getDataAttributes()) {
   			if (id.equals(element.getId())) {
				return element;
			}
		}
   		return null;
   	}

}
