package org.openbpmn.bpmn.discovery;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.openbpmn.bpmn.BPMNModel;
import org.openbpmn.bpmn.BPMNTypes;
import org.openbpmn.bpmn.discovery.model.DepthFirstSearch;
import org.openbpmn.bpmn.elements.Activity;
import org.openbpmn.bpmn.elements.BPMNProcess;
import org.openbpmn.bpmn.elements.Gateway;
import org.openbpmn.bpmn.elements.SequenceFlow;
import org.openbpmn.bpmn.elements.core.BPMNElement;
import org.openbpmn.bpmn.elements.core.BPMNElementNode;
import org.openbpmn.bpmn.exceptions.BPMNInvalidIDException;
import org.openbpmn.bpmn.exceptions.BPMNModelException;
import org.openbpmn.bpmn.util.BPMNModelFactory;

import io.process.analytics.tools.bpmn.generator.App;
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
	List<String> startsEvent;
	List<String> endsEvent;
	LinkedList<String> dependencies;
	Map<String, LinkedList<LinkedList<String>>> relations;
	Integer seqenceFlowId = 0;
	Integer gatewayId = 0;
	Gateway splitGateway = null;
	Boolean isAdded = false;
	Boolean isLoop = false;

	public BPMNDiscovery(List<String> startsEvent, List<String> endsEvent, LinkedList<String> dependencies,
			Map<String, LinkedList<LinkedList<String>>> relations) {
		logger.info("...creating new empty model");
		model = BPMNModelFactory.createInstance("demo", "1.0.0", "http://org.openbpmn");

		this.startsEvent = startsEvent;
		this.endsEvent = endsEvent;
		this.dependencies = dependencies;
		this.relations = relations;
	}

	/**
	 * This function is to convert Dependency graph to BPMN
	 * 
	 * @param startsEvent
	 * @param endsEvent
	 * @param dependencies the relation between pair of element
	 * @param relations    it contains the decision and parallel relations as list
	 * @throws BPMNModelException
	 */
	public void DependencyGraphToBPMN() throws BPMNModelException {

		BPMNProcess process = model.openDefaultProces();
		// it is used for the sequence flow
		LinkedList<String> loopOperations = new LinkedList();
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
					if (process.isPreceding(targetElement, sourceElement)) {
						loopOperations.add(dependency);
					} else {
						if (splitGateway == null) {
							// probably split
							if (sourceElement.getOutgoingSequenceFlows().size() > 0) {
								// call split gateway function
								addSplitGateway(sourceElement, targetElement);
							}
							// AND //OR

							// probably XOR join
							if (targetElement.getIngoingSequenceFlows().size() > 1) {
								System.out.println("add XOR gateway before the existing activity");
								// add gateway before activity
								Gateway newGateway = process.addGateway("gt-" + gatewayId.toString(), "",
										BPMNTypes.EXCLUSIVE_GATEWAY);
								newGateway.setAttribute(GATEWAY_NUM, "-3");
								gatewayId++;
								splitGateway = newGateway;

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
								seqenceFlowId++;

							}
						} else {

							// call join gateway function
							addJoinGateway(sourceElement, targetElement);
						}
					}

				}
			}

		}

		// call PostProcessing()

		// loop
		for (String dependency : loopOperations) {
			// Split the dependency string into source and target
			String[] parts = dependency.split("->");

			// Extract source and target
			String source = parts[0];
			String target = parts[1];
			BPMNElementNode sourceElement = (BPMNElementNode) process.findElementById(source);
			BPMNElementNode targetElement = (BPMNElementNode) process.findElementById(target);
			addLoopGateway(sourceElement, targetElement);
		}
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
			if ( // check activity; TODO: look at the type
			succeedElement.getType().contentEquals(BPMNTypes.TASK)) {
				System.out.println("Add new gateway after the activity");
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
						flowToProbablyGateway);
				if (isAdded == false) {
					System.out.println("add new gateway before the existing gateway");
//					System.out.println(probablyRelationType.get(GATEWAY_TYPE));
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
			// TODO: take into account events and activities
			if (successor.getType().equals(BPMNTypes.TASK) || successor.getType().equals(BPMNTypes.END_EVENT)) {
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
		// TODO: look at the type
		if (precedingElement.getType().contentEquals(BPMNTypes.TASK)) {
//			if (process.isPreceding(splitGateway, precedingElement)) {
			System.out.println("add new Gateway after preceding Element (task)");
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
			addJoinGatewayRec(sourceElement, precedingElement, targetElement);
		}
	}

	private void addJoinGatewayRec(BPMNElementNode sourceElement, BPMNElementNode selectedGateway,
			BPMNElementNode targetElement) throws BPMNModelException {
		System.out.println("JOIN-REC: " + sourceElement.getId() + "->" + targetElement.getId());
		BPMNProcess process = model.openDefaultProces();

//		// get target sequence flow
//		SequenceFlow sq = targetElement.getIngoingSequenceFlows().iterator().next();
//		// get succeed
//		BPMNElementNode precedingElement = sq.getSourceElement();
		// is gateway
		Integer flowCounter = 0;
		List<SequenceFlow> acceptedSequenceFlowList = new ArrayList<>();
		List<SequenceFlow> sqflowList = selectedGateway.getIngoingSequenceFlows().stream().collect(Collectors.toList());

		for (SequenceFlow currenctSecquenceFlow : sqflowList) {
			BPMNElementNode currenctProcessor = currenctSecquenceFlow.getSourceElement();
//			System.out.println(currenctProcessor.getName());
//			System.out.println(currenctProcessor.getType());
			flowCounter++;
			if (process.isPreceding(splitGateway, currenctProcessor)) {
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
				System.out.println("add new gateway after the precedingElement (gateway)");
				// add new gateway after the precedingElement
				Gateway newGateway = process.addGateway("gt-" + gatewayId.toString(), "", splitGateway.getType());
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
//				process.deleteSequenceFlow(sq.getId());
			}
		} else if (acceptedSequenceFlowList.size() >= 2) {
			System.out.println("add new gateway  for accepted element");

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
//			System.out.println("open 1 accepted");
			BPMNElementNode precedingElement = acceptedSequenceFlowList.get(0).getSourceElement();
//			System.out.println(precedingElement.getId());
			if (precedingElement.getType().contentEquals(BPMNTypes.TASK)) {
				System.out.println("add new gateway  for accepted element");
				process.deleteSequenceFlow(acceptedSequenceFlowList.get(0).getId());
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
				process.addSequenceFlow("sq-" + seqenceFlowId.toString(), newGateway.getId(), selectedGateway.getId());
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

			Gateway newGateway = process.addGateway("gt-" + gatewayId.toString(), "", gatewayType);
			newGateway.setAttribute(GATEWAY_NUM, gatewayNum);
			gatewayId++;
			splitGateway = null;

			SequenceFlow sq = targetElement.getIngoingSequenceFlows().iterator().next();
			System.out.println(sq.getSourceElement().getId());
			// add new sequence flow
			process.addSequenceFlow("sq-" + seqenceFlowId.toString(), selectedGateway.getId(), targetElement.getId());
			seqenceFlowId++;
			process.addSequenceFlow("sq-" + seqenceFlowId.toString(), sourceElement.getId(), newGateway.getId());
			seqenceFlowId++;
			process.addSequenceFlow("sq-" + seqenceFlowId.toString(), sq.getSourceElement().getId(),
					newGateway.getId());
			seqenceFlowId++;

			process.deleteSequenceFlow(sq.getId());

		}
	}

	private void addLoopGateway(BPMNElementNode sourceElement, BPMNElementNode targetElement)
			throws BPMNModelException {
		System.out.println("LOOP: " + sourceElement.getId() + "->" + targetElement.getId());
		BPMNProcess process = model.openDefaultProces();
		if (targetElement.getOutgoingSequenceFlows().size() > 0) {

//			System.out.println("add new loop gateways");
//			// add new gateway after source ( to start loop)
//			Gateway sourceGateway = process.addGateway("gt-" + gatewayId.toString(), "", BPMNTypes.EXCLUSIVE_GATEWAY);
//			sourceGateway.setAttribute(GATEWAY_NUM, "-2");
//			gatewayId++;
//
//			// get element connected to the target
//			SequenceFlow sqflow = sourceElement.getOutgoingSequenceFlows().iterator().next();
//			process.addSequenceFlow("sq-" + seqenceFlowId.toString(), sourceGateway.getId(),
//					sqflow.getTargetElement().getId());
//			seqenceFlowId++;
//			process.deleteSequenceFlow(sqflow.getId());
//
//			// add 2 new sequence flow
//			process.addSequenceFlow("sq-" + seqenceFlowId.toString(), sourceElement.getId(), sourceGateway.getId());
//			seqenceFlowId++;
//
//			// add new gateway before target ( end for loop)
//			Gateway targetGateway = process.addGateway("gt-" + gatewayId.toString(), "", BPMNTypes.EXCLUSIVE_GATEWAY);
//			targetGateway.setAttribute(GATEWAY_NUM, "-2");
//			gatewayId++;
//
//			// get element connected to the target
//			sqflow = targetElement.getIngoingSequenceFlows().iterator().next();
//			process.addSequenceFlow("sq-" + seqenceFlowId.toString(), sqflow.getSourceElement().getId(),
//					targetGateway.getId());
//			seqenceFlowId++;
//			process.deleteSequenceFlow(sqflow.getId());
//			// add new sequence flow
//			process.addSequenceFlow("sq-" + seqenceFlowId.toString(), targetGateway.getId(), targetElement.getId());
//			seqenceFlowId++;
//
//			// connect the gateways
//			process.addSequenceFlow("sq-" + seqenceFlowId.toString(), sourceGateway.getId(), targetGateway.getId());
//			seqenceFlowId++;
//
//			splitGateway = null;
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
			try {
				BPMNDiscovery bpmnDiscovery = new BPMNDiscovery(startsEvents, endEvents, orderedEvents, relations);
				bpmnDiscovery.DependencyGraphToBPMN();
				System.out.println("Finish discovery ...");
				bpmnDiscovery.saveMode(output);
			} catch (BPMNModelException e) {
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
			// example5();
			// example6();
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

	static String path = "C:\\Users\\AliNourEldin\\Desktop\\bpmn-layout\\bpmn-auto-layout\\test\\fixtures\\testt.bpmn";

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
		try {
			BPMNDiscovery bpmnDiscovery = new BPMNDiscovery(startsEvent, endEvents, orderedEvents, relations);
			bpmnDiscovery.DependencyGraphToBPMN();
			System.out.println("Finish discovery ...");
			bpmnDiscovery.saveMode(path);
		} catch (BPMNModelException e) {
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

		try {
			BPMNDiscovery bpmnDiscovery = new BPMNDiscovery(startsEvent, endEvents, orderedEvents, relations);
			bpmnDiscovery.DependencyGraphToBPMN();
			bpmnDiscovery.saveMode(path);
		} catch (BPMNModelException e) {
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

		try {
			BPMNDiscovery bpmnDiscovery = new BPMNDiscovery(startsEvent, endEvents, orderedEvents, relations);
			bpmnDiscovery.DependencyGraphToBPMN();
			bpmnDiscovery.saveMode(path);
		} catch (BPMNModelException e) {
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

		try {
			BPMNDiscovery bpmnDiscovery = new BPMNDiscovery(startsEvent, endEvents, orderedEvents, relations);
			bpmnDiscovery.DependencyGraphToBPMN();
			bpmnDiscovery.saveMode(path);
		} catch (BPMNModelException e) {
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

		try {
			BPMNDiscovery bpmnDiscovery = new BPMNDiscovery(startsEvent, endEvents, orderedEvents, relations);
			bpmnDiscovery.DependencyGraphToBPMN();
			bpmnDiscovery.saveMode(path);
		} catch (BPMNModelException e) {
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

		try {
			BPMNDiscovery bpmnDiscovery = new BPMNDiscovery(startsEvent, endEvents, orderedEvents, relations);
			bpmnDiscovery.DependencyGraphToBPMN();
			bpmnDiscovery.saveMode(path);
		} catch (BPMNModelException e) {
			e.printStackTrace();
		}
	}

}
