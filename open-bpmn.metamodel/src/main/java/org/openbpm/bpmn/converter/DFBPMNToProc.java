package org.openbpm.bpmn.converter;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

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
import org.openbpmn.bpmn.elements.DataProcessingExtension;
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
		System.out.println("Start converting to Bonita");
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

					OptionalInt width = openProcess.getAllElementNodes().stream().mapToInt(bpmnElement -> {
						try {
							return (int) bpmnElement.getBounds().getPosition().getY()
									+ (int) bpmnElement.getBounds().getDimension().getWidth();
						} catch (Exception e) {

						}
						return 0;
					}).max();
					OptionalInt height = openProcess.getAllElementNodes().stream().mapToInt(bpmnElement -> {
						try {
							return (int) bpmnElement.getBounds().getPosition().getX()
									+ (int) bpmnElement.getBounds().getDimension().getHeight();
						} catch (Exception e) {

						}
						return 0;
					}).max();

					System.out.println(String.valueOf(width.getAsInt()));
					System.out.println(String.valueOf(height.getAsInt()));
					Map<String, Element> pool = addPoolToProc(doc, mainProcess, mainDiagram, openProcess.getName(),
							String.valueOf(height.getAsInt()), String.valueOf(width.getAsInt()));
					Map<String, String> processVariable = openProcess.getDataObjectsExtensions();
					addProcessVariableToProc(pool, processVariable, doc);
					Map<String, Map<String, String>> businessDataModel = openProcess.getDataStoresExtensions();
					addBusinessDataToProc(pool, businessDataModel, doc);
					addActor(pool, doc);
//						Map<String, Element> lane = addLaneToProc(pool, doc, "Default Lane", "1320", "250",
//								actor.getAttribute("xmi:id"));
//						System.out.println(openProcess.getActivities().size());
					addElements(pool, false, null, doc, openProcess.getActivities(), openProcess.getEvents(),
							openProcess.getGateways());
					addSquenceFlowFromBPMN(pool, mainDiagram, doc, openProcess.getSequenceFlows());
				}
			} else {
				model.getParticipants().stream().forEach(participant -> {
					BPMNProcess openProcess;
					try {
						openProcess = model.openProcess(participant.getProcessRef());

//					System.out.println(openProcess.getActivities().size());
						if (openProcess.isPublicProcess()) {
							if (openProcess.getAllElementNodes().size() != 0) {
//								Map<String, Element> pool = addPoolToProc(doc, mainProcess, mainDiagram,
//										openProcess.getName(), "1320", "250");
								OptionalInt width = openProcess.getAllElementNodes().stream().mapToInt(bpmnElement -> {
									try {
										return (int) bpmnElement.getBounds().getPosition().getY()
												+ (int) bpmnElement.getBounds().getDimension().getWidth();
									} catch (Exception e) {

									}
									return 0;
								}).max();
								OptionalInt height = openProcess.getAllElementNodes().stream().mapToInt(bpmnElement -> {
									try {
										return (int) bpmnElement.getBounds().getPosition().getX()
												+ (int) bpmnElement.getBounds().getDimension().getHeight();
									} catch (Exception e) {

									}
									return 0;
								}).max();

								System.out.println(String.valueOf(width.getAsInt()));
								System.out.println(String.valueOf(height.getAsInt()));
								Map<String, Element> pool = addPoolToProc(doc, mainProcess, mainDiagram,
										openProcess.getName(), String.valueOf(height.getAsInt()),
										String.valueOf(width.getAsInt()));
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
								addSquenceFlowFromBPMN(pool, mainDiagram, doc, openProcess.getSequenceFlows());
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
									addSquenceFlowFromBPMN(pool, mainDiagram, doc, openProcess.getSequenceFlows());

								}
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				});
			}
			File procFile = createProcFile(doc);
			if (procFile == null) {
				logger.info("Generate failed to Bonita Proc");
			}
			System.out.println("Converting to Bonita done");
			return procFile;
		} catch (

		Exception e) {
			logger.info("Generate failed to Bonita Proc");
			System.out.println(e.getMessage());
			return null;
		}
	}

	/**
	 * This function to add the process variable to the pool process data within
	 * Bonita file.
	 * 
	 * @param pool
	 * @param processVariable
	 * @param doc
	 * @throws XPathExpressionException
	 */
	void addProcessVariableToProc(Map<String, Element> pool, Map<String, String> processVariable, Document doc)
			throws XPathExpressionException {

		// get types stored within the XML file of Bonita project
		Map<String, String> types = getDataTypes(doc);
		;

		// add the process variable
		processVariable.forEach((dataName, dataType) -> {
			Element data = doc.createElement("data");
			data.setAttribute("xmi:type", "process:Data");
			data.setAttribute("xmi:id", generateXmiId());
			data.setAttribute("name", dataName);
			// define the data type
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

	/**
	 * This function to add the BDM variables within the pool.
	 * 
	 * @param pool
	 * @param businessDataModel
	 * @param doc
	 * @throws XPathExpressionException
	 */
	void addBusinessDataToProc(Map<String, Element> pool, Map<String, Map<String, String>> businessDataModel,
			Document doc) throws XPathExpressionException {

		// get data types stored within the Bonita XML initiation.
		Map<String, String> types = getDataTypes(doc);

		// add the BDM
		businessDataModel.forEach((dataName, dataElement) -> {
			Element data = doc.createElement("data");
			data.setAttribute("xmi:type", "process:BusinessObjectData");
			data.setAttribute("xmi:id", generateXmiId());
			data.setAttribute("name", dataName);
			data.setAttribute("dataType", types.get("business_object")); // all the type are business object
			data.setAttribute("className", dataElement.get("type"));
			// check if it is multiple (list)
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

	/**
	 * This function to define the actor for each pool. The actor are define as
	 * initiator and then can be modified from Bonita plateform.
	 * 
	 * @param pool
	 * @param doc
	 * @return
	 */
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

	/**
	 * This function is to add all the data elements within a lane. Activities (and
	 * their data), Events, and gateways are automatically added to the lane.
	 * 
	 * @param lane
	 * @param isLane
	 * @param laneBpmn
	 * @param doc
	 * @param activityList
	 * @param eventList
	 * @param gatewayList
	 * @throws XPathExpressionException
	 */
	void addElements(Map<String, Element> lane, boolean isLane, BPMNElementNode laneBpmn, Document doc,
			Set<Activity> activityList, Set<Event> eventList, Set<Gateway> gatewayList)
			throws XPathExpressionException {
		addActivityFromBPMN(lane, laneBpmn, doc, activityList, isLane);
		addEventFromBPMN(lane, laneBpmn, doc, eventList, isLane);
		addGatewayFromBPMN(lane, laneBpmn, doc, gatewayList, isLane);
	}

	/**
	 * This function to get the bounds of and BPMNElementNode. Usually to get
	 * attribute "x" and attribute "y" from the diagram of BPMN of a selected
	 * element node (from the XML format).
	 * 
	 * @param elementNode
	 * @param attribute
	 * @return
	 */
	private int getAttributeBoundsValue(BPMNElementNode elementNode, String attribute) {
		String value = elementNode.getBpmnShape().getFirstChild().getAttributes().getNamedItem(attribute)
				.getNodeValue();
		int valueParser = (int) Float.parseFloat(value);
		return valueParser;
	}

	/**
	 * This function to add the event to a selected lane. !We only define 2 events:
	 * start and end events.
	 * 
	 * @param parentElement
	 * @param laneBmn
	 * @param doc
	 * @param events
	 * @param isLane
	 * @throws XPathExpressionException
	 */
	private void addEventFromBPMN(Map<String, Element> parentElement, BPMNElementNode laneBmn, Document doc,
			Set<Event> events, boolean isLane) throws XPathExpressionException {
		events.stream().forEach(event -> {
			try {
				EventType selectedEvent = null;
				// get selected event type
				if (event.getType().equals("startEvent")) {
					selectedEvent = EventType.START;
				} else if (event.getType().equals("endEvent")) {
					selectedEvent = EventType.END;
				} else {
					return;
				}
				// add the event to proc file (Bonita XML)
				addEventToProc(parentElement, doc, event,
						String.valueOf(getAttributeBoundsValue(event, "x")
								- (laneBmn != null ? getAttributeBoundsValue(laneBmn, "x") : 0)),
						String.valueOf(getAttributeBoundsValue(event, "y")
								- (laneBmn != null ? getAttributeBoundsValue(laneBmn, "y") : 0)),
						selectedEvent, isLane);
			} catch (XPathExpressionException e) {
			}
		});
	}

	/**
	 * This function to add the activity from BPMN to Bonita format File
	 * 
	 * @param parentElement
	 * @param laneBmn
	 * @param doc
	 * @param activities
	 * @param isLane
	 * @throws XPathExpressionException
	 */
	private void addActivityFromBPMN(Map<String, Element> parentElement, BPMNElementNode laneBmn, Document doc,
			Set<Activity> activities, boolean isLane) throws XPathExpressionException {
		activities.stream().forEach(activity -> {
			try {
				ActivityType activtyType = null;
				// get activity type
				// human type if the activity has user data
				if (activity.isHuman()) {
					activtyType = ActivityType.HUMAN;
				} else {
					activtyType = ActivityType.SERVICE;
				}
				// add to proc
				addActivityToProc(parentElement, doc, activity,
						String.valueOf(getAttributeBoundsValue(activity, "x")
								- (laneBmn != null ? getAttributeBoundsValue(laneBmn, "x") : 0)),
						String.valueOf(getAttributeBoundsValue(activity, "y")
								- (laneBmn != null ? getAttributeBoundsValue(laneBmn, "y") : 0)),
						activtyType, isLane);
			} catch (XPathExpressionException e) {
//				e.printStackTrace();
			}
		});
	}

	/**
	 * This function to add the sequence flow between the activities, gateways, and
	 * the events.
	 * 
	 * @param pool
	 * @param diagram
	 * @param doc
	 * @param sequenceFlowList
	 * @throws XPathExpressionException
	 */
	private void addSquenceFlowFromBPMN(Map<String, Element> pool, Node diagram, Document doc,
			Set<SequenceFlow> sequenceFlowList) throws XPathExpressionException {
		sequenceFlowList.stream().forEach(sequenceFlow -> {
			try {
				addSequenceFlowToProc(pool, diagram, doc, sequenceFlow, "0", "0");
			} catch (XPathExpressionException e) {
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

	/**
	 * THis function to add new lane to the proc file.
	 * 
	 * @param pool
	 * @param doc
	 * @param laneName
	 * @param width
	 * @param height
	 * @param actorID
	 * @return
	 * @throws XPathExpressionException
	 */
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
		// 7002 used for the elements of the lane
		// 5007 ?
		String[] types = { "5007", "7002" };
		for (String type : types) {
			Element child = doc.createElement("children");
			child.setAttribute("xmi:type", "notation:DecorationNode");
			child.setAttribute("xmi:id", generateXmiId() + type);
			child.setAttribute("type", type);
			childrenLane.appendChild(child);
		}

		// Create and append the <styles> elements with different types -> static stype
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

	/**
	 * This function to convert all the activity data from DF-BPMN to Bonita format
	 * (proc), to be executable within Bonita.
	 * 
	 * @param parentElement
	 * @param doc
	 * @param activity
	 * @param x
	 * @param y
	 * @param type
	 * @param isLane
	 * @return
	 * @throws XPathExpressionException
	 */
	private Element addActivityToProc(Map<String, Element> parentElement, Document doc, Activity activity, String x,
			String y, ActivityType type, boolean isLane) throws XPathExpressionException {
		// define data type used within Bonita
		Map<String, String> dataTypes = new HashMap();
		dataTypes.put("text", "text");
		dataTypes.put("string", "text");
		dataTypes.put("boolean", "boolean");
		dataTypes.put("bool", "boolean");
		dataTypes.put("decimal", "decimal");
		dataTypes.put("float", "decimal");
		dataTypes.put("double", "decimal");
		dataTypes.put("integer", "integer");
		dataTypes.put("int", "integer");
		dataTypes.put("date", "date");

		// Create the <elements> element activty to be added late to the process.
		Element elementsActivity = doc.createElement("elements");
		// define task type
		if (type == ActivityType.HUMAN)
			elementsActivity.setAttribute("xmi:type", "process:Task");
		else
			elementsActivity.setAttribute("xmi:type", "process:ServiceTask");

		elementsActivity.setAttribute("xmi:id", activity.getId());
		elementsActivity.setAttribute("name", activity.getName());
		// in case of the human need some other info
		if (type == ActivityType.HUMAN)
			elementsActivity.setAttribute("overrideActorsOfTheLane", "false");

		parentElement.get(PROCESS).appendChild(elementsActivity);

		// attributes predefined within any activity.
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

		// in case of human activity, we need to define the contract of the activity
		if (type == ActivityType.HUMAN) {
			// required element to add contract to the activity
			Element formMappingElement = doc.createElement("formMapping");
			formMappingElement.setAttribute("xmi:type", "process:FormMapping");
			formMappingElement.setAttribute("xmi:id", generateXmiId());
			elementsActivity.appendChild(formMappingElement);
			// required element to add contract to the activity
			Element targetFormElement = doc.createElement("targetForm");
			targetFormElement.setAttribute("xmi:type", "expression:Expression");
			targetFormElement.setAttribute("xmi:id", generateXmiId());
			targetFormElement.setAttribute("name", "");
			targetFormElement.setAttribute("content", "");
			targetFormElement.setAttribute("type", "FORM_REFERENCE_TYPE");
			targetFormElement.setAttribute("returnTypeFixed", "true");
			formMappingElement.appendChild(targetFormElement);

			// define the contract
			Element contractElement = doc.createElement("contract");
			contractElement.setAttribute("xmi:type", "process:Contract");
			contractElement.setAttribute("xmi:id", generateXmiId());
			elementsActivity.appendChild(contractElement);

			// get the data from DF-BPMN presented as DATA USER
			// DATA_INPUT_OBJECT_ENVIRONMENT_DATA_USER
			// DATA_INPUT_OBJECT_DEPENDENCY
			List<DataInputObjectExtension> dataList = activity.getDataInputObjects().stream()
					.filter(data -> data.getElementNode().getLocalName()
							.equals(BPMNTypes.DATA_INPUT_OBJECT_ENVIRONMENT_DATA_USER)
							|| data.getElementNode().getLocalName().equals(BPMNTypes.DATA_INPUT_OBJECT_DEPENDENCY))
					.collect(Collectors.toList());

			// loop of the data find to add to the contract
			dataList.stream().forEach(objectData -> {
				// Create root element used for each contract input
				Element dataElement = doc.createElement("inputs");
				dataElement.setAttribute("xmi:type", "process:ContractInput");
				dataElement.setAttribute("xmi:id", generateXmiId());
				dataElement.setAttribute("name", objectData.getName());
				// in case of multiple
				if (objectData.getAttribute("isMultiple").equals("true"))
					dataElement.setAttribute("multiple", "true");

				contractElement.appendChild(dataElement);

				// Create mapping element (required for the contract)
				Element mapping = doc.createElement("mapping");
				mapping.setAttribute("xmi:type", "process:ContractInputMapping");
				mapping.setAttribute("xmi:id", generateXmiId());
				dataElement.appendChild(mapping);

				// in case the object has attributes (child)
				if (objectData.getDataAttributes().size() > 0) {
					// it will be complex object
					dataElement.setAttribute("type", "COMPLEX");

					// get all the data attribute to add as child within the contract
					objectData.getDataAttributes().stream().forEach(dataAttribute -> {

						Element elementAttribute = doc.createElement("inputs");
						elementAttribute.setAttribute("xmi:type", "process:ContractInput");
						elementAttribute.setAttribute("xmi:id", generateXmiId());
						elementAttribute.setAttribute("name", dataAttribute.getName());
						// get data type
						if (dataTypes.containsKey(dataAttribute.getAttribute("type").toLowerCase())) {
							elementAttribute.setAttribute("type",
									dataTypes.get(dataAttribute.getAttribute("type")).toUpperCase());
						} else {
							dataElement.setAttribute("type", dataTypes.get("text").toUpperCase());
						}
						dataElement.appendChild(elementAttribute);

					});
				}
				// in case we dont have attribute (without childreen)
				else {
					// get the data type
					if (dataTypes.containsKey(objectData.getAttribute("type").toLowerCase())) {
						dataElement.setAttribute("type", dataTypes.get(objectData.getAttribute("type")).toUpperCase());
					} else {
						dataElement.setAttribute("type", dataTypes.get("text").toUpperCase());
					}
				}
			});

			// <expectedDuration> predefine element
			Element expectedDurationElement = doc.createElement("expectedDuration");
			expectedDurationElement.setAttribute("xmi:type", "expression:Expression");
			expectedDurationElement.setAttribute("xmi:id", generateXmiId());
			expectedDurationElement.setAttribute("name", "");
			expectedDurationElement.setAttribute("content", "");
			expectedDurationElement.setAttribute("returnType", "java.lang.Long");
			expectedDurationElement.setAttribute("returnTypeFixed", "true");
			elementsActivity.appendChild(expectedDurationElement);
		}
		// add data operation
		addOperationForActivity(doc, activity, elementsActivity, dataTypes);

		// add activity to diagram
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
	 * This function to get all the data types used for the variables
	 * 
	 * @param doc
	 * @return
	 * @throws XPathExpressionException
	 */
	Map<String, String> getDataTypes(Document doc) throws XPathExpressionException {
		Map<String, String> fixDataTypes = new HashMap<>();
		// Create an XPath instance
		XPathFactory xPathFactory = XPathFactory.newInstance();
		XPath xPath = xPathFactory.newXPath();
		// Define the XPath expression to find the <children> element
		String xpathExpression = "//datatypes";

		// Evaluate the XPath expression and get the matching node
		NodeList listNode = (NodeList) xPath.evaluate(xpathExpression, doc, XPathConstants.NODESET);
		if (listNode != null) {
			for (int i = 0; i < listNode.getLength(); i++) {
				fixDataTypes.put(
						listNode.item(i).getAttributes().getNamedItem("name").getNodeValue().toLowerCase().trim(),
						listNode.item(i).getAttributes().getNamedItem("xmi:id").getNodeValue());
			}
		}
		// used for process variable
		fixDataTypes.put("string", fixDataTypes.get("text"));
		fixDataTypes.put("decimal", fixDataTypes.get("double"));
		fixDataTypes.put("float", fixDataTypes.get("double"));
		fixDataTypes.put("int", fixDataTypes.get("integer"));

		return fixDataTypes;
	}

	/**
	 * add operation for the activity
	 * 
	 * @param doc
	 * @param activity
	 * @param activityElement
	 * @param dataType        (fix data type already defined within Bonita studio)
	 * @throws XPathExpressionException
	 */
	void addOperationForActivity(Document doc, Activity activity, Element activityElement,
			Map<String, String> dataTypes) throws XPathExpressionException {
		// get Data Types
		Map<String, String> variableDataTypes = getDataTypes(doc);

		// inputTypes used for BDM
		Map<String, String> returnDataTypes = new HashMap<>();
		returnDataTypes.put("string", "java.lang.String");
		returnDataTypes.put("text", "java.lang.String");
		returnDataTypes.put("boolean", "java.lang.Boolean");
		returnDataTypes.put("bool", "java.lang.Boolean");
		returnDataTypes.put("double", "java.lang.Double");
		returnDataTypes.put("float", "java.lang.Float");
		returnDataTypes.put("integer", "java.lang.Integer");
		returnDataTypes.put("int", "java.lang.Integer");
		returnDataTypes.put("long", "java.lang.Long");
		returnDataTypes.put("object", "java.lang.Object");
		returnDataTypes.put("date", "java.util.Date");
		returnDataTypes.put("list", "java.util.List");

		// used for the data already treated within the activity
		Set<String> addedData = new HashSet<>();

		// to initiat the operations
		OperationSet operationsSet = new OperationSet(new ArrayList<>());

		// get all the data from the activity to convert it to bonita operations
		// we are based on the dataflow
		activity.getDataFlows().stream().forEach(dataflow -> {
			// get the target and source of the data from
			BPMNElement targetElement = activity.findElementById(dataflow.getTargetRef());
			BPMNElement sourceElement = activity.findElementById(dataflow.getSourceRef());

			// validate of the target element already treated
			if (addedData.contains(targetElement.getId())) {
				return;
			}

			// root operation element -> define the operation
			Element operations = doc.createElement("operations");
			operations.setAttribute("xmi:type", "expression:Operation");
			operations.setAttribute("xmi:id", generateXmiId());

			OperationOrder operationOrder;
			OperationElement inOperation = new OperationElement(operations, new ArrayList<>());

			// ---------------------------------left operand
			// element----------------------------------------------------------
			Element leftOperand = doc.createElement("leftOperand");
			leftOperand.setAttribute("xmi:type", "expression:Expression");
			leftOperand.setAttribute("xmi:id", generateXmiId());

			// for business data object
			if (targetElement.getElementNode().getLocalName().equals(BPMNTypes.DATA_OBJECT_ATTRIBUTE) && targetElement
					.getElementNode().getParentNode().getLocalName().equals(BPMNTypes.DATA_OUTPUT_OBJECT_DATA_STORE)) {

				// get the pared element => BDM object => database instance
				// from BDM object we need the to access their attribute to modify it
				BPMNElement parentElement = ((DataObjectAttributeExtension) targetElement).getDataParent();

				// validate if ti was treated before
				if (addedData.contains(parentElement.getId())) {
					return;
				}

				// to order the operation
				operationOrder = new OperationOrder(parentElement.getName(), inOperation);

				// get the status of the BDM object

				// update object
				if (activity.isUpdateData(parentElement.getId())) {
					addedData.add(targetElement.getId());
					// define the attributes of the left operand
					// no code needed here
					leftOperand.setAttribute("name", parentElement.getName()); // name
					leftOperand.setAttribute("content", parentElement.getName()); // name
					leftOperand.setAttribute("type", "TYPE_VARIABLE");
					leftOperand.setAttribute("returnType", parentElement.getAttribute("type"));// class name in BDM

				} else
				// in case of deleted, it will treated later
				if (activity.isDeleteData(parentElement.getId())) {
					return;
				} else
				// in case of read
				if (activity.isReadData(parentElement.getId())) {
					// we only support reading of one data instance (NOT FOR MULTI DATA INSTANCES)

					addedData.add(parentElement.getId());
					// define the attributes of the left operand
					// we need to convert some part to code
					leftOperand.setAttribute("name", parentElement.getName()); // name
					leftOperand.setAttribute("content", parentElement.getName()); // name
					leftOperand.setAttribute("type", "TYPE_VARIABLE");
					leftOperand.setAttribute("returnType", parentElement.getAttribute("type"));// class name in BDM

					// reference element to reference the DBM object from the pool data variable
					Element refElemenet = doc.createElement("referencedElements");
					refElemenet.setAttribute("xmi:id", generateXmiId());
					refElemenet.setAttribute("name", parentElement.getName()); // reference by unique name
					refElemenet.setAttribute("className", parentElement.getAttribute("type"));
					refElemenet.setAttribute("xmi:type", "process:BusinessObjectData");
					refElemenet.setAttribute("dataType", variableDataTypes.get("Business_Object".toLowerCase())); // dataTypes
					leftOperand.appendChild(refElemenet);

					// add the right operand => script
					Element rightOperand = doc.createElement("rightOperand");
					rightOperand.setAttribute("xmi:type", "expression:Expression");
					rightOperand.setAttribute("xmi:id", generateXmiId());
					rightOperand.setAttribute("name", "newScript()");
					rightOperand.setAttribute("interpreter", "GROOVY");
					rightOperand.setAttribute("type", "TYPE_READ_ONLY_SCRIPT");
					rightOperand.setAttribute("returnType", parentElement.getAttribute("type"));// class name in BDM

					// for the code within the script
					final StringBuilder script = new StringBuilder();
					script.append(parentElement.getName() + "DAO.findBy");

					// reference for the DAO (read) object
					refElemenet = doc.createElement("referencedElements");
					refElemenet.setAttribute("xmi:id", generateXmiId());
					refElemenet.setAttribute("name", parentElement.getName() + "DAO"); // reference by unique name
					refElemenet.setAttribute("content", parentElement.getName() + "DAO"); // reference by unique name

					refElemenet.setAttribute("returnType", parentElement.getAttribute("type") + "DAO");
					refElemenet.setAttribute("xmi:type", "expression:Expression");
					refElemenet.setAttribute("type", "TYPE_BUSINESS_OBJECT_DAO"); // dataTypes
					refElemenet.setAttribute("returnTypeFixed", "true");

					rightOperand.appendChild(refElemenet);

					// get all the data connected to the left operand => DBM instance
					activity.getConnectDataFlowTo(parentElement).stream().forEach(df -> {
						// get data source and target
						// the target should be part of the BDM
						BPMNElement srcElement = activity.findElementById(df.getSourceRef());
						BPMNElement trgtElement = activity.findElementById(df.getTargetRef());
						// check if inputs
						// in case of DATA_INPUT_OBJECT_LOCAL
						// add the data value directly to the script
						if (srcElement.getElementNode().getLocalName().equals(BPMNTypes.DATA_INPUT_OBJECT_LOCAL)) {
							script.append((trgtElement.getName().substring(0, 1).toUpperCase()
									+ trgtElement.getName().substring(1)) + "(");
							script.append("'" + srcElement.getAttribute("value") + "'");
						} else
						// in case of process variable or a child of process variable
						// process variable => DATA_INPUT_OBJECT_PROCESS
						if (srcElement.getElementNode().getLocalName().equals(BPMNTypes.DATA_INPUT_OBJECT_PROCESS)
								|| srcElement.getElementNode().getParentNode().getLocalName()
										.equals(BPMNTypes.DATA_INPUT_OBJECT_PROCESS)) {
							// add reference for the process variable
							Element refE = doc.createElement("referencedElements");
							refE.setAttribute("dataType",
									variableDataTypes.get(trgtElement.getAttribute("type").toLowerCase()));
							refE.setAttribute("xmi:id", generateXmiId());
							refE.setAttribute("xmi:type", "process:Data");
							refE.setAttribute("name", srcElement.getName());
							// add to script
							script.append((trgtElement.getName().substring(0, 1).toUpperCase()
									+ trgtElement.getName().substring(1)) + "(");
							script.append(srcElement.getName());

							// for ordering the operations
							operationOrder.getOperationElement().getStrings().add(srcElement.getName());
							// to add refenre object to the rifht operand
							rightOperand.appendChild(refE);
						} else
						// in case of BDM
						// DATA_OUTPUT_OBJECT_DATA_STORE or DATA_INPUT_OBJECT_DATA_STORE
						if (srcElement.getElementNode().getLocalName().equals(BPMNTypes.DATA_OUTPUT_OBJECT_DATA_STORE)
								|| srcElement.getElementNode().getLocalName()
										.equals(BPMNTypes.DATA_INPUT_OBJECT_DATA_STORE)) {

							// add the reference to the BDM object to the operand
							Element refE = doc.createElement("referencedElements");
							refE.setAttribute("dataType", variableDataTypes.get("Business_Object".toLowerCase()));
							refE.setAttribute("xmi:id", generateXmiId());
							refE.setAttribute("xmi:type", "process:BusinessObjectData");
							refE.setAttribute("name", srcElement.getName());
							refE.setAttribute("className", trgtElement.getAttribute("type"));
							// ad the script
							script.append((trgtElement.getName().substring(0, 1).toUpperCase()
									+ trgtElement.getName().substring(1)) + "(");
							script.append(srcElement.getName());

							// add ref to the right operand
							rightOperand.appendChild(refE);

							// to add reference object for ordering operations
							operationOrder.getOperationElement().getStrings().add(srcElement.getName());

						} else
						// in case of attribute of BDM
						if ((srcElement.getElementNode().getLocalName().equals(BPMNTypes.DATA_OBJECT_ATTRIBUTE)
								&& srcElement.getElementNode().getParentNode().getLocalName()
										.equals(BPMNTypes.DATA_OUTPUT_OBJECT_DATA_STORE))
								|| (srcElement.getElementNode().getLocalName().equals(BPMNTypes.DATA_OBJECT_ATTRIBUTE)
										&& srcElement.getElementNode().getParentNode().getLocalName()
												.equals(BPMNTypes.DATA_INPUT_OBJECT_DATA_STORE))) {

							// add the reference to the BDM object to the operand
							Element refE = doc.createElement("referencedElements");
							refE.setAttribute("dataType", variableDataTypes.get("Business_Object".toLowerCase()));
							refE.setAttribute("xmi:id", generateXmiId());
							refE.setAttribute("xmi:type", "process:BusinessObjectData");
							refE.setAttribute("name", srcElement.getElementNode().getParentNode().getAttributes()
									.getNamedItem("name").getNodeValue());
							refE.setAttribute("className", srcElement.getElementNode().getParentNode().getAttributes()
									.getNamedItem("type").getNodeValue());
							// add script
							script.append((trgtElement.getName().substring(0, 1).toUpperCase()
									+ trgtElement.getName().substring(1)) + "(");

							script.append(srcElement.getElementNode().getParentNode().getAttributes()
									.getNamedItem("name").getNodeValue());

							// to add refere object
							rightOperand.appendChild(refE);

							// to add reference object for ordering operations
							operationOrder.getOperationElement().getStrings().add(srcElement.getElementNode()
									.getParentNode().getAttributes().getNamedItem("name").getNodeValue());

						} else
						// in case of data processing
//						if (sourceElement.getElementNode().getLocalName().equals(BPMNTypes.DATA_PROCESSING)) {
//							String content = addOperationFromDataProcessing(doc, activity, operationOrder, rightOperand,
//									sourceElement, parentElement.getAttribute("isMultiple").equals("true"),
//									fixDataTypes, dataTypes);
//							script.append(content);
//							script.append(",\n");
//						}
//						else
						{
							// we check the case of the data from user

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
								// contract with complex type
								if (parentUserSource) {
									String contractParent = srcElement.getElementNode().getParentNode().getAttributes()
											.getNamedItem("name").getNodeValue();
									// add script
									script.append((trgtElement.getName().substring(0, 1).toUpperCase()
											+ trgtElement.getName().substring(1)) + "(");

									script.append(contractParent + ".get(\""
											+ srcElement.getElementNode().getAttribute("name") + "\")");

									if (script.toString().split(contractParent + ".get").length == 2) {
										refE.setAttribute("xmi:id", generateXmiId());
										refE.setAttribute("xmi:type", "process:ContractInput");
										refE.setAttribute("name", srcElement.getElementNode().getParentNode()
												.getAttributes().getNamedItem("name").getNodeValue());
										refE.setAttribute("type", "COMPLEX");
										// add right operand
										rightOperand.appendChild(refE);

										Element mapping = doc.createElement("mapping");
										mapping.setAttribute("xmi:id", generateXmiId());
										mapping.setAttribute("xmi:type", "process:ContractInputMapping");
										refE.appendChild(mapping);
									}
								}
								// contract with simple type
								else {
									// add script
									script.append((trgtElement.getName().substring(0, 1).toUpperCase()
											+ trgtElement.getName().substring(1)) + "(");
									script.append(srcElement.getAttribute("name"));
									refE.setAttribute("xmi:id", generateXmiId());
									refE.setAttribute("xmi:type", "process:ContractInput");
									refE.setAttribute("name", srcElement.getName());

									// add types
									if (dataTypes.containsKey(srcElement.getAttribute("type").toLowerCase())) {
										refE.setAttribute("type", dataTypes
												.get(srcElement.getAttribute("type").toLowerCase()).toUpperCase());
									} else {
										refE.setAttribute("type", dataTypes.get("string").toUpperCase());
									}

									// add right operand
									rightOperand.appendChild(refE);

									Element mapping = doc.createElement("mapping");
									mapping.setAttribute("xmi:id", generateXmiId());
									mapping.setAttribute("xmi:type", "process:ContractInputMapping");
									refE.appendChild(mapping);

								}

							}
						}

					});

					script.append(", 0, 1)[0]");

					// add the script code to the right operand
					rightOperand.setAttribute("content", script.toString());

					// add the operation type
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
				// to create new instance of business data model
				// in this case we are worked on left and right operand
				else {
					addedData.add(parentElement.getId());
					// define the attributes of the left operand
					// we need to convert some part to code
					leftOperand.setAttribute("name", parentElement.getName()); // name
					leftOperand.setAttribute("content", parentElement.getName()); // name
					leftOperand.setAttribute("type", "TYPE_VARIABLE");
					leftOperand.setAttribute("returnType", parentElement.getAttribute("type"));// class name in BDM

					// reference element to reference the DBM object from the pool data variable
					Element refElemenet = doc.createElement("referencedElements");
					refElemenet.setAttribute("xmi:id", generateXmiId());
					refElemenet.setAttribute("name", parentElement.getName()); // reference by unique name
					refElemenet.setAttribute("className", parentElement.getAttribute("type"));
					refElemenet.setAttribute("xmi:type", "process:BusinessObjectData");
					refElemenet.setAttribute("dataType", variableDataTypes.get("Business_Object".toLowerCase())); // dataTypes
					leftOperand.appendChild(refElemenet);

					// add the right operand => script
					Element rightOperand = doc.createElement("rightOperand");
					rightOperand.setAttribute("xmi:type", "expression:Expression");
					rightOperand.setAttribute("xmi:id", generateXmiId());
					rightOperand.setAttribute("name", "newScript()");
					rightOperand.setAttribute("interpreter", "GROOVY");
					rightOperand.setAttribute("type", "TYPE_READ_ONLY_SCRIPT");

					// for the code within the script
					final StringBuilder script = new StringBuilder();

					// check what we need to return
					if (parentElement.getAttribute("isMultiple").equals("true")) {
						rightOperand.setAttribute("returnType", returnDataTypes.get("list"));
						leftOperand.setAttribute("returnType", returnDataTypes.get("list"));
						BPMNElement bpmnMult = activity.getFistMultiObjectFor(parentElement);
						script.append("for(int i=0;i<");
						if (bpmnMult != null) {
							script.append(bpmnMult.getName());
						}
						script.append(".size();i++){\n");
					} else {
						rightOperand.setAttribute("returnType", parentElement.getAttribute("type"));
					}
					// start by defining the script
					script.append("new " + parentElement.getAttribute("type") + "(\n");

					// get all the data connected to the left operand => DBM instance
					activity.getConnectDataFlowTo(parentElement).stream().forEach(df -> {
						// get data source and target
						// the target should be part of the BDM
						BPMNElement srcElement = activity.findElementById(df.getSourceRef());
						BPMNElement trgtElement = activity.findElementById(df.getTargetRef());
						// check if inputs
						// in case of DATA_INPUT_OBJECT_LOCAL
						// add the data value directly to the script
						if (srcElement.getElementNode().getLocalName().equals(BPMNTypes.DATA_INPUT_OBJECT_LOCAL)) {
							script.append(trgtElement.getName() + ": '" + srcElement.getAttribute("value") + "'");
							script.append(",\n");
						} else
						// in case of process variable or a child of process variable
						// process variable => DATA_INPUT_OBJECT_PROCESS
						if (srcElement.getElementNode().getLocalName().equals(BPMNTypes.DATA_INPUT_OBJECT_PROCESS)
								|| srcElement.getElementNode().getParentNode().getLocalName()
										.equals(BPMNTypes.DATA_INPUT_OBJECT_PROCESS)) {
							// add reference for the process variable
							Element refE = doc.createElement("referencedElements");
							refE.setAttribute("dataType",
									variableDataTypes.get(trgtElement.getAttribute("type").toLowerCase()));
							refE.setAttribute("xmi:id", generateXmiId());
							refE.setAttribute("xmi:type", "process:Data");
							refE.setAttribute("name", srcElement.getName());
							// add to script
							script.append(trgtElement.getName() + ": " + srcElement.getName() + "");
							script.append(",\n");

							// for ordering the operations
							operationOrder.getOperationElement().getStrings().add(srcElement.getName());
							// to add refenre object to the rifht operand
							rightOperand.appendChild(refE);
						} else
						// in case of BDM
						// DATA_OUTPUT_OBJECT_DATA_STORE or DATA_INPUT_OBJECT_DATA_STORE
						if (srcElement.getElementNode().getLocalName().equals(BPMNTypes.DATA_OUTPUT_OBJECT_DATA_STORE)
								|| srcElement.getElementNode().getLocalName()
										.equals(BPMNTypes.DATA_INPUT_OBJECT_DATA_STORE)) {

							// add the reference to the BDM object to the operand
							Element refE = doc.createElement("referencedElements");
							refE.setAttribute("dataType", variableDataTypes.get("Business_Object".toLowerCase()));
							refE.setAttribute("xmi:id", generateXmiId());
							refE.setAttribute("xmi:type", "process:BusinessObjectData");
							refE.setAttribute("name", srcElement.getName());
							refE.setAttribute("className", trgtElement.getAttribute("type"));
							// ad the script
							script.append(trgtElement.getName() + ": " + srcElement.getName() + "");
							if (srcElement.getAttribute("isMultiple").equals("true")) {
								script.append("[i]");
							}
							script.append(",\n");

							// add ref to the right operand
							rightOperand.appendChild(refE);

							// to add reference object for ordering operations
							operationOrder.getOperationElement().getStrings().add(srcElement.getName());

						} else
						// in case of attribute of BDM
						if ((srcElement.getElementNode().getLocalName().equals(BPMNTypes.DATA_OBJECT_ATTRIBUTE)
								&& srcElement.getElementNode().getParentNode().getLocalName()
										.equals(BPMNTypes.DATA_OUTPUT_OBJECT_DATA_STORE))
								|| (srcElement.getElementNode().getLocalName().equals(BPMNTypes.DATA_OBJECT_ATTRIBUTE)
										&& srcElement.getElementNode().getParentNode().getLocalName()
												.equals(BPMNTypes.DATA_INPUT_OBJECT_DATA_STORE))) {

							// add the reference to the BDM object to the operand
							Element refE = doc.createElement("referencedElements");
							refE.setAttribute("dataType", variableDataTypes.get("Business_Object".toLowerCase()));
							refE.setAttribute("xmi:id", generateXmiId());
							refE.setAttribute("xmi:type", "process:BusinessObjectData");
							refE.setAttribute("name", srcElement.getElementNode().getParentNode().getAttributes()
									.getNamedItem("name").getNodeValue());
							refE.setAttribute("className", srcElement.getElementNode().getParentNode().getAttributes()
									.getNamedItem("type").getNodeValue());
							// add script
							script.append(trgtElement.getName() + ": " + srcElement.getElementNode().getParentNode()
									.getAttributes().getNamedItem("name").getNodeValue() + "");

							if (srcElement.getAttribute("isMultiple").equals("true")) {
								script.append("[i]");
							}

							script.append("." + srcElement.getName());
							script.append(",\n");

							// to add refere object
							rightOperand.appendChild(refE);

							// to add reference object for ordering operations
							operationOrder.getOperationElement().getStrings().add(srcElement.getElementNode()
									.getParentNode().getAttributes().getNamedItem("name").getNodeValue());

						} else
						// in case of data processing
						if (sourceElement.getElementNode().getLocalName().equals(BPMNTypes.DATA_PROCESSING)) {
							String content = addOperationFromDataProcessing(doc, activity, operationOrder, rightOperand,
									sourceElement, parentElement.getAttribute("isMultiple").equals("true"),
									variableDataTypes, dataTypes);
							script.append(content);
							script.append(",\n");
						} else {
							// we check the case of the data from user

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
								// contract with complex type
								if (parentUserSource) {
									// add script
									String contractParent = srcElement.getElementNode().getParentNode().getAttributes()
											.getNamedItem("name").getNodeValue();
									script.append(trgtElement.getAttribute("name") + ": " + contractParent + ".get(\""
											+ srcElement.getElementNode().getAttribute("name") + "\")");

									if (srcElement.getElementNode().getParentNode().getAttributes()
											.getNamedItem("isMultiple").getNodeValue().equals("true")) {
										script.append("[i]");
									}
									script.append(",\n");

									if (script.toString().split(contractParent + ".get").length == 2) {
										refE.setAttribute("xmi:id", generateXmiId());
										refE.setAttribute("xmi:type", "process:ContractInput");
										refE.setAttribute("name",contractParent);
										refE.setAttribute("type", "COMPLEX");
										// add right operand
										rightOperand.appendChild(refE);

										Element mapping = doc.createElement("mapping");
										mapping.setAttribute("xmi:id", generateXmiId());
										mapping.setAttribute("xmi:type", "process:ContractInputMapping");
										refE.appendChild(mapping);
									}
								}
								// contract with simple type
								else {
									// add script
									script.append(
											trgtElement.getAttribute("name") + ": " + srcElement.getAttribute("name"));
									if (srcElement.getElementNode().getAttribute("isMultiple").equals("true")) {
										script.append("[i]");
									}
									script.append(",\n");

									refE.setAttribute("xmi:id", generateXmiId());
									refE.setAttribute("xmi:type", "process:ContractInput");
									refE.setAttribute("name", srcElement.getName());

									// add types
									if (dataTypes.containsKey(srcElement.getAttribute("type").toLowerCase())) {
										refE.setAttribute("type", dataTypes
												.get(srcElement.getAttribute("type").toLowerCase()).toUpperCase());
									} else {
										refE.setAttribute("type", dataTypes.get("string").toUpperCase());
									}
									// add right operand
									rightOperand.appendChild(refE);

									Element mapping = doc.createElement("mapping");
									mapping.setAttribute("xmi:id", generateXmiId());
									mapping.setAttribute("xmi:type", "process:ContractInputMapping");
									refE.appendChild(mapping);
								}

							}
						}

					});
					// script finished
					script.append(")");
					if (parentElement.getAttribute("isMultiple").equals("true")) {
						script.append("}");
					}
					// add the script code to the right operand
					rightOperand.setAttribute("content", script.toString());

					// add the operation type
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

			} else
			// process variable => left operand is PV
			if (targetElement.getElementNode().getLocalName().equals(BPMNTypes.DATA_OUTPUT_OBJECT_PROCESS)
					|| targetElement.getElementNode().getParentNode().getLocalName()
							.equals(BPMNTypes.DATA_OUTPUT_OBJECT_PROCESS)) {
				// left operand = the process variable without need a scipt
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

			// business data model BDM
			if (targetElement.getElementNode().getLocalName().equals(BPMNTypes.DATA_OBJECT_ATTRIBUTE) && targetElement
					.getElementNode().getParentNode().getLocalName().equals(BPMNTypes.DATA_OUTPUT_OBJECT_DATA_STORE)) {
				BPMNElement parentElement = ((DataObjectAttributeExtension) targetElement).getDataParent();
				refElemenet.setAttribute("className", parentElement.getAttribute("type"));
				refElemenet.setAttribute("xmi:type", "process:BusinessObjectData");
				refElemenet.setAttribute("dataType", variableDataTypes.get("Business_Object".toLowerCase())); // dataTypes

			} else
			// process variable
			if (targetElement.getElementNode().getLocalName().equals(BPMNTypes.DATA_OUTPUT_OBJECT_PROCESS)
					|| targetElement.getElementNode().getParentNode().getLocalName()
							.equals(BPMNTypes.DATA_OUTPUT_OBJECT_PROCESS)) {

				refElemenet.setAttribute("xmi:type", "process:Data");
				refElemenet.setAttribute("dataType",
						variableDataTypes.get(targetElement.getAttribute("type").toLowerCase())); // dataTypes

			} else {
				return;
			}

			leftOperand.appendChild(refElemenet);

			// ------------------------------------------------------- right
			// operand---------------------------------------------------
			Element rightOperand = doc.createElement("rightOperand");
			rightOperand.setAttribute("xmi:type", "expression:Expression");
			rightOperand.setAttribute("xmi:id", generateXmiId());
			rightOperand.setAttribute("name", sourceElement.getName());
			rightOperand.setAttribute("content", sourceElement.getName());

			// static data
			if (sourceElement.getElementNode().getLocalName().equals(BPMNTypes.DATA_INPUT_OBJECT_LOCAL)) {
				rightOperand.setAttribute("name", sourceElement.getAttribute("value"));
				rightOperand.setAttribute("content", sourceElement.getAttribute("value"));

			} else
			// process variable
			if (sourceElement.getElementNode().getLocalName().equals(BPMNTypes.DATA_INPUT_OBJECT_PROCESS)
					|| sourceElement.getElementNode().getParentNode().getLocalName()
							.equals(BPMNTypes.DATA_INPUT_OBJECT_PROCESS)) {
				rightOperand.setAttribute("type", "TYPE_VARIABLE");
				if (returnDataTypes.containsKey(targetElement.getAttribute("type").toLowerCase())) {
					rightOperand.setAttribute("returnType",
							returnDataTypes.get(targetElement.getAttribute("type").toLowerCase()));
					leftOperand.setAttribute("returnType",
							returnDataTypes.get(targetElement.getAttribute("type").toLowerCase()));
				}

				refElemenet = doc.createElement("referencedElements");
				if (variableDataTypes.containsKey(sourceElement.getAttribute("type").toLowerCase()))
					refElemenet.setAttribute("dataType",
							variableDataTypes.get(sourceElement.getAttribute("type").toLowerCase()));
				else
					refElemenet.setAttribute("dataType", variableDataTypes.get("text"));
				refElemenet.setAttribute("xmi:id", generateXmiId());
				refElemenet.setAttribute("xmi:type", "process:Data");
				refElemenet.setAttribute("name", sourceElement.getName());

				rightOperand.appendChild(refElemenet);
				// add refereable object
				operationOrder.getOperationElement().getStrings().add(sourceElement.getName());

			} else
			// data processing
			if (sourceElement.getElementNode().getLocalName().equals(BPMNTypes.DATA_PROCESSING)) {
				rightOperand.setAttribute("type", "TYPE_READ_ONLY_SCRIPT");
				rightOperand.setAttribute("interpreter", "GROOVY");
				rightOperand.setAttribute("name", "newScript()");
				if (returnDataTypes.containsKey(targetElement.getAttribute("type").toLowerCase())) {
					rightOperand.setAttribute("returnType",
							returnDataTypes.get(targetElement.getAttribute("type").toLowerCase()));
					leftOperand.setAttribute("returnType",
							returnDataTypes.get(targetElement.getAttribute("type").toLowerCase()));
				}

				String script = addOperationFromDataProcessing(doc, activity, operationOrder, rightOperand,
						sourceElement, false, variableDataTypes, dataTypes);
				rightOperand.setAttribute("content", script);
			} else {
				// user data
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

						if (returnDataTypes
								.containsKey(sourceElement.getElementNode().getAttribute("type").toLowerCase())) {
							rightOperand.setAttribute("returnType", returnDataTypes
									.get(sourceElement.getElementNode().getAttribute("type").toLowerCase()));
							leftOperand.setAttribute("returnType", returnDataTypes
									.get(sourceElement.getElementNode().getAttribute("type").toLowerCase()));
						}

						refElemenet.setAttribute("xmi:id", generateXmiId());
						refElemenet.setAttribute("xmi:type", "process:ContractInput");
						refElemenet.setAttribute("name", sourceElement.getElementNode().getParentNode().getAttributes()
								.getNamedItem("name").getNodeValue());
						refElemenet.setAttribute("type", "COMPLEX");
						// add right operand
						rightOperand.appendChild(refElemenet);

						Element mapping = doc.createElement("mapping");
						mapping.setAttribute("xmi:id", generateXmiId());
						mapping.setAttribute("xmi:type", "process:ContractInputMapping");
						refElemenet.appendChild(mapping);

					} else {
						rightOperand.setAttribute("type", "TYPE_CONTRACT_INPUT");

						if (returnDataTypes.containsKey(sourceElement.getAttribute("type").toLowerCase())) {
							rightOperand.setAttribute("returnType",
									returnDataTypes.get(sourceElement.getAttribute("type").toLowerCase()));
							leftOperand.setAttribute("returnType",
									returnDataTypes.get(sourceElement.getAttribute("type").toLowerCase()));
						}

						refElemenet.setAttribute("xmi:id", generateXmiId());
						refElemenet.setAttribute("xmi:type", "process:ContractInput");
						refElemenet.setAttribute("name", targetElement.getName());

						if (dataTypes.containsKey(sourceElement.getAttribute("type").toLowerCase()))
							refElemenet.setAttribute("type",
									dataTypes.get(sourceElement.getAttribute("type").toLowerCase()).toUpperCase());
						rightOperand.appendChild(refElemenet);

						Element mapping = doc.createElement("mapping");
						mapping.setAttribute("xmi:id", generateXmiId());
						mapping.setAttribute("xmi:type", "process:ContractInputMapping");
						refElemenet.appendChild(mapping);
					}

				}
			}

			// -------------------------------------- operator
			// -----------------------------------------
			Element operator = doc.createElement("operator");
			operator.setAttribute("xmi:type", "expression:Operator");
			operator.setAttribute("xmi:id", generateXmiId());

			// business data model
			if (targetElement.getElementNode().getLocalName().equals(BPMNTypes.DATA_OBJECT_ATTRIBUTE) && targetElement
					.getElementNode().getParentNode().getLocalName().equals(BPMNTypes.DATA_OUTPUT_OBJECT_DATA_STORE)) {
				operator.setAttribute("expression", "set" + targetElement.getName().substring(0, 1).toUpperCase()
						+ targetElement.getName().substring(1)); // name
				operator.setAttribute("type", "JAVA_METHOD");

				Element inputTypes = doc.createElement("inputTypes");
				inputTypes.setTextContent(returnDataTypes.get(targetElement.getAttribute("type").toLowerCase()));
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
			// order the operations
			orderOperation(operationsSet, operationOrder);

		});

		// to treat the data need to be deleted from the database
		activity.getAllDeleteObjectData().stream().forEach(dataObject -> {
			if (addedData.contains(dataObject.getId())) {
				return;
			}

			Element operations = doc.createElement("operations");
			operations.setAttribute("xmi:type", "expression:Operation");
			operations.setAttribute("xmi:id", generateXmiId());

			OperationOrder operationOrder;

			OperationElement inOperation = new OperationElement(operations, new ArrayList<>());

			// ----------------- left operand element --------------------
			Element leftOperand = doc.createElement("leftOperand");
			leftOperand.setAttribute("xmi:type", "expression:Expression");
			leftOperand.setAttribute("xmi:id", generateXmiId());

			addedData.add(dataObject.getId());
			leftOperand.setAttribute("name", dataObject.getName()); // name
			leftOperand.setAttribute("content", dataObject.getName()); // name
			leftOperand.setAttribute("type", "TYPE_VARIABLE");
			leftOperand.setAttribute("returnType", dataObject.getAttribute("type"));// class name in BDM
			// root operation element

			// reference element for left Operand
			Element refElemenet = doc.createElement("referencedElements");
			refElemenet.setAttribute("xmi:id", generateXmiId());
			refElemenet.setAttribute("name", dataObject.getName()); // reference by unique name
			refElemenet.setAttribute("className", dataObject.getAttribute("type"));
			refElemenet.setAttribute("xmi:type", "process:BusinessObjectData");
			refElemenet.setAttribute("dataType", variableDataTypes.get("Business_Object".toLowerCase())); // dataTypes
			leftOperand.appendChild(refElemenet);

			// ----------------------------- right operand ----------------------------
			Element rightOperand = doc.createElement("rightOperand");
			rightOperand.setAttribute("xmi:type", "expression:Expression");
			rightOperand.setAttribute("xmi:id", generateXmiId());
			rightOperand.setAttribute("content", "");

			// -------------------------operation ----------------------------------
			Element operator = doc.createElement("operator");
			operator.setAttribute("xmi:type", "expression:Operator");
			operator.setAttribute("xmi:id", generateXmiId());
			operator.setAttribute("type", "DELETION"); // delete to remove from bdm

			operationOrder = new OperationOrder(dataObject.getName(), inOperation);
			operationOrder.getOperationElement().getStrings().add(dataObject.getName());

			operations.appendChild(leftOperand);
			operations.appendChild(rightOperand);
			operations.appendChild(operator);
			// for ordering operations
			orderOperation(operationsSet, operationOrder);
		});

		// order the operations
		operationsSet.getOperationOrders()
				.forEach(operation -> activityElement.appendChild(operation.getOperationElement().getElement()));

	}

	/**
	 * This function to convert BDD to groovy script. BDD => Gherkin langauge
	 * 
	 * @param doc
	 * @param activity
	 * @param operationOrder
	 * @param rightOperand
	 * @param sourceElement
	 * @param isMultiple
	 * @param fixDataTypes
	 * @param dataTypes
	 * @return
	 */
	String addOperationFromDataProcessing(Document doc, Activity activity, OperationOrder operationOrder,
			Element rightOperand, BPMNElement sourceElement, boolean isMultiple, Map<String, String> fixDataTypes,
			Map<String, String> dataTypes) {
		StringBuilder script = new StringBuilder("");
		List<String> inputs = new ArrayList<>();
		// get all the incoming data
		List<BPMNElement> listIncoming = activity.getDataProcessingIncoming(sourceElement.getId());
		Iterator<BPMNElement> iterator = listIncoming.iterator();

		while (iterator.hasNext()) {
			BPMNElement element = iterator.next();

			// if the data are local data
			// we add the value
			if (element.getElementNode().getLocalName().equals(BPMNTypes.DATA_INPUT_OBJECT_LOCAL)) {
				script.append("'" + element.getAttribute("value") + "'");
			} else
			// in case of process data (input or output)
			// we add a reference to it
			if (element.getElementNode().getLocalName().equals(BPMNTypes.DATA_INPUT_OBJECT_PROCESS)
					|| element.getElementNode().getParentNode().getLocalName()
							.equals(BPMNTypes.DATA_INPUT_OBJECT_PROCESS)
					|| element.getElementNode().getLocalName().equals(BPMNTypes.DATA_OUTPUT_OBJECT_PROCESS)
					|| element.getElementNode().getParentNode().getLocalName()
							.equals(BPMNTypes.DATA_OUTPUT_OBJECT_PROCESS)) {
				Element refE = doc.createElement("referencedElements");
				refE.setAttribute("dataType", fixDataTypes.get(element.getAttribute("type").toLowerCase()));
				refE.setAttribute("xmi:id", generateXmiId());
				refE.setAttribute("xmi:type", "process:Data");
				refE.setAttribute("name", element.getName());
				inputs.add(element.getName());
				script.append(element.getName());
				operationOrder.getOperationElement().getStrings().add(element.getName());

				rightOperand.appendChild(refE);
			} else
			// in case of BDM
			// we add reference to it
			if (element.getElementNode().getLocalName().equals(BPMNTypes.DATA_OUTPUT_OBJECT_DATA_STORE)
					|| element.getElementNode().getLocalName().equals(BPMNTypes.DATA_INPUT_OBJECT_DATA_STORE)) {

				Element refE = doc.createElement("referencedElements");
				refE.setAttribute("dataType", fixDataTypes.get("Business_Object".toLowerCase()));
				refE.setAttribute("xmi:id", generateXmiId());
				refE.setAttribute("xmi:type", "process:BusinessObjectData");
				refE.setAttribute("name", element.getName());
				refE.setAttribute("className", element.getAttribute("type"));
				inputs.add(element.getName());
				script.append(element.getName());

				rightOperand.appendChild(refE);

				// to add refere object
				operationOrder.getOperationElement().getStrings().add(element.getName());
			} else
			// in case of BDM ( attributes)
			if ((element.getElementNode().getLocalName().equals(BPMNTypes.DATA_OBJECT_ATTRIBUTE) && element
					.getElementNode().getParentNode().getLocalName().equals(BPMNTypes.DATA_OUTPUT_OBJECT_DATA_STORE))
					|| (element.getElementNode().getLocalName().equals(BPMNTypes.DATA_OBJECT_ATTRIBUTE)
							&& element.getElementNode().getParentNode().getLocalName()
									.equals(BPMNTypes.DATA_INPUT_OBJECT_DATA_STORE))) {

				Element refE = doc.createElement("referencedElements");
				refE.setAttribute("dataType", fixDataTypes.get("Business_Object".toLowerCase()));
				refE.setAttribute("xmi:id", generateXmiId());
				refE.setAttribute("xmi:type", "process:BusinessObjectData");
				refE.setAttribute("name",
						element.getElementNode().getParentNode().getAttributes().getNamedItem("name").getNodeValue());
				refE.setAttribute("className",
						element.getElementNode().getParentNode().getAttributes().getNamedItem("type").getNodeValue());
				script.append(
						element.getElementNode().getParentNode().getAttributes().getNamedItem("name").getNodeValue());

				if (isMultiple && element.getAttribute("isMultiple").equals("true")) {
					script.append("[i]");
				}
				inputs.add(element.getElementNode().getParentNode().getAttributes().getNamedItem("name").getNodeValue()
						+ "." + element.getName());
				isMultiple = false;
				script.append("." + element.getName());
				rightOperand.appendChild(refE);

				// to add refere object
				operationOrder.getOperationElement().getStrings().add(
						element.getElementNode().getParentNode().getAttributes().getNamedItem("name").getNodeValue());
			} else {
				// in case of data from the user
				boolean userSource = (element.getElementNode().getLocalName()
						.equals(BPMNTypes.DATA_INPUT_OBJECT_ENVIRONMENT_DATA_USER)
						|| element.getElementNode().getLocalName().equals(BPMNTypes.DATA_INPUT_OBJECT_DEPENDENCY));

				boolean parentUserSource = (element.getElementNode().getParentNode().getLocalName()
						.equals(BPMNTypes.DATA_INPUT_OBJECT_ENVIRONMENT_DATA_USER)
						|| element.getElementNode().getParentNode().getLocalName()
								.equals(BPMNTypes.DATA_INPUT_OBJECT_DEPENDENCY));

				// data from user (found a contract)
				if (userSource || (parentUserSource
						&& element.getElementNode().getLocalName().equals(BPMNTypes.DATA_OBJECT_ATTRIBUTE))) {

					Element refE = doc.createElement("referencedElements");
					if (parentUserSource) {
						String contractParent = element.getElementNode().getParentNode().getAttributes().getNamedItem("name")
								.getNodeValue() ;
						inputs.add(contractParent+ ".get(\"" + element.getElementNode().getAttribute("name") + "\")");
						script.append(contractParent + ".get(\"" + element.getElementNode().getAttribute("name") + "\")");
						
						
						if (script.toString().split(contractParent + ".get").length == 2) {
							refE.setAttribute("xmi:id", generateXmiId());
							refE.setAttribute("xmi:type", "process:ContractInput");
							refE.setAttribute("name", contractParent);
							refE.setAttribute("type", "COMPLEX");
							// add right operand
							rightOperand.appendChild(refE);

							Element mapping = doc.createElement("mapping");
							mapping.setAttribute("xmi:id", generateXmiId());
							mapping.setAttribute("xmi:type", "process:ContractInputMapping");
							refE.appendChild(mapping);
						}
					} else {
						inputs.add(element.getAttribute("name"));
						script.append(element.getAttribute("name"));

						refE.setAttribute("xmi:id", generateXmiId());
						refE.setAttribute("xmi:type", "process:ContractInput");
						refE.setAttribute("name", element.getName());
						if (dataTypes.containsKey(element.getAttribute("type").toLowerCase()))
							refE.setAttribute("type",
									dataTypes.get(element.getAttribute("type").toLowerCase()).toUpperCase());
						rightOperand.appendChild(refE);

						Element mapping = doc.createElement("mapping");
						mapping.setAttribute("xmi:id", generateXmiId());
						mapping.setAttribute("xmi:type", "process:ContractInputMapping");
						refE.appendChild(mapping);
					}
				}
			}

			if (isMultiple && element.getAttribute("isMultiple").equals("true")) {
				script.append("[i]");
			}

			if (iterator.hasNext()) {
				script.append(" + ");
			}
		}
		List<String> output = activity.getDataProcessingOutgoing(sourceElement.getId()).stream()
				.map((element) -> element.getName()).collect(Collectors.toList());
		try {
			// get data result by connecting to LLM
			// convert gherkin to code
			String results = sendPost(inputs.toString(), sourceElement.getAttribute("gherkin"), output.get(0));
			return results;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return script.toString();
	}

	/**
	 * This function to connect to the LLM API to convert natural language to groovy
	 * script.
	 * 
	 * @param inputsValue
	 * @param descriptionValue
	 * @param outputValue
	 * @return
	 * @throws Exception
	 */
	// HTTP POST request
	private String sendPost(String inputsValue, String descriptionValue, String outputValue) throws Exception {
		// server url
		String url = "http://localhost:3001/groovy";
		URL obj = new URL(url);
		HttpURLConnection connection = (HttpURLConnection) obj.openConnection();

		// add request header
		connection.setRequestMethod("POST");

		// Set Do Output to true if you want to use URLConnection for output.
		connection.setDoOutput(true);

		String urlParameters = "inputs=" + inputsValue + "&description=" + descriptionValue + "&output=" + outputValue;
		byte[] postData = urlParameters.getBytes(StandardCharsets.UTF_8);

		try (OutputStream wr = connection.getOutputStream()) {
			wr.write(postData);
		}

		if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
			// Success

			BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine + "\n");
			}
			in.close();

			return response.toString();
		} else {
			// Error handling code goes here
			System.out.println("Request failed");
		}

		return "CONNECTION FAILED";

	}

	/**
	 * This function is used to order the data operation within the activity based
	 * on their needs and their connectivity .
	 * 
	 * @param operationsSet
	 * @param operationOrder
	 */
	void orderOperation(OperationSet operationsSet, OperationOrder operationOrder) {
		AtomicInteger index = new AtomicInteger(-1);
		operationOrder.getOperationElement().getStrings().forEach(objectName -> {
			List<OperationOrder> tempFilter = operationsSet.getOperationOrders().stream()
					.filter(data -> data.getName().equals(objectName)).collect(Collectors.toList());
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

	/**
	 * This function to add the activity diagram to the lane or pool diagram (as
	 * children)
	 * 
	 * @param doc
	 * @param parentElement
	 * @param elementID
	 * @param x
	 * @param y
	 * @param type
	 * @param isLane
	 * @return
	 * @throws XPathExpressionException
	 */
	Element addDiagramForActivity(Document doc, Map<String, Element> parentElement, String elementID, String x,
			String y, ActivityType type, boolean isLane) throws XPathExpressionException {

		// Create the <children> element used to be added within the diagram of lane
		Element childrenElement = doc.createElement("children");
		childrenElement.setAttribute("xmi:type", "notation:Shape");
		childrenElement.setAttribute("xmi:id", generateXmiId());
		if (type == ActivityType.HUMAN) {
			childrenElement.setAttribute("type", "3005"); // code of human activity
		} else {
			childrenElement.setAttribute("type", "3027"); // code type of the service activity
		}

		childrenElement.setAttribute("element", elementID);
		// static inputs can be modified within bonita studio
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
			// add to the lane or to the pool
			diagram.appendChild(childrenElement);
		}

		// for the design of the activity
		// Create the inner <children> element
		Element innerChildrenElement = doc.createElement("children");
		innerChildrenElement.setAttribute("xmi:type", "notation:DecorationNode");
		innerChildrenElement.setAttribute("xmi:id", generateXmiId());
		if (type == ActivityType.HUMAN) {
			innerChildrenElement.setAttribute("type", "5005");// code of human activity
		} else {
			innerChildrenElement.setAttribute("type", "5017");// code type of the service activity
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

	/**
	 * Convert event from BPMN to Proc (Bonita extension).
	 * 
	 * @param parentElement
	 * @param doc
	 * @param event
	 * @param x
	 * @param y
	 * @param type
	 * @param isLane
	 * @return
	 * @throws XPathExpressionException
	 */
	private Element addEventToProc(Map<String, Element> parentElement, Document doc, Event event, String x, String y,
			EventType type, boolean isLane) throws XPathExpressionException {

		// Create the <elements> element
		Element elementsEvent = doc.createElement("elements");

		// validate the event type
		if (type == EventType.START)
			elementsEvent.setAttribute("xmi:type", "process:StartEvent");
		else if (type == EventType.END)
			elementsEvent.setAttribute("xmi:type", "process:EndEvent");

		elementsEvent.setAttribute("xmi:id", event.getId());
		elementsEvent.setAttribute("name", event.getName());

		// add to the process
		parentElement.get(PROCESS).appendChild(elementsEvent);

		// some elements needed within proc
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

		// add the event to diagram
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

	/**
	 * This function to add the event element to the diagram to be rendered within
	 * Bonita studio.
	 * 
	 * @param doc
	 * @param parentElement
	 * @param elementID
	 * @param x
	 * @param y
	 * @param type
	 * @param isLane
	 * @return
	 * @throws XPathExpressionException
	 */
	Element addDiagramForEvent(Document doc, Map<String, Element> parentElement, String elementID, String x, String y,
			EventType type, boolean isLane) throws XPathExpressionException {

		// Create the <children> element for the lane (in the diagram parts)
		Element childrenElement = doc.createElement("children");
		childrenElement.setAttribute("xmi:type", "notation:Shape");
		childrenElement.setAttribute("xmi:id", generateXmiId());
		if (type == EventType.START)
			// code of start event within Bonita
			childrenElement.setAttribute("type", "3002");
		else if (type == EventType.END)
			// code of end event within Bonita
			childrenElement.setAttribute("type", "3003");

		childrenElement.setAttribute("element", elementID);
		childrenElement.setAttribute("fontName", "Segoe UI"); // static font can be modified within bonita studio

		// get lane child ref with type 7002/ pool 7001
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
			// add within the diagram (child of lane of pool)
			diagram.appendChild(childrenElement);
		}

		// Child element => event element design
		Element child = doc.createElement("children");
		child.setAttribute("xmi:type", "notation:DecorationNode");
		child.setAttribute("xmi:id", generateXmiId());
		if (type == EventType.START)
			child.setAttribute("type", "5024"); // code within bonita for the diagram of start event
		else if (type == EventType.END)
			child.setAttribute("type", "5025");// code within bonita for the diagram of end event
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

	/**
	 * This function to add the sequence flow between the activities, gateways,
	 * events, and each element of Lane => converting from BPMN to process file
	 * 
	 * @param pool
	 * @param diagram
	 * @param doc
	 * @param sequenceFlow
	 * @param x
	 * @param y
	 * @return
	 * @throws XPathExpressionException
	 */
	private Element addSequenceFlowToProc(Map<String, Element> pool, Node diagram, Document doc,
			SequenceFlow sequenceFlow, String x, String y) throws XPathExpressionException {

		// Create the <elements> element
		// Create root element to add the sequence between the connections between the
		// elements
		Element sequenceFlowElement = doc.createElement("connections");
		sequenceFlowElement.setAttribute("xmi:type", "process:SequenceFlow");
		sequenceFlowElement.setAttribute("xmi:id", sequenceFlow.getId());
		sequenceFlowElement.setAttribute("target", sequenceFlow.getTargetRef());
		sequenceFlowElement.setAttribute("source", sequenceFlow.getSourceRef());
		pool.get(PROCESS).appendChild(sequenceFlowElement);

		// Create decisionTable element - predefined (used within the studio)
		Element decisionTable = doc.createElement("decisionTable");
		decisionTable.setAttribute("xmi:type", "decision:DecisionTable");
		decisionTable.setAttribute("xmi:id", generateXmiId());
		sequenceFlowElement.appendChild(decisionTable);

		// Create condition element - predefined (used within the studio)
		Element condition = doc.createElement("condition");
		condition.setAttribute("xmi:type", "expression:Expression");
		condition.setAttribute("xmi:id", generateXmiId());
		condition.setAttribute("name", "");
		condition.setAttribute("returnType", "java.lang.Boolean");
		condition.setAttribute("returnTypeFixed", "true");
		sequenceFlowElement.appendChild(condition);

		// add to diagram
		addDiagramForSequenceFlow(doc, diagram, sequenceFlow, x, y);
		return sequenceFlowElement;
	}

	/**
	 * This function to add the sequence flow to the proc diagram
	 * 
	 * @param doc
	 * @param diagram
	 * @param squenceFlow
	 * @param x
	 * @param y
	 * @return
	 * @throws XPathExpressionException
	 */
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

		/// Create root element edges
		Element childrenElement = doc.createElement("edges");
		childrenElement.setAttribute("xmi:type", "notation:Connector");
		childrenElement.setAttribute("xmi:id", generateXmiId());
		childrenElement.setAttribute("type", "4001"); // fix value
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
		children.setAttribute("type", "6001");// fix value
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

	/**
	 * This function to convert the gateway to proc.
	 * 
	 * @param parentElement
	 * @param doc
	 * @param gateway
	 * @param x
	 * @param y
	 * @param type
	 * @param isLane
	 * @return
	 * @throws XPathExpressionException
	 */
	private Element addGatewayToProc(Map<String, Element> parentElement, Document doc, Gateway gateway, String x,
			String y, GatewayType type, boolean isLane) throws XPathExpressionException {

		// Create the <elements> element => gateway element
		Element elementsEvent = doc.createElement("elements");

		// gateway type
		if (type == GatewayType.AND)
			elementsEvent.setAttribute("xmi:type", "process:ANDGateway");
		else if (type == GatewayType.XOR)
			elementsEvent.setAttribute("xmi:type", "process:XORGateway");
		else if (type == GatewayType.INCLUSIVE)
			elementsEvent.setAttribute("xmi:type", "process:InclusiveGateway");

		elementsEvent.setAttribute("xmi:id", gateway.getId());
		elementsEvent.setAttribute("name", gateway.getName());

		parentElement.get(PROCESS).appendChild(elementsEvent);

		// dynamicLabel element -> predefine variable
		Element dynamicLabel = doc.createElement("dynamicLabel");
		elementsEvent.appendChild(dynamicLabel);

		// set attribute to dynamicLabel element -> predefine variable
		dynamicLabel.setAttribute("xmi:type", "expression:Expression");
		dynamicLabel.setAttribute("xmi:id", generateXmiId());
		dynamicLabel.setAttribute("name", "");
		dynamicLabel.setAttribute("content", "");
		dynamicLabel.setAttribute("returnTypeFixed", "true");

		// dynamicDescription element -> predefine variable
		Element dynamicDescription = doc.createElement("dynamicLabel");
		elementsEvent.appendChild(dynamicDescription);

		// set attribute to dynamicLabel element -> predefine variable
		dynamicDescription.setAttribute("xmi:type", "expression:Expression");
		dynamicDescription.setAttribute("xmi:id", generateXmiId());
		dynamicDescription.setAttribute("name", "");
		dynamicDescription.setAttribute("content", "");
		dynamicDescription.setAttribute("returnTypeFixed", "true");

		// dynamicLabel element -> predefine variable
		Element stepSummary = doc.createElement("dynamicLabel");
		elementsEvent.appendChild(stepSummary);

		// set attribute to dynamicLabel element -> predefine variable
		stepSummary.setAttribute("xmi:type", "expression:Expression");
		stepSummary.setAttribute("xmi:id", generateXmiId());
		stepSummary.setAttribute("name", "");
		stepSummary.setAttribute("content", "");
		stepSummary.setAttribute("returnTypeFixed", "true");

		// add to diagram
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

	/**
	 * This function is used to add the gateway to the diagram.
	 * 
	 * @param doc
	 * @param parentElement
	 * @param elementID
	 * @param x
	 * @param y
	 * @param type
	 * @param isLane
	 * @return
	 * @throws XPathExpressionException
	 */
	Element addDiagramForGateway(Document doc, Map<String, Element> parentElement, String elementID, String x, String y,
			GatewayType type, boolean isLane) throws XPathExpressionException {

		// Create the <children> element -> child gateway element of diagram
		Element childrenElement = doc.createElement("children");
		childrenElement.setAttribute("xmi:type", "notation:Shape");
		childrenElement.setAttribute("xmi:id", generateXmiId());
		// gateway type
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
			// add to lane or pool
			diagram.appendChild(childrenElement);
		}

		// Child element -> rendering the design of the data
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

		// Child's child element -> predefined for the layout
		Element layoutConstraint = doc.createElement("layoutConstraint");
		layoutConstraint.setAttribute("xmi:type", "notation:Location");
		layoutConstraint.setAttribute("xmi:id", generateXmiId());
		layoutConstraint.setAttribute("y", "5");
		child.appendChild(layoutConstraint);

		// Root's child element -> predefined for the layout
		Element layoutConstraintRoot = doc.createElement("layoutConstraint");
		layoutConstraintRoot.setAttribute("xmi:type", "notation:Bounds");
		layoutConstraintRoot.setAttribute("xmi:id", generateXmiId());
		layoutConstraintRoot.setAttribute("x", x);
		layoutConstraintRoot.setAttribute("y", y);
		childrenElement.appendChild(layoutConstraintRoot);

		return childrenElement;
	}

	/**
	 * This function to create the proc file => converting doc to XML file
	 */
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

	/**
	 * These classes are used for the operations to well access and order the
	 * operations data
	 * 
	 * @author Ali Nour Eldin
	 *
	 */
	public class OperationElement {

		OperationElement(Element element, List<String> strings) {
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
		OperationOrder(String name, OperationElement operationElement) {
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
		OperationSet(List<OperationOrder> operationOrders) {
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
