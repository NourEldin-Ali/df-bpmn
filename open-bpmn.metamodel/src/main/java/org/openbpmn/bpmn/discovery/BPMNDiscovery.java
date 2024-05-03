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
					// call join gateway function
					addJoinGateway(sourceElement, targetElement);
				}
			}

			// simple relations

		}
//		Activity activity1 = process.addTask("t 1 2 3", "tasssssss", BPMNTypes.TASK);
//		Activity activity2 = process.addTask("t2", "tas2", BPMNTypes.TASK);
//		process.addSequenceFlow("s1", activity1.getId(), activity2.getId());

	}

	private void addSplitGateway(BPMNElementNode sourceElement, BPMNElementNode targetElement)
			throws BPMNModelException {
		System.out.println("SPLIT: " + sourceElement.getId() + "->" + targetElement.getId());
		BPMNProcess process = model.openDefaultProces();

		// WARNING: ACTIVITY SHOUD HAVE ONLY ONE SEQUENCE FLOW
		if (sourceElement.getOutgoingSequenceFlows().size() > 1) {
//			throw new BPMNInvalidIDException("Source should have one sequence flow");

		} else

		if ( // check activity; TODO: look at the type
		sourceElement.getOutgoingSequenceFlows().stream()
				.filter((sequenceflow) -> sequenceflow.getTargetElement().getType().contentEquals(BPMNTypes.TASK))
				.findAny().isPresent()) {

			// get source sequence flow
			SequenceFlow sq = sourceElement.getOutgoingSequenceFlows().iterator().next();
			// get succeed
			BPMNElementNode succeedElement = sq.getTargetElement();
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
			// get source sequence flow
			SequenceFlow sq = sourceElement.getOutgoingSequenceFlows().iterator().next();
			Gateway selectedGateway = (Gateway) sq.getTargetElement();
			isAdded = false;
			addNewTarget(sq, selectedGateway, sourceElement, targetElement);

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
	private void addNewTarget(SequenceFlow selectedSequence, Gateway selectedGateway, BPMNElement sourceElement,
			BPMNElement targetElement) throws BPMNModelException {
		BPMNProcess process = model.openDefaultProces();

		for (SequenceFlow currenctSecquenceFlow : selectedGateway.getOutgoingSequenceFlows()) {
			BPMNElementNode successor = currenctSecquenceFlow.getTargetElement();
			if (!successor.getType().equals(BPMNTypes.TASK)) {
				addNewTarget(currenctSecquenceFlow, (Gateway) successor, sourceElement, targetElement);
			}
		}

		if (isAdded == false) {
			int taskCounter = 0;
			List<SequenceFlow> squenceFlowsList = new ArrayList<>();
			Map<String, String> relationType = new HashMap<String, String>();
			for (SequenceFlow currenctSequenceFlow : selectedGateway.getOutgoingSequenceFlows()) {
				BPMNElementNode successor = currenctSequenceFlow.getTargetElement();

				if (successor.getType().equals(BPMNTypes.TASK)) {
					taskCounter++;
					relationType = getGatewayType(successor, targetElement);
					if (relationType.get(GATEWAY_TYPE).contentEquals(selectedGateway.getType())
							&& relationType.get(GATEWAY_NUM).contentEquals(selectedGateway.getAttribute(GATEWAY_NUM))) {
					} else {
						squenceFlowsList.add(currenctSequenceFlow);
					}
				}
			}
			// add before gateway
			if (taskCounter == squenceFlowsList.size() && taskCounter > 0) {
				relationType = getGatewayType(squenceFlowsList.get(0).getTargetElement(), targetElement);
				// get succeed
				BPMNElementNode sElement = selectedSequence.getSourceElement();
				BPMNElementNode succeesElement = selectedSequence.getTargetElement();
				process.deleteSequenceFlow(selectedSequence.getId());

				Gateway newGateway = process.addGateway("gt-" + gatewayId.toString(), "",
						relationType.get(GATEWAY_TYPE));
				newGateway.setAttribute(GATEWAY_NUM, relationType.get(GATEWAY_NUM));
				gatewayId++;
				splitGateway = newGateway;
//
//			// add 3 new sequence flow
				process.addSequenceFlow("sq-" + seqenceFlowId.toString(), sElement.getId(), newGateway.getId());
				seqenceFlowId++;
				process.addSequenceFlow("sq-" + seqenceFlowId.toString(), newGateway.getId(), succeesElement.getId());
				seqenceFlowId++;
				process.addSequenceFlow("sq-" + seqenceFlowId.toString(), newGateway.getId(), targetElement.getId());
				seqenceFlowId++;
				isAdded = true;
			} else
			// add all the selected sequence to the new gateway
			if (squenceFlowsList.size() > 0) {
				relationType = getGatewayType(squenceFlowsList.get(0).getTargetElement(), targetElement);
				Gateway newGateway = process.addGateway("gt-" + gatewayId.toString(), "",
						relationType.get(GATEWAY_TYPE));
				newGateway.setAttribute(GATEWAY_NUM, relationType.get(GATEWAY_NUM));
				gatewayId++;
				splitGateway = newGateway;

				process.addSequenceFlow("sq-" + seqenceFlowId.toString(),
						squenceFlowsList.get(0).getSourceElement().getId(), newGateway.getId());
				seqenceFlowId++;

				for (SequenceFlow sq : squenceFlowsList) {
					BPMNElementNode succeesElement = sq.getTargetElement();
					process.deleteSequenceFlow(sq.getId());
					process.addSequenceFlow("sq-" + seqenceFlowId.toString(), newGateway.getId(),
							succeesElement.getId());
					seqenceFlowId++;

				}
				process.addSequenceFlow("sq-" + seqenceFlowId.toString(), newGateway.getId(), targetElement.getId());
				seqenceFlowId++;

				isAdded = true;
			} else {
				if ((relationType.get(GATEWAY_TYPE).contentEquals(selectedGateway.getType())
						&& relationType.get(GATEWAY_NUM).contentEquals(selectedGateway.getAttribute(GATEWAY_NUM))) 
						|| selectedGateway.getAttribute(GATEWAY_NUM).contentEquals("-1")) {
					// add new element as succeed on the gateway
					process.addSequenceFlow("sq-" + seqenceFlowId.toString(), selectedGateway.getId(),
							targetElement.getId());
					seqenceFlowId++;
					splitGateway = selectedGateway;
					isAdded = true;

				}
			}
		}
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
		List<String> minList = null;
		for (Map.Entry<String, LinkedList<LinkedList<String>>> entry : relations.entrySet()) {
			LinkedList<LinkedList<String>> lists = entry.getValue();
			for (LinkedList<String> list : lists) {
				num++;
				if (list.contains(succeesor.getId()) && list.contains(target.getId())) {
					if (minList == null || (minList.size() > list.size())) {
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
		data.put(GATEWAY_NUM, num.toString());
		return data;
	}

	private void addJoinGateway(BPMNElementNode sourceElement, BPMNElementNode targetElement)
			throws BPMNModelException {
		System.out.println("JOIN: " + sourceElement.getId() + "->" + targetElement.getId());
		BPMNProcess process = model.openDefaultProces();

		// There is a loop
		if (splitGateway == null) {
//			throw new BPMNInvalidIDException("LOOP IS NOT SUPPORTED CURRENTLY");
//			TODO: I should work within the is procesor in the to make sure that I'm not get the target more than time (after get the children)
//			
//			if (targetElement.getOutgoingSequenceFlows().size() > 0) {
//
//				System.out.println("add new loop gateways");
//				// add new gateway after source ( to start loop)
//				Gateway sourceGateway = process.addGateway("gt-" + gatewayId.toString(), "", BPMNTypes.EXCLUSIVE_GATEWAY);
//				sourceGateway.setAttribute(GATEWAY_NUM, "-1");
//				gatewayId++;
//
//				// add 2 new sequence flow
//				process.addSequenceFlow("sq-" + seqenceFlowId.toString(), sourceElement.getId(), sourceGateway.getId());
//				seqenceFlowId++;
//				
//				
//
//				// add new gateway before target ( end for loop)
//				Gateway targetGateway = process.addGateway("gt-" + gatewayId.toString(), "", BPMNTypes.EXCLUSIVE_GATEWAY);
//				targetGateway.setAttribute(GATEWAY_NUM, "-1");
//				gatewayId++;
//
//			
//				// get element connected to the target
//				SequenceFlow sqflow = targetElement.getIngoingSequenceFlows().iterator().next();
//				process.addSequenceFlow("sq-" + seqenceFlowId.toString(), sqflow.getSourceElement().getId(), targetGateway.getId());
//				seqenceFlowId++;
//				process.deleteSequenceFlow(sqflow.getId());
//				// add new sequence flow
//				process.addSequenceFlow("sq-" + seqenceFlowId.toString(), targetGateway.getId(),
//						targetElement.getId());
//				seqenceFlowId++;
//				
//				//connect the gateways
//				process.addSequenceFlow("sq-" + seqenceFlowId.toString(), sourceGateway.getId(), targetGateway.getId());
//				seqenceFlowId++;
//			
//				
//				splitGateway = null;
//			}
		} else {
			// get target sequence flow
			SequenceFlow sq = targetElement.getIngoingSequenceFlows().iterator().next();
			// get succeed
			BPMNElementNode proceesorElement = sq.getSourceElement();
			if(!targetElement.getType().contentEquals(BPMNTypes.TASK)) {
				proceesorElement = targetElement;
			}
			// check if it is activity
			// TODO: look at the type
			if (proceesorElement.getType().contentEquals(BPMNTypes.TASK)) {

				process.deleteSequenceFlow(sq.getId());
				// add new gateway

				Gateway newGateway = process.addGateway("gt-" + gatewayId.toString(), "", splitGateway.getType());
				newGateway.setAttribute(GATEWAY_NUM, splitGateway.getAttribute(GATEWAY_NUM));
				gatewayId++;
				splitGateway = null;

				// add 3 new sequence flow
				process.addSequenceFlow("sq-" + seqenceFlowId.toString(), sourceElement.getId(), newGateway.getId());
				seqenceFlowId++;
				process.addSequenceFlow("sq-" + seqenceFlowId.toString(), proceesorElement.getId(), newGateway.getId());
				seqenceFlowId++;
				process.addSequenceFlow("sq-" + seqenceFlowId.toString(), newGateway.getId(), targetElement.getId());
				seqenceFlowId++;
			} else {
				// is gateway
				Integer flowCounter = 0;
				List<SequenceFlow> acceptedSequenceFlowList = new ArrayList<>();
				List<SequenceFlow> sqflowList = proceesorElement.getIngoingSequenceFlows().stream()
						.collect(Collectors.toList());
				for (SequenceFlow currenctSecquenceFlow : sqflowList) {
					BPMNElementNode currenctProcessor = currenctSecquenceFlow.getSourceElement();
					flowCounter++;
					if (process.isProcceding(splitGateway, currenctProcessor)) {
						acceptedSequenceFlowList.add(currenctSecquenceFlow);
					}
				}
				// add to the same gateway
				if (acceptedSequenceFlowList.size() == flowCounter) {
					// check if the same gateway type
					process.addSequenceFlow("sq-" + seqenceFlowId.toString(), sourceElement.getId(),
							proceesorElement.getId());
					seqenceFlowId++;
					splitGateway = null;
				} else if (acceptedSequenceFlowList.size() > 1) {
					// add new gateway for accepted element
					Gateway newGateway = process.addGateway("gt-" + gatewayId.toString(), "", splitGateway.getType());
					newGateway.setAttribute(GATEWAY_NUM, splitGateway.getAttribute(GATEWAY_NUM));
					gatewayId++;
					splitGateway = null;

					// add new sequence flow
					process.addSequenceFlow("sq-" + seqenceFlowId.toString(), proceesorElement.getId(),
							newGateway.getId());
					seqenceFlowId++;
					process.addSequenceFlow("sq-" + seqenceFlowId.toString(), sourceElement.getId(),
							newGateway.getId());
					seqenceFlowId++;
					for (SequenceFlow sequence : acceptedSequenceFlowList) {
						process.addSequenceFlow("sq-" + seqenceFlowId.toString(), sequence.getSourceElement().getId(),
								newGateway.getId());
						seqenceFlowId++;
						process.deleteSequenceFlow(sequence.getId());
					}
				} 
				else if (acceptedSequenceFlowList.size() == 0) {
					// add new gateway for connected to exsitng element
					Gateway newGateway = process.addGateway("gt-" + gatewayId.toString(), "", splitGateway.getType());
					newGateway.setAttribute(GATEWAY_NUM, splitGateway.getAttribute(GATEWAY_NUM));
					gatewayId++;
					splitGateway = null;

					// add new sequence flow
					process.addSequenceFlow("sq-" + seqenceFlowId.toString(), newGateway.getId(),
							proceesorElement.getId());
					seqenceFlowId++;
					process.addSequenceFlow("sq-" + seqenceFlowId.toString(), sourceElement.getId(),
							newGateway.getId());
					seqenceFlowId++;

				} 
				else if (acceptedSequenceFlowList.size() == 1) {
					proceesorElement = acceptedSequenceFlowList.get(0).getSourceElement();
					if (proceesorElement.getType().contentEquals(BPMNTypes.TASK)) {

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
						process.addSequenceFlow("sq-" + seqenceFlowId.toString(), proceesorElement.getId(),
								newGateway.getId());
						seqenceFlowId++;
						process.addSequenceFlow("sq-" + seqenceFlowId.toString(), newGateway.getId(),
								acceptedSequenceFlowList.get(0).getTargetElement().getId());
						seqenceFlowId++;
					} else {
						addJoinGateway(sourceElement, proceesorElement);
					}
				} 
				else {
					// repeat
					addJoinGateway(sourceElement, proceesorElement);
				}
			}
		}
	}

	public void saveMode() {
		model.save("C:\\Users\\AliNourEldin\\Desktop\\bpmn-layout\\bpmn-auto-layout\\test\\fixtures\\testt.bpmn");
		logger.info("...model creation sucessful");
	}

	public static void main(String[] args) {
		LinkedList<String> events = new LinkedList();

		events.add("a->c");
		events.add("c->h");
		
		events.add("a->d");
		events.add("d->h");

		events.add("a->b");
		events.add("b->end");

		events.add("h->l");
		
		events.add("l->end1");

		
		
		events.add("start->a");

		events.add("a->f");
		events.add("a->e");
		events.add("e->h");

		events.add("f->h");
//
		events.add("a->g");
		events.add("g->h");

		events.add("h->a");
		
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
				add("f");
				add("g");
			}
		});
		decisionRelations.add(new LinkedList<String>() {

			{
				add("e");
				add("f");
				add("g");
			};

		});

		LinkedList<LinkedList<String>> parallelRelations = new LinkedList();
		parallelRelations.add(new LinkedList<String>() {
			{
				add("c");
				add("d");
				add("e");
				add("f");
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
}
