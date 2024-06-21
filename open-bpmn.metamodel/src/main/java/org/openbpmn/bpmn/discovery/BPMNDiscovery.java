package org.openbpmn.bpmn.discovery;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.logging.log4j.core.filter.RegexFilter;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.AllDirectedPaths;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.DirectedWeightedPseudograph;
import org.openbpmn.bpmn.BPMNModel;
import org.openbpmn.bpmn.BPMNTypes;
import org.openbpmn.bpmn.discovery.model.DependencyGraph;
import org.openbpmn.bpmn.discovery.model.DepthFirstSearch;
import org.openbpmn.bpmn.discovery.model.Pair;
import org.openbpmn.bpmn.elements.BPMNProcess;
import org.openbpmn.bpmn.elements.Gateway;
import org.openbpmn.bpmn.elements.SequenceFlow;
import org.openbpmn.bpmn.elements.core.BPMNElement;
import org.openbpmn.bpmn.elements.core.BPMNElementNode;
import org.openbpmn.bpmn.exceptions.BPMNInvalidReferenceException;
import org.openbpmn.bpmn.exceptions.BPMNInvalidTypeException;
import org.openbpmn.bpmn.exceptions.BPMNMissingElementException;
import org.openbpmn.bpmn.exceptions.BPMNModelException;
import org.openbpmn.bpmn.util.BPMNModelFactory;

import io.process.analytics.tools.bpmn.generator.BPMNLayoutGenerator;
import io.process.analytics.tools.bpmn.generator.BPMNLayoutGenerator.ExportType;
import io.process.analytics.tools.bpmn.generator.internal.FileUtils;

// mvn compile assembly:single
// java -cp open-bpmn.metamodel-1.0.0-SNAPSHOT-jar-with-dependencies.jar;lib\* org.openbpmn.bpmn.discovery.BPMNDiscovery test/test.bpmn

public class BPMNDiscovery {
	private static Logger logger = Logger.getLogger(BPMNDiscovery.class.getName());
	public static String PARALLEL = "parallel";
	public static String DECISION = "decision";

	static String GATEWAY_TYPE = "gatewayType";
	static String GATEWAY_NUM = "num";
	public BPMNModel model;
	public List<String> startsEvent;
	public List<String> endsEvent;
	public LinkedList<String> dependencies;
	public Map<String, LinkedList<LinkedList<String>>> relations;
	public LinkedList<Pair<List<String>, List<String>>> loops;
	public DependencyGraph dependenciesGraph = new DependencyGraph();
	private Integer seqenceFlowId = 0;
	private Integer gatewayId = 0;
	private Gateway splitGateway = null;
	private Boolean isAdded = false;

	public BPMNDiscovery(DependencyGraph dependenciesGraph) {
		logger.info("...Creating BPMN stated");
		model = BPMNModelFactory.createInstance("demo", "1.0.0", "http://org.openbpmn");

		this.dependencies = (LinkedList<String>) dependenciesGraph.getDependenciesDFA();

		this.startsEvent = dependenciesGraph.startActivities;
		this.endsEvent = dependenciesGraph.endActivities;

		DependencyGraph.regex(startsEvent);
		DependencyGraph.regex(endsEvent);

		this.dependenciesGraph = dependenciesGraph;
		this.loops = dependenciesGraph.getLoops();
//		this.dependenciesGraph.changeVertexNameToRegex();
		LinkedList<LinkedList<String>> decisionRelations = dependenciesGraph.getDecisions();
		LinkedList<LinkedList<String>> parallelRelations = dependenciesGraph.getParallelims();

		this.relations = new LinkedHashMap<>();
		this.relations.put(BPMNDiscovery.DECISION, decisionRelations);
		this.relations.put(BPMNDiscovery.PARALLEL, parallelRelations);

		relations.values().forEach(elements -> {
			elements.forEach(innerList -> {
				DependencyGraph.regex(innerList);
			});
		});

		Iterator<Pair<List<String>, List<String>>> loopIter = loops.iterator();
		while (loopIter.hasNext()) {
			Pair<List<String>, List<String>> elements = loopIter.next();
			DependencyGraph.regex(elements.getSource());
			DependencyGraph.regex(elements.getTarget());
		}

	}

	public BPMNDiscovery(List<String> startsEvent, List<String> endsEvent, LinkedList<String> dependencies,
			Map<String, LinkedList<LinkedList<String>>> relations, LinkedList<Pair<List<String>, List<String>>> loops) {
		logger.info("...creating new empty model");
		model = BPMNModelFactory.createInstance("demo", "1.0.0", "http://org.openbpmn");

		this.startsEvent = startsEvent;
		this.endsEvent = endsEvent;
		this.dependencies = DepthFirstSearch.DFSToList(dependencies, startsEvent.get(0));
		this.relations = relations;
		this.loops = loops;

		DependencyGraph.regex(startsEvent);
		DependencyGraph.regex(endsEvent);
		relations.values().forEach(elements -> {
			elements.forEach(innerList -> {
				DependencyGraph.regex(innerList);
			});
		});
		loops.forEach(elements -> {
			DependencyGraph.regex(elements.getSource());
			DependencyGraph.regex(elements.getTarget());
		});

		for (String dependency : dependencies) {
			// Split the dependency string into source and target
			String[] parts = dependency.split("->");

			// Extract source and target
			String sourceId = DependencyGraph.regex(parts[0]);
			String targetId = DependencyGraph.regex(parts[1]);
			dependenciesGraph.addVertex(sourceId);
			dependenciesGraph.addVertex(targetId);
			dependenciesGraph.addEdge(sourceId, targetId);
		}
		dependenciesGraph.sortLoop(this.loops);
	}

	public String getBPMNElementType(String type) {
		if (type.contentEquals("end")) {
			return BPMNTypes.END_EVENT;
		} else if (type.contentEquals("start")) {
			return BPMNTypes.START_EVENT;
		} else if (type.contentEquals("human")) {
			return BPMNTypes.USER_TASK;
		} else if (type.contentEquals("service")) {
			return BPMNTypes.SERVICE_TASK;
		} else {
			return BPMNTypes.TASK;
		}
	}

	public BPMNDiscovery(List<String> startsEvent, List<String> endsEvent, LinkedList<String> dependencies,
			Map<String, LinkedList<LinkedList<String>>> relations) {
		logger.info("...creating new empty model");
		model = BPMNModelFactory.createInstance("demo", "1.0.0", "http://org.openbpmn");

		this.startsEvent = startsEvent;
		this.endsEvent = endsEvent;

		this.relations = relations;

		DependencyGraph.regex(startsEvent);
		DependencyGraph.regex(endsEvent);
		relations.values().forEach(elements -> {
			elements.forEach(innerList -> {
				DependencyGraph.regex(innerList);
			});
		});

		for (String dependency : dependencies) {
			// Split the dependency string into source and target
			String[] parts = dependency.split("->");

			// Extract source and target
			String sourceId = DependencyGraph.regex(parts[0]);
			String targetId = DependencyGraph.regex(parts[1]);
			this.dependenciesGraph.addVertex(sourceId);
			this.dependenciesGraph.addVertex(targetId);
			this.dependenciesGraph.addEdge(sourceId, targetId);
		}

		this.dependenciesGraph.findAndRemoveLoops();
		this.loops = this.dependenciesGraph.getLoops();

		this.dependencies = DepthFirstSearch.DFSToList(dependencies, startsEvent.get(0));
	}

	/**
	 * This function is to convert Dependency graph to BPMN
	 * 
	 * @param startsEvent
	 * @param endsEvent
	 * @param dependencies the relation between pair of element
	 * @param relations    it contains the decision and parallel relations as list
	 * @throws BPMNModelException
	 * @throws CloneNotSupportedException
	 */
	public void DependencyGraphToBPMN() throws BPMNModelException, CloneNotSupportedException {

		BPMNProcess process = model.openDefaultProces();

		// it is used for the sequence flow
		for (String dependency : dependencies) {
			// Split the dependency string into source and target
			String[] parts = dependency.split("->");

			// Extract source and target

			String sourceId = parts[0];
			String targetId = parts[1];
			String source = dependenciesGraph.elementsName.get(sourceId);
			String target = dependenciesGraph.elementsName.get(targetId);
			boolean isNewTarget = false;
			boolean isNewSource = false;
			BPMNElementNode sourceElement = (BPMNElementNode) process.findElementById(sourceId);
			BPMNElementNode targetElement = (BPMNElementNode) process.findElementById(targetId);

			// element source is not added to the process mode
			if (sourceElement == null) {
				isNewSource = true;
				String elementType = getBPMNElementType("");
				if (dependenciesGraph.elementInformations.containsKey(sourceId)
						&& dependenciesGraph.elementInformations.get(sourceId).containsKey("type")) {
					elementType = getBPMNElementType(dependenciesGraph.elementInformations.get(sourceId).get("type"));
				}

				// add new event
				if (BPMNTypes.BPMN_EVENTS.contains(elementType)) {
					sourceElement = process.addEvent(sourceId, source, elementType);
				}
				// add new activity
				else {
					sourceElement = process.addTask(sourceId, source, elementType);
				}
			}

			// element target is not added to the process mode
			if (targetElement == null) {
				isNewTarget = true;
				String elementType = getBPMNElementType("");
				if (dependenciesGraph.elementInformations.containsKey(targetId)
						&& dependenciesGraph.elementInformations.get(targetId).containsKey("type")) {
					elementType = getBPMNElementType(dependenciesGraph.elementInformations.get(targetId).get("type"));
				}

				// add new event
				if (BPMNTypes.BPMN_EVENTS.contains(elementType)) {
					targetElement = process.addEvent(targetId, target, elementType);
					splitGateway = null;
				}
				// add new activity
				else {
					targetElement = process.addTask(targetId, target, elementType);

				}
			}

			if (sourceElement.getOutgoingSequenceFlows().size() == 0
					&& targetElement.getIngoingSequenceFlows().size() == 0) {
				System.out.println(sourceElement.getId() + "->" + targetElement.getId());
				process.addSequenceFlow("sq-" + seqenceFlowId.toString(), sourceElement.getId(), targetElement.getId());
				seqenceFlowId++;
			} else {

				if (isNewTarget) {
					// call split gateway function
					addSplitGateway(sourceElement, targetElement);
				} else if (isNewSource) {
					String gatewayType = BPMNTypes.EXCLUSIVE_GATEWAY;
					String gatewayNum = "-4";
					// add gateway before activity
					Gateway newGateway = process.addGateway("gt-" + gatewayId.toString(), "", gatewayType);
					newGateway.setAttribute(GATEWAY_NUM, gatewayNum);
					splitGateway = newGateway;
					process.deleteGateway(newGateway.getId());
//					System.out.println(sourceElement.getId() + "->" + targetElement.getId());
//					process.addSequenceFlow("sq-" + seqenceFlowId.toString(), sourceElement.getId(),
//							targetElement.getId());
//					seqenceFlowId++;
					// call split gateway function
					addJoinGateway(sourceElement, targetElement);
					// probably XOR join
					if (targetElement.getIngoingSequenceFlows().size() > 1) {
						addXorGateway(targetElement);
					}
					splitGateway = null;

				} else {
					// probably there exists a loop
					// boolean isLoop = false;
					if (splitGateway == null) {
						// probably the relation is exists
						if (process.isPreceding(targetElement, sourceElement, sourceElement)) {

						} else
						// probably split
						if (sourceElement.getOutgoingSequenceFlows().size() > 0) {
							System.out.println(
									"--- probably split ---: " + sourceElement.getId() + "->" + targetElement.getId());
//							System.out.println(targetElement.getIngoingSequenceFlows().size());
							if (targetElement.getIngoingSequenceFlows().size() > 0) {
								BPMNElementNode precedingOfTarget = targetElement.getIngoingSequenceFlows().iterator()
										.next().getSourceElement();
								if (BPMNTypes.BPMN_GATEWAYS.contains(precedingOfTarget.getType())) {
									System.out.println("--- probably do nothing ---");
//								System.out.println(precedingOfTarget.getId());
									Set<String> targetsElementFromBPMN = model.openDefaultProces()
											.getAllSuccesssors(precedingOfTarget).stream().map(e -> e.getId())
											.collect(Collectors.toSet());
									// get source of element1
									Set<DefaultWeightedEdge> targetsEdgeElementEdge = dependenciesGraph.dependencyGraph
											.outgoingEdgesOf(sourceElement.getId());
									Set<String> targetsElement = new HashSet<>();
									targetsEdgeElementEdge.stream().forEach(edge -> targetsElement
											.add(dependenciesGraph.dependencyGraph.getEdgeTarget(edge)));
//								System.out.println(targetsElementFromBPMN);
//								System.out.println(targetsElement);
									// TODO: valide it by examples
									if (targetsElement.containsAll(targetsElementFromBPMN)) {

										if (!process.isPreceding(sourceElement, targetElement, targetElement)) {
											System.out.println("--- error gateway---");
											continue;
										} else {
											// call split gateway function

											System.out.print("--- nothing to do ---");
											System.out.println("--- PROBABLY SKIP ---");
											continue;
										}

									} else {
										// call split gateway function
										System.out.println("--- split1 ---");
										addSplitGateway(sourceElement, targetElement);
									}

								} else {
									System.out.println("--- split2 ---");
									// call split gateway function
									addSplitGateway(sourceElement, targetElement);
								}
							} else {
								System.out.println("--- split3 ---");
								// call split gateway function
								addSplitGateway(sourceElement, targetElement);
							}
						}

						// probably XOR join
						if (targetElement.getIngoingSequenceFlows().size() > 1) {
							addXorGateway(targetElement);

						} else {
							splitGateway = null;
						}

					} else {

						// call join gateway function
						addJoinGateway(sourceElement, targetElement);
						// probably XOR join
						if (sourceElement.getOutgoingSequenceFlows().size() > 1) {
							System.out.println("add gateway aget the existing activity (" + sourceElement.getId()
									+ "): gt-" + gatewayId.toString());
							// TODO: XOR is generic?
							String gatewayType;
							String gatewayNum;
							gatewayType = BPMNTypes.EXCLUSIVE_GATEWAY;
							gatewayNum = "-3";

							// add gateway before activity
							Gateway newGateway = process.addGateway("gt-" + gatewayId.toString(), "", gatewayType);
							newGateway.setAttribute(GATEWAY_NUM, gatewayNum);
							gatewayId++;

//							splitGateway = newGateway;

							List<SequenceFlow> sequenceFlowList = sourceElement.getOutgoingSequenceFlows().stream()
									.collect(Collectors.toList());
							for (SequenceFlow flow : sequenceFlowList) {
								// get succeed
								BPMNElementNode targetE = flow.getTargetElement();
								process.deleteSequenceFlow(flow.getId());
								process.addSequenceFlow("sq-" + seqenceFlowId.toString(), newGateway.getId(),
										targetE.getId());
								seqenceFlowId++;
							}

							// add new sequence flow
							process.addSequenceFlow("sq-" + seqenceFlowId.toString(), sourceElement.getId(),
									newGateway.getId());
							seqenceFlowId++;

						}
					}

				}
			}
		}

		// block construction
		for (Pair<List<String>, List<String>> loop : loops) {
			ArrayList<BPMNElementNode> sourceElements = new ArrayList<>();
			for (String value : loop.getSource()) {
				String sourceId = value;
				BPMNElementNode sourceElement = (BPMNElementNode) process.findElementById(sourceId);
//				System.out.println(sourceElement);
//				System.out.println(value);
				sourceElements.add(sourceElement);
			}
			ArrayList<BPMNElementNode> targetElements = new ArrayList<>();
			for (String value : loop.getTarget()) {
				String targetId = value;
				BPMNElementNode targetElement = (BPMNElementNode) process.findElementById(targetId);
//				System.out.println(targetElement);
//				System.out.println(value);
				targetElements.add(targetElement);
			}
			try {
				loopBlockConstruction(sourceElements, targetElements);
			} catch (Exception e) {
				System.out.println("########### LOOP CONSTRACTION ERROR ##################");
				System.out.println(e.getMessage());
			}
		}

		// add loops
		for (Pair<List<String>, List<String>> loop : loops) {
			ArrayList<BPMNElementNode> sourceElements = new ArrayList<>();
			for (String value : loop.getSource()) {
				String sourceId = value;

				BPMNElementNode sourceElement = (BPMNElementNode) process.findElementById(sourceId);
				sourceElements.add(sourceElement);
			}

			ArrayList<BPMNElementNode> targetElements = new ArrayList<>();
			for (String value : loop.getTarget()) {
				String targetId = value;
				BPMNElementNode targetElement = (BPMNElementNode) process.findElementById(targetId);
				targetElements.add(targetElement);
			}
			try {
				addLoopGateway(sourceElements, targetElements);
			} catch (Exception e) {
				System.out.println("########### LOOP ERROR ##################");
				System.out.println(e.getMessage());
			}
		}

		PostProcessing();
	}

	private void addXorGateway(BPMNElementNode targetElement) throws BPMNModelException {
		System.out.println(
				"add gateway before the existing activity (" + targetElement.getId() + "): gt-" + gatewayId.toString());
		BPMNProcess process = model.openDefaultProces();
		// TODO: XOR is generic?
		// TODO: validate
		String gatewayType;
		String gatewayNum;
		if (splitGateway == null) {
			gatewayType = BPMNTypes.EXCLUSIVE_GATEWAY;
			gatewayNum = "-3";
		} else {
			gatewayType = splitGateway.getType();
			gatewayNum = splitGateway.getAttribute(GATEWAY_NUM);
		}

// add gateway before activity
		Gateway newGateway = process.addGateway("gt-" + gatewayId.toString(), "", gatewayType);
		newGateway.setAttribute(GATEWAY_NUM, gatewayNum);
		gatewayId++;
//	if(isLoop) {
//		splitGateway = null;
//	}else {
		splitGateway = newGateway;
//	}

		List<SequenceFlow> sequenceFlowList = targetElement.getIngoingSequenceFlows().stream()
				.collect(Collectors.toList());
		for (SequenceFlow flow : sequenceFlowList) {
			// get succeed
			BPMNElementNode srcElement = flow.getSourceElement();
			process.deleteSequenceFlow(flow.getId());
			process.addSequenceFlow("sq-" + seqenceFlowId.toString(), srcElement.getId(), newGateway.getId());
			seqenceFlowId++;
		}

// add new sequence flow
		process.addSequenceFlow("sq-" + seqenceFlowId.toString(), newGateway.getId(), targetElement.getId());
		seqenceFlowId++;

	}

	/**
	 * This function is to reduce the number of gateways and to change some gateway
	 * type
	 * 
	 * @throws BPMNInvalidTypeException
	 * @throws BPMNMissingElementException
	 * @throws BPMNInvalidReferenceException
	 */
	private void PostProcessing()
			throws BPMNInvalidReferenceException, BPMNMissingElementException, BPMNInvalidTypeException {
		BPMNProcess process = model.openDefaultProces();

		int timeOutCounter = 1000;
		int counter = 0;
		// meging of the gateways
		// we should merge all the followed gateway that have the same type
		while (counter < timeOutCounter) {
			counter++;
			List<Gateway> gateways = process.getGateways().stream().collect(Collectors.toList());
			boolean isMerge = false;
			for (Gateway gateway : gateways) {
				isMerge = false;
				// check if split
				if (gateway.getOutgoingSequenceFlows().size() > 1) {
					for (SequenceFlow sq : gateway.getOutgoingSequenceFlows()) {
						if (BPMNTypes.BPMN_GATEWAYS.contains(sq.getTargetElement().getType())) {
							Gateway succGateway = (Gateway) sq.getTargetElement();
							// check if the gateways are with the same type
							if (succGateway.getIngoingSequenceFlows().size() == 1
									&& succGateway.getType().contentEquals(gateway.getType())) {
								for (SequenceFlow flow : succGateway.getOutgoingSequenceFlows()) {
									try {
										process.deleteSequenceFlow(flow.getId());
										process.addSequenceFlow("sq-" + seqenceFlowId.toString(), gateway.getId(),
												flow.getTargetRef());
										seqenceFlowId++;
									} catch (Exception e) {
									}
								}
								process.deleteSequenceFlow(sq.getId());
								process.deleteElementNode(succGateway.getId());
								isMerge = true;
								break;
							}
						}
					}
				} else
				// join
				if (gateway.getIngoingSequenceFlows().size() > 1) {
					for (SequenceFlow sq : gateway.getIngoingSequenceFlows()) {
						if (BPMNTypes.BPMN_GATEWAYS.contains(sq.getSourceElement().getType())) {
							Gateway precedeGateway = (Gateway) sq.getSourceElement();
							// check if the gateways are with the same type
							if (precedeGateway.getOutgoingSequenceFlows().size() == 1
									&& precedeGateway.getType().contentEquals(gateway.getType())) {
								for (SequenceFlow flow : precedeGateway.getIngoingSequenceFlows()) {
									try {
										process.deleteSequenceFlow(flow.getId());
										process.addSequenceFlow("sq-" + seqenceFlowId.toString(), flow.getSourceRef(),
												gateway.getId());
										seqenceFlowId++;
									} catch (Exception e) {
									}
								}
								process.deleteSequenceFlow(sq.getId());
								process.deleteElementNode(precedeGateway.getId());
								isMerge = true;
								break;
							}
						}
					}
				}
				if (isMerge) {
					break;
				}
			}
			if (!isMerge) {
				break;
			}

		}

		// change gateway types, should be changed to AND or OR based on the case we
		// have
		// only join gateways should be changed
		// change to OR
//		List<Gateway> gateways = process.getGateways().stream().collect(Collectors.toList());
//		for (Gateway gateway : gateways) {
//			// join
//			if (gateway.getIngoingSequenceFlows().size() > 1) {
//				if(BPMNTypes.PARALLEL_GATEWAY.contentEquals(gateway.getType())) {
//					
//				}
//			}
//		}

		// change to AND or OR
//		gateways = process.getGateways().stream().collect(Collectors.toList());
//		for (Gateway gateway : gateways) {
//			// join
//			if (gateway.getIngoingSequenceFlows().size() > 1) {
//
//			}
//		}
	}

	Map<String, String> probablyRelationType;

	private void addSplitGateway(BPMNElementNode sourceElement, BPMNElementNode targetElement)
			throws BPMNModelException {
		System.out.println();
		System.out.println("SPLIT: " + sourceElement.getId() + "->" + targetElement.getId());
		BPMNProcess process = model.openDefaultProces();

		// WARNING: ACTIVITY SHOUD HAVE ONLY ONE SEQUENCE FLOW
		if (sourceElement.getOutgoingSequenceFlows().size() > 1) {
//			throw new BPMNInvalidIDException("Source should have one sequence flow");

		} else {
			// get source sequence flow
			SequenceFlow sq = sourceElement.getOutgoingSequenceFlows().iterator().next();
			BPMNElementNode succeedElement = sq.getTargetElement();
			if (BPMNTypes.BPMN_ACTIVITIES.contains(succeedElement.getType())
					|| BPMNTypes.BPMN_EVENTS.contains(succeedElement.getType())) {
				System.out.println("Add new gateway after the activity (" + succeedElement.getId() + "): gt-"
						+ gatewayId.toString());
				process.deleteSequenceFlow(sq.getId());

				// add new gateway
				Map<String, String> gateway = getGatewayType(succeedElement, targetElement);
				String gatewayType = gateway.get(GATEWAY_TYPE);
				Gateway newGateway = process.addGateway("gt-" + gatewayId.toString(), "", gatewayType);
				newGateway.setAttribute(GATEWAY_NUM, gateway.get(GATEWAY_NUM));
				gatewayId++;
				splitGateway = newGateway;

				// add 3 new sequence flow
				process.addSequenceFlow("sq-" + seqenceFlowId.toString(), sourceElement.getId(), newGateway.getId());
				seqenceFlowId++;
				process.addSequenceFlow("sq-" + seqenceFlowId.toString(), newGateway.getId(), succeedElement.getId());
				seqenceFlowId++;
				process.addSequenceFlow("sq-" + seqenceFlowId.toString(), newGateway.getId(), targetElement.getId());
				seqenceFlowId++;
			} else {
				Gateway selectedGateway = (Gateway) succeedElement;
				Gateway probablySelectedGateway = (Gateway) succeedElement;
				SequenceFlow flowToProbablyGateway = (SequenceFlow) sq;
				probablyRelationType = new HashMap();
				isAdded = false;
				addNewTarget(sq, selectedGateway, sourceElement, targetElement, probablySelectedGateway,
						flowToProbablyGateway, new HashMap());
				if (isAdded == false) {
					System.out.println("add new gateway before the existing gateway(" + probablySelectedGateway.getId()
							+ "): gt-" + gatewayId.toString());
//					System.out.println(probablyRelationType.get(GATEWAY_TYPE));
//					System.out.println(probablySelectedGateway.getIngoingSequenceFlows().size());
//					System.out.println(probablySelectedGateway.getOutgoingSequenceFlows().size());
					// add gateway before probablySelectedGateway
					Gateway newGateway = process.addGateway("gt-" + gatewayId.toString(), "",
							probablyRelationType.get(GATEWAY_TYPE));
					newGateway.setAttribute(GATEWAY_NUM, probablyRelationType.get(GATEWAY_NUM));
					gatewayId++;
					splitGateway = newGateway;

					process.deleteSequenceFlow(flowToProbablyGateway.getId());
					process.addSequenceFlow("sq-" + seqenceFlowId.toString(),
							flowToProbablyGateway.getSourceElement().getId(), newGateway.getId());
					seqenceFlowId++;
					// add 2 new sequence flow
					process.addSequenceFlow("sq-" + seqenceFlowId.toString(), newGateway.getId(),
							probablySelectedGateway.getId());
					seqenceFlowId++;
					process.addSequenceFlow("sq-" + seqenceFlowId.toString(), newGateway.getId(),
							targetElement.getId());
					seqenceFlowId++;
//					System.out.println(targetElement.getId());
//					process.deleteSequenceFlow(flowToProbablyGateway.getId());
//					process.addSequenceFlow("sq-" + seqenceFlowId.toString(),
//							flowToProbablyGateway.getSourceElement().getId(), newGateway.getId());
//					seqenceFlowId++;
//					// add 2 new sequence flow
//					process.addSequenceFlow("sq-" + seqenceFlowId.toString(), newGateway.getId(),
//							probablySelectedGateway.getId());
//					seqenceFlowId++;
//					process.addSequenceFlow("sq-" + seqenceFlowId.toString(), newGateway.getId(),
//							targetElement.getId());
//					seqenceFlowId++;
				}

			}
		}

	}

	/**
	 * This function is to add new target in the BPMN in the rigth position
	 * 
	 * @param selectedSequence
	 * @param selectedGateway
	 * @param sourceElement
	 * @param targetElement
	 * @throws BPMNModelException
	 */
	private boolean addNewTarget(SequenceFlow selectedSequence, Gateway selectedGateway, BPMNElement sourceElement,
			BPMNElement targetElement, Gateway probablySelectedGateway, SequenceFlow flowToProbablyGateway,
			Map<String, String> probablyRelation) throws BPMNModelException {

		BPMNProcess process = model.openDefaultProces();
		List<SequenceFlow> acceptedSequenceFlows = new ArrayList<>();
		List<SequenceFlow> allSequenceFlows = new ArrayList<>();
		Map<String, String> relationType = new HashMap();
		List<SequenceFlow> listSequenceFlow = selectedGateway.getOutgoingSequenceFlows().stream()
				.collect(Collectors.toList());
//		System.out.println("---------------GATEWAY--------------");
//		System.out.println(selectedGateway.getId());
//		System.out.println(selectedGateway.getType());
//		System.out.println("num=" + selectedGateway.getAttribute(GATEWAY_NUM));
//		System.out.println("next=" + selectedGateway.getOutgoingSequenceFlows().size());
		for (SequenceFlow currenctSequenceFlow : listSequenceFlow) {
//			System.out.println("---------------NEW FLOW--------------");
			BPMNElementNode successor = currenctSequenceFlow.getTargetElement();
//			System.out.println(successor.getType());
			if (BPMNTypes.BPMN_ACTIVITIES.contains(successor.getType())
					|| BPMNTypes.BPMN_EVENTS.contains(successor.getType())) {
				relationType = getGatewayType(successor, targetElement);
//				System.out.println((successor.getId()));
//				System.out.println((targetElement.getId()));
//				System.out.println(relationType.get(GATEWAY_TYPE));
//				System.out.println("num=" + relationType.get(GATEWAY_NUM));
				if (!relationType.get(GATEWAY_NUM).contentEquals("-1")) {
					allSequenceFlows.add(currenctSequenceFlow);
					if (!relationType.get(GATEWAY_NUM).contentEquals(selectedGateway.getAttribute(GATEWAY_NUM))) {
						acceptedSequenceFlows.add(currenctSequenceFlow);
						probablyRelation = getGatewayType(successor, targetElement);
						probablyRelationType = getGatewayType(successor, targetElement);
					}
				}
			} else {
//				System.out.println(selectedGateway.getId());
//				System.out.println(selectedGateway.getAttribute(GATEWAY_NUM));
//				System.out.println(successor.getId());
//				System.out.println(successor.getAttribute(GATEWAY_NUM));

				boolean result = addNewTarget(currenctSequenceFlow, (Gateway) successor, sourceElement, targetElement,
						probablySelectedGateway, flowToProbablyGateway, probablyRelation);

				if (result) {
					allSequenceFlows.add(currenctSequenceFlow);
					if (!probablyRelationType.get(GATEWAY_NUM).contains(selectedGateway.getAttribute(GATEWAY_NUM))) {
						acceptedSequenceFlows.add(currenctSequenceFlow);
					}
				}
			}
		}
		if (isAdded == true) {
			return false;
		}

//		System.out.println("---------------OUTPUT--------------");
//		System.out.println(selectedGateway.getId());
//		System.out.println("accepted=" + acceptedSequenceFlows.size());
//		System.out.println("all=" + allAcceptedSequenceFlows.size());
//		System.out.println("next=" + selectedGateway.getOutgoingSequenceFlows().size());
		if (selectedGateway.getOutgoingSequenceFlows().size() == 1) {
			return false;
		}
		if (selectedGateway.getOutgoingSequenceFlows().size() == acceptedSequenceFlows.size()) {
			// probably to add new gateway before selected gateway
			System.out.println("Propbably add before " + selectedGateway.getId());
//			System.out.println(selectedSequence.getSourceElement().getId());
			if (probablyRelation.size() > 0)
				probablyRelationType = probablyRelation;
			flowToProbablyGateway = selectedSequence;
			probablySelectedGateway = selectedGateway;
			return true;
		} else if (selectedGateway.getOutgoingSequenceFlows().size() == allSequenceFlows.size()
				&& acceptedSequenceFlows.size() == 0) {
			isAdded = true;
			System.out.println("add as successor of " + selectedGateway.getId());
			// add target as successor
			// add new element as succeed on the gateway
			process.addSequenceFlow("sq-" + seqenceFlowId.toString(), selectedGateway.getId(),
					selectedSequence.getTargetElement().getId());
			seqenceFlowId++;
			splitGateway = selectedGateway;

		} else if (acceptedSequenceFlows.size() >= 1) {
			isAdded = true;
			System.out.print("Add new gateway after existing gateway (" + selectedGateway.getId() + "): gt-"
					+ gatewayId.toString());
			String gatewayType = probablyRelationType.get(GATEWAY_TYPE);
			// TODO: general gateway
			if (gatewayType == null) {
				gatewayType = BPMNTypes.EXCLUSIVE_GATEWAY;
			}
			// add new gateway after selected gateway
			Gateway newGateway = process.addGateway("gt-" + gatewayId.toString(), "", gatewayType);
			newGateway.setAttribute(GATEWAY_NUM, probablyRelationType.get(GATEWAY_NUM));
			gatewayId++;
			splitGateway = newGateway;

			for (SequenceFlow flow : acceptedSequenceFlows) {
				// get succeed
				BPMNElementNode tElement = flow.getTargetElement();
				System.out.print(tElement.getId() + ", ");
				process.deleteSequenceFlow(flow.getId());
				process.addSequenceFlow("sq-" + seqenceFlowId.toString(), newGateway.getId(), tElement.getId());
				seqenceFlowId++;
			}

			// add 2 new sequence flow
			process.addSequenceFlow("sq-" + seqenceFlowId.toString(), selectedGateway.getId(), newGateway.getId());
			seqenceFlowId++;
			// TODO:verif
			process.addSequenceFlow("sq-" + seqenceFlowId.toString(), newGateway.getId(), targetElement.getId());
			seqenceFlowId++;

		}

//		System.out.println("-------------FINISH--------------");
		return false;
	}

	/**
	 * This function to get the gateway type based on the list of relation. it
	 * return the gateway type and the gateway num (like an id)
	 * 
	 * @param succeesor
	 * @param target
	 * @return
	 */
	private Map<String, String> getGatewayType(BPMNElement succeesor, BPMNElement target) {
		String gatewayType = BPMNTypes.EXCLUSIVE_GATEWAY;
		Integer num = -1;
		Integer selectedNum = num;
		List<String> minList = null;

		for (Map.Entry<String, LinkedList<LinkedList<String>>> entry : relations.entrySet()) {
			LinkedList<LinkedList<String>> lists = entry.getValue();
			for (LinkedList<String> list : lists) {
				num++;
				if (list.contains(succeesor.getId()) && list.contains(target.getId())) {
					if (minList == null || (minList.size() > list.size())) {
						selectedNum = num;
						minList = new ArrayList<>(list);
						if (entry.getKey().contentEquals(DECISION)) {
							gatewayType = BPMNTypes.EXCLUSIVE_GATEWAY;
						} else {
							gatewayType = BPMNTypes.PARALLEL_GATEWAY;
						}
					}
				}
			}
		}
		Map<String, String> data = new HashMap<>();
		data.put(GATEWAY_TYPE, gatewayType);
		data.put(GATEWAY_NUM, selectedNum.toString());
		return data;
	}

	/**
	 * This function to get the gateway type based on the list of relation. it
	 * return the gateway type and the gateway num (like an id)
	 * 
	 * @param succeesor
	 * @param target
	 * @return
	 */
	private Integer getGatewayNum(List<String> elementsIds) {
		Integer num = -1;
		Integer selectedNum = num;
		List<String> minList = null;

		for (Map.Entry<String, LinkedList<LinkedList<String>>> entry : relations.entrySet()) {
			LinkedList<LinkedList<String>> lists = entry.getValue();
			for (LinkedList<String> list : lists) {
				num++;
				if (list.containsAll(elementsIds)) {
					if (minList == null || (minList.size() > list.size())) {
						selectedNum = num;
						minList = new ArrayList<>(list);

					}
				}
			}
		}

		return selectedNum;
	}

	private void addJoinGateway(BPMNElementNode sourceElement, BPMNElementNode targetElement)
			throws BPMNModelException {
		System.out.println();
		System.out.println("JOIN: " + sourceElement.getId() + "->" + targetElement.getId());
		BPMNProcess process = model.openDefaultProces();

		// get target sequence flow
		SequenceFlow sq = targetElement.getIngoingSequenceFlows().iterator().next();
		// get succeed
		BPMNElementNode precedingElement = sq.getSourceElement();

		// check if it is activity
		if (BPMNTypes.BPMN_ACTIVITIES.contains(precedingElement.getType())
				|| BPMNTypes.BPMN_EVENTS.contains(precedingElement.getType())) {
//			if (process.isPreceding(splitGateway, precedingElement)) {
			System.out.println("add new Gateway after preceding Element (task) (" + precedingElement.getId() + "): gt-"
					+ gatewayId.toString());
			process.deleteSequenceFlow(sq.getId());
			// add new gateway
			Gateway newGateway = process.addGateway("gt-" + gatewayId.toString(), "", splitGateway.getType());
			newGateway.setAttribute(GATEWAY_NUM, splitGateway.getAttribute(GATEWAY_NUM));
			gatewayId++;
			splitGateway = null;
			// add 3 new sequence flow
			process.addSequenceFlow("sq-" + seqenceFlowId.toString(), sourceElement.getId(), newGateway.getId());
			seqenceFlowId++;
			process.addSequenceFlow("sq-" + seqenceFlowId.toString(), precedingElement.getId(), newGateway.getId());
			seqenceFlowId++;
			process.addSequenceFlow("sq-" + seqenceFlowId.toString(), newGateway.getId(), targetElement.getId());
			seqenceFlowId++;
//			}
		} else {
			addJoinGatewayRec(sourceElement, precedingElement, sq, targetElement);
		}
	}

	private void addJoinGatewayRec(BPMNElementNode sourceElement, BPMNElementNode selectedGateway,
			SequenceFlow selectedSq, BPMNElementNode targetElement) throws BPMNModelException {
		System.out.println("JOIN-REC: " + sourceElement.getId() + "->" + targetElement.getId());
		BPMNProcess process = model.openDefaultProces();
		if (selectedGateway.getOutgoingSequenceFlows().size() > 1) {
			System.out.println("JOIN-REC: " + "SPLIT");
			Set<String> targetsElementFromBPMN = model.openDefaultProces().getAllSuccesssors(selectedGateway).stream()
					.map(e -> e.getId()).collect(Collectors.toSet());
			// get source of element1
			Set<DefaultWeightedEdge> targetsEdgeElementEdge = dependenciesGraph.dependencyGraph
					.outgoingEdgesOf(sourceElement.getId());
			Set<String> targetsElement = new HashSet<>();
			targetsEdgeElementEdge.stream()
					.forEach(edge -> targetsElement.add(dependenciesGraph.dependencyGraph.getEdgeTarget(edge)));
			System.out.println(targetsElementFromBPMN);
			System.out.println(targetsElement);
			if (targetsElement.containsAll(targetsElementFromBPMN)
					&& targetsElementFromBPMN.containsAll(targetsElement)) {
				if (selectedGateway.getIngoingSequenceFlows().size() == 1) {
					SequenceFlow sq = selectedGateway.getIngoingSequenceFlows().iterator().next();
					BPMNElementNode precedingElement = sq.getSourceElement();
					addJoinGatewayRec(sourceElement, precedingElement, sq, selectedGateway);
				} else {
					System.out.println("------------------------------");
					System.out.println("------------ERROR-------------");
					System.out.println("------------------------------");
				}
			} else {
				// TODO: validate
				System.out.println("add new gateway after the precedingElement (gateway2) (gt-"
						+ selectedGateway.getId() + "): gt-" + gatewayId.toString());

//				 add new gateway after the precedingElement
				Gateway newGateway = process.addGateway("gt-" + gatewayId.toString(), "", splitGateway.getType());
				newGateway.setAttribute(GATEWAY_NUM, splitGateway.getAttribute(GATEWAY_NUM));
				gatewayId++;
				splitGateway = null;
				process.deleteSequenceFlow(selectedSq.getId());
				// add new sequence flow
				process.addSequenceFlow("sq-" + seqenceFlowId.toString(), selectedGateway.getId(), newGateway.getId());
				seqenceFlowId++;
				process.addSequenceFlow("sq-" + seqenceFlowId.toString(), sourceElement.getId(), newGateway.getId());
				seqenceFlowId++;
				process.addSequenceFlow("sq-" + seqenceFlowId.toString(), newGateway.getId(),
						selectedSq.getTargetRef());
				seqenceFlowId++;

			}
		} else {

			Integer flowCounter = 0;
			List<SequenceFlow> acceptedSequenceFlowList = new ArrayList<>();
			List<SequenceFlow> sqflowList = selectedGateway.getIngoingSequenceFlows().stream()
					.collect(Collectors.toList());

			for (SequenceFlow currenctSecquenceFlow : sqflowList) {
				BPMNElementNode currenctProcessor = currenctSecquenceFlow.getSourceElement();
				flowCounter++;
				if (process.isPreceding(splitGateway, currenctProcessor, selectedGateway)) {
					acceptedSequenceFlowList.add(currenctSecquenceFlow);
				}
			}

			// add to the same gateway
			if (acceptedSequenceFlowList.size() == flowCounter) {
				// check if the same gateway type and the num(like id) of the gateway
				if (selectedGateway.getType().contentEquals(splitGateway.getType()) && selectedGateway
						.getAttribute(GATEWAY_NUM).contentEquals(splitGateway.getAttribute(GATEWAY_NUM))) {
					System.out.println("Join at the same gateway (" + selectedGateway.getId() + ")");
					process.addSequenceFlow("sq-" + seqenceFlowId.toString(), sourceElement.getId(),
							selectedGateway.getId());
					seqenceFlowId++;
					splitGateway = null;
				} else {
					System.out.println("add new gateway after the precedingElement (gateway) (" + gatewayId.toString()
							+ "): gt-" + gatewayId.toString());
					// add new gateway after the precedingElement
					Gateway newGateway = process.addGateway("gt-" + gatewayId.toString(), "", splitGateway.getType());
					newGateway.setAttribute(GATEWAY_NUM, splitGateway.getAttribute(GATEWAY_NUM));
					gatewayId++;
					splitGateway = null;

					// add new sequence flow
					process.addSequenceFlow("sq-" + seqenceFlowId.toString(), selectedGateway.getId(),
							newGateway.getId());
					seqenceFlowId++;
					process.addSequenceFlow("sq-" + seqenceFlowId.toString(), sourceElement.getId(),
							newGateway.getId());
					seqenceFlowId++;
					process.addSequenceFlow("sq-" + seqenceFlowId.toString(), newGateway.getId(),
							selectedSq.getTargetElement().getId());
					seqenceFlowId++;

					process.deleteSequenceFlow(selectedSq.getId());
				}
			} else if (acceptedSequenceFlowList.size() >= 2) {
				System.out.println("add new gateway  for accepted element ("
						+ acceptedSequenceFlowList.stream().map(s -> s.getSourceRef() + ",") + "): gt-"
						+ gatewayId.toString());

				// add new gateway for accepted element
				Gateway newGateway = process.addGateway("gt-" + gatewayId.toString(), "", splitGateway.getType());
				newGateway.setAttribute(GATEWAY_NUM, splitGateway.getAttribute(GATEWAY_NUM));
				gatewayId++;
				splitGateway = null;

				// add new sequence flow
				process.addSequenceFlow("sq-" + seqenceFlowId.toString(), newGateway.getId(), selectedGateway.getId());
				seqenceFlowId++;
				process.addSequenceFlow("sq-" + seqenceFlowId.toString(), sourceElement.getId(), newGateway.getId());
				seqenceFlowId++;
				for (SequenceFlow sequence : acceptedSequenceFlowList) {
					process.addSequenceFlow("sq-" + seqenceFlowId.toString(), sequence.getSourceElement().getId(),
							newGateway.getId());
					seqenceFlowId++;
					process.deleteSequenceFlow(sequence.getId());
				}
			} else if (acceptedSequenceFlowList.size() == 1) {
				BPMNElementNode precedingElement = acceptedSequenceFlowList.get(0).getSourceElement();
				if (BPMNTypes.BPMN_ACTIVITIES.contains(precedingElement.getType())
						|| BPMNTypes.BPMN_EVENTS.contains(precedingElement.getType())) {
					System.out.println("add new gateway  for accepted element (" + precedingElement.getId() + ")"
							+ "-: gt-" + gatewayId.toString());
					process.deleteSequenceFlow(acceptedSequenceFlowList.get(0).getId());

					// add new gateway
					Gateway newGateway = process.addGateway("gt-" + gatewayId.toString(), "", splitGateway.getType());
					newGateway.setAttribute(GATEWAY_NUM, splitGateway.getAttribute(GATEWAY_NUM));
					gatewayId++;
					splitGateway = null;

					// add 3 new sequence flow
					process.addSequenceFlow("sq-" + seqenceFlowId.toString(), sourceElement.getId(),
							newGateway.getId());
					seqenceFlowId++;
					process.addSequenceFlow("sq-" + seqenceFlowId.toString(), precedingElement.getId(),
							newGateway.getId());
					seqenceFlowId++;
					process.addSequenceFlow("sq-" + seqenceFlowId.toString(), newGateway.getId(),
							selectedGateway.getId());
					seqenceFlowId++;

				} else {
					addJoinGatewayRec(sourceElement, precedingElement, acceptedSequenceFlowList.get(0), targetElement);
				}
			}
			// TODO: validate
			else
			// cannot access the split gateway => should add XOR gateway if the gateway ==
			// parallel
			if (acceptedSequenceFlowList.size() == 0) {
				System.out.println("add new Default XOR-1: gt-" + gatewayId.toString());
				// add new gateway for connected to existing element
				String gatewayType = splitGateway.getType();
				String gatewayNum = splitGateway.getAttribute(GATEWAY_NUM);
				// TODO: add generic gateway?
				if (gatewayType.contentEquals(BPMNTypes.PARALLEL_GATEWAY)) {
					gatewayType = BPMNTypes.EXCLUSIVE_GATEWAY;
					gatewayNum = "-1";
				}

				Gateway newGateway = process.addGateway("gt-" + gatewayId.toString(), "", gatewayType);
				newGateway.setAttribute(GATEWAY_NUM, gatewayNum);
				gatewayId++;
				splitGateway = null;

				SequenceFlow sq = targetElement.getIngoingSequenceFlows().iterator().next();
				process.addSequenceFlow("sq-" + seqenceFlowId.toString(), selectedGateway.getId(), newGateway.getId());
				seqenceFlowId++;
				process.addSequenceFlow("sq-" + seqenceFlowId.toString(), sourceElement.getId(), newGateway.getId());
				seqenceFlowId++;
				process.addSequenceFlow("sq-" + seqenceFlowId.toString(), newGateway.getId(),
						sq.getTargetElement().getId());
				seqenceFlowId++;
				process.deleteSequenceFlow(sq.getId());
			}
		}
	}

	private void loopBlockConstruction(List<BPMNElementNode> sourceElements, List<BPMNElementNode> targetElements)
			throws BPMNModelException, CloneNotSupportedException {
		BPMNProcess process = model.openDefaultProces();
		int countSqOfsSource = 0;
		for (BPMNElementNode element : sourceElements) {
			if (element.getOutgoingSequenceFlows().size() > 0) {
				countSqOfsSource++;
			}
		}

		if (countSqOfsSource != sourceElements.size()) {
			System.out.println("LOOP BLOCK CONSTRUCTION: "
					+ sourceElements.stream().map(src -> src.getId()).collect(Collectors.toList()));
			List<Pair<BPMNElementNode, BPMNElementNode>> sourcesAsPair = new ArrayList<>();
			for (int i = 0; i < sourceElements.size() - 1; i++) {
				for (int j = i + 1; j < sourceElements.size(); j++) {
					sourcesAsPair.add(
							new Pair<BPMNElementNode, BPMNElementNode>(sourceElements.get(i), sourceElements.get(j)));
				}
			}

			Map<BPMNElementNode, Set<BPMNElementNode>> splitGateways = new HashMap<>();

			// get relation between all pairs
			for (Pair<BPMNElementNode, BPMNElementNode> pairOfActivites : sourcesAsPair) {
				Gateway tempSplitGateway = null;
				String ancestor = dependenciesGraph.getLCA(pairOfActivites.getSource().getId(),
						pairOfActivites.getTarget().getId());

				BPMNElementNode ancestorNode = process.findElementNodeById(ancestor);
				List<BPMNElementNode> successorsList = process.getAllSuccesssors(ancestorNode);
				List<GraphPath<String, DefaultWeightedEdge>> listOfPaths = new ArrayList<>();
				for (BPMNElementNode successor : successorsList) {
					AllDirectedPaths path = new AllDirectedPaths(dependenciesGraph.dependencyGraph);
//					System.out.println(successor.getId());
					listOfPaths.addAll(
							path.getAllPaths(successor.getId(), pairOfActivites.getSource().getId(), true, null));
					listOfPaths.addAll(
							path.getAllPaths(successor.getId(), pairOfActivites.getTarget().getId(), true, null));
				}
//				System.out.println(listOfPaths.size());
				List<String> sourcesId = listOfPaths.stream().map(path -> path.getStartVertex())
						.collect(Collectors.toList());
				int num = getGatewayNum(sourcesId);
				StringBuffer gatewayNum = new StringBuffer();
				gatewayNum.append(num);
				if (!gatewayNum.toString().contentEquals("-1")) {
					tempSplitGateway = model.openDefaultProces().getGateways().stream()
							.filter(gateway -> gateway.getAttribute(GATEWAY_NUM).contentEquals(gatewayNum)
									&& gateway.getIngoingSequenceFlows().size() == 1)
							.findAny().orElse(null);
				} else {
					System.out.println("Error in detect loop Source relation/ Cannot constract block!");
					return;
				}
				if (splitGateways.containsKey(tempSplitGateway)) {
					splitGateways.get(tempSplitGateway).add(pairOfActivites.getSource());
					splitGateways.get(tempSplitGateway).add(pairOfActivites.getTarget());
				} else {
					splitGateways.put(tempSplitGateway, new HashSet<>());
					splitGateways.get(tempSplitGateway).add(pairOfActivites.getSource());
					splitGateways.get(tempSplitGateway).add(pairOfActivites.getTarget());
				}
			}
			// TreeMap with a custom comparator that sorts keys based on the size of their
			// sets
			TreeMap<BPMNElementNode, Set<BPMNElementNode>> sortedGatewaysElements = new TreeMap<>(
					Comparator.comparingInt(key -> splitGateways.get(key).size()));
			sortedGatewaysElements.putAll(splitGateways);

			// Print the sorted map
			for (Map.Entry<BPMNElementNode, Set<BPMNElementNode>> entry : sortedGatewaysElements.entrySet()) {
				Gateway split = (Gateway) entry.getKey();

				Gateway join = process.getGateways().stream()
						.filter(gateway -> gateway.getAttribute(GATEWAY_NUM).contentEquals(
								split.getAttribute(GATEWAY_NUM)) && gateway.getOutgoingSequenceFlows().size() == 1)
						.findAny().orElse(null);

				if (join == null) {
					// should add new join
					join = process.addGateway("gt-" + gatewayId.toString(), "", split.getType());
					join.setAttribute(GATEWAY_NUM, split.getAttribute(GATEWAY_NUM));
					gatewayId++;
					for (BPMNElementNode element : entry.getValue()) {
						process.addSequenceFlow("sq-" + seqenceFlowId, element.getId(), join.getId());
						seqenceFlowId++;
						if (element.getOutgoingSequenceFlows().size() > 1) {
							System.out
									.println("add XOR gateway after the existing element: gt-" + gatewayId.toString());
							// TODO: XOR is generic?
							String gatewayType = BPMNTypes.EXCLUSIVE_GATEWAY;

							// add gateway before activity
							Gateway newGateway = process.addGateway("gt-" + gatewayId.toString(), "", gatewayType);
							newGateway.setAttribute(GATEWAY_NUM, "-3");
							gatewayId++;

							List<SequenceFlow> sequenceFlowList = element.getOutgoingSequenceFlows().stream()
									.collect(Collectors.toList());
							for (SequenceFlow flow : sequenceFlowList) {
								// get succeed
								BPMNElementNode srcElement = flow.getTargetElement();
//								System.out.println(srcElement.getId());
								process.deleteSequenceFlow(flow.getId());
								process.addSequenceFlow("sq-" + seqenceFlowId.toString(), newGateway.getId(),
										srcElement.getId());
								seqenceFlowId++;
							}

							// add new sequence flow
							process.addSequenceFlow("sq-" + seqenceFlowId.toString(), element.getId(),
									newGateway.getId());
							seqenceFlowId++;
						}
					}
				}

				for (Map.Entry<BPMNElementNode, Set<BPMNElementNode>> gtyList : sortedGatewaysElements.entrySet()) {
					if (gtyList.getKey() != split) {
						if (gtyList.getValue().containsAll(entry.getValue())) {
							gtyList.getValue().removeAll(entry.getValue());
							gtyList.getValue().add(join);
						}
					}
				}

			}
		}

		if (targetElements.size() != 1) {

			// get root gateway based on the relation
			List<String> targetIds = targetElements.stream().map(src -> src.getId()).collect(Collectors.toList());
//			int num = getGatewayNum(targetIds);
//			StringBuffer gatewayNum = new StringBuffer();
//			gatewayNum.append(num);
//			if (gatewayNum.toString().contentEquals("-1")) {
			System.out.println("LOOP BLOCK TARGET CONSTRUCTION: "
					+ sourceElements.stream().map(src -> src.getId()).collect(Collectors.toList()));
			String tempActivity = "___temp___";
			BPMNElementNode tempElement = process.addTask(tempActivity, tempActivity, BPMNTypes.TASK);
			for (String target : targetIds) {
				BPMNElementNode targetElement = process.findElementNodeById(target);
//				System.out.println(targetElement.getId());
				if (tempElement.getOutgoingSequenceFlows().size() > 0) {
					addSplitGateway(tempElement, targetElement);
					splitGateway = null;

				} else {
					process.addSequenceFlow("sq-" + seqenceFlowId.toString(), tempActivity, target);
					seqenceFlowId++;
				}
			}

			for (SequenceFlow sq : tempElement.getOutgoingSequenceFlows()) {
				process.deleteSequenceFlow(sq.getId());
			}
			process.deleteElementNode(tempElement.getId());
			for (String target : targetIds) {
				BPMNElementNode targetElement = process.findElementNodeById(target);
				// probably XOR join
				if (targetElement.getIngoingSequenceFlows().size() > 1) {
					addXorGateway(targetElement);
				}
				splitGateway = null;
			}

		}

	}

	private void addLoopGateway(List<BPMNElementNode> sourceElements, List<BPMNElementNode> targetElements)
			throws BPMNModelException, CloneNotSupportedException {
		System.out.println("LOOP: " + sourceElements.stream().map(src -> src.getId()).collect(Collectors.toList())
				+ "->" + targetElements.stream().map(src -> src.getId()).collect(Collectors.toList()));
		BPMNProcess process = model.openDefaultProces();
		BPMNElementNode targetElement = null;
		if (targetElements.size() == 1) {
			targetElement = targetElements.get(0);
		} else {
			// get root gateway based on the relation
			List<String> targetIds = targetElements.stream().map(src -> src.getId()).collect(Collectors.toList());
			int num = getGatewayNum(targetIds);
			StringBuffer gatewayNum = new StringBuffer();
			gatewayNum.append(num);
			if (!gatewayNum.toString().contentEquals("-1")) {
				targetElement = process.getGateways().stream()
						.filter(gateway -> gateway.getAttribute(GATEWAY_NUM).contentEquals(gatewayNum)).findAny()
						.orElse(null);
				if (targetElement == null) {
					System.out.println("Error in detect loop targets relation");
					return;
				}
			} else {
				Queue<BPMNElementNode> queue = new LinkedList<>();
				Set<BPMNElementNode> visited = new HashSet<>();

				for (BPMNElementNode node : targetElements) {
					List<BPMNElementNode> children = node.getIngoingSequenceFlows().stream()
							.map(sq -> sq.getSourceElement()).collect(Collectors.toList());
					for (BPMNElementNode child : children) {
						if (!visited.contains(child)) {
							visited.add(child);
							queue.add(child);
						}
					}
				}

				while (!queue.isEmpty()) {
					BPMNElementNode precedingElement = queue.poll();

					List<BPMNElementNode> sucessors = process.getAllSuccesssors(precedingElement);
					if (sucessors.containsAll(targetElements)) {
						targetElement = precedingElement;
						break;
					} else {
						List<BPMNElementNode> children = precedingElement.getIngoingSequenceFlows().stream()
								.map(sq -> sq.getSourceElement()).collect(Collectors.toList());

						for (BPMNElementNode child : children) {
							if (!visited.contains(child)) {
								visited.add(child);
								queue.add(child);
							}
						}
					}
				}

				if (targetElement == null) {
					System.out.println("Error in detect loop targets relation-1");
					return;
				}
			}
		}

		BPMNElementNode sourceElement = null;
		if (sourceElements.size() == 1) {
			sourceElement = sourceElements.get(0);
		} else {

			// get sourceElement (The merge or the)
			List<BPMNElementNode> gatewaysofSource = sourceElements.stream().map(src -> src.getOutgoingSequenceFlows())
					.flatMap(Set::stream).map(sq -> sq.getTargetElement()).collect(Collectors.toList());
			for (BPMNElementNode target : gatewaysofSource) {
				int countCanAccess = 0;
				for (BPMNElementNode src : sourceElements) {
					if (process.isPreceding(src, target, target)) {
						countCanAccess++;
					}
				}
				if (countCanAccess == sourceElements.size()) {
					sourceElement = target;
					break;
				}
			}

			if (sourceElement == null) {
				System.out.println("Error in detect loop targets relation");
				return;
			}
		}

		if (sourceElement.getOutgoingSequenceFlows().size() == 0) {
			if (targetElement.getIngoingSequenceFlows().size() > 0) {
				// add only one gateway before the target
				System.out.println("add new loop gateway before the target (" + targetElement.getIngoingSequenceFlows()
						.stream().map(sq -> sq.getSourceRef()).collect(Collectors.toList()) + "): gt-"
						+ gatewayId.toString());
				Gateway targetGateway = process.addGateway("gt-" + gatewayId.toString(), "",
						BPMNTypes.EXCLUSIVE_GATEWAY);
				targetGateway.setAttribute(GATEWAY_NUM, "-2");
				gatewayId++;

				// get element connected to the target
				SequenceFlow sqflow = targetElement.getIngoingSequenceFlows().iterator().next();
				process.addSequenceFlow("sq-" + seqenceFlowId.toString(), sqflow.getSourceElement().getId(),
						targetGateway.getId());
				seqenceFlowId++;
				process.deleteSequenceFlow(sqflow.getId());
				// add new sequence flow
				process.addSequenceFlow("sq-" + seqenceFlowId.toString(), targetGateway.getId(), targetElement.getId());
				seqenceFlowId++;

				// connect the gateways
				process.addSequenceFlow("sq-" + seqenceFlowId.toString(), sourceElement.getId(), targetGateway.getId());
				seqenceFlowId++;
			} else {
				System.out.println("add new sequence flow to the target("
						+ targetElement.getIngoingSequenceFlows().stream().map(sq -> sq.getSourceRef() + ",") + "): gt-"
						+ gatewayId.toString());
				// add new sequence flow
				process.addSequenceFlow("sq-" + seqenceFlowId.toString(), sourceElement.getId(), targetElement.getId());
				seqenceFlowId++;

			}
		} else {
			if (targetElement.getIngoingSequenceFlows().size() > 0) {
				System.out.println("add new loop gateways (2) before the target("
						+ targetElement.getIngoingSequenceFlows().stream().map(sq -> sq.getSourceRef())
								.collect(Collectors.toList())
						+ "), and after source (" + sourceElement.getOutgoingSequenceFlows().stream()
								.map(sq -> sq.getTargetRef()).collect(Collectors.toList())
						+ ") : gt-" + gatewayId.toString());
				// add new gateway after source ( to start loop)
				Gateway sourceGateway = process.addGateway("gt-" + gatewayId.toString(), "",
						BPMNTypes.EXCLUSIVE_GATEWAY);
				sourceGateway.setAttribute(GATEWAY_NUM, "-2");
				gatewayId++;

				// get element connected to the source
				SequenceFlow sqflow = sourceElement.getOutgoingSequenceFlows().iterator().next();
				process.addSequenceFlow("sq-" + seqenceFlowId.toString(), sourceGateway.getId(),
						sqflow.getTargetElement().getId());
				seqenceFlowId++;
				process.deleteSequenceFlow(sqflow.getId());

				// add 2 new sequence flow
				process.addSequenceFlow("sq-" + seqenceFlowId.toString(), sourceElement.getId(), sourceGateway.getId());
				seqenceFlowId++;

				// add new gateway before source ( end for loop)
				Gateway targetGateway = process.addGateway("gt-" + gatewayId.toString(), "",
						BPMNTypes.EXCLUSIVE_GATEWAY);
				targetGateway.setAttribute(GATEWAY_NUM, "-2");
				gatewayId++;

				// get element connected to the target
				sqflow = targetElement.getIngoingSequenceFlows().iterator().next();
				process.addSequenceFlow("sq-" + seqenceFlowId.toString(), sqflow.getSourceElement().getId(),
						targetGateway.getId());
				seqenceFlowId++;
				process.deleteSequenceFlow(sqflow.getId());
				// add new sequence flow
				process.addSequenceFlow("sq-" + seqenceFlowId.toString(), targetGateway.getId(), targetElement.getId());
				seqenceFlowId++;

				// connect the gateways
				process.addSequenceFlow("sq-" + seqenceFlowId.toString(), sourceGateway.getId(), targetGateway.getId());
				seqenceFlowId++;
			} else {
				System.out.println("add new loop gateways after source("
						+ sourceElement.getOutgoingSequenceFlows().stream().map(sq -> sq.getTargetRef() + ",")
						+ "): gt-" + gatewayId.toString());
				// add new gateway after source ( to start loop)
				Gateway sourceGateway = process.addGateway("gt-" + gatewayId.toString(), "",
						BPMNTypes.EXCLUSIVE_GATEWAY);
				sourceGateway.setAttribute(GATEWAY_NUM, "-2");
				gatewayId++;

				// get element connected to the source
				SequenceFlow sqflow = sourceElement.getOutgoingSequenceFlows().iterator().next();
				process.addSequenceFlow("sq-" + seqenceFlowId.toString(), sourceGateway.getId(),
						sqflow.getTargetElement().getId());
				seqenceFlowId++;
				process.deleteSequenceFlow(sqflow.getId());

				// add 2 new sequence flow
				process.addSequenceFlow("sq-" + seqenceFlowId.toString(), sourceElement.getId(), sourceGateway.getId());
				seqenceFlowId++;

				// connect the target
				process.addSequenceFlow("sq-" + seqenceFlowId.toString(), sourceGateway.getId(), targetElement.getId());
				seqenceFlowId++;
			}
		}
	}

	public void saveMode(String path) {
		try {
			String output;
			BPMNLayoutGenerator bpmnLayoutGenerator = new BPMNLayoutGenerator();
			output = bpmnLayoutGenerator.generateLayoutFromBPMNSemantic(model.getXml(), ExportType.valueOf("BPMN"));

			File outputFile = new File(path);
			FileUtils.touch(outputFile);
			Files.write(outputFile.toPath(), output.getBytes());

		} catch (Exception e) {
			// TODO Auto-generated catch block
			model.save(path);
			System.out.println("ERROR- The model is saved without layouting");
			e.printStackTrace();
		}

		logger.info("...model creation sucessful");
	}

}
