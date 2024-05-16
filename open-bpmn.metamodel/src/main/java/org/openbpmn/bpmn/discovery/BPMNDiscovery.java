package org.openbpmn.bpmn.discovery;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.openbpmn.bpmn.BPMNModel;
import org.openbpmn.bpmn.BPMNTypes;
import org.openbpmn.bpmn.elements.Activity;
import org.openbpmn.bpmn.elements.BPMNProcess;
import org.openbpmn.bpmn.elements.Gateway;
import org.openbpmn.bpmn.elements.SequenceFlow;
import org.openbpmn.bpmn.elements.core.BPMNElement;
import org.openbpmn.bpmn.elements.core.BPMNElementNode;
import org.openbpmn.bpmn.exceptions.BPMNInvalidIDException;
import org.openbpmn.bpmn.exceptions.BPMNModelException;
import org.openbpmn.bpmn.util.BPMNModelFactory;

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
			boolean isNewTarget = false;
			BPMNElementNode sourceElement = (BPMNElementNode) process.findElementById(source);
			BPMNElementNode targetElement = (BPMNElementNode) process.findElementById(target);

			// element source is not added to the process mode
			if (sourceElement == null) {
				// add new start event
				if (startsEvent.contains(source)) {
					sourceElement = process.addEvent(source, source, BPMNTypes.START_EVENT);
				}
				// add new activity
				else {
					sourceElement = process.addTask(source, source, BPMNTypes.TASK);
				}
				// TODO: Take into account the events
			}

			// element target is not added to the process mode
			if (targetElement == null) {
				isNewTarget = true;
				// add new start event
				if (endsEvent.contains(target)) {
					targetElement = process.addEvent(target, target, BPMNTypes.END_EVENT);
					splitGateway = null;
				}
				// add new activity
				else {
					targetElement = process.addTask(target, target, BPMNTypes.TASK);
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
					if (splitGateway == null) {
						// probably there exists a loop
						if (process.isPreceding(targetElement, sourceElement)) {
							loopOperations.add(dependency);
						} else {

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
						}
					} else {

						// call join gateway function
						addJoinGateway(sourceElement, targetElement);
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
				addNewTarget(sq, selectedGateway, sourceElement, targetElement, probablySelectedGateway,flowToProbablyGateway);
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
					process.addSequenceFlow("sq-" + seqenceFlowId.toString(), flowToProbablyGateway.getSourceElement().getId(), newGateway.getId());
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
			BPMNElement targetElement, Gateway probablySelectedGateway,SequenceFlow flowToProbablyGateway) throws BPMNModelException {

		BPMNProcess process = model.openDefaultProces();
		List<SequenceFlow> acceptedSequenceFlows = new ArrayList<>();
		List<SequenceFlow> allAcceptedSequenceFlows = new ArrayList<>();
		Map<String, String> relationType = new HashMap();
		List<SequenceFlow> listSequenceFlow = selectedGateway.getOutgoingSequenceFlows().stream()
				.collect(Collectors.toList());
//		System.out.println("---------------GATEWAY--------------");
//		System.out.println(selectedGateway.getId());
//		System.out.println(selectedGateway.getType());
//		System.out.println("num="+selectedGateway.getAttribute(GATEWAY_NUM));
//		System.out.println("next="+selectedGateway.getOutgoingSequenceFlows().size());
		for (SequenceFlow currenctSequenceFlow : listSequenceFlow) {
//			System.out.println("---------------NEW FLOW--------------");
			BPMNElementNode successor = currenctSequenceFlow.getTargetElement();
//			System.out.println(successor.getId());
			if (successor.getType().equals(BPMNTypes.TASK)) {
				relationType = getGatewayType(successor, targetElement);
//				System.out.println(relationType.get(GATEWAY_TYPE));
//				System.out.println("num="+relationType.get(GATEWAY_NUM));
				if (!relationType.get(GATEWAY_NUM).contentEquals("-1")) {
					allAcceptedSequenceFlows.add(currenctSequenceFlow);
					if (!relationType.get(GATEWAY_NUM).contentEquals(selectedGateway.getAttribute(GATEWAY_NUM))) {
						acceptedSequenceFlows.add(currenctSequenceFlow);
						probablyRelationType = getGatewayType(successor, targetElement);
					}
				}
			} else {
				boolean result = addNewTarget(currenctSequenceFlow, (Gateway) successor, sourceElement, targetElement,
						probablySelectedGateway,flowToProbablyGateway);

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
//		System.out.println("accepted="+acceptedSequenceFlows.size());
//		System.out.println("all="+allAcceptedSequenceFlows.size());
//		System.out.println("next="+selectedGateway.getOutgoingSequenceFlows().size());
		if (selectedGateway.getOutgoingSequenceFlows().size() == acceptedSequenceFlows.size()) {
			// probably to add new gateway before selected gateway
			System.out.println("Propbably add before");
			flowToProbablyGateway = selectedSequence;
			probablySelectedGateway = selectedGateway;
			return true;
		} else if (selectedGateway.getOutgoingSequenceFlows().size() == allAcceptedSequenceFlows.size()
				&& acceptedSequenceFlows.size() == 0) {
			System.out.println("add as successor");
			// add target as successor
			// add new element as succeed on the gateway
			process.addSequenceFlow("sq-" + seqenceFlowId.toString(), selectedGateway.getId(), targetElement.getId());
			seqenceFlowId++;
			splitGateway = selectedGateway;
			isAdded = true;

		} else if (acceptedSequenceFlows.size() >= 1) {
			System.out.print("Add new gateway after old gateway: ");

			// add new gateway after selected gateway
			Gateway newGateway = process.addGateway("gt-" + gatewayId.toString(), "",
					probablyRelationType.get(GATEWAY_TYPE));
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

			isAdded = true;
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
		System.out.println("JOIN: " + sourceElement.getId() + "->" + targetElement.getId());
		BPMNProcess process = model.openDefaultProces();

		// get target sequence flow
		SequenceFlow sq = targetElement.getIngoingSequenceFlows().iterator().next();
		// get succeed
		BPMNElementNode precedingElement = sq.getSourceElement();

		// check if it is activity
		// TODO: look at the type
		if (precedingElement.getType().contentEquals(BPMNTypes.TASK)) {

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
		} else {
			// is gateway
			Integer flowCounter = 0;
			List<SequenceFlow> acceptedSequenceFlowList = new ArrayList<>();
			List<SequenceFlow> sqflowList = precedingElement.getIngoingSequenceFlows().stream()
					.collect(Collectors.toList());
			for (SequenceFlow currenctSecquenceFlow : sqflowList) {
				BPMNElementNode currenctProcessor = currenctSecquenceFlow.getSourceElement();
				flowCounter++;
				if (process.isPreceding(splitGateway, currenctProcessor)) {
					acceptedSequenceFlowList.add(currenctSecquenceFlow);
				}
			}
			// add to the same gateway
			if (acceptedSequenceFlowList.size() == flowCounter) {
				// check if the same gateway type and the num(like id) of the gateway
				if (precedingElement.getElementNode().getLocalName()
						.contentEquals(splitGateway.getElementNode().getLocalName())
						&& precedingElement.getAttribute(GATEWAY_NUM)
								.contentEquals(splitGateway.getAttribute(GATEWAY_NUM))) {
					process.addSequenceFlow("sq-" + seqenceFlowId.toString(), sourceElement.getId(),
							precedingElement.getId());
					seqenceFlowId++;
					splitGateway = null;
				} else {
					// add new gateway after the precedingElement
					Gateway newGateway = process.addGateway("gt-" + gatewayId.toString(), "", splitGateway.getType());
					newGateway.setAttribute(GATEWAY_NUM, splitGateway.getAttribute(GATEWAY_NUM));
					gatewayId++;
					splitGateway = null;

					// add new sequence flow
					process.addSequenceFlow("sq-" + seqenceFlowId.toString(), precedingElement.getId(),
							newGateway.getId());
					seqenceFlowId++;
					process.addSequenceFlow("sq-" + seqenceFlowId.toString(), sourceElement.getId(),
							newGateway.getId());
					seqenceFlowId++;
					process.addSequenceFlow("sq-" + seqenceFlowId.toString(), newGateway.getId(),
							targetElement.getId());
					seqenceFlowId++;
					process.deleteSequenceFlow(sq.getId());
				}
			} else if (acceptedSequenceFlowList.size() >= 2) {
				// add new gateway for accepted element
				Gateway newGateway = process.addGateway("gt-" + gatewayId.toString(), "", splitGateway.getType());
				newGateway.setAttribute(GATEWAY_NUM, splitGateway.getAttribute(GATEWAY_NUM));
				gatewayId++;
				splitGateway = null;

				// add new sequence flow
				process.addSequenceFlow("sq-" + seqenceFlowId.toString(), newGateway.getId(), precedingElement.getId());
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
				precedingElement = acceptedSequenceFlowList.get(0).getSourceElement();
				if (precedingElement.getType().contentEquals(BPMNTypes.TASK)) {

					process.deleteSequenceFlow(acceptedSequenceFlowList.get(0).getId());
					// add new gateway

					Gateway newGateway = process.addGateway("gt-" + gatewayId.toString(), "",
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
							acceptedSequenceFlowList.get(0).getTargetElement().getId());
					seqenceFlowId++;
				} else {
					addJoinGateway(sourceElement, precedingElement);
				}
			}else
				//cannot access the split gateway => should add XOR gateway if the gateway == parallel
				if (acceptedSequenceFlowList.size() == 0) {
				// add new gateway for connected to existing element
				String gatewayType = splitGateway.getType();
				String gatewayNum = splitGateway.getAttribute(GATEWAY_NUM);
				//TODO: add generic gateway?
				if(gatewayType.contentEquals(BPMNTypes.PARALLEL_GATEWAY)) {
					gatewayType = BPMNTypes.EXCLUSIVE_GATEWAY;
					gatewayNum = "-1";
				}
				
				Gateway newGateway = process.addGateway("gt-" + gatewayId.toString(), "", gatewayType);
				newGateway.setAttribute(GATEWAY_NUM, gatewayNum);
				gatewayId++;
				splitGateway = null;

				// add new sequence flow
				
				process.addSequenceFlow("sq-" + seqenceFlowId.toString(), newGateway.getId(),
						sq.getTargetElement().getId());
				seqenceFlowId++;
				process.addSequenceFlow("sq-" + seqenceFlowId.toString(), sourceElement.getId(),
						newGateway.getId());
				seqenceFlowId++;
				process.addSequenceFlow("sq-" + seqenceFlowId.toString(),	sq.getSourceElement().getId(), newGateway.getId()
					);
				seqenceFlowId++;
				
				process.deleteSequenceFlow(sq.getId());

			}
		}
	}

	private void addLoopGateway(BPMNElementNode sourceElement, BPMNElementNode targetElement)
			throws BPMNModelException {
		System.out.println("LOOP: " + sourceElement.getId() + "->" + targetElement.getId());
		BPMNProcess process = model.openDefaultProces();
		if (targetElement.getOutgoingSequenceFlows().size() > 0) {

			System.out.println("add new loop gateways");
			// add new gateway after source ( to start loop)
			Gateway sourceGateway = process.addGateway("gt-" + gatewayId.toString(), "", BPMNTypes.EXCLUSIVE_GATEWAY);
			sourceGateway.setAttribute(GATEWAY_NUM, "-2");
			gatewayId++;

			// add 2 new sequence flow
			process.addSequenceFlow("sq-" + seqenceFlowId.toString(), sourceElement.getId(), sourceGateway.getId());
			seqenceFlowId++;
			// get element connected to the target
			SequenceFlow sqflow = sourceElement.getOutgoingSequenceFlows().iterator().next();
			process.addSequenceFlow("sq-" + seqenceFlowId.toString(), sourceGateway.getId(),
					sqflow.getTargetElement().getId());
			seqenceFlowId++;
			process.deleteSequenceFlow(sqflow.getId());

			// add new gateway before target ( end for loop)
			Gateway targetGateway = process.addGateway("gt-" + gatewayId.toString(), "", BPMNTypes.EXCLUSIVE_GATEWAY);
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

			splitGateway = null;
		}
	}

	// old JOIND CODE
//	private void addJoinGatewayOld(BPMNElementNode sourceElement, BPMNElementNode targetElement)
//			throws BPMNModelException {
//		System.out.println("JOIN: " + sourceElement.getId() + "->" + targetElement.getId());
//		BPMNProcess process = model.openDefaultProces();
//
//		// There is a loop
//		if (splitGateway == null) {
////			throw new BPMNInvalidIDException("LOOP IS NOT SUPPORTED CURRENTLY");
////			TODO: I should work within the is procesor in the to make sure that I'm not get the target more than time (after get the children)
////			
////			if (targetElement.getOutgoingSequenceFlows().size() > 0) {
////
////				System.out.println("add new loop gateways");
////				// add new gateway after source ( to start loop)
////				Gateway sourceGateway = process.addGateway("gt-" + gatewayId.toString(), "", BPMNTypes.EXCLUSIVE_GATEWAY);
////				sourceGateway.setAttribute(GATEWAY_NUM, "-1");
////				gatewayId++;
////
////				// add 2 new sequence flow
////				process.addSequenceFlow("sq-" + seqenceFlowId.toString(), sourceElement.getId(), sourceGateway.getId());
////				seqenceFlowId++;
////				
////				
////
////				// add new gateway before target ( end for loop)
////				Gateway targetGateway = process.addGateway("gt-" + gatewayId.toString(), "", BPMNTypes.EXCLUSIVE_GATEWAY);
////				targetGateway.setAttribute(GATEWAY_NUM, "-1");
////				gatewayId++;
////
////			
////				// get element connected to the target
////				SequenceFlow sqflow = targetElement.getIngoingSequenceFlows().iterator().next();
////				process.addSequenceFlow("sq-" + seqenceFlowId.toString(), sqflow.getSourceElement().getId(), targetGateway.getId());
////				seqenceFlowId++;
////				process.deleteSequenceFlow(sqflow.getId());
////				// add new sequence flow
////				process.addSequenceFlow("sq-" + seqenceFlowId.toString(), targetGateway.getId(),
////						targetElement.getId());
////				seqenceFlowId++;
////				
////				//connect the gateways
////				process.addSequenceFlow("sq-" + seqenceFlowId.toString(), sourceGateway.getId(), targetGateway.getId());
////				seqenceFlowId++;
////			
////				
////				splitGateway = null;
////			}
//		} else {
//			// get target sequence flow
//			SequenceFlow sq = targetElement.getIngoingSequenceFlows().iterator().next();
//			// get succeed
//			BPMNElementNode proceesorElement = sq.getSourceElement();
//			if (!targetElement.getType().contentEquals(BPMNTypes.TASK)) {
//				proceesorElement = targetElement;
//			}
//			// check if it is activity
//			// TODO: look at the type
//			if (proceesorElement.getType().contentEquals(BPMNTypes.TASK)) {
//
//				process.deleteSequenceFlow(sq.getId());
//				// add new gateway
//
//				Gateway newGateway = process.addGateway("gt-" + gatewayId.toString(), "", splitGateway.getType());
//				newGateway.setAttribute(GATEWAY_NUM, splitGateway.getAttribute(GATEWAY_NUM));
//				gatewayId++;
//				splitGateway = null;
//
//				// add 3 new sequence flow
//				process.addSequenceFlow("sq-" + seqenceFlowId.toString(), sourceElement.getId(), newGateway.getId());
//				seqenceFlowId++;
//				process.addSequenceFlow("sq-" + seqenceFlowId.toString(), proceesorElement.getId(), newGateway.getId());
//				seqenceFlowId++;
//				process.addSequenceFlow("sq-" + seqenceFlowId.toString(), newGateway.getId(), targetElement.getId());
//				seqenceFlowId++;
//			} else {
//				// is gateway
//				Integer flowCounter = 0;
//				List<SequenceFlow> acceptedSequenceFlowList = new ArrayList<>();
//				List<SequenceFlow> sqflowList = proceesorElement.getIngoingSequenceFlows().stream()
//						.collect(Collectors.toList());
//				for (SequenceFlow currenctSecquenceFlow : sqflowList) {
//					BPMNElementNode currenctProcessor = currenctSecquenceFlow.getSourceElement();
//					flowCounter++;
//					if (process.isProcceding(splitGateway, currenctProcessor)) {
//						acceptedSequenceFlowList.add(currenctSecquenceFlow);
//					}
//				}
//				// add to the same gateway
//				if (acceptedSequenceFlowList.size() == flowCounter) {
//					// check if the same gateway type
//					process.addSequenceFlow("sq-" + seqenceFlowId.toString(), sourceElement.getId(),
//							proceesorElement.getId());
//					seqenceFlowId++;
//					splitGateway = null;
//				} else if (acceptedSequenceFlowList.size() > 1) {
//					// add new gateway for accepted element
//					Gateway newGateway = process.addGateway("gt-" + gatewayId.toString(), "", splitGateway.getType());
//					newGateway.setAttribute(GATEWAY_NUM, splitGateway.getAttribute(GATEWAY_NUM));
//					gatewayId++;
//					splitGateway = null;
//
//					// add new sequence flow
//					process.addSequenceFlow("sq-" + seqenceFlowId.toString(), proceesorElement.getId(),
//							newGateway.getId());
//					seqenceFlowId++;
//					process.addSequenceFlow("sq-" + seqenceFlowId.toString(), sourceElement.getId(),
//							newGateway.getId());
//					seqenceFlowId++;
//					for (SequenceFlow sequence : acceptedSequenceFlowList) {
//						process.addSequenceFlow("sq-" + seqenceFlowId.toString(), sequence.getSourceElement().getId(),
//								newGateway.getId());
//						seqenceFlowId++;
//						process.deleteSequenceFlow(sequence.getId());
//					}
//				} else if (acceptedSequenceFlowList.size() == 0) {
//					// add new gateway for connected to exsitng element
//					Gateway newGateway = process.addGateway("gt-" + gatewayId.toString(), "", splitGateway.getType());
//					newGateway.setAttribute(GATEWAY_NUM, splitGateway.getAttribute(GATEWAY_NUM));
//					gatewayId++;
//					splitGateway = null;
//
//					// add new sequence flow
//					process.addSequenceFlow("sq-" + seqenceFlowId.toString(), newGateway.getId(),
//							proceesorElement.getId());
//					seqenceFlowId++;
//					process.addSequenceFlow("sq-" + seqenceFlowId.toString(), sourceElement.getId(),
//							newGateway.getId());
//					seqenceFlowId++;
//
//				} else if (acceptedSequenceFlowList.size() == 1) {
//					proceesorElement = acceptedSequenceFlowList.get(0).getSourceElement();
//					if (proceesorElement.getType().contentEquals(BPMNTypes.TASK)) {
//
//						process.deleteSequenceFlow(acceptedSequenceFlowList.get(0).getId());
//						// add new gateway
//
//						Gateway newGateway = process.addGateway("gt-" + gatewayId.toString(), "",
//								splitGateway.getType());
//						newGateway.setAttribute(GATEWAY_NUM, splitGateway.getAttribute(GATEWAY_NUM));
//						gatewayId++;
//						splitGateway = null;
//
//						// add 3 new sequence flow
//						process.addSequenceFlow("sq-" + seqenceFlowId.toString(), sourceElement.getId(),
//								newGateway.getId());
//						seqenceFlowId++;
//						process.addSequenceFlow("sq-" + seqenceFlowId.toString(), proceesorElement.getId(),
//								newGateway.getId());
//						seqenceFlowId++;
//						process.addSequenceFlow("sq-" + seqenceFlowId.toString(), newGateway.getId(),
//								acceptedSequenceFlowList.get(0).getTargetElement().getId());
//						seqenceFlowId++;
//					} else {
//						addJoinGateway(sourceElement, proceesorElement);
//					}
//				} else {
//					// repeat
//					addJoinGateway(sourceElement, proceesorElement);
//				}
//			}
//		}
//	}

	public void saveMode() {
		model.save("C:\\Users\\AliNourEldin\\Desktop\\bpmn-layout\\bpmn-auto-layout\\test\\fixtures\\testt.bpmn");
		logger.info("...model creation sucessful");
	}

	public static void main(String[] args) {
		// correct
//		example1();
		// correct
//		example2();
		// correct
//		example3();
		// correct
		example4();
	}

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
			bpmnDiscovery.saveMode();
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
			bpmnDiscovery.saveMode();
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
				bpmnDiscovery.saveMode();
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
				bpmnDiscovery.saveMode();
			} catch (BPMNModelException e) {
				e.printStackTrace();
			}
		}

}
