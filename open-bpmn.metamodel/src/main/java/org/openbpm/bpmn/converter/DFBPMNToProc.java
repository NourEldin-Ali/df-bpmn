package org.openbpm.bpmn.converter;


import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.openbpmn.bpmn.BPMNModel;
import org.openbpmn.bpmn.BPMNTypes;
import org.openbpmn.bpmn.elements.Activity;
import org.openbpmn.bpmn.elements.BPMNProcess;
import org.openbpmn.bpmn.elements.DataInputObjectExtension;
import org.openbpmn.bpmn.elements.DataObjectAttributeExtension;
import org.openbpmn.bpmn.elements.Event;
import org.openbpmn.bpmn.elements.Gateway;
import org.openbpmn.bpmn.elements.SequenceFlow;
import org.openbpmn.bpmn.elements.core.BPMNElement;
import org.openbpmn.bpmn.elements.core.BPMNElementNode;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

//import javafx.util.Pair;

public class DFBPMNToProc {
	BPMNModel model;

	enum ActivityType {
		SERVICE, HUMAN
	}

	enum EventType {
		START, END
	}

	enum GatewayType {
		INCLUSIVE, AND, XOR
	}

	private static Logger logger = Logger.getLogger(DFBPMNToProc.class.getName());

	private String outputName;
	private String projectPath;
	private final String PROCESS = "process";
	private final String DIAGRAM = "diagram";

	public DFBPMNToProc(BPMNModel model, String outputName, String projectPath) {
		this.model = model;
		this.outputName = outputName;
		this.projectPath = projectPath;
	}

	public File createDiagrame() {
		try {

			File file = new File("src/main/resources/initDiagram.proc");
			// an instance of factory that gives a document builder
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			// an instance of builder to parse the specified xml file
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(file);
			doc.getDocumentElement().normalize();
//			logger.info("get main process");
			Node mainProcess = doc.getElementsByTagName("process:MainProcess").item(0);

			mainProcess.getAttributes().getNamedItem("name").setNodeValue(outputName);
//			logger.info("get notation diagram");
			Node mainDiagram = doc.getElementsByTagName("notation:Diagram").item(0);

			// read data from bpmn file

			if (model.getParticipants().size() == 0) {
				BPMNProcess openProcess = model.openDefaultProces();
				if (openProcess.getAllElementNodes().size() != 0) {
					Map<String, Element> pool = addPoolToProc(doc, mainProcess, mainDiagram, openProcess.getName(),
							"1320", "250");
					Map<String, String> processVariable = openProcess.getDataObjectsExtensions();
					addProcessVariableToProc(pool, processVariable, doc);
					Map<String, Map<String, String>> businessDataModel = openProcess.getDataStoresExtensions();
					addBusinessDataToProc(pool, businessDataModel, doc);
					Element actor = addActor(pool, doc);
//						Map<String, Element> lane = addLaneToProc(pool, doc, "Default Lane", "1320", "250",
//								actor.getAttribute("xmi:id"));
//						System.out.println(openProcess.getActivities().size());
					addElements(pool, false, null, doc, openProcess.getActivities(), openProcess.getEvents(),
							openProcess.getGateways());
				}
			} else {
				model.getParticipants().stream().forEach(participant -> {
					BPMNProcess openProcess;
					try {
						openProcess = model.openProcess(participant.getProcessRef());

//					System.out.println(openProcess.getActivities().size());
						if (openProcess.isPublicProcess()) {
							if (openProcess.getAllElementNodes().size() != 0) {
								Map<String, Element> pool = addPoolToProc(doc, mainProcess, mainDiagram,
										openProcess.getName(), "1320", "250");
								Map<String, String> processVariable = openProcess.getDataObjectsExtensions();
								addProcessVariableToProc(pool, processVariable, doc);
								Map<String, Map<String, String>> businessDataModel = openProcess
										.getDataStoresExtensions();
								addBusinessDataToProc(pool, businessDataModel, doc);
								Element actor = addActor(pool, doc);
//							Map<String, Element> lane = addLaneToProc(pool, doc, "Default Lane", "1320", "250",
//									actor.getAttribute("xmi:id"));
//							System.out.println(openProcess.getActivities().size());
								addElements(pool, false, null, doc, openProcess.getActivities(),
										openProcess.getEvents(), openProcess.getGateways());
							}
						} else {
							if (openProcess.getAllElementNodes().size() > 0) {
//				
								Map<String, Element> pool = addPoolToProc(doc, mainProcess, mainDiagram,
										participant.getName(),
										String.valueOf(getAttributeBoundsValue(participant, "width")),
										String.valueOf(getAttributeBoundsValue(participant, "height")));
								Element actor = addActor(pool, doc);
								Map<String, String> processVariable = openProcess.getDataObjectsExtensions();
								addProcessVariableToProc(pool, processVariable, doc);
								Map<String, Map<String, String>> businessDataModel = openProcess
										.getDataStoresExtensions();
								addBusinessDataToProc(pool, businessDataModel, doc);
								if (openProcess.getLanes().size() == 0) {
									try {

//									Map<String, Element> lane = addLaneToProc(pool, doc, "Default Lane",
//											String.valueOf(getAttributeBoundsValue(participant, "width")),
//											String.valueOf(getAttributeBoundsValue(participant, "height")),
//											actor.getAttribute("xmi:id"));
										addElements(pool, false, participant, doc, openProcess.getActivities(),
												openProcess.getEvents(), openProcess.getGateways());
									} catch (XPathExpressionException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
									addSquenceFlowFromBPMN(pool, mainDiagram, doc, openProcess.getSequenceFlows());

								} else {
//								System.out.println(openProcess.getAllElementNodes().size());
									openProcess.getLanes().stream().forEach(laneBpmn -> {

										try {

											int widthParserLane = getAttributeBoundsValue(participant, "width")
													/ openProcess.getLanes().size();

											Map<String, Element> lane = addLaneToProc(pool, doc, laneBpmn.getName(),
													String.valueOf(widthParserLane),
													String.valueOf(String
															.valueOf(getAttributeBoundsValue(laneBpmn, "height"))),
													actor.getAttribute("xmi:id"));

											Set<Activity> activityList = new HashSet<Activity>();
											openProcess.getActivities().stream().forEach(activity -> {
												if (laneBpmn.contains(activity)) {
													activityList.add(activity);
												}
											});

											Set<Event> eventsList = new HashSet<Event>();
											openProcess.getEvents().stream().forEach(event -> {
												if (laneBpmn.contains(event)) {
													eventsList.add(event);
												}
											});

											Set<Gateway> gatwayList = new HashSet<Gateway>();
											openProcess.getGateways().stream().forEach(gateway -> {
												if (laneBpmn.contains(gateway)) {
													gatwayList.add(gateway);
												}
											});
											addElements(lane, true, laneBpmn, doc, activityList, eventsList,
													gatwayList);
//										addSequenceFlowToProc(mainProcess,diagram,)
										} catch (XPathExpressionException e) {
											e.printStackTrace();
										}
									});
								}
								addSquenceFlowFromBPMN(pool, mainDiagram, doc, openProcess.getSequenceFlows());
							}
						}
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				});
			}
			File procFile = createProcFile(doc);
			if (procFile == null) {
				logger.info("Generate failed to Bonita Proc");
			}
			return procFile;
		} catch (Exception e) {
			logger.info("Generate failed to Bonita Proc");
			System.out.println(e.getMessage());
			return null;
		}
	}

	void addProcessVariableToProc(Map<String, Element> pool, Map<String, String> processVariable, Document doc)
			throws XPathExpressionException {

		Map<String, String> types = new HashMap<>();

		// Create an XPath instance
		XPathFactory xPathFactory = XPathFactory.newInstance();
		XPath xPath = xPathFactory.newXPath();
		// Define the XPath expression to find the <children> element with type="7001"
		String xpathExpression = "//datatypes";

		// Evaluate the XPath expression and get the matching node
		NodeList listNode = (NodeList) xPath.evaluate(xpathExpression, doc, XPathConstants.NODESET);
		if (listNode != null) {
			for (int i = 0; i < listNode.getLength(); i++) {
				types.put(listNode.item(i).getAttributes().getNamedItem("name").getNodeValue().toLowerCase().trim(),
						listNode.item(i).getAttributes().getNamedItem("xmi:id").getNodeValue());
			}
		}
//		System.out.println(types);
		processVariable.forEach((dataName, dataType) -> {
			Element data = doc.createElement("data");
			data.setAttribute("xmi:type", "process:Data");
			data.setAttribute("xmi:id", generateXmiId());
			data.setAttribute("name", dataName);
			if (types.keySet().contains(dataType.toLowerCase().trim()))
				data.setAttribute("dataType", types.get(dataType.toLowerCase().trim()));
			else
				data.setAttribute("dataType", types.get("text"));
			pool.get(PROCESS).appendChild(data);

			Element defaultValue = doc.createElement("defaultValue");
			defaultValue.setAttribute("xmi:type", "expression:Expression");
			defaultValue.setAttribute("xmi:id", generateXmiId());
			defaultValue.setAttribute("content", "");
			data.appendChild(defaultValue);
		});
	}

	void addBusinessDataToProc(Map<String, Element> pool, Map<String, Map<String, String>> businessDataModel,
			Document doc) throws XPathExpressionException {

		Map<String, String> types = new HashMap<>();

		// Create an XPath instance
		XPathFactory xPathFactory = XPathFactory.newInstance();
		XPath xPath = xPathFactory.newXPath();
		// Define the XPath expression to find the <children> element with type="7001"
		String xpathExpression = "//datatypes";

		// Evaluate the XPath expression and get the matching node
		NodeList listNode = (NodeList) xPath.evaluate(xpathExpression, doc, XPathConstants.NODESET);
		if (listNode != null) {
			for (int i = 0; i < listNode.getLength(); i++) {
				types.put(listNode.item(i).getAttributes().getNamedItem("name").getNodeValue().toLowerCase().trim(),
						listNode.item(i).getAttributes().getNamedItem("xmi:id").getNodeValue());
			}
		}
//		System.out.println(types);
		businessDataModel.forEach((dataName, dataElement) -> {
			Element data = doc.createElement("data");
			data.setAttribute("xmi:type", "process:BusinessObjectData");
			data.setAttribute("xmi:id", generateXmiId());
			data.setAttribute("name", dataName);
			data.setAttribute("dataType", types.get("business_object"));
			data.setAttribute("className", dataElement.get("type"));
			if (dataElement.get("multiple").equals("true")) {
				data.setAttribute("multiple", "true");
			}
			pool.get(PROCESS).appendChild(data);

			Element defaultValue = doc.createElement("defaultValue");
			defaultValue.setAttribute("xmi:type", "expression:Expression");
			defaultValue.setAttribute("xmi:id", generateXmiId());
			defaultValue.setAttribute("content", "");
			data.appendChild(defaultValue);
		});
	}

	Element addActor(Map<String, Element> pool, Document doc) {
		// Create the root element <elements>
		Element actor = doc.createElement("actors");
		actor.setAttribute("xmi:type", "process:Actor");
		actor.setAttribute("xmi:id", generateXmiId());
		actor.setAttribute("name", "Default actor");
		actor.setAttribute("initiator", "true");
		pool.get(PROCESS).appendChild(actor);
		return actor;
	}

	void addElements(Map<String, Element> lane, boolean isLane, BPMNElementNode laneBpmn, Document doc,
			Set<Activity> activityList, Set<Event> eventList, Set<Gateway> gatewayList)
			throws XPathExpressionException {
		addActivityFromBPMN(lane, laneBpmn, doc, activityList, isLane);
		addEventFromBPMN(lane, laneBpmn, doc, eventList, isLane);
		addGatewayFromBPMN(lane, laneBpmn, doc, gatewayList, isLane);
	}

	private int getAttributeBoundsValue(BPMNElementNode elementNode, String attribute) {
		String value = elementNode.getBpmnShape().getFirstChild().getAttributes().getNamedItem(attribute)
				.getNodeValue();
		int valueParser = (int) Float.parseFloat(value);
		return valueParser;
	}

	private void addEventFromBPMN(Map<String, Element> parentElement, BPMNElementNode laneBmn, Document doc,
			Set<Event> events, boolean isLane) throws XPathExpressionException {
		events.stream().forEach(event -> {
			try {
//				System.out.println(event.getType());
				if (event.getType().equals("startEvent")) {
					addEventToProc(parentElement, doc, event,
							String.valueOf(getAttributeBoundsValue(event, "x")
									- (laneBmn != null ? getAttributeBoundsValue(laneBmn, "x") : 0)),
							String.valueOf(getAttributeBoundsValue(event, "y")
									- (laneBmn != null ? getAttributeBoundsValue(laneBmn, "y") : 0)),
							EventType.START, isLane);
				} else if (event.getType().equals("endEvent")) {
					addEventToProc(parentElement, doc, event,
							String.valueOf(getAttributeBoundsValue(event, "x")
									- (laneBmn != null ? getAttributeBoundsValue(laneBmn, "x") : 0)),
							String.valueOf(getAttributeBoundsValue(event, "y")
									- (laneBmn != null ? getAttributeBoundsValue(laneBmn, "y") : 0)),
							EventType.END, isLane);
				}

			} catch (XPathExpressionException e) {
			}
		});
	}

	private void addActivityFromBPMN(Map<String, Element> parentElement, BPMNElementNode laneBmn, Document doc,
			Set<Activity> activities, boolean isLane) throws XPathExpressionException {
		activities.stream().forEach(activity -> {
			try {
				if (activity.isHuman()) {
					addActivityToProc(parentElement, doc, activity,
							String.valueOf(getAttributeBoundsValue(activity, "x")
									- (laneBmn != null ? getAttributeBoundsValue(laneBmn, "x") : 0)),
							String.valueOf(getAttributeBoundsValue(activity, "y")
									- (laneBmn != null ? getAttributeBoundsValue(laneBmn, "y") : 0)),
							ActivityType.HUMAN, isLane);
				} else {

					addActivityToProc(parentElement, doc, activity,
							String.valueOf(getAttributeBoundsValue(activity, "x")
									- (laneBmn != null ? getAttributeBoundsValue(laneBmn, "x") : 0)),
							String.valueOf(getAttributeBoundsValue(activity, "y")
									- (laneBmn != null ? getAttributeBoundsValue(laneBmn, "y") : 0)),
							ActivityType.SERVICE, isLane);
				}
			} catch (XPathExpressionException e) {
				// TODO Auto-generated catch block
//				e.printStackTrace();
			}
		});
	}

	private void addSquenceFlowFromBPMN(Map<String, Element> pool, Node diagram, Document doc,
			Set<SequenceFlow> sequenceFlowList) throws XPathExpressionException {
		sequenceFlowList.stream().forEach(sequenceFlow -> {
			try {
				addSequenceFlowToProc(pool, diagram, doc, sequenceFlow, "0", "0");
//					addActivityToProc(lane, doc, activity,
//							String.valueOf(getAttributeBoundsValue(activity, "x")
//									- (laneBmn != null ? getAttributeBoundsValue(laneBmn, "x") : 0)),
//							String.valueOf(getAttributeBoundsValue(activity, "y")
//									- (laneBmn != null ? getAttributeBoundsValue(laneBmn, "y") : 0)),
//							ActivityType.SERVICE);

			} catch (XPathExpressionException e) {
				// TODO Auto-generated catch block
//				e.printStackTrace();
			}
		});
	}

	private void addGatewayFromBPMN(Map<String, Element> parentElement, BPMNElementNode laneBmn, Document doc,
			Set<Gateway> gateways, boolean isLane) throws XPathExpressionException {
		gateways.stream().forEach(gateway -> {
			try {
//				
				GatewayType type = null;
				if (gateway.getType().equals("parallelGateway")) {
					type = GatewayType.AND;
				} else if (gateway.getType().equals("exclusiveGateway")) {
					type = GatewayType.XOR;
				} else if (gateway.getType().equals("inclusiveGateway")) {
					type = GatewayType.INCLUSIVE;
				}
				if (type != null)
					addGatewayToProc(parentElement, doc, gateway,
							String.valueOf(getAttributeBoundsValue(gateway, "x")
									- (laneBmn != null ? getAttributeBoundsValue(laneBmn, "x") : 0)),
							String.valueOf(getAttributeBoundsValue(gateway, "y")
									- (laneBmn != null ? getAttributeBoundsValue(laneBmn, "y") : 0)),
							type, isLane);

			} catch (XPathExpressionException e) {
			}
		});
	}

	public static String generateXmiId() {
		UUID uuid = UUID.randomUUID();
		return "_" + uuid.toString().replace("-", "");
	}

	private Map<String, Element> addPoolToProc(Document doc, Node mainProcess, Node diagram, String poolName,
			String width, String heigth) {
//		logger.info("add pool element");

		// Create the root element <elements>
		Element elementsPool = doc.createElement("elements");
		elementsPool.setAttribute("xmi:type", "process:Pool");
		elementsPool.setAttribute("xmi:id", generateXmiId());
		elementsPool.setAttribute("name", poolName);
		mainProcess.appendChild(elementsPool);

		// Create and append the <formMapping> element
		Element formMapping = doc.createElement("formMapping");
		formMapping.setAttribute("xmi:type", "process:FormMapping");
		formMapping.setAttribute("xmi:id", generateXmiId());
		formMapping.setAttribute("type", "NONE");
		elementsPool.appendChild(formMapping);

		// Create and append the <targetForm> element
		Element targetForm = doc.createElement("targetForm");
		targetForm.setAttribute("xmi:type", "expression:Expression");
		targetForm.setAttribute("xmi:id", generateXmiId());
		targetForm.setAttribute("name", "");
		targetForm.setAttribute("content", "");
		targetForm.setAttribute("type", "FORM_REFERENCE_TYPE");
		targetForm.setAttribute("returnTypeFixed", "true");
		formMapping.appendChild(targetForm);

		// Create and append the <overviewFormMapping> element
		Element overviewFormMapping = doc.createElement("overviewFormMapping");
		overviewFormMapping.setAttribute("xmi:type", "process:FormMapping");
		overviewFormMapping.setAttribute("xmi:id", generateXmiId());
		overviewFormMapping.setAttribute("type", "NONE");
		elementsPool.appendChild(overviewFormMapping);

		// Create and append the <targetForm> element for <overviewFormMapping>
		Element targetFormOverview = doc.createElement("targetForm");
		targetFormOverview.setAttribute("xmi:type", "expression:Expression");
		targetFormOverview.setAttribute("xmi:id", generateXmiId());
		targetFormOverview.setAttribute("name", "");
		targetFormOverview.setAttribute("content", "");
		targetFormOverview.setAttribute("type", "FORM_REFERENCE_TYPE");
		targetFormOverview.setAttribute("returnTypeFixed", "true");
		overviewFormMapping.appendChild(targetFormOverview);

		// Create and append the <contract> element
		Element contract = doc.createElement("contract");
		contract.setAttribute("xmi:type", "process:Contract");
		contract.setAttribute("xmi:id", generateXmiId());
		elementsPool.appendChild(contract);

		// Create and append multiple <searchIndexes> elements
		for (int i = 0; i < 5; i++) {
			Element searchIndexes = doc.createElement("searchIndexes");
			searchIndexes.setAttribute("xmi:type", "process:SearchIndex");
			searchIndexes.setAttribute("xmi:id", generateXmiId());
			elementsPool.appendChild(searchIndexes);

			Element name = doc.createElement("name");
			name.setAttribute("xmi:type", "expression:Expression");
			name.setAttribute("xmi:id", generateXmiId());
			name.setAttribute("content", "");
			name.setAttribute("returnTypeFixed", "true");
			searchIndexes.appendChild(name);

			Element value = doc.createElement("value");
			value.setAttribute("xmi:type", "expression:Expression");
			value.setAttribute("xmi:id", generateXmiId());
			value.setAttribute("content", "");
			value.setAttribute("returnTypeFixed", "true");
			searchIndexes.appendChild(value);
		}

		// Diagram pool
		// Create the root element <children>
		Element childrenPool = doc.createElement("children");
		childrenPool.setAttribute("xmi:type", "notation:Node");
		childrenPool.setAttribute("xmi:id", generateXmiId());
		childrenPool.setAttribute("type", "2007");
		childrenPool.setAttribute("element", elementsPool.getAttribute("xmi:id"));
		diagram.appendChild(childrenPool);

		// Create and append the <children> elements with different types
		String[] types = { "5008", "7001" };
		for (String type : types) {
			Element child = doc.createElement("children");
			child.setAttribute("xmi:type", "notation:DecorationNode");
			child.setAttribute("xmi:id", generateXmiId() + type);
			child.setAttribute("type", type);
			childrenPool.appendChild(child);
		}

		// Create and append the <styles> elements with different types
		String[] styleTypes = { "notation:DescriptionStyle", "notation:FontStyle", "notation:LineStyle",
				"notation:FillStyle" };
		for (String styleType : styleTypes) {
			Element style = doc.createElement("styles");
			style.setAttribute("xmi:type", styleType);
			style.setAttribute("xmi:id", generateXmiId());
			if (styleType.equals("notation:FontStyle")) {
				style.setAttribute("fontName", "Segoe UI");
			}
			childrenPool.appendChild(style);
		}

		// Create and append the <layoutConstraint> element
		Element layoutConstraint = doc.createElement("layoutConstraint");
		layoutConstraint.setAttribute("xmi:type", "notation:Bounds");
		layoutConstraint.setAttribute("xmi:id", generateXmiId());
		layoutConstraint.setAttribute("width", width);
		layoutConstraint.setAttribute("height", heigth);
		childrenPool.appendChild(layoutConstraint);
		HashMap<String, Element> pool = new HashMap<String, Element>();
		pool.put(PROCESS, elementsPool);
		pool.put(DIAGRAM, childrenPool);

		return pool;
	}

	private Map<String, Element> addLaneToProc(Map<String, Element> pool, Document doc, String laneName, String width,
			String height, String actorID) throws XPathExpressionException {
//		logger.info("add lane element");

		// add lane to pool (element)
		Element elementsLane = doc.createElement("elements");
		elementsLane.setAttribute("xmi:type", "process:Lane");
		elementsLane.setAttribute("xmi:id", generateXmiId());
		elementsLane.setAttribute("name", laneName);
		elementsLane.setAttribute("actor", actorID);
		pool.get(PROCESS).appendChild(elementsLane);

		// Diagram lane
		// Create the root element <children>
		Element childrenLane = doc.createElement("children");
		childrenLane.setAttribute("xmi:type", "notation:Node");
		childrenLane.setAttribute("xmi:id", generateXmiId());
		childrenLane.setAttribute("type", "3007");
		childrenLane.setAttribute("element", elementsLane.getAttribute("xmi:id"));

		// Create an XPath instance
		XPathFactory xPathFactory = XPathFactory.newInstance();
		XPath xPath = xPathFactory.newXPath();
		// Define the XPath expression to find the <children> element with type="7001"
		String xpathExpression = "//children[@element='" + pool.get(PROCESS).getAttribute("xmi:id")
				+ "']/children[@type='7001']";

		// Evaluate the XPath expression and get the matching node
		Node node = (Node) xPath.evaluate(xpathExpression, doc, XPathConstants.NODE);
		if (node != null) {
			Element diagram = (Element) node;
			diagram.appendChild(childrenLane);
		}

		// Create and append the <children> elements with different types
		String[] types = { "5007", "7002" };
		for (String type : types) {
			Element child = doc.createElement("children");
			child.setAttribute("xmi:type", "notation:DecorationNode");
			child.setAttribute("xmi:id", generateXmiId() + type);
			child.setAttribute("type", type);
			childrenLane.appendChild(child);
		}

		// Create and append the <styles> elements with different types
		String[] styleTypes = { "notation:DescriptionStyle", "notation:FontStyle", "notation:LineStyle",
				"notation:FillStyle" };
		for (String styleType : styleTypes) {
			Element style = doc.createElement("styles");
			style.setAttribute("xmi:type", styleType);
			style.setAttribute("xmi:id", generateXmiId());
			if (styleType.equals("notation:FontStyle")) {
				style.setAttribute("fontName", "Segoe UI");
			}
			childrenLane.appendChild(style);
		}

		// Create and append the <layoutConstraint> element
		Element layoutConstraint = doc.createElement("layoutConstraint");
		layoutConstraint.setAttribute("xmi:type", "notation:Bounds");
		layoutConstraint.setAttribute("xmi:id", generateXmiId());
		layoutConstraint.setAttribute("width", width);
		layoutConstraint.setAttribute("height", height);
		childrenLane.appendChild(layoutConstraint);

		HashMap<String, Element> lane = new HashMap<String, Element>();
		lane.put(PROCESS, elementsLane);
		lane.put(DIAGRAM, childrenLane);

		return lane;
	}

	private Element addActivityToProc(Map<String, Element> parentElement, Document doc, Activity activity, String x,
			String y, ActivityType type, boolean isLane) throws XPathExpressionException {
//		logger.info("add activity element");

		// Create the <elements> element
		Element elementsActivity = doc.createElement("elements");
		if (type == ActivityType.HUMAN)
			elementsActivity.setAttribute("xmi:type", "process:Task");
		else
			elementsActivity.setAttribute("xmi:type", "process:ServiceTask");
		elementsActivity.setAttribute("xmi:id", activity.getId());
		elementsActivity.setAttribute("name", activity.getName());
		if (type == ActivityType.HUMAN)
			elementsActivity.setAttribute("overrideActorsOfTheLane", "false");

		parentElement.get(PROCESS).appendChild(elementsActivity);

		String[][] attributes = { { "dynamicLabel", "", "true" }, { "dynamicDescription", "", "true" },
				{ "stepSummary", "", "true" }, { "loopCondition", "java.lang.Boolean", "true" },
				{ "loopMaximum", "java.lang.Integer", "true" },
				{ "cardinalityExpression", "java.lang.Integer", "true" },
				{ "iteratorExpression", "java.lang.Object", "true" },
				{ "completionCondition", "java.lang.Boolean", "true" }, };

		for (String[] attr : attributes) {
			Element expressionElement = doc.createElement(attr[0]);
			expressionElement.setAttribute("xmi:type", "expression:Expression");
			expressionElement.setAttribute("xmi:id", generateXmiId());
			expressionElement.setAttribute("name", "");
			expressionElement.setAttribute("content", "");
			expressionElement.setAttribute("returnType", attr[1]);
			expressionElement.setAttribute("returnTypeFixed", attr[2]);
			elementsActivity.appendChild(expressionElement);
		}

		if (type == ActivityType.HUMAN) {
			Element formMappingElement = doc.createElement("formMapping");
			formMappingElement.setAttribute("xmi:type", "process:FormMapping");
			formMappingElement.setAttribute("xmi:id", generateXmiId());
			elementsActivity.appendChild(formMappingElement);

			Element targetFormElement = doc.createElement("targetForm");
			targetFormElement.setAttribute("xmi:type", "expression:Expression");
			targetFormElement.setAttribute("xmi:id", generateXmiId());
			targetFormElement.setAttribute("name", "");
			targetFormElement.setAttribute("content", "");
			targetFormElement.setAttribute("type", "FORM_REFERENCE_TYPE");
			targetFormElement.setAttribute("returnTypeFixed", "true");
			formMappingElement.appendChild(targetFormElement);

			// <contract>
			Element contractElement = doc.createElement("contract");
			contractElement.setAttribute("xmi:type", "process:Contract");
			contractElement.setAttribute("xmi:id", generateXmiId());
			elementsActivity.appendChild(contractElement);

			List<DataInputObjectExtension> dataList = activity.getDataInputObjects().stream()
					.filter(data -> data.getElementNode().getLocalName()
							.equals(BPMNTypes.DATA_INPUT_OBJECT_ENVIRONMENT_DATA_USER)
							|| data.getElementNode().getLocalName().equals(BPMNTypes.DATA_INPUT_OBJECT_DEPENDENCY))
					.collect(Collectors.toList());
			Set<String> dataType = Set.of("text", "boolean", "decimal", "integer", "date");

			dataList.stream().forEach(objectData -> {
				// Create root element

				Element dataElement = doc.createElement("inputs");
				dataElement.setAttribute("xmi:type", "process:ContractInput");
				dataElement.setAttribute("xmi:id", generateXmiId());
				dataElement.setAttribute("name", objectData.getName());
				if (objectData.getAttribute("isMultiple").equals("true"))
					dataElement.setAttribute("multiple", "true");

				contractElement.appendChild(dataElement);

				// Create mapping element
				Element mapping = doc.createElement("mapping");
				mapping.setAttribute("xmi:type", "process:ContractInputMapping");
				mapping.setAttribute("xmi:id", generateXmiId());
				dataElement.appendChild(mapping);

				if (objectData.getDataAttributes().size() > 0) {
					dataElement.setAttribute("type", "COMPLEX");
					objectData.getDataAttributes().stream().forEach(dataAttribute -> {

						Element elementAttribute = doc.createElement("inputs");
						elementAttribute.setAttribute("xmi:type", "process:ContractInput");
						elementAttribute.setAttribute("xmi:id", generateXmiId());
						elementAttribute.setAttribute("name", dataAttribute.getName());
						if (dataType.contains(dataAttribute.getAttribute("type").toLowerCase())) {
							elementAttribute.setAttribute("type", dataAttribute.getAttribute("type").toUpperCase());
						}
						dataElement.appendChild(elementAttribute);

					});
				} else {
					if (dataType.contains(objectData.getAttribute("type").toLowerCase())) {
						dataElement.setAttribute("type", objectData.getAttribute("type").toUpperCase());
					}
				}
			});
			// <expectedDuration>
			Element expectedDurationElement = doc.createElement("expectedDuration");
			expectedDurationElement.setAttribute("xmi:type", "expression:Expression");
			expectedDurationElement.setAttribute("xmi:id", generateXmiId());
			expectedDurationElement.setAttribute("name", "");
			expectedDurationElement.setAttribute("content", "");
			expectedDurationElement.setAttribute("returnType", "java.lang.Long");
			expectedDurationElement.setAttribute("returnTypeFixed", "true");
			elementsActivity.appendChild(expectedDurationElement);
		}

		addOperationForActivity(doc, activity, elementsActivity);

		if (type == ActivityType.HUMAN) {
			addDiagramForActivity(doc, parentElement, elementsActivity.getAttribute("xmi:id"), x, y, ActivityType.HUMAN,
					isLane);
		} else {
			addDiagramForActivity(doc, parentElement, elementsActivity.getAttribute("xmi:id"), x, y,
					ActivityType.SERVICE, isLane);
		}

		// add incoming and outgoing
		activity.getIngoingSequenceFlows().stream().forEach(incoming -> {
			elementsActivity.setAttribute("incoming",
					elementsActivity.getAttribute("incoming") + " " + incoming.getId());
		});
		activity.getOutgoingSequenceFlows().stream().forEach(outgoing -> {
			elementsActivity.setAttribute("outgoing",
					elementsActivity.getAttribute("outgoing") + " " + outgoing.getId());
		});

		return elementsActivity;
	}

	/**
	 * add operation for the activity
	 * 
	 * @param doc
	 * @param activity
	 * @param activityElement
	 * @throws XPathExpressionException
	 */
	void addOperationForActivity(Document doc, Activity activity, Element activityElement)
			throws XPathExpressionException {
		// get Data Types
		Map<String, String> dataTypes = new HashMap<>();

		// Create an XPath instance
		XPathFactory xPathFactory = XPathFactory.newInstance();
		XPath xPath = xPathFactory.newXPath();
		// Define the XPath expression to find the <children> element with type="7001"
		String xpathExpression = "//datatypes";

		// Evaluate the XPath expression and get the matching node
		NodeList listNode = (NodeList) xPath.evaluate(xpathExpression, doc, XPathConstants.NODESET);
		if (listNode != null) {
			for (int i = 0; i < listNode.getLength(); i++) {
				dataTypes.put(listNode.item(i).getAttributes().getNamedItem("name").getNodeValue().toLowerCase().trim(),
						listNode.item(i).getAttributes().getNamedItem("xmi:id").getNodeValue());
			}
		}

		dataTypes.put("string", dataTypes.get("text"));

		// inputTypes
		Map<String, String> inputDataTypes = new HashMap<>();
		inputDataTypes.put("string", "java.lang.String");
		inputDataTypes.put("text", "java.lang.String");
		inputDataTypes.put("boolean", "java.lang.Boolean");
		inputDataTypes.put("double", "java.lang.Double");
		inputDataTypes.put("float", "java.lang.Float");
		inputDataTypes.put("integer", "java.lang.Integer");
		inputDataTypes.put("long", "java.lang.Long");
		inputDataTypes.put("object", "java.lang.Object");
		inputDataTypes.put("date", "java.util.Date");
		inputDataTypes.put("list", "java.util.List");

		Set<String> addedData = new HashSet<>();

//		Set<Element> operationsSet = new HashSet<>();
//		List<Pair<String, Pair<Element, List<String>>>> operationsSet = new ArrayList<>();
		OperationSet operationsSet = new OperationSet(new ArrayList<>());

		// I supposed that there is only simple way
		activity.getDataFlows().stream().forEach(dataflow -> {
			BPMNElement targetElement = activity.findElementById(dataflow.getTargetRef());
			BPMNElement sourceElement = activity.findElementById(dataflow.getSourceRef());

			if (addedData.contains(targetElement.getId())) {
				return;
			}
			// root operation element
			Element operations = doc.createElement("operations");
			operations.setAttribute("xmi:type", "expression:Operation");
			operations.setAttribute("xmi:id", generateXmiId());

//			Pair<String, Pair<Element, List<String>>> operationOrder;
//			Pair<Element, List<String>> inOpertation = new Pair<Element, List<String>>(operations,
//					new ArrayList<String>());
			OperationOrder operationOrder;
			
			OperationElement inOperation = new OperationElement(operations, new ArrayList<>());


			// left operand element
			Element leftOperand = doc.createElement("leftOperand");
			leftOperand.setAttribute("xmi:type", "expression:Expression");
			leftOperand.setAttribute("xmi:id", generateXmiId());

			// business data object
			if (targetElement.getElementNode().getLocalName().equals(BPMNTypes.DATA_OBJECT_ATTRIBUTE) && targetElement
					.getElementNode().getParentNode().getLocalName().equals(BPMNTypes.DATA_OUTPUT_OBJECT_DATA_STORE)) {
				BPMNElement parentElement = ((DataObjectAttributeExtension) targetElement).getDataParent();

				if (addedData.contains(parentElement.getId())) {
					return;
				}
				// to order the operation
				operationOrder = new OperationOrder(parentElement.getName(), inOperation);
//				operationOrder = new Pair<>(parentElement.getName(), inOpertation);
				if (activity.dataHasReference(parentElement.getId())) {
					addedData.add(targetElement.getId());
					leftOperand.setAttribute("name", parentElement.getName()); // name
					leftOperand.setAttribute("content", parentElement.getName()); // name
					leftOperand.setAttribute("type", "TYPE_VARIABLE");
					leftOperand.setAttribute("returnType", parentElement.getAttribute("type"));// class name in BDM

				} else {// to create new instance of business data model
					addedData.add(parentElement.getId());
					leftOperand.setAttribute("name", parentElement.getName()); // name
					leftOperand.setAttribute("content", parentElement.getName()); // name
					leftOperand.setAttribute("type", "TYPE_VARIABLE");
					leftOperand.setAttribute("returnType", parentElement.getAttribute("type"));// class name in BDM
//					leftOperand.setAttribute("returnType", parentElement.getAttribute("type"));

					Element refElemenet = doc.createElement("referencedElements");
					refElemenet.setAttribute("xmi:id", generateXmiId());
					refElemenet.setAttribute("name", parentElement.getName()); // reference by unique name
					refElemenet.setAttribute("className", parentElement.getAttribute("type"));
					refElemenet.setAttribute("xmi:type", "process:BusinessObjectData");
					refElemenet.setAttribute("dataType", dataTypes.get("Business_Object".toLowerCase())); // dataTypes
					leftOperand.appendChild(refElemenet);

					Element rightOperand = doc.createElement("rightOperand");
					rightOperand.setAttribute("xmi:type", "expression:Expression");
					rightOperand.setAttribute("xmi:id", generateXmiId());
					rightOperand.setAttribute("name", "newScript()");
					rightOperand.setAttribute("interpreter", "GROOVY");
					rightOperand.setAttribute("type", "TYPE_READ_ONLY_SCRIPT");

					final StringBuilder script = new StringBuilder();

					if (parentElement.getAttribute("isMultiple").equals("true")) {
						rightOperand.setAttribute("returnType", inputDataTypes.get("list"));
						leftOperand.setAttribute("returnType", inputDataTypes.get("list"));
						BPMNElement bpmnMult = activity.getFistMultiObjectFor(parentElement);
						script.append("for(int i=0;i<");
						if (bpmnMult != null) {
							script.append(bpmnMult.getName());
						}
						script.append(".size();i++){\n");
					} else {
						rightOperand.setAttribute("returnType", parentElement.getAttribute("type"));
					}

					script.append("new " + parentElement.getAttribute("type") + "(\n");

					activity.getConnectDataFlowTo(parentElement).stream().forEach(df -> {

						BPMNElement srcElement = activity.findElementById(df.getSourceRef());
						BPMNElement trgtElement = activity.findElementById(df.getTargetRef());
						if (srcElement.getElementNode().getLocalName().equals(BPMNTypes.DATA_INPUT_OBJECT_LOCAL)) {
							script.append(trgtElement.getName() + ": '" + srcElement.getAttribute("value") + "'");
							script.append(",\n");
						} else if (srcElement.getElementNode().getLocalName()
								.equals(BPMNTypes.DATA_INPUT_OBJECT_PROCESS)
								|| srcElement.getElementNode().getParentNode().getLocalName()
										.equals(BPMNTypes.DATA_INPUT_OBJECT_PROCESS)) {
							Element refE = doc.createElement("referencedElements");
							refE.setAttribute("dataType",
									dataTypes.get(trgtElement.getAttribute("type").toLowerCase()));
							refE.setAttribute("xmi:id", generateXmiId());
							refE.setAttribute("xmi:type", "process:Data");
							refE.setAttribute("name", srcElement.getName());

							script.append(trgtElement.getName() + ": " + srcElement.getName() + "");
							script.append(",\n");

							// to add refere object
							operationOrder.getOperationElement().getStrings().add(srcElement.getName());
							rightOperand.appendChild(refE);
						} else if (srcElement.getElementNode().getLocalName()
								.equals(BPMNTypes.DATA_OUTPUT_OBJECT_DATA_STORE)
								|| srcElement.getElementNode().getLocalName()
										.equals(BPMNTypes.DATA_INPUT_OBJECT_DATA_STORE)) {

							Element refE = doc.createElement("referencedElements");
							refE.setAttribute("dataType", dataTypes.get("Business_Object".toLowerCase()));
							refE.setAttribute("xmi:id", generateXmiId());
							refE.setAttribute("xmi:type", "process:BusinessObjectData");
							refE.setAttribute("name", srcElement.getName());
							refE.setAttribute("className", trgtElement.getAttribute("type"));
							script.append(trgtElement.getName() + ": " + srcElement.getName() + "");
							if (srcElement.getAttribute("isMultiple").equals("true")) {
								script.append("[i]");
							}
							script.append(",\n");
							rightOperand.appendChild(refE);

							// to add refere object
							operationOrder.getOperationElement().getStrings().add(srcElement.getName());

						} else if ((srcElement.getElementNode().getLocalName().equals(BPMNTypes.DATA_OBJECT_ATTRIBUTE)
								&& srcElement.getElementNode().getParentNode().getLocalName()
										.equals(BPMNTypes.DATA_OUTPUT_OBJECT_DATA_STORE))
								|| (srcElement.getElementNode().getLocalName().equals(BPMNTypes.DATA_OBJECT_ATTRIBUTE)
										&& srcElement.getElementNode().getParentNode().getLocalName()
												.equals(BPMNTypes.DATA_INPUT_OBJECT_DATA_STORE))) {

							Element refE = doc.createElement("referencedElements");
							refE.setAttribute("dataType", dataTypes.get("Business_Object".toLowerCase()));
							refE.setAttribute("xmi:id", generateXmiId());
							refE.setAttribute("xmi:type", "process:BusinessObjectData");
							refE.setAttribute("name", srcElement.getElementNode().getParentNode().getAttributes()
									.getNamedItem("name").getNodeValue());
							refE.setAttribute("className", srcElement.getElementNode().getParentNode().getAttributes()
									.getNamedItem("type").getNodeValue());
							script.append(trgtElement.getName() + ": " + srcElement.getElementNode().getParentNode()
									.getAttributes().getNamedItem("name").getNodeValue() + "");

							if (srcElement.getAttribute("isMultiple").equals("true")) {
								script.append("[i]");
							}

							script.append("." + srcElement.getName().toLowerCase());
							script.append(",\n");
							rightOperand.appendChild(refE);

							// to add refere object
							operationOrder.getOperationElement().getStrings().add(srcElement.getElementNode().getParentNode()
									.getAttributes().getNamedItem("name").getNodeValue());

						} else {
							boolean userSource = (srcElement.getElementNode().getLocalName()
									.equals(BPMNTypes.DATA_INPUT_OBJECT_ENVIRONMENT_DATA_USER)
									|| srcElement.getElementNode().getLocalName()
											.equals(BPMNTypes.DATA_INPUT_OBJECT_DEPENDENCY));

							boolean parentUserSource = (srcElement.getElementNode().getParentNode().getLocalName()
									.equals(BPMNTypes.DATA_INPUT_OBJECT_ENVIRONMENT_DATA_USER)
									|| srcElement.getElementNode().getParentNode().getLocalName()
											.equals(BPMNTypes.DATA_INPUT_OBJECT_DEPENDENCY));

							// data from user (found a contract)
							if (userSource || (parentUserSource && srcElement.getElementNode().getLocalName()
									.equals(BPMNTypes.DATA_OBJECT_ATTRIBUTE))) {

								Element refE = doc.createElement("referencedElements");
								if (parentUserSource) {
									script.append(trgtElement.getAttribute("name") + ": "
											+ srcElement.getElementNode().getParentNode().getAttributes()
													.getNamedItem("name").getNodeValue()
											+ ".get(\"" + srcElement.getElementNode().getAttribute("name") + "\")");

									if (srcElement.getElementNode().getParentNode().getAttributes()
											.getNamedItem("isMultiple").getNodeValue().equals("true")) {
										script.append("[i]");
									}
									script.append(",\n");
									refE.setAttribute("type", "COMPLEX");
								} else {
									script.append(
											trgtElement.getAttribute("name") + ": " + srcElement.getAttribute("name"));
									if (srcElement.getElementNode().getAttribute("isMultiple").equals("true")) {
										script.append("[i]");
									}
									script.append(",\n");
								}

//								rightOperand.setAttribute("returnType",
//								inputDataTypes.get(targetElement.getAttribute("type").toLowerCase()));
//								leftOperand.setAttribute("returnType",
//								inputDataTypes.get(sourceElement.getAttribute("type").toLowerCase()));

								refE.setAttribute("xmi:id", generateXmiId());
								refE.setAttribute("xmi:type", "process:ContractInput");
								refE.setAttribute("name", srcElement.getName());

								rightOperand.appendChild(refE);

								Element mapping = doc.createElement("mapping");
								mapping.setAttribute("xmi:id", generateXmiId());
								mapping.setAttribute("xmi:type", "process:ContractInputMapping");
								refE.appendChild(mapping);

							}
						}

					});

					script.append(")");
					if (parentElement.getAttribute("isMultiple").equals("true")) {
						script.append("}");
					}
					rightOperand.setAttribute("content", script.toString());

					Element operator = doc.createElement("operator");
					operator.setAttribute("xmi:type", "expression:Operator");

					operator.setAttribute("xmi:id", generateXmiId());
					operator.setAttribute("type", "ASSIGNMENT"); // ASSIGNMENT or JAVA_METHOD

					operations.appendChild(leftOperand);
					operations.appendChild(rightOperand);
					operations.appendChild(operator);

					// check best place to insert
					orderOperation(operationsSet, operationOrder);
					return;
				}

				// process variable
			} else if (targetElement.getElementNode().getLocalName().equals(BPMNTypes.DATA_OUTPUT_OBJECT_PROCESS)
					|| targetElement.getElementNode().getParentNode().getLocalName()
							.equals(BPMNTypes.DATA_OUTPUT_OBJECT_PROCESS)) {
				leftOperand.setAttribute("name", targetElement.getName()); // name
				leftOperand.setAttribute("content", targetElement.getName()); // name
				leftOperand.setAttribute("type", "TYPE_VARIABLE");
				addedData.add(targetElement.getId());
				// to order the operation
//				operationOrder = new Pair<>(targetElement.getName(), inOperation);
				operationOrder = new OperationOrder(targetElement.getName(), inOperation);
			} else {
				return;
			}

			// reference element for left Operand
			Element refElemenet = doc.createElement("referencedElements");
			refElemenet.setAttribute("xmi:id", generateXmiId());
			refElemenet.setAttribute("name", targetElement.getName()); // reference by unique name

			// business data model
			if (targetElement.getElementNode().getLocalName().equals(BPMNTypes.DATA_OBJECT_ATTRIBUTE) && targetElement
					.getElementNode().getParentNode().getLocalName().equals(BPMNTypes.DATA_OUTPUT_OBJECT_DATA_STORE)) {
				BPMNElement parentElement = ((DataObjectAttributeExtension) targetElement).getDataParent();
				refElemenet.setAttribute("className", parentElement.getAttribute("type"));
				refElemenet.setAttribute("xmi:type", "process:BusinessObjectData");
				refElemenet.setAttribute("dataType", dataTypes.get("Business_Object".toLowerCase())); // dataTypes
				// process variable
			} else if (targetElement.getElementNode().getLocalName().equals(BPMNTypes.DATA_OUTPUT_OBJECT_PROCESS)
					|| targetElement.getElementNode().getParentNode().getLocalName()
							.equals(BPMNTypes.DATA_OUTPUT_OBJECT_PROCESS)) {

				refElemenet.setAttribute("xmi:type", "process:Data");
				refElemenet.setAttribute("dataType", dataTypes.get(targetElement.getAttribute("type").toLowerCase())); // dataTypes

			} else {
				return;
			}

			leftOperand.appendChild(refElemenet);

			// right operand

			// this is for the simple way if the text is fix not from a variable
			Element rightOperand = doc.createElement("rightOperand");
			rightOperand.setAttribute("xmi:type", "expression:Expression");
			rightOperand.setAttribute("xmi:id", generateXmiId());
			rightOperand.setAttribute("name", sourceElement.getName());
			rightOperand.setAttribute("content", sourceElement.getName());

			// static data
			if (sourceElement.getElementNode().getLocalName().equals(BPMNTypes.DATA_INPUT_OBJECT_LOCAL)) {
//				rightOperand.setAttribute("type", "TYPE_VARIABLE");
				rightOperand.setAttribute("name", sourceElement.getAttribute("value"));
				rightOperand.setAttribute("content", sourceElement.getAttribute("value"));
				// sourceElement.getElementNode().getLocalName().equals(BPMNTypes.DATA_OBJECT_ATTRIBUTE)
				// &&

			} else if (sourceElement.getElementNode().getLocalName().equals(BPMNTypes.DATA_INPUT_OBJECT_PROCESS)
					|| sourceElement.getElementNode().getParentNode().getLocalName()
							.equals(BPMNTypes.DATA_INPUT_OBJECT_PROCESS)) {
				rightOperand.setAttribute("type", "TYPE_VARIABLE");
				refElemenet = doc.createElement("referencedElements");
				refElemenet.setAttribute("dataType", dataTypes.get(sourceElement.getAttribute("type").toLowerCase()));
				refElemenet.setAttribute("xmi:id", generateXmiId());
				refElemenet.setAttribute("xmi:type", "process:Data");
				refElemenet.setAttribute("name", sourceElement.getName());

				rightOperand.appendChild(refElemenet);
				// add refereable object
				operationOrder.getOperationElement().getStrings().add(sourceElement.getName());

			} else {
				boolean userSource = (sourceElement.getElementNode().getLocalName()
						.equals(BPMNTypes.DATA_INPUT_OBJECT_ENVIRONMENT_DATA_USER)
						|| sourceElement.getElementNode().getLocalName()
								.equals(BPMNTypes.DATA_INPUT_OBJECT_DEPENDENCY));

				boolean parentUserSource = (sourceElement.getElementNode().getParentNode().getLocalName()
						.equals(BPMNTypes.DATA_INPUT_OBJECT_ENVIRONMENT_DATA_USER)
						|| sourceElement.getElementNode().getParentNode().getLocalName()
								.equals(BPMNTypes.DATA_INPUT_OBJECT_DEPENDENCY));

				// data from user (found a contract)
				if (userSource || (parentUserSource
						&& sourceElement.getElementNode().getLocalName().equals(BPMNTypes.DATA_OBJECT_ATTRIBUTE))) {

					refElemenet = doc.createElement("referencedElements");
					if (parentUserSource) {
						rightOperand.setAttribute("type", "TYPE_READ_ONLY_SCRIPT");
						rightOperand.setAttribute("interpreter", "GROOVY");
						rightOperand.setAttribute("name", "newScript()");
						rightOperand.setAttribute("content",
								sourceElement.getElementNode().getParentNode().getAttributes().getNamedItem("name")
										.getNodeValue() + ".get(\""
										+ sourceElement.getElementNode().getAttribute("name") + "\")");

						refElemenet.setAttribute("type", "COMPLEX");
					} else {
						rightOperand.setAttribute("type", "TYPE_CONTRACT_INPUT");
					}

//					rightOperand.setAttribute("returnType",
//					inputDataTypes.get(targetElement.getAttribute("type").toLowerCase()));
//					leftOperand.setAttribute("returnType",
//					inputDataTypes.get(sourceElement.getAttribute("type").toLowerCase()));

					refElemenet.setAttribute("xmi:id", generateXmiId());
					refElemenet.setAttribute("xmi:type", "process:ContractInput");
					refElemenet.setAttribute("name", targetElement.getName());

					rightOperand.appendChild(refElemenet);

					Element mapping = doc.createElement("mapping");
					mapping.setAttribute("xmi:id", generateXmiId());
					mapping.setAttribute("xmi:type", "process:ContractInputMapping");
					refElemenet.appendChild(mapping);

				}
			}

			// operator
			// this is for the simple way if the text is fix not from a variable
			Element operator = doc.createElement("operator");
			operator.setAttribute("xmi:type", "expression:Operator");
			operator.setAttribute("xmi:id", generateXmiId());

			// business data model
			if (targetElement.getElementNode().getLocalName().equals(BPMNTypes.DATA_OBJECT_ATTRIBUTE) && targetElement
					.getElementNode().getParentNode().getLocalName().equals(BPMNTypes.DATA_OUTPUT_OBJECT_DATA_STORE)) {
				operator.setAttribute("expression", "set" + targetElement.getName().substring(0, 1).toUpperCase()
						+ targetElement.getName().substring(1).toLowerCase()); // name
				operator.setAttribute("type", "JAVA_METHOD");

				Element inputTypes = doc.createElement("inputTypes");
				inputTypes.setTextContent(inputDataTypes.get(sourceElement.getAttribute("type").toLowerCase()));
				operator.appendChild(inputTypes);
				// process variable
			} else if (targetElement.getElementNode().getLocalName().equals(BPMNTypes.DATA_OUTPUT_OBJECT_PROCESS)
					|| targetElement.getElementNode().getParentNode().getLocalName()
							.equals(BPMNTypes.DATA_OUTPUT_OBJECT_PROCESS)) {
				operator.setAttribute("type", "ASSIGNMENT"); // ASSIGNMENT or JAVA_METHOD
			} else {
				return;
			}

			// add to activityElement
			operations.appendChild(leftOperand);
			operations.appendChild(rightOperand);
			operations.appendChild(operator);
			orderOperation(operationsSet, operationOrder);

		});

		operationsSet.getOperationOrders()
				.forEach(
				operation -> activityElement.appendChild(operation.getOperationElement().getElement())
				);

	}
	void orderOperation(OperationSet operationsSet, OperationOrder operationOrder) {
	    AtomicInteger index = new AtomicInteger(-1);
	    operationOrder.getOperationElement().getStrings().forEach(objectName -> {
	        List<OperationOrder> tempFilter = operationsSet.getOperationOrders().stream()
	                .filter(data -> data.getName().equals(objectName))
	                .collect(Collectors.toList());
	        if (!tempFilter.isEmpty()) {
	            int x = operationsSet.getOperationOrders().indexOf(tempFilter.get(0));
	            if (x > index.get()) {
	                index.set(x);
	            }
	        }
	    });
	    if (index.get() == -1) {
	        operationsSet.getOperationOrders().add(operationOrder);

	    } else {
	        operationsSet.getOperationOrders().add(index.get() + 1, operationOrder);
	    }
	}
//	void orderOperation(List<Pair<String, Pair<Element, List<String>>>> operationsSet,
//			Pair<String, Pair<Element, List<String>>> operationOrder) {
//		AtomicInteger index = new AtomicInteger(-1);
//		operationOrder.getValue().getValue().forEach(objectName -> {
//			List tempFilter = operationsSet.stream().filter(data -> data.getKey().equals(objectName))
//					.collect(Collectors.toList());
//			if (tempFilter.size() != 0) {
//				int x = operationsSet.indexOf(tempFilter.get(0));
//				if (x > index.get()) {
//					index.set(x);
//				}
//			}
//		});
//		if (index.get() == -1) {
//			operationsSet.add(operationOrder);
//
//		} else {
//			operationsSet.add(index.get() + 1, operationOrder);
//		}
//	}

	Element addDiagramForActivity(Document doc, Map<String, Element> parentElement, String elementID, String x,
			String y, ActivityType type, boolean isLane) throws XPathExpressionException {

		// Create the <children> element
		Element childrenElement = doc.createElement("children");
		childrenElement.setAttribute("xmi:type", "notation:Shape");
		childrenElement.setAttribute("xmi:id", generateXmiId());
		if (type == ActivityType.HUMAN) {
			childrenElement.setAttribute("type", "3005");
		} else {
			childrenElement.setAttribute("type", "3027");
		}

		childrenElement.setAttribute("element", elementID);
		childrenElement.setAttribute("fontName", "Segoe UI");
		childrenElement.setAttribute("fillColor", "14334392");
		childrenElement.setAttribute("lineColor", "10710316");

		// get lane child with type 7002/ pool 7001
		// Create an XPath instance
		XPathFactory xPathFactory = XPathFactory.newInstance();
		XPath xPath = xPathFactory.newXPath();
		// Define the XPath expression to find the <children> element with type="7002"
		String xpathExpression = "";

		if (isLane) {
			xpathExpression = "//children[@element='" + parentElement.get(PROCESS).getAttribute("xmi:id")
					+ "']/children[@type='7002']";
		} else {
			xpathExpression = "//children[@element='" + parentElement.get(PROCESS).getAttribute("xmi:id")
					+ "']/children[@type='7001']";
		}

		// Evaluate the XPath expression and get the matching node
		Node node = (Node) xPath.evaluate(xpathExpression, doc, XPathConstants.NODE);
		if (node != null) {
			Element diagram = (Element) node;
			diagram.appendChild(childrenElement);
		}

		// Create the inner <children> element
		Element innerChildrenElement = doc.createElement("children");
		innerChildrenElement.setAttribute("xmi:type", "notation:DecorationNode");
		innerChildrenElement.setAttribute("xmi:id", generateXmiId());
		if (type == ActivityType.HUMAN) {
			innerChildrenElement.setAttribute("type", "5005");
		} else {
			innerChildrenElement.setAttribute("type", "5017");
		}
		childrenElement.appendChild(innerChildrenElement);

		// Create the <layoutConstraint> element
		Element layoutConstraintElement = doc.createElement("layoutConstraint");
		layoutConstraintElement.setAttribute("xmi:type", "notation:Bounds");
		layoutConstraintElement.setAttribute("xmi:id", generateXmiId());
		layoutConstraintElement.setAttribute("x", x);
		layoutConstraintElement.setAttribute("y", y);
		childrenElement.appendChild(layoutConstraintElement);
		return childrenElement;
	}

	private Element addEventToProc(Map<String, Element> parentElement, Document doc, Event event, String x, String y,
			EventType type, boolean isLane) throws XPathExpressionException {

//		logger.info("add event element");

		// Create the <elements> element
		Element elementsEvent = doc.createElement("elements");

		if (type == EventType.START)
			elementsEvent.setAttribute("xmi:type", "process:StartEvent");
		else if (type == EventType.END)
			elementsEvent.setAttribute("xmi:type", "process:EndEvent");

		elementsEvent.setAttribute("xmi:id", event.getId());
		elementsEvent.setAttribute("name", event.getName());

		parentElement.get(PROCESS).appendChild(elementsEvent);

		// dynamicLabel element
		Element dynamicLabel = doc.createElement("dynamicLabel");
		elementsEvent.appendChild(dynamicLabel);

		// set attribute to dynamicLabel element
		dynamicLabel.setAttribute("xmi:type", "expression:Expression");
		dynamicLabel.setAttribute("xmi:id", generateXmiId());
		dynamicLabel.setAttribute("name", "");
		dynamicLabel.setAttribute("content", "");
		dynamicLabel.setAttribute("returnTypeFixed", "true");

		// dynamicDescription element
		Element dynamicDescription = doc.createElement("dynamicLabel");
		elementsEvent.appendChild(dynamicDescription);

		// set attribute to dynamicLabel element
		dynamicDescription.setAttribute("xmi:type", "expression:Expression");
		dynamicDescription.setAttribute("xmi:id", generateXmiId());
		dynamicDescription.setAttribute("name", "");
		dynamicDescription.setAttribute("content", "");
		dynamicDescription.setAttribute("returnTypeFixed", "true");

		// dynamicLabel element
		Element stepSummary = doc.createElement("dynamicLabel");
		elementsEvent.appendChild(stepSummary);

		// set attribute to dynamicLabel element
		stepSummary.setAttribute("xmi:type", "expression:Expression");
		stepSummary.setAttribute("xmi:id", generateXmiId());
		stepSummary.setAttribute("name", "");
		stepSummary.setAttribute("content", "");
		stepSummary.setAttribute("returnTypeFixed", "true");

		addDiagramForEvent(doc, parentElement, elementsEvent.getAttribute("xmi:id"), x, y, type, isLane);

		// add incoming and outgoing
		event.getIngoingSequenceFlows().stream().forEach(incoming -> {
			elementsEvent.setAttribute("incoming", elementsEvent.getAttribute("incoming") + " " + incoming.getId());
		});
		event.getOutgoingSequenceFlows().stream().forEach(outgoing -> {
			elementsEvent.setAttribute("outgoing", elementsEvent.getAttribute("outgoing") + " " + outgoing.getId());
		});

		return elementsEvent;
	}

	Element addDiagramForEvent(Document doc, Map<String, Element> parentElement, String elementID, String x, String y,
			EventType type, boolean isLane) throws XPathExpressionException {

		// Create the <children> element
		Element childrenElement = doc.createElement("children");
		childrenElement.setAttribute("xmi:type", "notation:Shape");
		childrenElement.setAttribute("xmi:id", generateXmiId());
		if (type == EventType.START)
			childrenElement.setAttribute("type", "3002");
		else if (type == EventType.END)
			childrenElement.setAttribute("type", "3003");
		childrenElement.setAttribute("element", elementID);
		childrenElement.setAttribute("fontName", "Segoe UI");

		// get lane child with type 7002/ pool 7001
		// Create an XPath instance
		XPathFactory xPathFactory = XPathFactory.newInstance();
		XPath xPath = xPathFactory.newXPath();
		// Define the XPath expression to find the <children> element with type="7002"
		String xpathExpression = "";

		if (isLane) {
			xpathExpression = "//children[@element='" + parentElement.get(PROCESS).getAttribute("xmi:id")
					+ "']/children[@type='7002']";
		} else {
			xpathExpression = "//children[@element='" + parentElement.get(PROCESS).getAttribute("xmi:id")
					+ "']/children[@type='7001']";
		}
		// Evaluate the XPath expression and get the matching node
		Node node = (Node) xPath.evaluate(xpathExpression, doc, XPathConstants.NODE);
		if (node != null) {
			Element diagram = (Element) node;
			diagram.appendChild(childrenElement);
		}

		// Child element
		Element child = doc.createElement("children");
		child.setAttribute("xmi:type", "notation:DecorationNode");
		child.setAttribute("xmi:id", generateXmiId());
		if (type == EventType.START)
			child.setAttribute("type", "5024");
		else if (type == EventType.END)
			child.setAttribute("type", "5025");
		childrenElement.appendChild(child);

		// Child's child element
		Element layoutConstraint = doc.createElement("layoutConstraint");
		layoutConstraint.setAttribute("xmi:type", "notation:Location");
		layoutConstraint.setAttribute("xmi:id", generateXmiId());
		layoutConstraint.setAttribute("y", "5");
		child.appendChild(layoutConstraint);

		// Root's child element
		Element layoutConstraintRoot = doc.createElement("layoutConstraint");
		layoutConstraintRoot.setAttribute("xmi:type", "notation:Bounds");
		layoutConstraintRoot.setAttribute("xmi:id", generateXmiId());
		layoutConstraintRoot.setAttribute("x", x);
		layoutConstraintRoot.setAttribute("y", y);
		childrenElement.appendChild(layoutConstraintRoot);

		return childrenElement;
	}

	private Element addSequenceFlowToProc(Map<String, Element> pool, Node diagram, Document doc,
			SequenceFlow sequenceFlow, String x, String y) throws XPathExpressionException {
//		logger.info("add sequence flow");

		// Create the <elements> element
		// Create root element
		Element sequenceFlowElement = doc.createElement("connections");
		sequenceFlowElement.setAttribute("xmi:type", "process:SequenceFlow");
		sequenceFlowElement.setAttribute("xmi:id", sequenceFlow.getId());
		sequenceFlowElement.setAttribute("target", sequenceFlow.getTargetRef());
		sequenceFlowElement.setAttribute("source", sequenceFlow.getSourceRef());
		pool.get(PROCESS).appendChild(sequenceFlowElement);

		// Create decisionTable element
		Element decisionTable = doc.createElement("decisionTable");
		decisionTable.setAttribute("xmi:type", "decision:DecisionTable");
		decisionTable.setAttribute("xmi:id", generateXmiId());
		sequenceFlowElement.appendChild(decisionTable);

		// Create condition element
		Element condition = doc.createElement("condition");
		condition.setAttribute("xmi:type", "expression:Expression");
		condition.setAttribute("xmi:id", generateXmiId());
		condition.setAttribute("name", "");
		condition.setAttribute("returnType", "java.lang.Boolean");
		condition.setAttribute("returnTypeFixed", "true");
		sequenceFlowElement.appendChild(condition);

		addDiagramForSequenceFlow(doc, diagram, sequenceFlow, x, y);
		return sequenceFlowElement;
	}

	Element addDiagramForSequenceFlow(Document doc, Node diagram, SequenceFlow squenceFlow, String x, String y)
			throws XPathExpressionException {

		// get source node
		// Create an XPath instance
		XPathFactory xPathFactory = XPathFactory.newInstance();
		XPath xPath = xPathFactory.newXPath();
		// Define the XPath expression to find the <children> element
		String xpathExpression = "//children[@element='" + squenceFlow.getSourceRef() + "' and @type='notation:Shape']";
		// Evaluate the XPath expression and get the matching node
		Node source = (Node) xPath.evaluate(xpathExpression, doc, XPathConstants.NODE);

		// get target node
		xPathFactory = XPathFactory.newInstance();
		xPath = xPathFactory.newXPath();
		// Define the XPath expression to find the <children> element
		xpathExpression = "//children[@element='" + squenceFlow.getTargetRef() + "' and @type='notation:Shape']";
		// Evaluate the XPath expression and get the matching node
		Node target = (Node) xPath.evaluate(xpathExpression, doc, XPathConstants.NODE);

		/// Create root element
		Element childrenElement = doc.createElement("edges");
		childrenElement.setAttribute("xmi:type", "notation:Connector");
		childrenElement.setAttribute("xmi:id", generateXmiId());
		childrenElement.setAttribute("type", "4001");
		childrenElement.setAttribute("element", squenceFlow.getId());
		childrenElement.setAttribute("source", source.getAttributes().getNamedItem("xmi:id").getNodeValue());
		childrenElement.setAttribute("target", target.getAttributes().getNamedItem("xmi:id").getNodeValue());
		childrenElement.setAttribute("roundedBendpointsRadius", "10");
		childrenElement.setAttribute("routing", "Rectilinear");
		diagram.appendChild(childrenElement);

		// Create children element
		Element children = doc.createElement("children");
		children.setAttribute("xmi:type", "notation:DecorationNode");
		children.setAttribute("xmi:id", generateXmiId());
		children.setAttribute("type", "6001");
		children.setAttribute("element", squenceFlow.getId());
		childrenElement.appendChild(children);

		// Create layoutConstraint element
		Element layoutConstraint = doc.createElement("layoutConstraint");
		layoutConstraint.setAttribute("xmi:type", "notation:Location");
		layoutConstraint.setAttribute("xmi:id", generateXmiId());
		layoutConstraint.setAttribute("y", "-10");
		children.appendChild(layoutConstraint);

		// Create styles element
		Element styles = doc.createElement("styles");
		styles.setAttribute("xmi:type", "notation:FontStyle");
		styles.setAttribute("xmi:id", generateXmiId());
		styles.setAttribute("fontName", "Segoe UI");
		childrenElement.appendChild(styles);

		// Create bendpoints element
		Element bendpoints = doc.createElement("bendpoints");
		bendpoints.setAttribute("xmi:type", "notation:RelativeBendpoints");
		bendpoints.setAttribute("xmi:id", generateXmiId());
		bendpoints.setAttribute("points", "[0, 0, 0, 0]$[0, 0, 0, 0]");
		childrenElement.appendChild(bendpoints);

		return childrenElement;
	}

	private Element addGatewayToProc(Map<String, Element> parentElement, Document doc, Gateway gateway, String x,
			String y, GatewayType type, boolean isLane) throws XPathExpressionException {

//		logger.info("add gateway element");

		// Create the <elements> element
		Element elementsEvent = doc.createElement("elements");

		if (type == GatewayType.AND)
			elementsEvent.setAttribute("xmi:type", "process:ANDGateway");
		else if (type == GatewayType.XOR)
			elementsEvent.setAttribute("xmi:type", "process:XORGateway");
		else if (type == GatewayType.INCLUSIVE)
			elementsEvent.setAttribute("xmi:type", "process:InclusiveGateway");

		elementsEvent.setAttribute("xmi:id", gateway.getId());
		elementsEvent.setAttribute("name", gateway.getName());

		parentElement.get(PROCESS).appendChild(elementsEvent);

		// dynamicLabel element
		Element dynamicLabel = doc.createElement("dynamicLabel");
		elementsEvent.appendChild(dynamicLabel);

		// set attribute to dynamicLabel element
		dynamicLabel.setAttribute("xmi:type", "expression:Expression");
		dynamicLabel.setAttribute("xmi:id", generateXmiId());
		dynamicLabel.setAttribute("name", "");
		dynamicLabel.setAttribute("content", "");
		dynamicLabel.setAttribute("returnTypeFixed", "true");

		// dynamicDescription element
		Element dynamicDescription = doc.createElement("dynamicLabel");
		elementsEvent.appendChild(dynamicDescription);

		// set attribute to dynamicLabel element
		dynamicDescription.setAttribute("xmi:type", "expression:Expression");
		dynamicDescription.setAttribute("xmi:id", generateXmiId());
		dynamicDescription.setAttribute("name", "");
		dynamicDescription.setAttribute("content", "");
		dynamicDescription.setAttribute("returnTypeFixed", "true");

		// dynamicLabel element
		Element stepSummary = doc.createElement("dynamicLabel");
		elementsEvent.appendChild(stepSummary);

		// set attribute to dynamicLabel element
		stepSummary.setAttribute("xmi:type", "expression:Expression");
		stepSummary.setAttribute("xmi:id", generateXmiId());
		stepSummary.setAttribute("name", "");
		stepSummary.setAttribute("content", "");
		stepSummary.setAttribute("returnTypeFixed", "true");

		addDiagramForGateway(doc, parentElement, elementsEvent.getAttribute("xmi:id"), x, y, type, isLane);

		// add incoming and outgoing
		gateway.getIngoingSequenceFlows().stream().forEach(incoming -> {
			elementsEvent.setAttribute("incoming", elementsEvent.getAttribute("incoming") + " " + incoming.getId());
		});
		gateway.getOutgoingSequenceFlows().stream().forEach(outgoing -> {
			elementsEvent.setAttribute("outgoing", elementsEvent.getAttribute("outgoing") + " " + outgoing.getId());
		});

		return elementsEvent;
	}

	Element addDiagramForGateway(Document doc, Map<String, Element> parentElement, String elementID, String x, String y,
			GatewayType type, boolean isLane) throws XPathExpressionException {

		// Create the <children> element
		Element childrenElement = doc.createElement("children");
		childrenElement.setAttribute("xmi:type", "notation:Shape");
		childrenElement.setAttribute("xmi:id", generateXmiId());
		if (type == GatewayType.AND)
			childrenElement.setAttribute("type", "3009");
		else if (type == GatewayType.XOR)
			childrenElement.setAttribute("type", "3008");
		else if (type == GatewayType.INCLUSIVE)
			childrenElement.setAttribute("type", "3051");

		childrenElement.setAttribute("element", elementID);
		childrenElement.setAttribute("fontName", "Segoe UI");

		// get lane child with type 7002/ pool 7001
		// Create an XPath instance
		XPathFactory xPathFactory = XPathFactory.newInstance();
		XPath xPath = xPathFactory.newXPath();
		// Define the XPath expression to find the <children> element with type="7002"
		String xpathExpression = "";

		if (isLane) {
			xpathExpression = "//children[@element='" + parentElement.get(PROCESS).getAttribute("xmi:id")
					+ "']/children[@type='7002']";
		} else {
			xpathExpression = "//children[@element='" + parentElement.get(PROCESS).getAttribute("xmi:id")
					+ "']/children[@type='7001']";
		}
		// Evaluate the XPath expression and get the matching node
		Node node = (Node) xPath.evaluate(xpathExpression, doc, XPathConstants.NODE);
		if (node != null) {
			Element diagram = (Element) node;
			diagram.appendChild(childrenElement);
		}

		// Child element
		Element child = doc.createElement("children");
		child.setAttribute("xmi:type", "notation:DecorationNode");
		child.setAttribute("xmi:id", generateXmiId());
		if (type == GatewayType.AND)
			child.setAttribute("type", "5020");
		else if (type == GatewayType.XOR)
			child.setAttribute("type", "5026");
		else if (type == GatewayType.INCLUSIVE)
			child.setAttribute("type", "5075");
		childrenElement.appendChild(child);

		// Child's child element
		Element layoutConstraint = doc.createElement("layoutConstraint");
		layoutConstraint.setAttribute("xmi:type", "notation:Location");
		layoutConstraint.setAttribute("xmi:id", generateXmiId());
		layoutConstraint.setAttribute("y", "5");
		child.appendChild(layoutConstraint);

		// Root's child element
		Element layoutConstraintRoot = doc.createElement("layoutConstraint");
		layoutConstraintRoot.setAttribute("xmi:type", "notation:Bounds");
		layoutConstraintRoot.setAttribute("xmi:id", generateXmiId());
		layoutConstraintRoot.setAttribute("x", x);
		layoutConstraintRoot.setAttribute("y", y);
		childrenElement.appendChild(layoutConstraintRoot);

		return childrenElement;
	}

	private File createProcFile(Document doc) {
		try {

			// Save the modified XML document
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
//			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			DOMSource source = new DOMSource(doc);
			File outfile = new File(projectPath + "\\app\\diagrams\\" + outputName + "-0.0.proc");
			StreamResult result = new StreamResult(outfile);
			transformer.transform(source, result);
			return outfile;
		} catch (Exception e) {
			return null;
		}

	}
	
	public class OperationElement {
		
		OperationElement(Element element,List<String> strings){
			setElement(element);
			setStrings(strings);
			
		}
	    public Element getElement() {
			return element;
		}
		public void setElement(Element element) {
			this.element = element;
		}
		public List<String> getStrings() {
			return strings;
		}
		public void setStrings(List<String> strings) {
			this.strings = strings;
		}
		private Element element;
	    private List<String> strings;

	    // constructor, getters, setters...
	}

	public class OperationOrder {
		OperationOrder(String name,OperationElement operationElement){
			setName(name);
			setOperationElement(operationElement);
		}
	    public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public OperationElement getOperationElement() {
			return operationElement;
		}
		public void setOperationElement(OperationElement operationElement) {
			this.operationElement = operationElement;
		}
		private String name;
	    private OperationElement operationElement;

	    // constructor, getters, setters...
	}

	public class OperationSet {
		OperationSet(List<OperationOrder> operationOrders){
			setOperationOrders(operationOrders);
		}
	    public List<OperationOrder> getOperationOrders() {
			return operationOrders;
		}

		public void setOperationOrders(List<OperationOrder> operationOrders) {
			this.operationOrders = operationOrders;
		}

		private List<OperationOrder> operationOrders;

	    // constructor, getters, setters...
	}
}



