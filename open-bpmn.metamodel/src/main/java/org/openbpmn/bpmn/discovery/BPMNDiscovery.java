package org.openbpmn.bpmn.discovery;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.jgrapht.graph.DefaultWeightedEdge;
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
	static String PARALLEL = "parallel";
	static String DECISION = "decision";

	static String GATEWAY_TYPE = "gatewayType";
	static String GATEWAY_NUM = "num";
	BPMNModel model;
	BPMNModel modelWithoutLoops;
	List<String> startsEvent;
	List<String> endsEvent;
	LinkedList<String> dependencies;
	Map<String, LinkedList<LinkedList<String>>> relations;
	Integer seqenceFlowId = 0;
	Integer gatewayId = 0;
	Gateway splitGateway = null;
	Boolean isAdded = false;
	LinkedList<Pair<List<String>, List<String>>> loops;

	DependencyGraph dependenciesGraph = new DependencyGraph();

	public BPMNDiscovery(List<String> startsEvent, List<String> endsEvent, LinkedList<String> dependencies,
			Map<String, LinkedList<LinkedList<String>>> relations, LinkedList<Pair<List<String>, List<String>>> loops) {
		logger.info("...creating new empty model");
		model = BPMNModelFactory.createInstance("demo", "1.0.0", "http://org.openbpmn");

		this.startsEvent = startsEvent;
		this.endsEvent = endsEvent;
		this.dependencies = dependencies;
		this.relations = relations;
		this.loops = loops;
		relations.values().forEach(elements -> {
			elements.forEach(innerList -> {
				innerList.replaceAll(String::trim);
				innerList.replaceAll(s -> s.replace(" ", "-"));
				innerList.replaceAll(String::toLowerCase);
			});
		});
		loops.forEach(elements -> {
			elements.getSource().replaceAll(String::trim);
			elements.getSource().replaceAll(s -> s.replace(" ", "-"));
			elements.getSource().replaceAll(String::toLowerCase);

			elements.getTarget().replaceAll(String::trim);
			elements.getTarget().replaceAll(s -> s.replace(" ", "-"));
			elements.getTarget().replaceAll(String::toLowerCase);

		});

		for (String dependency : dependencies) {
			// Split the dependency string into source and target
			String[] parts = dependency.split("->");

			// Extract source and target
			String sourceId = parts[0].replace(" ", "-").toLowerCase();
			String targetId = parts[1].replace(" ", "-").toLowerCase();
			dependenciesGraph.addVertex(sourceId);
			dependenciesGraph.addVertex(targetId);
			dependenciesGraph.addEdge(sourceId, targetId);
		}
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
			String source = parts[0];
			String target = parts[1];
			String sourceId = parts[0].replace(" ", "-").toLowerCase();
			String targetId = parts[1].replace(" ", "-").toLowerCase();
			boolean isNewTarget = false;
			BPMNElementNode sourceElement = (BPMNElementNode) process.findElementById(sourceId);
			BPMNElementNode targetElement = (BPMNElementNode) process.findElementById(targetId);

			// element source is not added to the process mode
			if (sourceElement == null) {
				// add new start event
				if (startsEvent.contains(source)) {
					sourceElement = process.addEvent(sourceId, source, BPMNTypes.START_EVENT);
				}
				// add new activity
				else {
					sourceElement = process.addTask(sourceId, source, BPMNTypes.TASK);
				}
				// TODO: Take into account the events
			}

			// element target is not added to the process mode
			if (targetElement == null) {
				isNewTarget = true;
				// add new start event
				if (endsEvent.contains(target)) {
					targetElement = process.addEvent(targetId, target, BPMNTypes.END_EVENT);
					splitGateway = null;
				}
				// add new activity
				else {
					targetElement = process.addTask(targetId, target, BPMNTypes.TASK);
				}
				// TODO: Take into account the events
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
				} else {
					// probably there exists a loop
					// boolean isLoop = false;
					if (splitGateway == null) {
						// probably the relation is exists
						if (process.isPreceding(targetElement, sourceElement, sourceElement)) {

						} else
						// probably split
						if (sourceElement.getOutgoingSequenceFlows().size() > 0) {
							System.out.println("--- probably split ---");

							BPMNElementNode precedingOfTarget = targetElement.getIngoingSequenceFlows().iterator()
									.next().getSourceElement();
							if (BPMNTypes.BPMN_GATEWAYS.contains(precedingOfTarget.getType())) {
								System.out.println("--- probably do nothing ---");
								Set<String> targetsElementFromBPMN = model.openDefaultProces()
										.getAllSuccesssors(precedingOfTarget).stream().map(e -> e.getId())
										.collect(Collectors.toSet());
								// get source of element1
								Set<DefaultWeightedEdge> targetsEdgeElementEdge = dependenciesGraph.dependencyGraph
										.outgoingEdgesOf(sourceElement.getId());
								Set<String> targetsElement = new HashSet<>();
								targetsEdgeElementEdge.stream().forEach(edge -> targetsElement
										.add(dependenciesGraph.dependencyGraph.getEdgeTarget(edge)));
								System.out.println(targetsElementFromBPMN);
								System.out.println(targetsElement);
								if (targetsElement.containsAll(targetsElementFromBPMN)
										&& targetsElementFromBPMN.containsAll(targetsElement)) {
									System.out.println("--- nothing to do ---");
									continue;
								} else {
									// call split gateway function
									System.out.println("--- split ---");
									addSplitGateway(sourceElement, targetElement);
								}

							} else {
								System.out.println("--- split2 ---");
								// call split gateway function
								addSplitGateway(sourceElement, targetElement);
							}
						}
						splitGateway = null;
						// AND //OR

						// probably XOR join
						if (targetElement.getIngoingSequenceFlows().size() > 1) {
							System.out.println("add gateway before the existing activity");
							// TODO: XOR is generic?
							String gatewayType = BPMNTypes.EXCLUSIVE_GATEWAY;
//							Set<String> sourceElementType = targetElement.getIngoingSequenceFlows().stream()
//									.map(sq -> sq.getSourceElement().getType()).collect(Collectors.toSet());
//							
//							String gatewayType = BPMNTypes.EXCLUSIVE_GATEWAY;
//							if (sourceElementType.size() == 1) {
//								String existingType = sourceElementType.iterator().next();
//								if (BPMNTypes.BPMN_GATEWAYS.contains(existingType)) {
//									gatewayType = existingType;
//								}
//							}

							// add gateway before activity
							Gateway newGateway = process.addGateway("gt-" + gatewayId.toString(), gatewayId.toString(),
									gatewayType);
							newGateway.setAttribute(GATEWAY_NUM, "-3");
							gatewayId++;
//								if(isLoop) {
//									splitGateway = null;
//								}else {
							splitGateway = newGateway;
//								}

							List<SequenceFlow> sequenceFlowList = targetElement.getIngoingSequenceFlows().stream()
									.collect(Collectors.toList());
							for (SequenceFlow flow : sequenceFlowList) {
								// get succeed
								BPMNElementNode srcElement = flow.getSourceElement();
								System.out.println(srcElement.getId());
								process.deleteSequenceFlow(flow.getId());
								process.addSequenceFlow("sq-" + seqenceFlowId.toString(), srcElement.getId(),
										newGateway.getId());
								seqenceFlowId++;
							}

							// add new sequence flow
							process.addSequenceFlow("sq-" + seqenceFlowId.toString(), newGateway.getId(),
									targetElement.getId());
							System.out.println(targetElement.getId());
							seqenceFlowId++;

						}

					} else {

						// call join gateway function
						addJoinGateway(sourceElement, targetElement);
					}

				}
			}
		}

		// call PostProcessing()

		modelWithoutLoops = (BPMNModel) model.clone();

		// Sort pairs based on the size of the second element
//		Collections.sort(loops, new Comparator<Pair<List<String>, List<String>>>() {
//			@Override
//			public int compare(Pair<List<String>, List<String>> p1, Pair<List<String>, List<String>> p2) {
//				return Integer.compare(p1.getSource().size(), p2.getSource().size());
//			}
//		});

		// Sort pairs by the size of the source list, then move pairs with source =
		// target to the end
		Collections.sort(loops, new Comparator<Pair<List<String>, List<String>>>() {
			@Override
			public int compare(Pair<List<String>, List<String>> p1, Pair<List<String>, List<String>> p2) {
				// block loop
				if (p2.getSource().size() == p2.getTarget().size()) {
					return -1;
				}
				// First, compare by the size of the source list
				int sizeComparison = Integer.compare(p2.getSource().size(), p1.getSource().size());
				if (sizeComparison != 0) {
					return sizeComparison;
				}

				// If the sizes are the same, check if source equals target and move such pairs
				// to the end
				boolean p1SourceEqualsTarget = p1.getSource().equals(p1.getTarget());
				boolean p2SourceEqualsTarget = p2.getSource().equals(p2.getTarget());
				if (p1SourceEqualsTarget && p2SourceEqualsTarget) {
					return 0;
				} else {
					return -1;
				}

			}
		});
		System.out.println(loops);

		for (Pair<List<String>, List<String>> loop : loops) {
			ArrayList<BPMNElementNode> sourceElements = new ArrayList<>();
			for (String value : loop.getSource()) {
				String sourceId = value.replace(" ", "-").toLowerCase();
				BPMNElementNode sourceElement = (BPMNElementNode) process.findElementById(sourceId);
				sourceElements.add(sourceElement);
			}

			ArrayList<BPMNElementNode> targetElements = new ArrayList<>();
			for (String value : loop.getTarget()) {
				String targetId = value.replace(" ", "-").toLowerCase();
				BPMNElementNode targetElement = (BPMNElementNode) process.findElementById(targetId);
				targetElements.add(targetElement);
			}

			addLoopGateway(sourceElements, targetElements);
		}
		// loop
		// Sort the sublists within each key's value
//		Map<String, LinkedList<LinkedList<String>>> sortedData = sortSublists(loops);
//
//		// Sort the map based on the size of the largest sublist in the values
//		Map<String, LinkedList<LinkedList<String>>> sortedLoop = sortMapByLargestSublistSize(sortedData);
//
//		for (Entry<String, LinkedList<LinkedList<String>>> entry : sortedLoop.entrySet()) {
//			String targetId = entry.getKey().replace(" ", "-").toLowerCase();
//			BPMNElementNode targetElement = (BPMNElementNode) process.findElementById(targetId);
//
//			LinkedList<LinkedList<String>> sources = entry.getValue();
//
//			// Looping through the outer ArrayList
//			for (LinkedList<String> innerArrayList : sources) {
//				ArrayList<BPMNElementNode> sourceElements = new ArrayList<>();
//
//				// Looping through the inner ArrayList
//				for (String value : innerArrayList) {
//					String sourceId = value.replace(" ", "-").toLowerCase();
//					BPMNElementNode sourceElement = (BPMNElementNode) process.findElementById(sourceId);
//					sourceElements.add(sourceElement);
//				}
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
				System.out.println("Add new gateway after the activity");
				process.deleteSequenceFlow(sq.getId());

				// add new gateway
				Map<String, String> gateway = getGatewayType(succeedElement, targetElement);
				String gatewayType = gateway.get(GATEWAY_TYPE);
				Gateway newGateway = process.addGateway("gt-" + gatewayId.toString(), gatewayId.toString(),
						gatewayType);
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
						flowToProbablyGateway);
				if (isAdded == false) {
					System.out
							.println("add new gateway before the existing gateway " + probablySelectedGateway.getId());
//					System.out.println(probablyRelationType.get(GATEWAY_TYPE));
					System.out.println(probablySelectedGateway.getIngoingSequenceFlows().size());
					System.out.println(probablySelectedGateway.getOutgoingSequenceFlows().size());
					// add gateway before probablySelectedGateway
					Gateway newGateway = process.addGateway("gt-" + gatewayId.toString(), gatewayId.toString(),
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
			BPMNElement targetElement, Gateway probablySelectedGateway, SequenceFlow flowToProbablyGateway)
			throws BPMNModelException {

		BPMNProcess process = model.openDefaultProces();
		List<SequenceFlow> acceptedSequenceFlows = new ArrayList<>();
		List<SequenceFlow> allAcceptedSequenceFlows = new ArrayList<>();
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
					allAcceptedSequenceFlows.add(currenctSequenceFlow);
					if (!relationType.get(GATEWAY_NUM).contentEquals(selectedGateway.getAttribute(GATEWAY_NUM))) {
						acceptedSequenceFlows.add(currenctSequenceFlow);
						probablyRelationType = getGatewayType(successor, targetElement);
					}
				}
			} else {
				boolean result = addNewTarget(currenctSequenceFlow, (Gateway) successor, sourceElement, targetElement,
						probablySelectedGateway, flowToProbablyGateway);

				if (result) {
					allAcceptedSequenceFlows.add(currenctSequenceFlow);
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
		if (selectedGateway.getOutgoingSequenceFlows().size() == acceptedSequenceFlows.size()) {
			// probably to add new gateway before selected gateway
			System.out.println("Propbably add before");
//			System.out.println(selectedSequence.getSourceElement().getId());
			flowToProbablyGateway = selectedSequence;
			probablySelectedGateway = selectedGateway;
			return true;
		} else if (selectedGateway.getOutgoingSequenceFlows().size() == allAcceptedSequenceFlows.size()
				&& acceptedSequenceFlows.size() == 0) {
			isAdded = true;
			System.out.println("add as successor");
			// add target as successor
			// add new element as succeed on the gateway
			process.addSequenceFlow("sq-" + seqenceFlowId.toString(), selectedGateway.getId(), targetElement.getId());
			seqenceFlowId++;
			splitGateway = selectedGateway;

		} else if (acceptedSequenceFlows.size() >= 1) {
			isAdded = true;
			System.out.print("Add new gateway after old gateway: ");
			String gatewayType = probablyRelationType.get(GATEWAY_TYPE);
			// TODO: general gateway
			if (gatewayType == null) {
				gatewayType = BPMNTypes.EXCLUSIVE_GATEWAY;
			}
			// add new gateway after selected gateway
			Gateway newGateway = process.addGateway("gt-" + gatewayId.toString(), gatewayId.toString(), gatewayType);
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

			System.out.print(targetElement.getId() + "\n");
			// add 2 new sequence flow
			process.addSequenceFlow("sq-" + seqenceFlowId.toString(), selectedGateway.getId(), newGateway.getId());
			seqenceFlowId++;
			process.addSequenceFlow("sq-" + seqenceFlowId.toString(), newGateway.getId(), targetElement.getId());
			seqenceFlowId++;

		}

//		System.out.println("-------------FINISH--------------");
		return false;
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
	private boolean isShouldSplit(Gateway selectedGateway, BPMNElementNode sourceElement, BPMNElementNode targetElement)
			throws BPMNModelException {

		BPMNProcess process = model.openDefaultProces();
		List<SequenceFlow> acceptedSequenceFlows = new ArrayList<>();
		List<SequenceFlow> listSequenceFlow = selectedGateway.getOutgoingSequenceFlows().stream()
				.collect(Collectors.toList());
		System.out.println("---------------GATEWAY--------------");
		System.out.println(selectedGateway.getId());
		System.out.println(selectedGateway.getType());
//		System.out.println("num=" + selectedGateway.getAttribute(GATEWAY_NUM));
//		System.out.println("next=" + selectedGateway.getOutgoingSequenceFlows().size());
		for (SequenceFlow currenctSequenceFlow : listSequenceFlow) {
			System.out.println("---------------NEW FLOW--------------");
			BPMNElementNode successor = currenctSequenceFlow.getTargetElement();
			System.out.println(successor.getType());
			if (BPMNTypes.BPMN_ACTIVITIES.contains(successor.getType())
					|| BPMNTypes.BPMN_EVENTS.contains(successor.getType())) {
				System.out.println((sourceElement.getId()));
				System.out.println((successor.getId()));

//				System.out.println(relationType.get(GATEWAY_TYPE));
//				System.out.println("num=" + relationType.get(GATEWAY_NUM));
				if (process.isPreceding(sourceElement, successor, successor)) {
					acceptedSequenceFlows.add(currenctSequenceFlow);
				}
			} else {
				boolean result = isShouldSplit((Gateway) successor, sourceElement, targetElement);

				if (!result) {
					acceptedSequenceFlows.add(currenctSequenceFlow);
				}
			}
		}
		System.out.println(selectedGateway.getOutgoingSequenceFlows().size());
		System.out.println(acceptedSequenceFlows.size());
		if (selectedGateway.getOutgoingSequenceFlows().size() == acceptedSequenceFlows.size()) {
			System.out.println("Propbably accepted");
			return false;
		}

		return true;
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
			System.out.println("add new Gateway after preceding Element (task)");
			process.deleteSequenceFlow(sq.getId());
			// add new gateway
			Gateway newGateway = process.addGateway("gt-" + gatewayId.toString(), gatewayId.toString(),
					splitGateway.getType());
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
			addJoinGatewayRec(sourceElement, precedingElement, targetElement);
		}
	}

	private void addJoinGatewayRec(BPMNElementNode sourceElement, BPMNElementNode selectedGateway,
			BPMNElementNode targetElement) throws BPMNModelException {
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
					BPMNElementNode precedingElement = selectedGateway.getIngoingSequenceFlows().iterator().next()
							.getSourceElement();
					System.out.println(precedingElement.getId());
					addJoinGatewayRec(sourceElement, precedingElement, selectedGateway);
				} else {
					System.out.println("------------------------------");
					System.out.println("------------ERROR-------------");
					System.out.println("------------------------------");
				}
			} else {
				System.out.println("add new gateway after the precedingElement (gateway2)");
				// add new gateway after the precedingElement
				Gateway newGateway = process.addGateway("gt-" + gatewayId.toString(), gatewayId.toString(),
						splitGateway.getType());
				newGateway.setAttribute(GATEWAY_NUM, splitGateway.getAttribute(GATEWAY_NUM));
				gatewayId++;
				splitGateway = null;
				process.deleteSequenceFlow(targetElement.getIngoingSequenceFlows().iterator().next().getId());
				// add new sequence flow
				process.addSequenceFlow("sq-" + seqenceFlowId.toString(), selectedGateway.getId(), newGateway.getId());
				seqenceFlowId++;
				process.addSequenceFlow("sq-" + seqenceFlowId.toString(), sourceElement.getId(), newGateway.getId());
				seqenceFlowId++;
				process.addSequenceFlow("sq-" + seqenceFlowId.toString(), newGateway.getId(), targetElement.getId());
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
					System.out.println("Join at the same gateway");
					process.addSequenceFlow("sq-" + seqenceFlowId.toString(), sourceElement.getId(),
							selectedGateway.getId());
					seqenceFlowId++;
					splitGateway = null;
				} else {
					System.out.println("add new gateway after the precedingElement (gateway) " + gatewayId.toString());
					// add new gateway after the precedingElement
					Gateway newGateway = process.addGateway("gt-" + gatewayId.toString(), gatewayId.toString(),
							splitGateway.getType());
					newGateway.setAttribute(GATEWAY_NUM, splitGateway.getAttribute(GATEWAY_NUM));
					gatewayId++;
					splitGateway = null;
					process.deleteSequenceFlow(targetElement.getIngoingSequenceFlows().iterator().next().getId());
					// add new sequence flow
					process.addSequenceFlow("sq-" + seqenceFlowId.toString(), selectedGateway.getId(),
							newGateway.getId());
					seqenceFlowId++;
					process.addSequenceFlow("sq-" + seqenceFlowId.toString(), sourceElement.getId(),
							newGateway.getId());
					seqenceFlowId++;
					process.addSequenceFlow("sq-" + seqenceFlowId.toString(), newGateway.getId(),
							targetElement.getId());
					seqenceFlowId++;
				}
			} else if (acceptedSequenceFlowList.size() >= 2) {
				System.out.println("add new gateway  for accepted element");

				// add new gateway for accepted element
				Gateway newGateway = process.addGateway("gt-" + gatewayId.toString(), gatewayId.toString(),
						splitGateway.getType());
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
					System.out.println("add new gateway  for accepted element");
					process.deleteSequenceFlow(acceptedSequenceFlowList.get(0).getId());

					// add new gateway
					Gateway newGateway = process.addGateway("gt-" + gatewayId.toString(), gatewayId.toString(),
							splitGateway.getType());
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
					addJoinGatewayRec(sourceElement, precedingElement, targetElement);
				}
			} else
			// cannot access the split gateway => should add XOR gateway if the gateway ==
			// parallel
			if (acceptedSequenceFlowList.size() == 0) {
				System.out.println("add new Default XOR");
				// add new gateway for connected to existing element
				String gatewayType = splitGateway.getType();
				String gatewayNum = splitGateway.getAttribute(GATEWAY_NUM);
				// TODO: add generic gateway?
				if (gatewayType.contentEquals(BPMNTypes.PARALLEL_GATEWAY)) {
					gatewayType = BPMNTypes.EXCLUSIVE_GATEWAY;
					gatewayNum = "-1";
				}

				Gateway newGateway = process.addGateway("gt-" + gatewayId.toString(), gatewayId.toString(),
						gatewayType);
				newGateway.setAttribute(GATEWAY_NUM, gatewayNum);
				gatewayId++;
				splitGateway = null;

				SequenceFlow sq = targetElement.getIngoingSequenceFlows().iterator().next();
				System.out.println(sq.getSourceElement().getId());
				// add new sequence flow
				process.addSequenceFlow("sq-" + seqenceFlowId.toString(), selectedGateway.getId(),
						targetElement.getId());
				seqenceFlowId++;
				process.addSequenceFlow("sq-" + seqenceFlowId.toString(), sourceElement.getId(), newGateway.getId());
				seqenceFlowId++;
				process.addSequenceFlow("sq-" + seqenceFlowId.toString(), sq.getSourceElement().getId(),
						newGateway.getId());
				seqenceFlowId++;

				process.deleteSequenceFlow(sq.getId());
			}
		}
	}

	private void addLoopGateway(List<BPMNElementNode> sourceElements, List<BPMNElementNode> targetElements)
			throws BPMNModelException, CloneNotSupportedException {
		System.out.println("LOOP: " + sourceElements.stream().map(src -> src.getId()).collect(Collectors.toList())
				+ "->" + targetElements.stream().map(src -> src.getId()).collect(Collectors.toList()));
		BPMNProcess process = model.openDefaultProces();
		BPMNElementNode targetElement;
		if (targetElements.size() == 1) {
			targetElement = targetElements.get(0);
		} else {
			// get root gateway based on the relation
			List<String> targetIds = targetElements.stream().map(src -> src.getId()).collect(Collectors.toList());
			int num = getGatewayNum(targetIds);
			StringBuffer gatewayNum = new StringBuffer();
			gatewayNum.append(num);
			if (!gatewayNum.toString().contentEquals("-1")) {
				targetElement = model.openDefaultProces().getGateways().stream()
						.filter(gateway -> gateway.getAttribute(GATEWAY_NUM).contentEquals(gatewayNum)
								&& gateway.getIngoingSequenceFlows().size() == 1)
						.findAny().orElse(null);
			} else {
				System.out.println("Error in detect loop targets relation");
				return;
			}
		}

		BPMNElementNode sourceElement = null;
		if (sourceElements.size() == 1) {
			sourceElement = sourceElements.get(0);
		} else {

			int countSqOfsSource = 0;
			for (BPMNElementNode element : sourceElements) {
				if (element.getOutgoingSequenceFlows().size() > 0) {
					countSqOfsSource++;
				}
			}

			if (countSqOfsSource != sourceElements.size()) {
				System.out.println("should connect the activities to construct a block");
				// new source should be selected
			} else {

				// get sourceElement (The merge or the)
				List<BPMNElementNode> gatewaysofSource = sourceElements.stream()
						.map(src -> src.getOutgoingSequenceFlows()).flatMap(Set::stream)
						.map(sq -> sq.getTargetElement()).collect(Collectors.toList());
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
		}

//		BPMNProcess processWithoutLoop = modelWithoutLoops.openDefaultProces();

		if (sourceElement.getOutgoingSequenceFlows().size() == 0) {
			// add only one gateway before the target
			System.out.println("add new loop gateway before the target");
			Gateway targetGateway = process.addGateway("gt-" + gatewayId.toString(), gatewayId.toString(),
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
			process.addSequenceFlow("sq-" + seqenceFlowId.toString(), targetElement.getId(), targetGateway.getId());
			seqenceFlowId++;
		} else {
			System.out.println("add new loop gateways (2) before the target, and after source");
			// add new gateway after source ( to start loop)
			Gateway sourceGateway = process.addGateway("gt-" + gatewayId.toString(), gatewayId.toString(),
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
			Gateway targetGateway = process.addGateway("gt-" + gatewayId.toString(), gatewayId.toString(),
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

//		String[] args = new  String[]{  
//				"-o", "C:\\Users\\AliNourEldin\\Desktop\\bpmn-layout\\bpmn-auto-layout\\test\\fixtures\\testt2.bpmn",
//				"C:\\Users\\AliNourEldin\\Desktop\\bpmn-layout\\bpmn-auto-layout\\test\\fixtures\\testt.bpmn"};
//		try {
//			App.main(args);
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		logger.info("...model creation sucessful");
	}

	/**
	 * These functions are used for sort the data in loop maps
	 * 
	 * @param loops2
	 * @return
	 */
	public static Map<String, LinkedList<LinkedList<String>>> sortSublists(
			Map<String, LinkedList<LinkedList<String>>> loops2) {
		Map<String, LinkedList<LinkedList<String>>> sortedMap = new HashMap<>();
		for (Entry<String, LinkedList<LinkedList<String>>> entry : loops2.entrySet()) {
			LinkedList<LinkedList<String>> sublists = entry.getValue();
			sublists.sort((list1, list2) -> Integer.compare(list2.size(), list1.size()));
			sortedMap.put(entry.getKey(), sublists);
		}
		return sortedMap;
	}

	public static Map<String, LinkedList<LinkedList<String>>> sortMapByLargestSublistSize(
			Map<String, LinkedList<LinkedList<String>>> map) {
		LinkedList<Map.Entry<String, LinkedList<LinkedList<String>>>> entries = new LinkedList<>(map.entrySet());
		entries.sort((entry1, entry2) -> {
			int maxSize1 = entry1.getValue().stream().mapToInt(List::size).max().orElse(0);
			int maxSize2 = entry2.getValue().stream().mapToInt(List::size).max().orElse(0);
			return Integer.compare(maxSize2, maxSize1);
		});

		Map<String, LinkedList<LinkedList<String>>> sortedMap = new LinkedHashMap<>();
		for (Entry<String, LinkedList<LinkedList<String>>> entry : entries) {
			sortedMap.put(entry.getKey(), entry.getValue());
		}
		return sortedMap;
	}

	public static void main(String[] args) {
		// Start timing
		long startTime = System.nanoTime();
//		System.out.println(args.length);
		if (args.length == 6) {
			/**
			 * "start_a" "[end_f,end_b]" "[a->b, a->c, a->d, a->e, c->f, d->f, e->f,
			 * start_a->a, b->end_b, f->end_f]" "[[b, c, d, e], [c, d]]" "[[c, e, d]]"
			 */
			String output = args[0];
			String startEventString = args[1];
			String endEventsString = args[2];
			String dependencyRelationsString = args[3];
			String parallelRelationString = args[4];
			String decisionRelationString = args[5];

			List<String> startsEvents = new ArrayList<>();
			startsEvents.add(startEventString);
			List<String> endEvents = stringToList(endEventsString);
			LinkedList<String> events = stringToList(dependencyRelationsString);

			LinkedList<LinkedList<String>> decisionRelations = transformStringToList(decisionRelationString);
			System.out.println(decisionRelations);
			System.out.println(decisionRelations.size());

			LinkedList<LinkedList<String>> parallelRelations = transformStringToList(parallelRelationString);
			System.out.println(parallelRelations);
			System.out.println(parallelRelations.size());

//			System.out.println(events);
			LinkedList<String> orderedEvents = DepthFirstSearch.DFSToList(events, startEventString);
//			System.out.println(orderedEvents);

			Map<String, LinkedList<LinkedList<String>>> relations = new LinkedHashMap<>();
			relations.put(DECISION, decisionRelations);

			relations.put(PARALLEL, parallelRelations);
			System.out.println(parallelRelations);
			// Initialize the HashMap

			LinkedList<Pair<List<String>, List<String>>> loops = new LinkedList<>();

			try {
				BPMNDiscovery bpmnDiscovery = new BPMNDiscovery(startsEvents, endEvents, orderedEvents, relations,
						loops);
				bpmnDiscovery.DependencyGraphToBPMN();
				System.out.println("Finish discovery ...");
				bpmnDiscovery.saveMode(output);
			} catch (BPMNModelException e) {
				e.printStackTrace();
			} catch (CloneNotSupportedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
//			String output = path;
//			String startEventString = "Customer brings in a defective computer";
//			String endEventsString = "[Customer takes the computer home unrepaired,Repair process is considered complete]";
//			String dependencyRelationsString = "[Customer brings in a defective computer->Bring in defective computer,Bring in defective computer->Check defect and calculate repair cost,Check defect and calculate repair cost->Present repair cost,Present repair cost->Decide on repair cost acceptability,Decide on repair cost acceptability->Check and repair hardware,Decide on repair cost acceptability->Check and configure software,Decide on repair cost acceptability->Take computer home unrepaired,Check and repair hardware->Test system functionality,Check and configure software->Test system functionality,Test system functionality->Consider repair process complete,Consider repair process complete->Repair process is considered complete,Take computer home unrepaired->Customer takes the computer home unrepaired]";
//			String parallelRelationString = "[[Check and configure software,Check and repair hardware]]";
//			String decisionRelationString = " [[Take computer home unrepaired, Check and configure software, Check and repair hardware]]";
//
//			List<String> startsEvents = new ArrayList<>();
//			startsEvents.add(startEventString);
//			List<String> endEvents = stringToList(endEventsString);
//			LinkedList<String> events = stringToList(dependencyRelationsString);
//
//			LinkedList<LinkedList<String>> decisionRelations = transformStringToList(decisionRelationString);
//			System.out.println(decisionRelations);
//			System.out.println(decisionRelations.size());
//
//			LinkedList<LinkedList<String>> parallelRelations = transformStringToList(parallelRelationString);
//			System.out.println(parallelRelations);
//			System.out.println(parallelRelations.size());
//
////			System.out.println(events);
//			LinkedList<String> orderedEvents = DepthFirstSearch.DFSToList(events, startEventString);
////			System.out.println(orderedEvents);
//
//			Map<String, LinkedList<LinkedList<String>>> relations = new LinkedHashMap<>();
//			relations.put(DECISION, decisionRelations);
//			relations.put(PARALLEL, parallelRelations);
//			try {
//				BPMNDiscovery bpmnDiscovery = new BPMNDiscovery(startsEvents, endEvents, orderedEvents, relations);
//				bpmnDiscovery.DependencyGraphToBPMN();
//				System.out.println("Finish discovery ...");
//				bpmnDiscovery.saveMode(output);
//			} catch (BPMNModelException e) {
//				e.printStackTrace();
//			}
			System.out.println(
					"INVALID INPUTS: outputPath (/src/example.bpmn), startEvent, endEvents (List), dependencyRelations(List), parallelRelation (List[List]), decisionRelation(List[List])");
			// correct
//			example1();
			// correct
			// example2();
			// correct
			// example3();
			// correct
			// example4();
			// double loop
//			example5();
//			 example6();
			// three loops
			example7();
//			example8();
		}
		long endTime = System.nanoTime();
		long duration = (endTime - startTime) / 1_000_000; // Convert nanoseconds to milliseco
		System.out.println("Execution time: " + duration + " ms");
	}

	private static LinkedList<String> stringToList(String data) {
		// Remove the surrounding square brackets
		data = data.substring(1, data.length() - 1);

		// Split the string by commas
		String[] elements = data.split(",\\s*");

		// Create a list and add the elements
		LinkedList<String> list = new LinkedList<>();
		for (String element : elements) {
			list.add(element.trim());
		}
		return list;
	}

	private static LinkedList<LinkedList<String>> transformStringToList(String data) {
		LinkedList<LinkedList<String>> outerList = new LinkedList<>();

		// Remove the outer brackets
		String trimmedInput = data.substring(1, data.length() - 1);
		if (!trimmedInput.isEmpty()) {
			// Handle the case for a single list inside the outer brackets
			if (trimmedInput.charAt(0) == '[') {
				trimmedInput = trimmedInput.substring(1, trimmedInput.length() - 1);
			}

			// Split into individual list strings
			String[] listStrings = trimmedInput.split("],\\[");

			for (String listString : listStrings) {
				// Remove any remaining brackets
				listString = listString.replaceAll("[\\[\\]]", "");

				// Split the string by commas and convert to a list
				LinkedList<String> innerList = new LinkedList<>(Arrays.asList(listString.split(",")));
				innerList.replaceAll(String::trim);
				innerList.replaceAll(s -> s.replace(" ", "-"));
				innerList.replaceAll(String::toLowerCase);
				// Add the inner list to the outer list
				outerList.add(innerList);
			}
		}
		return outerList;
	}

	static String path = "C:\\Users\\AliNourEldin\\Desktop\\testt.bpmn";

	// the ex use to describe the algo in the confluence page
	private static void example1() {
		LinkedList<String> events = new LinkedList();
		events.add("start->a");

		events.add("a->b");
		events.add("a->c");
		events.add("a->d");
		events.add("a->e");

		events.add("b->end");

		events.add("d->f");
		events.add("c->f");

		events.add("e->f");

		events.add("f->end1");

		events.add("a->g");
		events.add("g->f");
//		events.add("f->a");

		String startEvent = "start";

		LinkedList<String> orderedEvents = DepthFirstSearch.DFSToList(events, startEvent);
		System.out.println(orderedEvents);
		List<String> startsEvent = new ArrayList<>();
		startsEvent.add("start");

		List<String> endEvents = new ArrayList<>();
		endEvents.add("end");
		endEvents.add("end1");

		LinkedList<LinkedList<String>> decisionRelations = new LinkedList();
		decisionRelations.add(new LinkedList<String>() {
			{
				add("b");
				add("c");
				add("d");
				add("e");
				add("g");
			}
		});
		decisionRelations.add(new LinkedList<String>() {
			{
				add("c");
				add("d");
			};

		});

		LinkedList<LinkedList<String>> parallelRelations = new LinkedList();
		parallelRelations.add(new LinkedList<String>() {
			{
				add("c");
				add("e");
				add("d");
			}
		});

		Map<String, LinkedList<LinkedList<String>>> relations = new LinkedHashMap<>();
		relations.put(DECISION, decisionRelations);
		relations.put(PARALLEL, parallelRelations);
		// Initialize the HashMap
		LinkedList<Pair<List<String>, List<String>>> loops = new LinkedList<>();

		try {
			BPMNDiscovery bpmnDiscovery = new BPMNDiscovery(startsEvent, endEvents, orderedEvents, relations, loops);
			bpmnDiscovery.DependencyGraphToBPMN();
			BPMNElementNode gateway = bpmnDiscovery.model.findElementNodeById("gt-3");
			System.out.println(gateway.getType());

			System.out.println("Finish discovery ...");
			bpmnDiscovery.saveMode(path);
		} catch (BPMNModelException e) {
			e.printStackTrace();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
	}

	// ex1 in tested on in the page confluence
	private static void example2() {
		LinkedList<String> events = new LinkedList();
		events.add("start->a");
		events.add("a->b");
		events.add("a->c");

		events.add("b->d");
		events.add("b->g");

		events.add("c->f");
		events.add("c->e");

		events.add("d->f");
		events.add("e->g");

		events.add("f->h");
		events.add("g->h");
		events.add("h->end");

		String startEvent = "start";

		LinkedList<String> orderedEvents = DepthFirstSearch.DFSToList(events, startEvent);
		System.out.println(orderedEvents);
		List<String> startsEvent = new ArrayList<>();
		startsEvent.add("start");

		List<String> endEvents = new ArrayList<>();
		endEvents.add("end");

		LinkedList<LinkedList<String>> decisionRelations = new LinkedList();
		decisionRelations.add(new LinkedList<String>() {
			{
				add("c");
				add("b");
			}

		});

		LinkedList<LinkedList<String>> parallelRelations = new LinkedList();
		parallelRelations.add(new LinkedList<String>() {
			{
				add("d");
				add("g");
			}

		});
		parallelRelations.add(new LinkedList<String>() {
			{
				add("e");
				add("f");
			}

		});

		Map<String, LinkedList<LinkedList<String>>> relations = new LinkedHashMap<>();
		relations.put(DECISION, decisionRelations);
		relations.put(PARALLEL, parallelRelations);

		// Initialize the HashMap
		LinkedList<Pair<List<String>, List<String>>> loops = new LinkedList<>();

		try {
			BPMNDiscovery bpmnDiscovery = new BPMNDiscovery(startsEvent, endEvents, orderedEvents, relations, loops);
			bpmnDiscovery.DependencyGraphToBPMN();
			bpmnDiscovery.saveMode(path);
		} catch (BPMNModelException | CloneNotSupportedException e) {
			e.printStackTrace();
		}
	}

	// ex2 in tested on in the page confluence
	private static void example3() {
		LinkedList<String> events = new LinkedList();
		events.add("start->a");
		events.add("start->b");

		events.add("a->c");
		events.add("a->j");

		events.add("b->d");
		events.add("b->j");

		events.add("c->i");
		events.add("j->i");

		events.add("d->k");
		events.add("i->k");

		events.add("k->end");

		String startEvent = "start";

		LinkedList<String> orderedEvents = DepthFirstSearch.DFSToList(events, startEvent);
		System.out.println(orderedEvents);
		List<String> startsEvent = new ArrayList<>();
		startsEvent.add("start");

		List<String> endEvents = new ArrayList<>();
		endEvents.add("end");

		LinkedList<LinkedList<String>> decisionRelations = new LinkedList();
		decisionRelations.add(new LinkedList<String>() {
			{
				add("c");
				add("b");
			}

		});
		decisionRelations.add(new LinkedList<String>() {
			{
				add("c");
				add("j");
			}

		});
		decisionRelations.add(new LinkedList<String>() {
			{
				add("d");
				add("j");
			}

		});

		LinkedList<LinkedList<String>> parallelRelations = new LinkedList();

		Map<String, LinkedList<LinkedList<String>>> relations = new LinkedHashMap<>();
		relations.put(DECISION, decisionRelations);
		relations.put(PARALLEL, parallelRelations);

		// Initialize the HashMap
		LinkedList<Pair<List<String>, List<String>>> loops = new LinkedList<>();

		try {
			BPMNDiscovery bpmnDiscovery = new BPMNDiscovery(startsEvent, endEvents, orderedEvents, relations, loops);
			bpmnDiscovery.DependencyGraphToBPMN();
			bpmnDiscovery.saveMode(path);
		} catch (BPMNModelException e) {
			e.printStackTrace();
		} catch (CloneNotSupportedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// ex3 in tested on in the page confluence
	private static void example4() {
		LinkedList<String> events = new LinkedList();
		events.add("start->a");
		events.add("a->b");
		events.add("a->c");
		events.add("a->d");

		events.add("b->e");
		events.add("b->f");

		events.add("e->h");
		events.add("f->g");

		events.add("g->h");

		events.add("c->g");
		events.add("d->g");

		events.add("h->end");

		String startEvent = "start";

		LinkedList<String> orderedEvents = DepthFirstSearch.DFSToList(events, startEvent);
		System.out.println(orderedEvents);
		List<String> startsEvent = new ArrayList<>();
		startsEvent.add("start");

		List<String> endEvents = new ArrayList<>();
		endEvents.add("end");

		LinkedList<LinkedList<String>> decisionRelations = new LinkedList();
		decisionRelations.add(new LinkedList<String>() {
			{
				add("c");
				add("d");
			}

		});
		decisionRelations.add(new LinkedList<String>() {
			{
				add("e");
				add("f");
			}

		});

		LinkedList<LinkedList<String>> parallelRelations = new LinkedList();
		parallelRelations.add(new LinkedList<String>() {
			{
				add("b");
				add("c");
				add("d");
			}

		});

		Map<String, LinkedList<LinkedList<String>>> relations = new LinkedHashMap<>();
		relations.put(DECISION, decisionRelations);
		relations.put(PARALLEL, parallelRelations);

		// Initialize the HashMap
		LinkedList<Pair<List<String>, List<String>>> loops = new LinkedList<>();

		try {
			BPMNDiscovery bpmnDiscovery = new BPMNDiscovery(startsEvent, endEvents, orderedEvents, relations, loops);
			bpmnDiscovery.DependencyGraphToBPMN();
			bpmnDiscovery.saveMode(path);
		} catch (BPMNModelException | CloneNotSupportedException e) {
			e.printStackTrace();
		}
	}

	// double loop
	private static void example5() {
		LinkedList<String> events = new LinkedList();
		events.add("start->a");

		events.add("a->b");
		events.add("a->c");
		events.add("a->d");
		events.add("a->e");

		events.add("b->end");

		events.add("d->f");
		events.add("c->f");

		events.add("e->f");

		events.add("f->end1");

		events.add("a->g");
		events.add("g->f");

		// loop
		events.add("c->a");
		events.add("f->a");

		String startEvent = "start";

		LinkedList<String> orderedEvents = DepthFirstSearch.DFSToList(events, startEvent);
		System.out.println(orderedEvents);
		List<String> startsEvent = new ArrayList<>();
		startsEvent.add("start");

		List<String> endEvents = new ArrayList<>();
		endEvents.add("end");
		endEvents.add("end1");

		LinkedList<LinkedList<String>> decisionRelations = new LinkedList();
		decisionRelations.add(new LinkedList<String>() {
			{
				add("b");
				add("c");
				add("d");
				add("e");
				add("g");
			}
		});
		decisionRelations.add(new LinkedList<String>() {
			{
				add("c");
				add("d");
			};

		});

		LinkedList<LinkedList<String>> parallelRelations = new LinkedList();
		parallelRelations.add(new LinkedList<String>() {
			{
				add("c");
				add("d");
				add("e");
				add("g");
			}
		});

		Map<String, LinkedList<LinkedList<String>>> relations = new LinkedHashMap<>();
		relations.put(DECISION, decisionRelations);
		relations.put(PARALLEL, parallelRelations);

		// Initialize the HashMap
		LinkedList<Pair<List<String>, List<String>>> loops = new LinkedList<>();

		try {
			BPMNDiscovery bpmnDiscovery = new BPMNDiscovery(startsEvent, endEvents, orderedEvents, relations, loops);
			bpmnDiscovery.DependencyGraphToBPMN();
			bpmnDiscovery.saveMode(path);
		} catch (BPMNModelException | CloneNotSupportedException e) {
			e.printStackTrace();
		}
	}

	// exercice nour
	private static void example6() {
		LinkedList<String> events = new LinkedList();
		events.add("start->a");

		events.add("a->b");
		events.add("b->g");
		events.add("g->i");
		events.add("i->end");

		events.add("a->c");
		events.add("c->g");
		events.add("c->h");

		events.add("h->end1");

		events.add("a->d");
		events.add("d->e");
		events.add("e->end2");

		String startEvent = "start";

		LinkedList<String> orderedEvents = DepthFirstSearch.DFSToList(events, startEvent);
		System.out.println(orderedEvents);
		List<String> startsEvent = new ArrayList<>();
		startsEvent.add("start");

		List<String> endEvents = new ArrayList<>();
		endEvents.add("end");
		endEvents.add("end1");
		endEvents.add("end2");

		LinkedList<LinkedList<String>> decisionRelations = new LinkedList();
		decisionRelations.add(new LinkedList<String>() {
			{
				add("b");
				add("c");
			}
		});
		decisionRelations.add(new LinkedList<String>() {
			{
				add("g");
				add("h");
			};

		});

		LinkedList<LinkedList<String>> parallelRelations = new LinkedList();
		parallelRelations.add(new LinkedList<String>() {
			{
				add("b");
				add("c");
				add("d");
			}
		});
//		parallelRelations.add(new LinkedList<String>() {
//			{
//				add("c");
//				
//			}
//		});

		Map<String, LinkedList<LinkedList<String>>> relations = new LinkedHashMap<>();
		relations.put(DECISION, decisionRelations);
		relations.put(PARALLEL, parallelRelations);
		// Initialize the HashMap
		LinkedList<Pair<List<String>, List<String>>> loops = new LinkedList<>();

		try {
			BPMNDiscovery bpmnDiscovery = new BPMNDiscovery(startsEvent, endEvents, orderedEvents, relations, loops);
			bpmnDiscovery.DependencyGraphToBPMN();
			bpmnDiscovery.saveMode(path);
		} catch (BPMNModelException e) {
			e.printStackTrace();
		} catch (CloneNotSupportedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// three loops
	private static void example7() {
		LinkedList<String> list = new LinkedList<>();
		list.add("start->a");
		list.add("a->f");
		list.add("a->b");
		list.add("a->c");
		list.add("a->d");
		list.add("c->g");
		list.add("d->e");
		list.add("e->g");
		list.add("f->g");
		list.add("g->end1");
		list.add("b->end2");
//		list.add("c->a");
//		list.add("f->a");
//		list.add("e->a");
//		list.add("g->a");
//	        list.add("g->z");
//	        list.add("z->g");

		String startEvent = "start";

		LinkedList<String> orderedEvents = DepthFirstSearch.DFSToList(list, startEvent);
		System.out.println(orderedEvents);
		List<String> startsEvent = new ArrayList<>();
		startsEvent.add("start");

		List<String> endEvents = new ArrayList<>();
		endEvents.add("end1");
		endEvents.add("end2");

		LinkedList<LinkedList<String>> decisionRelations = new LinkedList();
		decisionRelations.add(new LinkedList<String>() {
			{
				add("c");
				add("f");
				add("d");
				add("b");
			}
		});
		decisionRelations.add(new LinkedList<String>() {
			{
				add("d");
				add("c");
			};

		});
		decisionRelations.add(new LinkedList<String>() {
			{
				add("g");
				add("a");
			};

		});
//		decisionRelations.add(new LinkedList<String>() {
//			{
//				add("end1");
//				add("z");
//				add("a");
//			};
//		});
		decisionRelations.add(new LinkedList<String>() {
			{
				add("end1");
				add("a");
			};

		});
		LinkedList<LinkedList<String>> parallelRelations = new LinkedList();
		parallelRelations.add(new LinkedList<String>() {
			{
				add("f");
				add("d");
				add("c");
			}
		});

		Map<String, LinkedList<LinkedList<String>>> relations = new LinkedHashMap<>();
		relations.put(DECISION, decisionRelations);
		relations.put(PARALLEL, parallelRelations);

		// Initialize the HashMap
//		HashMap<String, LinkedList<LinkedList<String>>> loops = new HashMap<>();

		LinkedList<Pair<List<String>, List<String>>> loops = new LinkedList<>();

		loops.add(new Pair<>(Arrays.asList("c", "e", "f"), Arrays.asList("a")));
		loops.add(new Pair<>(Arrays.asList("a"), Arrays.asList("a")));
		loops.add(new Pair<>(Arrays.asList("g"), Arrays.asList("g")));
		loops.add(new Pair<>(Arrays.asList("f"), Arrays.asList("f", "c", "d")));

		loops.add(new Pair<>(Arrays.asList("g"), Arrays.asList("a")));

		// Populate the map with values
//		loops.put("a", new LinkedList<>(Arrays.asList(new LinkedList<>(Arrays.asList("c", "e", "f"))
////				,new LinkedList<>(Arrays.asList("a")), new LinkedList<>(Arrays.asList("g"))
//				)));
//		loops.put("g", new LinkedList<>(Arrays.asList(new LinkedList<>(Arrays.asList("g")))));

		try {
			BPMNDiscovery bpmnDiscovery = new BPMNDiscovery(startsEvent, endEvents, orderedEvents, relations, loops);
			bpmnDiscovery.DependencyGraphToBPMN();
			bpmnDiscovery.saveMode(path);
		} catch (BPMNModelException e) {
			e.printStackTrace();
		} catch (CloneNotSupportedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// closed loops
	private static void example8() {
		LinkedList<String> list = new LinkedList<>();
		list.add("start->a");
		list.add("a->b");
		list.add("b->c");
		list.add("b->e");
		list.add("b->d");
		list.add("b->g");
		list.add("g->end1");

		list.add("c->z");
		list.add("z->end2");

		list.add("c->k");
		list.add("k->end3");

		list.add("e->z");
		list.add("e->k");

		list.add("d->f");
		list.add("f->z");
		list.add("f->k");
		String startEvent = "start";

		LinkedList<String> orderedEvents = DepthFirstSearch.DFSToList(list, startEvent);
		System.out.println(orderedEvents);
		List<String> startsEvent = new ArrayList<>();
		startsEvent.add("start");

		List<String> endEvents = new ArrayList<>();
		endEvents.add("end1");
		endEvents.add("end2");
		endEvents.add("end3");

		LinkedList<LinkedList<String>> decisionRelations = new LinkedList();
		decisionRelations.add(new LinkedList<String>() {
			{
				add("c");
				add("e");
				add("d");
				add("g");
			}
		});
		decisionRelations.add(new LinkedList<String>() {
			{
				add("z");
				add("k");
			};

		});
		decisionRelations.add(new LinkedList<String>() {
			{
				add("e");
				add("d");
			};

		});
		LinkedList<LinkedList<String>> parallelRelations = new LinkedList();
		parallelRelations.add(new LinkedList<String>() {
			{
				add("c");
				add("e");
				add("d");
			};

		});
		Map<String, LinkedList<LinkedList<String>>> relations = new LinkedHashMap<>();
		relations.put(DECISION, decisionRelations);
		relations.put(PARALLEL, parallelRelations);

		// Initialize the HashMap
		LinkedList<Pair<List<String>, List<String>>> loops = new LinkedList<>();

		// Populate the map with values
//			loops.put("a", new LinkedList<>(Arrays.asList(new LinkedList<>(Arrays.asList("c", "e", "f")),
//					new LinkedList<>(Arrays.asList("a")), new LinkedList<>(Arrays.asList("g")))));
//			loops.put("g", new LinkedList<>(Arrays.asList(new LinkedList<>(Arrays.asList("g")))));

		try {
			BPMNDiscovery bpmnDiscovery = new BPMNDiscovery(startsEvent, endEvents, orderedEvents, relations, loops);
			bpmnDiscovery.DependencyGraphToBPMN();
			bpmnDiscovery.saveMode(path);
		} catch (BPMNModelException e) {
			e.printStackTrace();
		} catch (CloneNotSupportedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
