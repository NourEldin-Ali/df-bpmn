package org.openbpm.bpmn.converter;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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
import org.openbpmn.bpmn.elements.Event;
import org.openbpmn.bpmn.elements.Gateway;
import org.openbpmn.bpmn.elements.Participant;
import org.openbpmn.bpmn.elements.SequenceFlow;
import org.openbpmn.bpmn.elements.core.BPMNElementNode;
import org.openbpmn.bpmn.exceptions.BPMNModelException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

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
			logger.info("get info from proc");

			File file = new File("src/main/resources/initDiagram.proc");
			// an instance of factory that gives a document builder
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			// an instance of builder to parse the specified xml file
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(file);
			doc.getDocumentElement().normalize();
			logger.info("get main process");
			Node mainProcess = doc.getElementsByTagName("process:MainProcess").item(0);

			mainProcess.getAttributes().getNamedItem("name").setNodeValue(outputName);
			logger.info("get notation diagram");
			Node mainDiagram = doc.getElementsByTagName("notation:Diagram").item(0);

			// read data from bpmn file
			model.getParticipants().stream().forEach(participant -> {
				BPMNProcess openProcess;
				try {
					openProcess = model.openProcess(participant.getProcessRef());

//					System.out.println(openProcess.getActivities().size());
					if (openProcess.isPublicProcess()) {
						if (openProcess.getAllElementNodes().size() != 0) {
							Map<String, Element> pool = addPoolToProc(doc, mainProcess, mainDiagram,
									openProcess.getName(), "1320", "250");
							Element actor = addActor(pool,doc);
							Map<String, Element> lane = addLaneToProc(pool,actor.getAttribute("xmi:id"), doc, "Default Lane", "1320", "250");
							System.out.println(openProcess.getActivities().size());
							addElementsToLane(lane, null, doc, openProcess.getActivities(), openProcess.getEvents(),
									openProcess.getGateways());
						}
					} else {
						if (openProcess.getAllElementNodes().size() > 0) {
//				
							Map<String, Element> pool = addPoolToProc(doc, mainProcess, mainDiagram,
									participant.getName(),
									String.valueOf(getAttributeBoundsValue(participant, "width")),
									String.valueOf(getAttributeBoundsValue(participant, "height")));
							Element actor = addActor(pool,doc);
							if (openProcess.getLanes().size() == 0) {
								try {
									
									Map<String, Element> lane = addLaneToProc(pool,actor.getAttribute("xmi:id"), doc, "Default Lane",
											String.valueOf(getAttributeBoundsValue(participant, "width")),
											String.valueOf(getAttributeBoundsValue(participant, "height")));
									addElementsToLane(lane, participant, doc, openProcess.getActivities(),
											openProcess.getEvents(), openProcess.getGateways());
								} catch (XPathExpressionException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								addSquenceFlowFromBPMN(pool,mainDiagram, doc, openProcess.getSequenceFlows());

							} else {
//								System.out.println(openProcess.getAllElementNodes().size());
								openProcess.getLanes().stream().forEach(laneBpmn -> {

									try {

										int widthParserLane = getAttributeBoundsValue(participant, "width")
												/ openProcess.getLanes().size();

										Map<String, Element> lane = addLaneToProc(pool, actor.getAttribute("xmi:id"),doc, laneBpmn.getName(),
												String.valueOf(widthParserLane), String.valueOf(
														String.valueOf(getAttributeBoundsValue(laneBpmn, "height"))));

										Set<Activity> activityList = new HashSet();
										openProcess.getActivities().stream().forEach(activity -> {
											if (laneBpmn.contains(activity)) {
												activityList.add(activity);
											}

										});

										Set<Event> eventsList = new HashSet();
										openProcess.getEvents().stream().forEach(event -> {
											if (laneBpmn.contains(event)) {
												eventsList.add(event);
											}

										});

										Set<Gateway> gatwayList = new HashSet();
										openProcess.getGateways().stream().forEach(gateway -> {
											if (laneBpmn.contains(gateway)) {
												gatwayList.add(gateway);
											}
										});
										addElementsToLane(lane, laneBpmn, doc, activityList, eventsList, gatwayList);

//										addSequenceFlowToProc(mainProcess,diagram,)
									} catch (XPathExpressionException e) {
										e.printStackTrace();
									}
								});

							}
							addSquenceFlowFromBPMN(pool, mainDiagram,doc, openProcess.getSequenceFlows());

						}
					}

				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			});

			createProcFile(doc);

		} catch (Exception e) {
			return null;
		}
		return null;
	}

	Element addActor(Map<String, Element> pool, Document doc) {
		// Create the root element <elements>
		Element actor = doc.createElement("actors");
		actor.setAttribute("xmi:type", "process:Actor");
		actor.setAttribute("xmi:id", generateXmiId());
		actor.setAttribute("name", "test");
		pool.get(PROCESS).appendChild(actor);
		return actor;
	}

	void addElementsToLane(Map<String, Element> lane, BPMNElementNode laneBpmn, Document doc,
			Set<Activity> activityList, Set<Event> eventList, Set<Gateway> gatewayList)
			throws XPathExpressionException {
		addActivityFromBPMN(lane, laneBpmn, doc, activityList);
		addEventFromBPMN(lane, laneBpmn, doc, eventList);
		addGatewayFromBPMN(lane, laneBpmn, doc, gatewayList);
	}

	private int getAttributeBoundsValue(BPMNElementNode elementNode, String attribute) {
		String value = elementNode.getBpmnShape().getFirstChild().getAttributes().getNamedItem(attribute)
				.getNodeValue();
		int valueParser = (int) Float.parseFloat(value);
		return valueParser;
	}

	private void addEventFromBPMN(Map<String, Element> lane, BPMNElementNode laneBmn, Document doc, Set<Event> events)
			throws XPathExpressionException {
		events.stream().forEach(event -> {
			try {
//				System.out.println(event.getType());
				if (event.getType().equals("startEvent")) {
					addEventToProc(lane, doc, event,
							String.valueOf(getAttributeBoundsValue(event, "x")
									- (laneBmn != null ? getAttributeBoundsValue(laneBmn, "x") : 0)),
							String.valueOf(getAttributeBoundsValue(event, "y")
									- (laneBmn != null ? getAttributeBoundsValue(laneBmn, "y") : 0)),
							EventType.START);
				} else if (event.getType().equals("endEvent")) {
					addEventToProc(lane, doc, event,
							String.valueOf(getAttributeBoundsValue(event, "x")
									- (laneBmn != null ? getAttributeBoundsValue(laneBmn, "x") : 0)),
							String.valueOf(getAttributeBoundsValue(event, "y")
									- (laneBmn != null ? getAttributeBoundsValue(laneBmn, "y") : 0)),
							EventType.END);
				}

			} catch (XPathExpressionException e) {
			}
		});
	}

	private void addActivityFromBPMN(Map<String, Element> lane, BPMNElementNode laneBmn, Document doc,
			Set<Activity> activities) throws XPathExpressionException {
		activities.stream().forEach(activity -> {
			try {
				if (activity.isHuman()) {
					addActivityToProc(lane, doc, activity,
							String.valueOf(getAttributeBoundsValue(activity, "x")
									- (laneBmn != null ? getAttributeBoundsValue(laneBmn, "x") : 0)),
							String.valueOf(getAttributeBoundsValue(activity, "y")
									- (laneBmn != null ? getAttributeBoundsValue(laneBmn, "y") : 0)),
							ActivityType.HUMAN);
				} else {

					addActivityToProc(lane, doc, activity,
							String.valueOf(getAttributeBoundsValue(activity, "x")
									- (laneBmn != null ? getAttributeBoundsValue(laneBmn, "x") : 0)),
							String.valueOf(getAttributeBoundsValue(activity, "y")
									- (laneBmn != null ? getAttributeBoundsValue(laneBmn, "y") : 0)),
							ActivityType.SERVICE);
				}
			} catch (XPathExpressionException e) {
				// TODO Auto-generated catch block
//				e.printStackTrace();
			}
		});
	}

	private void addSquenceFlowFromBPMN(Map<String,Element> pool,Node diagram, Document doc,
			Set<SequenceFlow> sequenceFlowList) throws XPathExpressionException {
		sequenceFlowList.stream().forEach(sequenceFlow -> {
			try {
				addSequenceFlowToProc(pool,diagram, doc, sequenceFlow, "0", "0");
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

	private void addGatewayFromBPMN(Map<String, Element> lane, BPMNElementNode laneBmn, Document doc,
			Set<Gateway> gateways) throws XPathExpressionException {
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
					addGatewayToProc(lane, doc, gateway,
							String.valueOf(getAttributeBoundsValue(gateway, "x")
									- (laneBmn != null ? getAttributeBoundsValue(laneBmn, "x") : 0)),
							String.valueOf(getAttributeBoundsValue(gateway, "y")
									- (laneBmn != null ? getAttributeBoundsValue(laneBmn, "y") : 0)),
							type);

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
		logger.info("add pool element");

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

	private Map<String, Element> addLaneToProc(Map<String, Element> pool, String actorID, Document doc, String laneName,
			String width, String height) throws XPathExpressionException {
		logger.info("add lane element");

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

	private Element addActivityToProc(Map<String, Element> lane, Document doc, Activity activity, String x, String y,
			ActivityType type) throws XPathExpressionException {
		logger.info("add activity element");

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

		lane.get(PROCESS).appendChild(elementsActivity);

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

			List<DataInputObjectExtension> dataList = activity.getDataInputObjects().stream().filter(data -> data
					.getElementNode().getLocalName().equals(BPMNTypes.DATA_INPUT_OBJECT_ENVIRONMENT_DATA_USER))
					.collect(Collectors.toList());

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
		if (type == ActivityType.HUMAN) {
			addDiagramForActivity(doc, lane, elementsActivity.getAttribute("xmi:id"), x, y, ActivityType.HUMAN);
		} else {
			addDiagramForActivity(doc, lane, elementsActivity.getAttribute("xmi:id"), x, y, ActivityType.SERVICE);
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

	Element addDiagramForActivity(Document doc, Map<String, Element> lane, String elementID, String x, String y,
			ActivityType type) throws XPathExpressionException {

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

		// get lane child with type 7002
		// Create an XPath instance
		XPathFactory xPathFactory = XPathFactory.newInstance();
		XPath xPath = xPathFactory.newXPath();
		// Define the XPath expression to find the <children> element with type="7002"
		String xpathExpression = "//children[@element='" + lane.get(PROCESS).getAttribute("xmi:id")
				+ "']/children[@type='7002']";
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

	private Element addEventToProc(Map<String, Element> lane, Document doc, Event event, String x, String y,
			EventType type) throws XPathExpressionException {

		logger.info("add event element");

		// Create the <elements> element
		Element elementsEvent = doc.createElement("elements");

		if (type == EventType.START)
			elementsEvent.setAttribute("xmi:type", "process:StartEvent");
		else if (type == EventType.END)
			elementsEvent.setAttribute("xmi:type", "process:EndEvent");

		elementsEvent.setAttribute("xmi:id", event.getId());
		elementsEvent.setAttribute("name", event.getName());

		lane.get(PROCESS).appendChild(elementsEvent);

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

		addDiagramForEvent(doc, lane, elementsEvent.getAttribute("xmi:id"), x, y, type);

		// add incoming and outgoing
		event.getIngoingSequenceFlows().stream().forEach(incoming -> {
			elementsEvent.setAttribute("incoming", elementsEvent.getAttribute("incoming") + " " + incoming.getId());
		});
		event.getOutgoingSequenceFlows().stream().forEach(outgoing -> {
			elementsEvent.setAttribute("outgoing", elementsEvent.getAttribute("outgoing") + " " + outgoing.getId());
		});

		return elementsEvent;
	}

	Element addDiagramForEvent(Document doc, Map<String, Element> lane, String elementID, String x, String y,
			EventType type) throws XPathExpressionException {

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

		// get lane child with type 7002
		// Create an XPath instance
		XPathFactory xPathFactory = XPathFactory.newInstance();
		XPath xPath = xPathFactory.newXPath();
		// Define the XPath expression to find the <children> element with type="7002"
		String xpathExpression = "//children[@element='" + lane.get(PROCESS).getAttribute("xmi:id")
				+ "']/children[@type='7002']";
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

	private Element addSequenceFlowToProc(Map<String,Element> pool, Node diagram, Document doc, SequenceFlow sequenceFlow,
			String x, String y) throws XPathExpressionException {
		logger.info("add sequence flow");

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
		// Define the XPath expression to find the <children> element with type="7002"
		String xpathExpression = "//children[@element='" + squenceFlow.getSourceRef() + "' and @type='notation:Shape']";
		// Evaluate the XPath expression and get the matching node
		Node source = (Node) xPath.evaluate(xpathExpression, doc, XPathConstants.NODE);

		// get target node
		xPathFactory = XPathFactory.newInstance();
		xPath = xPathFactory.newXPath();
		// Define the XPath expression to find the <children> element with type="7002"
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

	private Element addGatewayToProc(Map<String, Element> lane, Document doc, Gateway gateway, String x, String y,
			GatewayType type) throws XPathExpressionException {

		logger.info("add gateway element");

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

		lane.get(PROCESS).appendChild(elementsEvent);

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

		addDiagramForGateway(doc, lane, elementsEvent.getAttribute("xmi:id"), x, y, type);

		// add incoming and outgoing
		gateway.getIngoingSequenceFlows().stream().forEach(incoming -> {
			elementsEvent.setAttribute("incoming", elementsEvent.getAttribute("incoming") + " " + incoming.getId());
		});
		gateway.getOutgoingSequenceFlows().stream().forEach(outgoing -> {
			elementsEvent.setAttribute("outgoing", elementsEvent.getAttribute("outgoing") + " " + outgoing.getId());
		});

		return elementsEvent;
	}

	Element addDiagramForGateway(Document doc, Map<String, Element> lane, String elementID, String x, String y,
			GatewayType type) throws XPathExpressionException {

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

		// get lane child with type 7002
		// Create an XPath instance
		XPathFactory xPathFactory = XPathFactory.newInstance();
		XPath xPath = xPathFactory.newXPath();
		// Define the XPath expression to find the <children> element with type="7002"
		String xpathExpression = "//children[@element='" + lane.get(PROCESS).getAttribute("xmi:id")
				+ "']/children[@type='7002']";
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

	private void createProcFile(Document doc) {
		try {

			// Save the modified XML document
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
//			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(new File(projectPath + "\\diagrams\\" + outputName + "-0.0.proc"));
			transformer.transform(source, result);
		} catch (Exception e) {
		}

	}
}
