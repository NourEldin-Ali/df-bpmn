package org.openbpmn.bpmn.elements;

import java.io.DataOutput;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.openbpmn.bpmn.BPMNModel;
import org.openbpmn.bpmn.BPMNNS;
import org.openbpmn.bpmn.BPMNTypes;
import org.openbpmn.bpmn.elements.core.BPMNElement;
import org.openbpmn.bpmn.elements.core.BPMNElementEdge;
import org.openbpmn.bpmn.elements.core.BPMNElementNode;
import org.openbpmn.bpmn.exceptions.BPMNInvalidTypeException;
import org.openbpmn.bpmn.exceptions.BPMNMissingElementException;
import org.openbpmn.bpmn.exceptions.BPMNModelException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * An Activity is work that is performed within a Business Process. An Activity
 * can be atomic or non-atomic (compound). The types of Activities that are a
 * part of a Process are: Task, Sub-Process, and Call Activity, which allows the
 * inclusion of re-usable Tasks and Processes in the diagram. However, a Process
 * is not a specific graphical object. Instead, it is a set of graphical
 * objects. The following sub clauses will focus on the graphical objects Sub-
 * Process and Task. Activities represent points in a Process flow where work is
 * performed. They are the executable elements of a BPMN Process. The Activity
 * class is an abstract element, sub-classing from FlowElement
 * 
 * @author rsoika
 *
 *
 *
 *         Each activity regarding our extension that may contains data
 * 
 * @author Ali Nour Eldin
 */
public class Activity extends BPMNElementNode {

	public final static double DEFAULT_WIDTH = 165.0;
	public final static double DEFAULT_HEIGHT = 55.0;

	protected Activity(BPMNModel model, Element node, String type, BPMNProcess bpmnProcess) throws BPMNModelException {
		super(model, node, type, bpmnProcess);
		dataInputObjects = new LinkedHashSet<DataInputObjectExtension>();
		dataOutputObjects = new LinkedHashSet<DataOutputObjectExtension>();
		dataFlows = new LinkedHashSet<DataFlowExtension>();
		setDataReferences(new LinkedHashSet<DataReferenceExtension>());
		setDataProcessing(new LinkedHashSet<DataProcessingExtension>());
	}

	public void openActivity() {
		if (this.elementNode.hasChildNodes()) {
			try {

				initData((Element) this.elementNode);
			} catch (BPMNModelException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@Override
	public double getDefaultWidth() {
		return DEFAULT_WIDTH;
	}

	/*
	 * initialize all data in the activity
	 * 
	 * @author Ali Nour Eldin
	 */
	private boolean initalized = false;

	private void initData(Element node) throws BPMNModelException {
		if (!initalized) {
			// now find all relevant bpmn meta elements
			NodeList childs = node.getChildNodes();
			for (int j = 0; j < childs.getLength(); j++) {
				Node child = childs.item(j);
				if (child.getNodeType() != Node.ELEMENT_NODE) {
					// continue if not a new element node
					continue;
				}

				// check element type
				if (this.isDataInputExtension(child)) {
					DataInputObjectExtension dataInput = this.createDataInputObjectByNode((Element) child);
					if (((Element) child).hasChildNodes())
						initAttributes(dataInput, (Element) child);
				} else if (this.isDataOutputExtension(child)) {
					DataOutputObjectExtension dataOutput = this.createDataOutputObjectByNode((Element) child);
					if (((Element) child).hasChildNodes())
						initAttributes(dataOutput, (Element) child);
				} else if (this.isDataFlowExtension(child)) {
					this.createDataFlowByNode((Element) child);
				} else if (this.isDataReferenceExtension(child)) {
					this.createDataReferenceByNode((Element) child);
				} else if (this.isDataProcessingExtension(child)) {
					this.createDataProcessingByNode((Element) child);
				} else {
					// unsupported node type
				}
			}
			initalized = true;
		}

	}

	/*
	 * Add data lists
	 * 
	 * @author Ali Nour Eldin
	 */
	private Set<DataInputObjectExtension> dataInputObjects;
	private Set<DataOutputObjectExtension> dataOutputObjects;
	private Set<DataFlowExtension> dataFlows;
	private Set<DataReferenceExtension> dataReferences;
	private Set<DataProcessingExtension> dataProcessing;

	public Set<DataInputObjectExtension> getDataInputObjects() {
		return dataInputObjects;
	}

	public void setDataInputObjects(Set<DataInputObjectExtension> dataObjects) {
		this.dataInputObjects = dataObjects;
	}

	public Set<DataOutputObjectExtension> getDataOutputObjects() {
		return dataOutputObjects;
	}

	public void setDataOutputObjects(Set<DataOutputObjectExtension> dataOutputObjects) {
		this.dataOutputObjects = dataOutputObjects;
	}

	public Set<DataFlowExtension> getDataFlows() {
		return dataFlows;
	}

	public void setDataFlows(Set<DataFlowExtension> dataFlows) {
		this.dataFlows = dataFlows;
	}

	public Set<DataReferenceExtension> getDataReferences() {
		return dataReferences;
	}

	public void setDataReferences(Set<DataReferenceExtension> dataReferences) {
		this.dataReferences = dataReferences;
	}

	public Set<DataProcessingExtension> getDataProcessing() {
		return dataProcessing;
	}

	public void setDataProcessing(Set<DataProcessingExtension> dataProcessing) {
		this.dataProcessing = dataProcessing;
	}

	/**
	 * Add data input element
	 * 
	 * @param type        - type of input (local, environment data, ...)
	 * @param objectName  - name of object
	 * @param objectType  - type of object (string, int, ...)
	 * @param isMultiple  - for multiple instance
	 * @param objectValue - value if the type is local
	 * 
	 * @return new DataInputObjectExtension
	 * @throws BPMNMissingElementException
	 * @throws BPMNInvalidTypeException
	 */
	public DataInputObjectExtension addDataInputObject(String type, String objectName, String objectType,
			boolean isMultiple, String objectValue) throws BPMNModelException {
		if (this.getElementNode() == null) {
			throw new BPMNMissingElementException("Missing ElementNode!");
		}

		for (DataInputObjectExtension dataInputObjectExtension : dataInputObjects) {
			if (objectName.isEmpty()) {
				if (dataInputObjectExtension.getAttribute("name").isEmpty()
						&& dataInputObjectExtension.getAttribute("type").contentEquals(objectType)) {
					throw new BPMNInvalidTypeException("objectType with empty objectName already used");
				}
			} else {
//				if (dataInputObjectExtension.getAttribute("name").contentEquals(objectName)) {
//					throw new BPMNInvalidTypeException("objectName already used");
//				}
			}
		}

		if (this.getElementNode().getTagName().contains(BPMNTypes.DATA_INPUT_OBJECT_LOCAL) && objectValue.isEmpty()) {
			throw new BPMNInvalidTypeException("objectValue shoud be filled");
		}

		if (!BPMNTypes.BPMN_DATA_INPUT_EXTENSION.stream().anyMatch(type::contentEquals)) {
			throw new BPMNInvalidTypeException("Invalid type for the inputDataObject");
		}
		Element dataObject = model.createElement(BPMNNS.BPMN2, type);

		dataObject.setAttribute("id", BPMNModel.generateShortID(BPMNTypes.DATA_INPUT_OBJECT_Extension));
		dataObject.setAttribute("name", objectName);
		dataObject.setAttribute("type", objectType);
		dataObject.setAttribute("isMultiple", isMultiple ? "true" : "false");
		if (type.contentEquals(BPMNTypes.DATA_INPUT_OBJECT_LOCAL)) {
			dataObject.setAttribute("value", objectValue);
		}

		this.getElementNode().appendChild(dataObject);

		DataInputObjectExtension data = this.createDataInputObjectByNode(dataObject);
		return data;
	}

	private DataInputObjectExtension createDataInputObjectByNode(Element element) throws BPMNModelException {
		DataInputObjectExtension dataObject = null;
		dataObject = new DataInputObjectExtension(model, element, element.getLocalName(), this.getBpmnProcess(), this);
		getDataInputObjects().add(dataObject);
		return dataObject;
	}

	/**
	 * Add data output element
	 * 
	 * @param type       - type of input (local, environment data, ...)
	 * @param objectName - name of object
	 * @param objectType - type of object (string, int, ...)
	 * @param isMultiple - for multiple instance
	 * @param state      - state of the element
	 * 
	 * @return new DataOutputObjectExtension
	 * @throws BPMNMissingElementException
	 * @throws BPMNInvalidTypeException
	 */
	public DataOutputObjectExtension addDataOutputObject(String type, String objectName, String objectType,
			boolean isMultiple, String state) throws BPMNModelException {
		if (this.getElementNode() == null) {
			throw new BPMNMissingElementException("Missing ElementNode!");
		}

		for (DataInputObjectExtension dataInputObjectExtension : dataInputObjects) {
			if (objectName.isEmpty()) {
				if (dataInputObjectExtension.getAttribute("name").isEmpty()
						&& dataInputObjectExtension.getAttribute("type").contentEquals(objectType)) {
					throw new BPMNInvalidTypeException("objectType with empty objectName already used");
				}
			} else {
//				if (dataInputObjectExtension.getAttribute("name").contentEquals(objectName)) {
//					throw new BPMNInvalidTypeException("objectName already used");
//				}
			}
		}
		if (!BPMNTypes.BPMN_DATA_OUTPUT_EXTENSION.stream().anyMatch(type::contentEquals)) {
			throw new BPMNInvalidTypeException("Invalid type for the inputDataObject");
		}
		Element dataObject = model.createElement(BPMNNS.BPMN2, type);

		dataObject.setAttribute("id", BPMNModel.generateShortID(BPMNTypes.DATA_OUTPUT_OBJECT_Extension));
		dataObject.setAttribute("name", objectName);
		dataObject.setAttribute("type", objectType);
		dataObject.setAttribute("isMultiple", isMultiple ? "true" : "false");
		dataObject.setAttribute("state", state);

		this.getElementNode().appendChild(dataObject);

		DataOutputObjectExtension data = this.createDataOutputObjectByNode(dataObject);
		return data;
	}

	private DataOutputObjectExtension createDataOutputObjectByNode(Element element) throws BPMNModelException {
		DataOutputObjectExtension dataObject = null;
		dataObject = new DataOutputObjectExtension(model, element, element.getLocalName(), this.getBpmnProcess(), this);
		getDataOutputObjects().add(dataObject);
		return dataObject;
	}

	/**
	 * Add data flow element
	 * 
	 * @param sourceObject - id of start point element
	 * @param targetObject - id of end point element
	 * @return new DataFlowExtension
	 * @throws BPMNMissingElementException
	 * @throws BPMNInvalidTypeException
	 */
	public DataFlowExtension addDataFlow(String sourceObject, String targetObject) throws BPMNModelException {
		if (this.getElementNode() == null) {
			throw new BPMNMissingElementException("Missing ElementNode!");
		}

//
		if (sourceObject.contentEquals(targetObject)) {
			throw new BPMNInvalidTypeException("soruce and target should be different");
		}

		if (sourceObject.isEmpty() || targetObject.isEmpty()) {
			throw new BPMNInvalidTypeException("Invalid source or targed");
		}

		// check input
		if (!checkAvailibility(sourceObject)) {
			throw new BPMNInvalidTypeException("Invalid source");
		}

		// check out
		if (!checkAvailibility(targetObject)) {
			throw new BPMNInvalidTypeException("Invalid target");
		}

		Element dataFlow = model.createElement(BPMNNS.BPMN2, BPMNTypes.DATA_FLOW);
		dataFlow.setAttribute("id", BPMNModel.generateShortID(BPMNTypes.DATA_FLOW_Extension));
		dataFlow.setAttribute("sourceRef", sourceObject);
		dataFlow.setAttribute("targetRef", targetObject);

		this.getElementNode().appendChild(dataFlow);

		// add incoming and outgoing to the element
		addIncominOutgoingToElement(dataFlow.getAttribute("id"), sourceObject, true);
		addIncominOutgoingToElement(dataFlow.getAttribute("id"), targetObject, false);

		DataFlowExtension data = this.createDataFlowByNode(dataFlow);
		return data;
	}

	private DataFlowExtension createDataFlowByNode(Element element) throws BPMNModelException {
		DataFlowExtension dataFlow = null;
		dataFlow = new DataFlowExtension(model, element, element.getLocalName(), this.getBpmnProcess(), this);
		getDataFlows().add(dataFlow);
		return dataFlow;
	}

	/**
	 * Add data reference element
	 * 
	 * @param sourceObject - id of start point element
	 * @param targetObject - id of end point element
	 * @return new DataReferenceExtension
	 * @throws BPMNMissingElementException
	 * @throws BPMNInvalidTypeException
	 */
	public DataReferenceExtension addDataReference(String sourceObject, String targetObject) throws BPMNModelException {
		if (this.getElementNode() == null) {
			throw new BPMNMissingElementException("Missing ElementNode!");
		}

//
		if (sourceObject.contentEquals(targetObject)) {
			throw new BPMNInvalidTypeException("soruce and target should be different");
		}

		if (sourceObject.isEmpty() || targetObject.isEmpty()) {
			throw new BPMNInvalidTypeException("Invalid source or targed");
		}

		// check input
		if (!checkAvailibility(sourceObject)) {
			throw new BPMNInvalidTypeException("Invalid source");
		}

		// check out
		if (!checkAvailibility(targetObject)) {
			throw new BPMNInvalidTypeException("Invalid target");
		}

		Element dataRef = model.createElement(BPMNNS.BPMN2, BPMNTypes.DATA_REFERENCE);
		dataRef.setAttribute("id", BPMNModel.generateShortID(BPMNTypes.DATA_REFERENCE_EXTENSION));
		dataRef.setAttribute("sourceRef", sourceObject);
		dataRef.setAttribute("targetRef", targetObject);

		this.getElementNode().appendChild(dataRef);

		// add incoming and outgoing to the element
		addIncominOutgoingToElement(dataRef.getAttribute("id"), sourceObject, true);
		addIncominOutgoingToElement(dataRef.getAttribute("id"), targetObject, false);

		DataReferenceExtension data = this.createDataReferenceByNode(dataRef);
		return data;
	}

	private DataReferenceExtension createDataReferenceByNode(Element element) throws BPMNModelException {
		DataReferenceExtension dataReference = null;
		dataReference = new DataReferenceExtension(model, element, element.getLocalName(), this.getBpmnProcess(), this);
		getDataReferences().add(dataReference);
		return dataReference;
	}

	/**
	 * Add data processing element
	 * 
	 * @param objectName - name putted by the user
	 * @return new DataProcessingExtension
	 * @throws BPMNMissingElementException
	 * @throws BPMNInvalidTypeException
	 * 
	 */
	public DataProcessingExtension addDataProcessing(String objectName) throws BPMNModelException {
		if (this.getElementNode() == null) {
			throw new BPMNMissingElementException("Missing ElementNode!");
		}
		if (objectName.isEmpty()) {
			throw new BPMNInvalidTypeException("Invalid object name");
		}

//		for (DataInputObjectExtension dataInputObjectExtension : dataInputObjects) {
//			if (dataInputObjectExtension.getAttribute("name").contentEquals(objectName)) {
//				throw new BPMNInvalidTypeException("objectName already used");
//			}
//		}

		Element dataProcessing = model.createElement(BPMNNS.BPMN2, BPMNTypes.DATA_PROCESSING);

		dataProcessing.setAttribute("id", BPMNModel.generateShortID(BPMNTypes.DATA_PROCESSING_EXTENSION));
		dataProcessing.setAttribute("name", objectName);

		this.getElementNode().appendChild(dataProcessing);

		DataProcessingExtension data = this.createDataProcessingByNode(dataProcessing);
		return data;
	}

	private DataProcessingExtension createDataProcessingByNode(Element element) throws BPMNModelException {
		DataProcessingExtension dataProcessing = null;
		dataProcessing = new DataProcessingExtension(model, element, element.getLocalName(), this.getBpmnProcess(),
				this);
		getDataProcessing().add(dataProcessing);
		return dataProcessing;
	}

	/**
	 * add the incoming and outgoing to the element
	 * 
	 * @param objectBase - dataflow id or references id
	 * @param object     - source or target
	 * @param isSource   - type of object, source = true, target = false
	 */
	private void addIncominOutgoingToElement(String objectBase, // dataflow id or references id
			String object, // source or target
			boolean isSource//
	) {
		if (object.startsWith(BPMNTypes.DATA_INPUT_OBJECT_Extension)) {
			for (DataInputObjectExtension dataInputObjectExtension : dataInputObjects)
				if (dataInputObjectExtension.getId().contentEquals(object)) {
					Element element = null;
					if (!isSource)
						element = model.createElement(BPMNNS.BPMN2, "incoming");
					else
						element = model.createElement(BPMNNS.BPMN2, "outgoing");
					element.setTextContent(objectBase);
					dataInputObjectExtension.getElementNode().appendChild(element);
					return;
				}

		} else if (object.startsWith(BPMNTypes.DATA_OUTPUT_OBJECT_Extension)) {
			for (DataOutputObjectExtension dataOutputObjectExtension : dataOutputObjects)
				if (dataOutputObjectExtension.getId().contentEquals(object)) {
					Element element = null;
					if (!isSource)
						element = model.createElement(BPMNNS.BPMN2, "incoming");
					else
						element = model.createElement(BPMNNS.BPMN2, "outgoing");
					element.setTextContent(objectBase);
					dataOutputObjectExtension.getElementNode().appendChild(element);
					return;
				}
		} else if (object.startsWith(BPMNTypes.DATA_PROCESSING_EXTENSION)) {
			for (DataProcessingExtension dataProcessing : getDataProcessing())
				if (dataProcessing.getId().contentEquals(object)) {
					Element element = null;
					if (!isSource)
						element = model.createElement(BPMNNS.BPMN2, "incoming");
					else
						element = model.createElement(BPMNNS.BPMN2, "outgoing");
					element.setTextContent(objectBase);
					dataProcessing.getElementNode().appendChild(element);
					return;
				}

		} else if (object.startsWith(BPMNTypes.DATA_OBJECT_ATTRIBUTE_Extension)) {
			for (DataInputObjectExtension dataInputObjectExtension : dataInputObjects)
				for (DataObjectAttributeExtension dataAttributeExtension : dataInputObjectExtension.getDataAttributes())
					if (dataAttributeExtension.getId().contentEquals(object)) {
						Element element = null;
						if (!isSource)
							element = model.createElement(BPMNNS.BPMN2, "incoming");
						else
							element = model.createElement(BPMNNS.BPMN2, "outgoing");
						element.setTextContent(objectBase);
						dataAttributeExtension.getElementNode().appendChild(element);
						return;
					}

			for (DataOutputObjectExtension dataOutputObjectExtension : dataOutputObjects)
				for (DataObjectAttributeExtension dataAttributeExtension : dataOutputObjectExtension
						.getDataAttributes())
					if (dataAttributeExtension.getId().contentEquals(object)) {
						Element element = null;
						if (!isSource)
							element = model.createElement(BPMNNS.BPMN2, "incoming");
						else
							element = model.createElement(BPMNNS.BPMN2, "outgoing");
						element.setTextContent(objectBase);
						dataAttributeExtension.getElementNode().appendChild(element);
						return;
					}
		}

	}

	/**
	 * check if the id is existing the activity element return true if the element
	 * available
	 * 
	 * @param id
	 * @return
	 */
	private boolean checkAvailibility(String id) {

		if (id.startsWith(BPMNTypes.DATA_INPUT_OBJECT_Extension)) {
			for (DataInputObjectExtension dataInputObjectExtension : dataInputObjects)
				if (dataInputObjectExtension.getId().contentEquals(id))
					return true;

		} else if (id.startsWith(BPMNTypes.DATA_OUTPUT_OBJECT_Extension)) {
			for (DataOutputObjectExtension dataOutputObjectExtension : dataOutputObjects)
				if (dataOutputObjectExtension.getId().contentEquals(id))
					return true;

		} else if (id.startsWith(BPMNTypes.DATA_PROCESSING_EXTENSION)) {
			for (DataProcessingExtension dataProcessing : getDataProcessing())
				if (dataProcessing.getId().contentEquals(id))
					return true;

		} else if (id.startsWith(BPMNTypes.DATA_OBJECT_ATTRIBUTE_Extension)) {
			for (DataInputObjectExtension dataInputObjectExtension : dataInputObjects)
				for (DataObjectAttributeExtension dataAttributeExtension : dataInputObjectExtension.getDataAttributes())
					if (dataAttributeExtension.getId().contentEquals(id))
						return true;

			for (DataOutputObjectExtension dataOutputObjectExtension : dataOutputObjects)
				for (DataObjectAttributeExtension dataAttributeExtension : dataOutputObjectExtension
						.getDataAttributes())
					if (dataAttributeExtension.getId().contentEquals(id))
						return true;
		}

		return false;
	}

	/**
	 * initialize the attributes of the data object (input and output)
	 * 
	 * @param bpmnElement
	 * @param node
	 */
	private void initAttributes(BPMNElement bpmnElement, Element node) {
		NodeList childs = node.getChildNodes();
		for (int j = 0; j < childs.getLength(); j++) {
			Node child = childs.item(j);
			if (child.getNodeType() != Node.ELEMENT_NODE) {
				// continue if not a new element node
				continue;
			}
//			System.out.println(bpmnElement.getId());
			if (bpmnElement instanceof DataInputObjectExtension) {
				try {
//					System.out.println(((Element) child).getLocalName());
					if (((Element) child).getLocalName().contentEquals(BPMNTypes.DATA_OBJECT_ATTRIBUTE))
						((DataInputObjectExtension) bpmnElement).createDataAttributObjectByNode((Element) child);
				} catch (BPMNModelException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else if (bpmnElement instanceof DataOutputObjectExtension) {
				try {
//					System.out.println(((Element)child).getId("id"));
					if (((Element) child).getLocalName().contentEquals(BPMNTypes.DATA_OBJECT_ATTRIBUTE))
						((DataOutputObjectExtension) bpmnElement).createDataAttributObjectByNode((Element) child);
				} catch (BPMNModelException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * check if the type node is dataFlow return true if in
	 * 
	 * @param node
	 * @return
	 */
	private boolean isDataFlowExtension(Node node) {
		return BPMNTypes.DATA_FLOW.contentEquals(node.getLocalName());
	}

	/**
	 * check if the type node is dataReference return true if in
	 * 
	 * @param node
	 * @return
	 */
	private boolean isDataReferenceExtension(Node node) {
		return BPMNTypes.DATA_REFERENCE.contentEquals(node.getLocalName());
	}

	/**
	 * check if the type node is dataProcessing return true if in
	 * 
	 * @param node
	 * @return
	 */
	private boolean isDataProcessingExtension(Node node) {
		return BPMNTypes.DATA_PROCESSING.contentEquals(node.getLocalName());
	}

	/**
	 * check if the type node is dataOutputExtension return true if in
	 * 
	 * @param node
	 * @return
	 */
	private boolean isDataOutputExtension(Node node) {
		return BPMNTypes.BPMN_DATA_OUTPUT_EXTENSION.contains(node.getLocalName());
	}

	/**
	 * check if the type node is dataInputExtension return true if in
	 * 
	 * @param node
	 * @return
	 */
	private boolean isDataInputExtension(Node node) {
		return BPMNTypes.BPMN_DATA_INPUT_EXTENSION.contains(node.getLocalName());
	}

	/**
	 * Return element a BPMN Element from this activity if exists, and null if not
	 * 
	 * @param id
	 * @return
	 */
	public BPMNElement findElementById(String id) {
		for (DataInputObjectExtension element : getDataInputObjects()) {
			if (id.equals(element.getId())) {
				return element;
			}
		}
		for (DataOutputObjectExtension element : getDataOutputObjects()) {
			if (id.equals(element.getId())) {
				return element;
			}
		}
		for (DataProcessingExtension element : getDataProcessing()) {
			if (id.equals(element.getId())) {
				return element;
			}
		}
		for (DataReferenceExtension element : getDataReferences()) {
			if (id.equals(element.getId())) {
				return element;
			}
		}

		for (DataFlowExtension element : getDataFlows()) {
			if (id.equals(element.getId())) {
				return element;
			}
		}

		for (DataProcessingExtension element : getDataProcessing()) {
			if (id.equals(element.getId())) {
				return element;
			}
		}

		for (DataInputObjectExtension element : getDataInputObjects()) {
			BPMNElement ele = element.findElementById(id);
			if (ele != null)
				return ele;
		}

		for (DataOutputObjectExtension element : getDataOutputObjects()) {
			BPMNElement ele = element.findElementById(id);
			if (ele != null)
				return ele;
		}
		return null;
	}

	/**
	 * Deletes a BPMN Element from this process
	 * 
	 * @param id
	 */
	public void deleteAllElements() {
		Iterator<DataInputObjectExtension> dataInput = this.getDataInputObjects().iterator();
		while (dataInput.hasNext()) {
			DataInputObjectExtension di = dataInput.next();
			Iterator<DataObjectAttributeExtension> att = di.getDataAttributes().iterator();
			while (att.hasNext()) {
				model.getBpmnPlane().removeChild(att.next().getBpmnShape());
			}
			model.getBpmnPlane().removeChild(di.getBpmnShape());
		}

		Iterator<DataOutputObjectExtension> dataOut = this.getDataOutputObjects().iterator();
		while (dataOut.hasNext()) {
			DataOutputObjectExtension d = dataOut.next();
			Iterator<DataObjectAttributeExtension> att = d.getDataAttributes().iterator();
			while (att.hasNext()) {
				model.getBpmnPlane().removeChild(att.next().getBpmnShape());
			}
			model.getBpmnPlane().removeChild(d.getBpmnShape());
		}

		Iterator<DataProcessingExtension> dataProcessing = this.getDataProcessing().iterator();
		while (dataProcessing.hasNext()) {
			model.getBpmnPlane().removeChild(dataProcessing.next().getBpmnShape());
		}

		Iterator<DataFlowExtension> dataFlow = this.getDataFlows().iterator();
		while (dataFlow.hasNext()) {
			model.getBpmnPlane().removeChild(dataFlow.next().getBpmnEdge());
		}

		Iterator<DataReferenceExtension> dataReferences = this.getDataReferences().iterator();
		while (dataReferences.hasNext()) {
			model.getBpmnPlane().removeChild(dataReferences.next().getBpmnEdge());
		}

	}

	public void deleteElementById(String id) {
		BPMNElement bpmnElement = this.findElementById(id);
		if (bpmnElement == null) {
			// does not exist
			return;
		}

		if (bpmnElement instanceof BPMNElementNode) {
			BPMNElementNode bpmnElementNode = ((BPMNElementNode) bpmnElement);

			// remove all flows
			removeAllEdgesFromElement(bpmnElementNode.getId());

			// delete the shape
			if (bpmnElement instanceof DataObjectAttributeExtension) {
				((DataObjectAttributeExtension) bpmnElement).getElementNode().getParentNode()
						.removeChild(bpmnElementNode.getElementNode());

			} else {
				if (bpmnElement instanceof DataInputObjectExtension
						|| bpmnElement instanceof DataOutputObjectExtension) {
					Set<DataObjectAttributeExtension> attL = null;
					if (bpmnElement instanceof DataInputObjectExtension) {
						attL = ((DataInputObjectExtension) bpmnElement).getDataAttributes();
					} else {
						attL = ((DataOutputObjectExtension) bpmnElement).getDataAttributes();
					}

					for (DataObjectAttributeExtension att : attL) {
						model.getBpmnPlane().removeChild(att.getBpmnShape());
					}

				}
				this.getElementNode().removeChild(bpmnElementNode.getElementNode());
			}

			// finally delete the element from the corresponding list
			if (bpmnElementNode.getBpmnShape() != null) {
				model.getBpmnPlane().removeChild(bpmnElementNode.getBpmnShape());
			}
			if (bpmnElement instanceof DataInputObjectExtension) {

				this.getDataInputObjects().remove(bpmnElement);
			}
			if (bpmnElement instanceof DataOutputObjectExtension) {
				this.getDataOutputObjects().remove(bpmnElement);
			}
			if (bpmnElement instanceof DataProcessingExtension) {
				this.getDataProcessing().remove(bpmnElement);
			}
			if (bpmnElement instanceof DataObjectAttributeExtension) {
				((DataObjectAttributeExtension) bpmnElement).getAttributesObject().remove(bpmnElementNode);
				try {
					rePosiningAttributes((DataObjectAttributeExtension) bpmnElement);
				} catch (BPMNMissingElementException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		} else if (bpmnElement instanceof BPMNElementEdge) {

			if (bpmnElement instanceof DataFlowExtension) {
				deleteDataFlow(id);
			} else if (bpmnElement instanceof DataReferenceExtension) {
				deleteReference(id);
			}
		}

	}

	private void rePosiningAttributes(DataObjectAttributeExtension bpmnElement) throws BPMNMissingElementException {
		int count = 0;
		BPMNElementNode data = bpmnElement.getDataParent();

		for (DataObjectAttributeExtension attribute : bpmnElement.getAttributesObject()) {
			double elementX = data.getBounds().getPosition().getX() + 25;
			double elementY = data.getBounds().getPosition().getY() + data.getBounds().getDimension().getHeight()
					+ DataObjectAttributeExtension.DEFAULT_HEIGHT * count;
			attribute.getBounds().setPosition(elementX, elementY);
			attribute.getBounds().setDimension(DataObjectAttributeExtension.DEFAULT_WIDTH,
					DataObjectAttributeExtension.DEFAULT_HEIGHT);
			count++;
		}

	}

	/**
	 * Helper method to deletes all SequenceFlows, Associations and MessageFlows
	 * from an element
	 * 
	 */
	private void removeAllEdgesFromElement(String elementId) {
		// TODO Auto-generated method stub
		// remove all SequenceFlows
		Set<DataFlowExtension> flowList = findDataFlowsByElementId(elementId);
		for (DataFlowExtension flow : flowList) {
			deleteDataFlow(flow.getId());
		}

		// remove all Associations
		Set<DataReferenceExtension> referencesList = findDataReferenceByElementId(elementId);
		for (DataReferenceExtension flow : referencesList) {
			deleteReference(flow.getId());
		}
	}

	/**
	 * Returns a list of dataFlows associated with a given FlowElement
	 * 
	 * @param id of a flowElement
	 * @return list of dataFlows
	 */
	private Set<DataFlowExtension> findDataFlowsByElementId(String id) {
		Set<DataFlowExtension> result = new LinkedHashSet<DataFlowExtension>();
		if (id == null || id.isEmpty()) {
			return result;
		}
		Set<DataFlowExtension> listA = this.getDataFlows();
		for (DataFlowExtension flow : listA) {
			if (id.equals(flow.getSourceRef()) || id.equals(flow.getTargetRef())) {
				result.add(flow);
			}
		}
		return result;
	}

	/**
	 * Returns a list of accociations associated with a given FlowElement
	 * 
	 * @param id of a flowElement
	 * @return list of Accociations
	 */
	private Set<DataReferenceExtension> findDataReferenceByElementId(String id) {
		Set<DataReferenceExtension> result = new LinkedHashSet<DataReferenceExtension>();
		if (id == null || id.isEmpty()) {
			return result;
		}
		Set<DataReferenceExtension> listA = this.getDataReferences();
		for (DataReferenceExtension flow : listA) {
			if (id.equals(flow.getSourceRef()) || id.equals(flow.getTargetRef())) {
				result.add(flow);
			}
		}
		return result;
	}

	/**
	 * Helper method to delete a BPMNEdge element from this context.
	 * <p>
	 * 
	 * @param id
	 */
	private void removeElementEdge(String id) {
		BPMNElementEdge bpmnEdge = (BPMNElementEdge) findElementById(id);
		if (bpmnEdge == null) {
			// does not exist
			return;
		}

		String targetRef = bpmnEdge.getTargetRef();
		String soureRef = bpmnEdge.getSourceRef();
		// In case of a SequenceFlow we need to update the referenced inside the
		// referred elements
		// <bpmn2:incoming>SequenceFlow_4</bpmn2:incoming>
		// <bpmn2:outgoing>SequenceFlow_5</bpmn2:outgoing>
		BPMNElementNode targetElement = (BPMNElementNode) findElementById(targetRef);
		if (targetElement != null) {
			NodeList childs = targetElement.getElementNode().getChildNodes();
			for (int j = 0; j < childs.getLength(); j++) {
				Node child = childs.item(j);
				if (child.getNodeType() == Node.ELEMENT_NODE
						&& (child.getNodeName().equals(getModel().getPrefix(BPMNNS.BPMN2) + ":incoming")
								|| child.getNodeName().equals(getModel().getPrefix(BPMNNS.BPMN2) + ":outgoing"))) {
					if (id.equals(child.getTextContent())) {
						targetElement.getElementNode().removeChild(child);
						break;
					}
				}
			}
		}
		BPMNElementNode sourceElement = (BPMNElementNode) findElementById(soureRef);
		if (sourceElement != null) {
			NodeList childs = sourceElement.getElementNode().getChildNodes();
			for (int j = 0; j < childs.getLength(); j++) {
				Node child = childs.item(j);
				if (child.getNodeType() == Node.ELEMENT_NODE
						&& (child.getNodeName().equals(getModel().getPrefix(BPMNNS.BPMN2) + ":incoming")
								|| child.getNodeName().equals(getModel().getPrefix(BPMNNS.BPMN2) + ":outgoing"))) {
					if (id.equals(child.getTextContent())) {
						sourceElement.getElementNode().removeChild(child);
						break;
					}
				}
			}
		}

		// Finally delete the flow element and the edge
		this.getElementNode().removeChild(bpmnEdge.getElementNode());
		if (bpmnEdge.getBpmnEdge() != null) {
			model.getBpmnPlane().removeChild(bpmnEdge.getBpmnEdge());
		}

	}

	/**
	 * Deletes a DataFlow from this context.
	 * <p>
	 * 
	 * @param id
	 */
	private void deleteDataFlow(String id) {

		BPMNElementEdge bpmnEdge = (BPMNElementEdge) findElementById(id);
		if (bpmnEdge != null && bpmnEdge instanceof DataFlowExtension) {
			removeElementEdge(id);
			this.getDataFlows().remove(bpmnEdge);
		}
	}

	/**
	 * Deletes a Reference from this context.
	 * <p>
	 * 
	 * @param id
	 */
	private void deleteReference(String id) {
		BPMNElementEdge bpmnEdge = (BPMNElementEdge) findElementById(id);
		if (bpmnEdge != null && bpmnEdge instanceof DataReferenceExtension) {
			removeElementEdge(id);
			this.getDataReferences().remove(bpmnEdge);
		}
	}

	/**
	 * get All node in the activity without attribute
	 * 
	 * @return BPMNElementNode
	 */
	public Set<BPMNElementNode> getAllNodes() {
		Set<BPMNElementNode> results = new LinkedHashSet<>();
		results.addAll(getDataInputObjects());
		results.addAll(getDataOutputObjects());
		results.addAll(getDataProcessing());
//		for (DataInputObjectExtension data : getDataInputObjects()) {
//			results.addAll(data.getDataAttributes());
//
//		}
//		for (DataOutputObjectExtension data : getDataOutputObjects()) {
//			results.addAll(data.getDataAttributes());
//
//		}
		return results;
	}

	/**
	 * get All node in the activity without attribute
	 * 
	 * @return BPMNElementNode
	 */
	public Set<BPMNElementNode> getNodesWithAttributes() {
		Set<BPMNElementNode> results = new LinkedHashSet<>();
		results.addAll(getDataInputObjects());
		results.addAll(getDataOutputObjects());
		results.addAll(getDataProcessing());
		for (DataInputObjectExtension data : getDataInputObjects()) {
			results.addAll(data.getDataAttributes());

		}
		for (DataOutputObjectExtension data : getDataOutputObjects()) {
			results.addAll(data.getDataAttributes());

		}
		return results;
	}

	@Override
	public double getDefaultHeight() {
		return DEFAULT_HEIGHT;
	}

	/**
	 * Remove any embedded bpmndi:BPMNLabel element within the bpmndi:BPMNShape
	 * 
	 * Positioning of the label is part of the client. Any position update should
	 * ignore these settings in Open-BPMN.
	 * 
	 */
	@Override
	public void setPosition(double x, double y) {
		super.setPosition(x, y);

		// remove optional BPMNLabel
		Element bpmnLabel = getModel().findChildNodeByName(this.bpmnShape, BPMNNS.BPMNDI, "BPMNLabel");
		if (bpmnLabel != null) {
			this.bpmnShape.removeChild(bpmnLabel);
		}
	}

	public boolean hasData() {
		if (this.dataInputObjects.size() > 0) {
			return true;
		}
		if (this.dataOutputObjects.size() > 0) {
			return true;
		}
		return false;
	}

	public boolean isHuman() {
		return this.dataInputObjects.stream().anyMatch(
				data -> data.getElementNode().getLocalName().equals(BPMNTypes.DATA_INPUT_OBJECT_ENVIRONMENT_DATA_USER)
						|| data.getElementNode().getLocalName().equals(BPMNTypes.DATA_INPUT_OBJECT_DEPENDENCY));
	}

	public boolean isUpdateData(String dataObjectId) {
		return 
//				this.dataReferences.stream()
//				.anyMatch((ref -> ref.getTargetRef().equals(dataObjectId) || ref.getSourceRef().equals(dataObjectId)))
//				&&
				!this.dataOutputObjects.stream().filter(dataObject -> dataObject.getId().equals(dataObjectId))
						.findFirst().get().getAttribute("state").toLowerCase().equals("init")
				&& !this.dataOutputObjects.stream().filter(dataObject -> dataObject.getId().equals(dataObjectId))
						.findFirst().get().getAttribute("state").toLowerCase().equals("delete")
				&& !this.dataOutputObjects.stream().filter(dataObject -> dataObject.getId().equals(dataObjectId))
						.findFirst().get().getAttribute("state").toLowerCase().equals("read");

	}

	public boolean isDeleteData(String dataObjectId) {
		return this.dataOutputObjects.stream().filter(dataObject -> dataObject.getId().equals(dataObjectId)).findFirst()
				.get().getAttribute("state").toLowerCase().equals("delete");

	}

	public boolean isReadData(String dataObjectId) {
		return this.dataOutputObjects.stream().filter(dataObject -> dataObject.getId().equals(dataObjectId)).findFirst()
				.get().getAttribute("state").toLowerCase().equals("read");

	}

	public List<DataFlowExtension> getConnectDataFlowTo(BPMNElement dataObject) {
		if (dataObject instanceof DataOutputObjectExtension) {
			return dataFlows.stream()
					.filter(dataflow -> dataflow.getTargetRef().equals(dataObject.getId())
							|| ((DataOutputObjectExtension) dataObject).getDataAttributes().stream()
									.anyMatch(att -> att.getId().equals(dataflow.getTargetRef())))
					.collect(Collectors.toList());

		}
		return new ArrayList<>();
	}

	public List<DataOutputObjectExtension> getAllDeleteObjectData() {

		return this.dataOutputObjects.stream()
				.filter(dataObject -> dataObject.getAttribute("state").toLowerCase().equals("delete")
						&& dataObject.getElementNode().getLocalName().equals(BPMNTypes.DATA_OUTPUT_OBJECT_DATA_STORE))
				.collect(Collectors.toList());
	}

	public BPMNElement getFistMultiObjectFor(BPMNElement dataObject) {
		if (dataObject instanceof DataOutputObjectExtension) {
			DataFlowExtension element = getConnectDataFlowTo(dataObject).stream().filter(data -> {
				return findElementById(data.getSourceRef()).getAttribute("isMultiple").equals("true");
			}).findAny().orElse(null);
			if (element != null) {
				return findElementById(element.getSourceRef());
			}
			return null;
		}
		return null;
	}

	public List<BPMNElement> getDataProcessingIncoming(String dataProcessingId) {
		List<BPMNElement> listIncomingElement = new ArrayList<>();

		DataProcessingExtension dataProcessing = (DataProcessingExtension) findElementById(dataProcessingId);

		dataProcessing.getIncomingDataFlows().stream().forEach(sequenceFlow -> {
			BPMNElement dataElementInc = findElementById(sequenceFlow.getSourceRef());
			if (dataElementInc.getElementNode().getLocalName().equals(BPMNTypes.DATA_PROCESSING)) {
				listIncomingElement.addAll(getDataProcessingIncoming(dataElementInc.getId()));
			} else {
				listIncomingElement.add(dataElementInc);
			}
		});

		return listIncomingElement;

	}

	public List<BPMNElement> getDataProcessingOutgoing(String dataProcessingId) {
		List<BPMNElement> listOutgingElement = new ArrayList<>();

		DataProcessingExtension dataProcessing = (DataProcessingExtension) findElementById(dataProcessingId);

		dataProcessing.getOutgoingDataFlows().stream().forEach(sequenceFlow -> {
			BPMNElement dataElementInc = findElementById(sequenceFlow.getTargetRef());
			if (dataElementInc.getElementNode().getLocalName().equals(BPMNTypes.DATA_PROCESSING)) {
				listOutgingElement.addAll(getDataProcessingOutgoing(dataElementInc.getId()));
			} else {
				listOutgingElement.add(dataElementInc);
			}
		});

		return listOutgingElement;

	}

	public BPMNElementNode getFirstMuliObject() {
		List<DataOutputObjectExtension> dataOutput = this.getDataOutputObjects().stream()
				.filter(data -> data.getElementNode().getLocalName().equals(BPMNTypes.DATA_OUTPUT_OBJECT_DATA_STORE))
				.collect(Collectors.toList());

		for (DataOutputObjectExtension dataObj : dataOutput) {
			if (this.getBpmnProcess().getActivities().stream().anyMatch(activity -> activity.getDataOutputObjects()
					.stream()
					.anyMatch(data -> (data.getElementNode().getLocalName()
							.equals(BPMNTypes.DATA_OUTPUT_OBJECT_DATA_STORE)
							&& data.getElementNode().getAttribute("isMultiple").contentEquals("true") && dataObj
									.getElementNode().getAttribute("name").contentEquals(data.getAttribute("name"))))))
				return dataObj;
		}

		return null;
	}

	public boolean checkObjectIsMulti(BPMNElement bpmnElement) {

		if (this.getBpmnProcess().getActivities().stream().anyMatch(activity -> activity.getDataOutputObjects().stream()
				.anyMatch(data -> (data.getElementNode().getLocalName().equals(BPMNTypes.DATA_OUTPUT_OBJECT_DATA_STORE)
						&& data.getElementNode().getAttribute("isMultiple").contentEquals("true")
						&& bpmnElement.getAttribute("name").contentEquals(data.getAttribute("name"))))))
			return true;

		return false;
	}
}
