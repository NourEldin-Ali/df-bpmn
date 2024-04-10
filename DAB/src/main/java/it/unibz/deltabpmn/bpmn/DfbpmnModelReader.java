package it.unibz.deltabpmn.bpmn;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collector;
import java.util.stream.Collectors;

//import org.camunda.bpm.model.bpmn.Bpmn;
//import org.camunda.bpm.model.bpmn.impl.instance.ServiceTaskImpl;
//import org.camunda.bpm.model.bpmn.impl.instance.StartEventImpl;
//import org.camunda.bpm.model.bpmn.impl.instance.TaskImpl;
//import org.camunda.bpm.model.bpmn.instance.Activity;
//import org.camunda.bpm.model.bpmn.instance.BoundaryEvent;
//import org.camunda.bpm.model.bpmn.instance.CatchEvent;
//import org.camunda.bpm.model.bpmn.instance.EndEvent;
//import org.camunda.bpm.model.bpmn.instance.ExclusiveGateway;
//import org.camunda.bpm.model.bpmn.instance.ExtensionElements;
//import org.camunda.bpm.model.bpmn.instance.FlowNode;
//import org.camunda.bpm.model.bpmn.instance.ParallelGateway;
//import org.camunda.bpm.model.bpmn.instance.Process;
//import org.camunda.bpm.model.bpmn.instance.StartEvent;
//import org.camunda.bpm.model.bpmn.instance.SubProcess;
//import org.camunda.bpm.model.xml.ModelInstance;
import org.openbpmn.bpmn.BPMNModel;
import org.openbpmn.bpmn.BPMNTypes;
import org.openbpmn.bpmn.elements.Activity;
import org.openbpmn.bpmn.elements.Event;
import org.openbpmn.bpmn.elements.core.BPMNElementNode;
import org.openbpmn.bpmn.util.BPMNModelFactory;
import org.w3c.dom.Element;

import it.unibz.deltabpmn.bpmn.extractors.CaseVariableExtractor;
import it.unibz.deltabpmn.bpmn.extractors.CatalogRelationExtractor;
import it.unibz.deltabpmn.bpmn.extractors.RepositoryRelationExtractor;
import it.unibz.deltabpmn.bpmn.parsers.GatewayConditionParser;
import it.unibz.deltabpmn.bpmn.parsers.SafetyPropertyParser;
import it.unibz.deltabpmn.bpmn.parsers.UpdateExpressionParser;
import it.unibz.deltabpmn.datalogic.ComplexTransition;
import it.unibz.deltabpmn.datalogic.ConjunctiveSelectQuery;
import it.unibz.deltabpmn.dataschema.core.DataSchema;
import it.unibz.deltabpmn.processschema.blocks.Block;
import it.unibz.deltabpmn.processschema.blocks.DeferredChoiceBlock;
import it.unibz.deltabpmn.processschema.blocks.ExclusiveChoiceBlock;
import it.unibz.deltabpmn.processschema.blocks.LoopBlock;
import it.unibz.deltabpmn.processschema.blocks.ParallelBlock;
import it.unibz.deltabpmn.processschema.blocks.PossibleCompletion;
import it.unibz.deltabpmn.processschema.blocks.ProcessBlock;
import it.unibz.deltabpmn.processschema.blocks.SequenceBlock;
import it.unibz.deltabpmn.processschema.core.EmptyBlock;
import it.unibz.deltabpmn.processschema.core.ProcessSchema;
import it.unibz.deltabpmn.verification.mcmt.NameManager;
import it.unibz.deltabpmn.verification.mcmt.translation.DABProcessTranslator;

/**
 * A class that parses a .bpmn model and extracts from it three main DAB
 * components such as data schema, process schema and data (manipulation) logic.
 */
public class DfbpmnModelReader {
	private BPMNModel modelInstanceNew;
	private Deque<BPMNElementNode> bpmnNodeQueueNew = new ArrayDeque<>();
	private Set<BPMNElementNode> visitedNodesNew = new HashSet<BPMNElementNode>();
	private Map<BPMNElementNode, Integer> visitedEndGatesNew = new HashMap<>();
	private Stack<BPMNElementNode> visitedOpenXORLoopGatesNew = new Stack<>();// used for opening XORs in LOOP blocks

	private DataSchema dataSchema;
	private ProcessSchema processSchema;
	private Stack<XORSplitGate> visitedXORSplitGates = new Stack<>();
	private Stack<DeferredANDSplitGate> visitedDeferredANDSplitGates = new Stack<>();
	private Stack<XORLoopGate> visitedLoopGates = new Stack<>();// remember all loop gates
	private Stack<Block> stackBlocks = new Stack<>();
	// private int taskBlockCounter = 1;//counter for task blocks
	private int seqBlockCounter = 1; // counter for sequence blocks
	private int xorBlockCounter = 1;// counter for XOR (exclusive choice) blocks
	private int deferredANDBlockCounter = 1;// counter for AND (parallel) blocks
	private int loopBlockCounter = 1;// counter for LOOP blocks
	private ProcessBlock dabProcess;
	private List<ConjunctiveSelectQuery> propertiesToVerify = new ArrayList<>();
	private String processName;

	public DfbpmnModelReader(String filePath) throws Exception {
		File file = new File(filePath);

		this.modelInstanceNew = BPMNModelFactory.read(file);
		// 1. generate the DAB data schema
		this.dataSchema = DataSchema.getInstance();
		// extract case variables
		this.dataSchema = CaseVariableExtractor.extract(this.modelInstanceNew, this.dataSchema);
		// extract catalog relations
		this.dataSchema = CatalogRelationExtractor.extract(this.modelInstanceNew, this.dataSchema);
		// extract repository relations
		this.dataSchema = RepositoryRelationExtractor.extract(this.modelInstanceNew, this.dataSchema);

		this.processSchema = new ProcessSchema(dataSchema);

		this.processName = this.modelInstanceNew.openDefaultProces().getName();
		System.out.println(this.processName);
		System.out.println("-----------------------");
		this.dabProcess = processSchema.newProcessBlock(processName);
		dfbpmnModelExplorer();
		this.dabProcess.addBlock(this.stackBlocks.pop());

		// get property to verify
		this.propertiesToVerify = SafetyPropertyParser.parse(this.modelInstanceNew, dataSchema);
	}

	public DataSchema getDataSchema() {
		return this.dataSchema;
	}

	public ProcessBlock getDabProcess() {
		return this.dabProcess;
	}

	public List<ConjunctiveSelectQuery> getPropertiesToVerify() {
		return this.propertiesToVerify;
	}

	public List<DABProcessTranslator> getProcessTranslators() {
		// remove eevars from case variable declarations
		this.dataSchema.eevarsOut();

		List<DABProcessTranslator> processTranslators = new ArrayList<>();
		if (this.propertiesToVerify.size() == 1) {
			DABProcessTranslator processTranslator = new DABProcessTranslator(processName, this.dabProcess,
					this.dataSchema);
			processTranslator.setSafetyFormula(propertiesToVerify.get(0));
			processTranslators.add(processTranslator);
		} else {
			int cnt = 1;
			for (ConjunctiveSelectQuery property : this.propertiesToVerify) {
				DABProcessTranslator processTranslator = new DABProcessTranslator(processName + cnt, this.dabProcess,
						this.dataSchema);
				processTranslator.setSafetyFormula(property);
				processTranslators.add(processTranslator);
				cnt++;
			}
		}
		return processTranslators;
	}

	private void dfbpmnModelExplorer() throws Exception {
		// 1. Extract start events and check if there are more than two
		Collection<Event> startEvents = modelInstanceNew.findAllEvents().stream()
				.filter((e) -> e.getElementNode().getLocalName().equals(BPMNTypes.START_EVENT))
				.collect(Collectors.toList());

		try {
			if (startEvents.size() > 1)
				// thiw warning might not work as there are models that have subprocesses with
				// local start events
				throw new Exception("Warning: your model contains more than one start event!");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		Event start = startEvents.iterator().next();

		// 2. find all boundary events and generate a map of activities attached to them
//        Map<Activity, BoundaryEvent> boundaryEventMap = new HashMap<>();
//        //needed for locating sub-processes
//        Collection<BoundaryEvent> boundaryEvents = modelInstance.getModelElementsByType(BoundaryEvent.class);
//        for (BoundaryEvent event : boundaryEvents)
//            boundaryEventMap.put(event.getAttachedTo(), event);

		// 3. initialize the queue, prepare a set of visited nodes and a current node
		this.bpmnNodeQueueNew.add(start);
		BPMNElementNode currentNode;

		// 4. run the breadth-first traversal

		while (!bpmnNodeQueueNew.isEmpty()) {
			currentNode = bpmnNodeQueueNew.removeFirst();
			if (!visitedNodesNew.contains(currentNode)) {
				List<BPMNElementNode> newFrontier = currentNode.getOutgoingSequenceFlows().stream()
						.map((sq) -> sq.getTargetElement()).collect(Collectors.toList());

				// ****************************************
				// process a START Event
				// ****************************************
				if (currentNode.getElementNode().getLocalName().equals(BPMNTypes.START_EVENT)) {
					newFrontier.stream().forEach(c -> bpmnNodeQueueNew.addLast(c));
					visitedNodesNew.add(currentNode);

				}
				// ****************************************
				
				//****************************************
                //process a CATCH Event (should be only message or timer)
                //****************************************
				if (currentNode.getElementNode().getLocalName().equals(BPMNTypes.CATCH_EVENT)
						&& !currentNode.getIngoingSequenceFlows().isEmpty() && !currentNode.getOutgoingSequenceFlows().isEmpty()) {
                
                    // --> (E) -->
                    System.out.println("--------[Catch EVENT] : " + currentNode.getId() +" - " + currentNode.getName());
                    newFrontier.stream().forEach(c -> bpmnNodeQueueNew.addFirst(c));
                    visitedNodesNew.add(currentNode);
                    it.unibz.deltabpmn.processschema.blocks.Event eventBlock;
                    Event event = (Event) currentNode;
                    Set<Element> conditionalEventDefinition = event.getEventDefinitionsByType("conditionalEventDefinition");
                    if (conditionalEventDefinition.size() > 0) 
                        eventBlock = processSchema.newEvent(currentNode.getId(), UpdateExpressionParser.parse(currentNode.getId(), conditionalEventDefinition, dataSchema));
                    else
                        eventBlock = processSchema.newEvent(currentNode.getId());
                    stackBlocks.push(eventBlock);
                    //ToDo: in our case, Catch Events can be only of certain types --> NARROW THE SCOPE
                }
                //****************************************
				// ****************************************
				// process a Task
				// ****************************************
				if (currentNode.getElementNode().getLocalName().equals(BPMNTypes.TASK)) {
					// --> [ T ] -->
					System.out.println("--------[TASK] : " + currentNode.getId() + "-" + currentNode.getName());
					newFrontier.stream().forEach(c -> bpmnNodeQueueNew.addFirst(c));
					visitedNodesNew.add(currentNode);
					System.out.println(currentNode.getName());
					// start parsing the task data update (if any is available)
					it.unibz.deltabpmn.processschema.blocks.Task taskBlock;
					int dataObjects = ((Activity) currentNode).getAllNodes().size();

					if (dataObjects > 0) {
						ComplexTransition transition = UpdateExpressionParser.parse(currentNode.getId(),
								(Activity) currentNode, dataSchema);
						taskBlock = processSchema.newTask(currentNode.getId(), transition);
					} else
						taskBlock = processSchema.newTask(currentNode.getId());
					stackBlocks.push(taskBlock);
					// this.taskBlockCounter++;
				}
				// ****************************************

				// ****************************************
				// process a Deferred Parallel block
				// ****************************************
				// AND split
				if (currentNode.getElementNode().getLocalName().equals(BPMNTypes.PARALLEL_GATEWAY)
						&& currentNode.getIngoingSequenceFlows().size() == 1) {
					System.out.println("--------[Parallel SPLIT] : " + currentNode.getId()+" - " + currentNode.getName());
					// the order has to be reversed as forEach reverses the order of added elements;
					// when we start forking, add the frontier to the front of the deque
					newFrontier.stream().collect(
							Collector.of(ArrayDeque<BPMNElementNode>::new, (deq, t) -> deq.addFirst(t), (d1, d2) -> {
								d2.addAll(d1);
								return d2;
							})).stream().forEach(c -> bpmnNodeQueueNew.addFirst(c));
					DeferredANDSplitGate gate = new DeferredANDSplitGate(currentNode.getId());
					visitedDeferredANDSplitGates.push(gate);
					stackBlocks.push(gate);// remember where did we start forking
				}

				// AND join
				if (currentNode.getElementNode().getLocalName().equals(BPMNTypes.PARALLEL_GATEWAY)
						&& currentNode.getIngoingSequenceFlows().size() == 2) {

					System.out.println("--------[Parallel JOIN] : " + currentNode.getId()+" - " + currentNode.getName());
					// if it has two inputs and the next element is in visited ==> can be backward
					// exception or loop block!
					visitedEndGatesNew.merge(currentNode, 1, (prev, one) -> prev + one);// does upsert into the map with
																						// AND-JOIN gates (if a gate
																						// hasn't been visited, it is
																						// added with counter 1;
																						// otherwise it's counter gets
																						// incremented by 1

					if (visitedEndGatesNew.get(currentNode) == 2) {
						newFrontier.stream().forEach(c -> bpmnNodeQueueNew.addLast(c));// we start assembling the XOR
																						// block after we encounter the
																						// join for the second time;
																						// then we populate its frontier

						DeferredANDSplitGate startingANDGate = visitedDeferredANDSplitGates.pop();// get the last XOR
																									// gate from the
																									// stack of visited
																									// gates and collect
																									// all nodes in the
																									// stack until we
																									// reach it
						this.seqBlockCounter = assembleSecondANDBranch(startingANDGate, this.seqBlockCounter,
								this.stackBlocks, this.processSchema);
						ParallelBlock newANDBlock = processSchema
								.newParallelBlock("ANDblock" + this.deferredANDBlockCounter);
						newANDBlock.addSecondBlock(stackBlocks.pop());// add second block
						newANDBlock.addFirstBlock(stackBlocks.pop());// add first block
						// remove (+)_F from the stack
						System.out.println("AND completed:" + newANDBlock);
						stackBlocks.push(newANDBlock);
						this.deferredANDBlockCounter++;
					} else {
						// we are on one of the branches of the AND block, walk back until (+)_F on the
						// stack so as to create a unique block on the branch
						DeferredANDSplitGate startingANDGate = visitedDeferredANDSplitGates.peek();// get the last AND
																									// gate from the
																									// stack of visited
																									// gates and
						this.seqBlockCounter = assembleFirstANDBranch(startingANDGate, this.seqBlockCounter,
								this.stackBlocks, this.processSchema);
					}
				}
				// ****************************************

				// ****************************************
				// process a Exclusive Choice block
				// ****************************************
				// XOR split
//                && currentNode.getSucceedingNodes().filterByType(EndEvent.class).list().isEmpty() //to check that it's not a possible completion block
				System.out.println(lookAheadLoop(newFrontier, this.visitedOpenXORLoopGatesNew));
				if (currentNode.getElementNode().getLocalName().equals(BPMNTypes.EXCLUSIVE_GATEWAY)
						&& currentNode.getIngoingSequenceFlows().size() == 1
						&& !currentNode.getIngoingSequenceFlows().stream()
								.anyMatch((sequence) -> sequence.getTargetElement().getElementNode().getLocalName()
										.equals(BPMNTypes.END_EVENT)) // to check that it's not a possible
																				// completion block
						&& lookAheadLoop(newFrontier, this.visitedOpenXORLoopGatesNew) == null) {
					System.out.println("--------[XOR SPLIT] : " + currentNode.getId()+" - " + currentNode.getName());
					// the order has to be reversed as forEach reverses the order of added elements;
					// when we start forking, add the frontier to the front of the deque
					newFrontier.stream().collect(
							Collector.of(ArrayDeque<BPMNElementNode>::new, (deq, t) -> deq.addFirst(t), (d1, d2) -> {
								d2.addAll(d1);
								return d2;
							})).stream().forEach(c -> bpmnNodeQueueNew.addFirst(c));
//					ExtensionElements extensionElements = currentNode.getExtensionElements();
					String condition = currentNode.getAttribute("condition");
					// ToDo: do we always force a condition in the gate for a XOR split?
					if (condition.isEmpty())
						throw new Exception("The condition of the XOR split " + currentNode.getId() + " "+ currentNode.getName() +" is empty!");
					XORSplitGate gate = new XORSplitGate(currentNode.getId(),
							GatewayConditionParser.parseGatewayCondition(condition, this.dataSchema));
					visitedXORSplitGates.push(gate);
					stackBlocks.push(gate);// remember where did we start forking
					// ToDo: add visited start gates for detecting loops; if we saw that gate twice,
					// then it's a loop
				}

				// XOR join (can be confused with the start of a LOOP block; need to check that
				// we haven't started with XOR loop by checking the stack of open LOOP blocks
				if (currentNode.getElementNode().getLocalName().equals(BPMNTypes.EXCLUSIVE_GATEWAY)
						&& currentNode.getIngoingSequenceFlows().size() == 2
						&& isExclusiveChoiceXORJoin(currentNode, this.bpmnNodeQueueNew, this.visitedNodesNew,
								this.visitedXORSplitGates, this.modelInstanceNew)) {
					System.out.println("--------[XOR JOIN] : " + currentNode.getId()+" - " + currentNode.getName());
					// ToDO: when we have two incoming arrows, it can mean that we're dealing with a
					// gateway of a backward (or forward) exception or a loop block!
					// if it has two inputs and the next element is in visited ==> can be backward
					// exception or loop block!
					visitedEndGatesNew.merge(currentNode, 1, (prev, one) -> prev + one);// does upsert into the map with
																						// XOR-JOIN gates (if a gate
																						// hasn't been visited, it is
																						// added with counter 1;
																						// otherwise it's counter gets
																						// incremented by 1

					if (visitedEndGatesNew.get(currentNode) == 2) {
						newFrontier.stream().forEach(c -> bpmnNodeQueueNew.addLast(c));// we start assembling the XOR
																						// block after we encounter the
																						// join for the second time;
																						// then we populate its frontier
						// ExclusiveChoiceBlock newXORBlock =
						// processSchema.newExclusiveChoiceBlock("XORblock" + this.xorBlockCounter);
						// first we need to make sure that all the elements on the second XOR block
						// branch have been assembled
						XORSplitGate startingXORGate = visitedXORSplitGates.pop();// get the last XOR gate from the
																					// stack of visited gates and
																					// collect all nodes in the stack
																					// until we reach it
						this.seqBlockCounter = assembleSecondXORBranch(startingXORGate, this.seqBlockCounter,
								this.stackBlocks, this.processSchema);
						// start with extracting assembled blocks from the stack
						Block b2 = stackBlocks.pop();// get second block
						Block b1 = stackBlocks.pop();// get first block
						ConjunctiveSelectQuery condition = ((XORSplitGate) stackBlocks.pop()).getCondition();// remove
																												// (X)_F
																												// and
																												// get
																												// condition
																												// stored
																												// in it
																												// for
																												// the
																												// XOR
																												// block
						if (condition.isEmpty()) {
							// if the precondition is empty, then it's TRUE and we're dealing with a
							// Deferred choice block
							DeferredChoiceBlock newXORBlock = processSchema
									.newDeferredChoiceBlock("XORblock" + this.xorBlockCounter);
							newXORBlock.addSecondBlock(b2);// add second block
							newXORBlock.addFirstBlock(b1);// add first block
							System.out.println("XOR completed: " + newXORBlock);
							stackBlocks.push(newXORBlock);
						} else {
							// if the precondition is not empty, then we're dealing with a Eclusive choice
							// block
							ExclusiveChoiceBlock newXORBlock = processSchema
									.newExclusiveChoiceBlock("XORChoiceblock" + this.xorBlockCounter);
							newXORBlock.addSecondBlock(b2);// add second block
							newXORBlock.addFirstBlock(b1);// add first block
							newXORBlock.addCondition(condition);
							System.out.println("XOR completed: " + newXORBlock);
							stackBlocks.push(newXORBlock);
						}
						this.xorBlockCounter++;
						continue;
					} else {
						// we are on one of the branches of the XOR block, walk back until (X)_F on the
						// stack so as to create a unique block on the branch
						XORSplitGate startingXORGate = visitedXORSplitGates.peek();// get the last XOR gate from the
																					// stack of visited gates and
																					// collect all nodes in the stack
																					// until we reach it
						this.seqBlockCounter = assembleFirstXORBranch(startingXORGate, this.seqBlockCounter,
								this.stackBlocks, this.processSchema);
//                        while (stackBlocks.search(startingXORGate) > 2) {
//                            SequenceBlock newSequenceBlock = processSchema.newSequenceBlock("SEQblock" + this.seqBlockCounter);
//                            newSequenceBlock.addSecondBlock(stackBlocks.pop());//add second block
//                            newSequenceBlock.addFirstBlock(stackBlocks.pop());//add first block
//                            stackBlocks.push(newSequenceBlock);
//                            this.seqBlockCounter++;
//                        }
						continue;
					}
				}
				// ****************************************

				// ****************************************
				// process a LOOP block
				// ****************************************
				// XOR split for LOOP
				if (currentNode.getElementNode().getLocalName().equals(BPMNTypes.EXCLUSIVE_GATEWAY)
						&& currentNode.getIngoingSequenceFlows().size() == 2) {

//                if (currentNode instanceof ExclusiveGateway && currentNode.getIncoming().size() == 2) {
					System.out.println("--------[LOOP START] : " + currentNode.getId()+" - " + currentNode.getName());

					// visiting the gate for the second time, close the block
					if (this.visitedOpenXORLoopGatesNew.contains(currentNode)) {
						this.visitedOpenXORLoopGatesNew.pop();// remove the gate that we've just visited for the second
																// time
						// go until the first XOR block on the stack of blocks, this will be an
						// iteration XOR gate with a condition
						// use this gate to generate a second block of the LOOP
						XORLoopGate iterationGate = this.visitedLoopGates.pop();
						while (stackBlocks.search(iterationGate) > 2) {
							SequenceBlock newSequenceBlock = processSchema
									.newSequenceBlock("SEQBlock" + this.seqBlockCounter);
							newSequenceBlock.addSecondBlock(stackBlocks.pop());// add second block
							newSequenceBlock.addFirstBlock(stackBlocks.pop());// add first block
							stackBlocks.push(newSequenceBlock);
							this.seqBlockCounter++;
						}
						// now assemble the whole LOOP block
						LoopBlock newLOOPBlock = processSchema.newLoopBlock("LOOPBlock" + loopBlockCounter,
								iterationGate.getCondition());
						Block b2 = stackBlocks.pop();
						if (!(b2 instanceof XORLoopGate)) {
							newLOOPBlock.addSecondBlock(b2);// add second block
							stackBlocks.pop();
						} // remove (X)_LF2
						else
							newLOOPBlock.addSecondBlock(new EmptyBlock(this.dataSchema));
						Block b1 = stackBlocks.pop();
						if (!(b1 instanceof XORLoopGate)) {
							newLOOPBlock.addFirstBlock(b1);// add first block
							stackBlocks.pop();
						} // remove (X)_LF1
						else
							newLOOPBlock.addFirstBlock(new EmptyBlock(this.dataSchema));
						stackBlocks.push(newLOOPBlock);
						this.loopBlockCounter++;
						continue;
					}
					// visiting the gate for the first time (comes after the first check as it
					// clashes with the XOR gate being added to the stack of visited XOR gates)
					// if (!Collections.disjoint(this.visitedNodes,
					// currentNode.getPreviousNodes().list())) {
					else {
						// a XOR split of the loop should have only one successor (i.e.,
						// newFrontier.size()=1)!
						this.bpmnNodeQueueNew.addFirst(newFrontier.get(0));
						// add visited start gates for detecting loops: when we see that gate for a
						// second time, then it's a loop block!
						this.visitedOpenXORLoopGatesNew.push(currentNode);
						XORLoopGate gate = new XORLoopGate(currentNode.getId());
						this.visitedLoopGates.push(gate);// needed to perform a loop for creating the first sub-block of
															// the LOOP block
						stackBlocks.push(gate);// remember where did we start forking
					}
					continue;
				}

				// XOR "join" for LOOP
				if (currentNode.getElementNode().getLocalName().equals(BPMNTypes.EXCLUSIVE_GATEWAY)
						&& currentNode.getIngoingSequenceFlows().size() == 1 &&
						!this.visitedOpenXORLoopGatesNew.empty()) {
//                if (currentNode instanceof ExclusiveGateway && currentNode.getIncoming().size() == 1 && !this.visitedOpenXORLoopGatesNew.empty()) {
					System.out.println("--------[LOOP CONTINUE] : " + currentNode.getId()+" - " + currentNode.getName());

					// System.out.println("TAKING A TURN IN A LOOP BLOCK");
					// detect which branch brings to the looping gate and take this branch for
					// elements to be put into the frontier
					BPMNElementNode loopBranchNode = lookAheadLoop(newFrontier, this.visitedOpenXORLoopGatesNew);
					if (loopBranchNode != null) {
						newFrontier.remove(loopBranchNode);
						this.bpmnNodeQueueNew.addFirst(newFrontier.get(0));// first add the element that we want to
																			// check after the loop has been assembled
						this.bpmnNodeQueueNew.addFirst(loopBranchNode);
						XORLoopGate openGate = visitedLoopGates.pop();
						// generate from all the visited blocks in the first part of the loop a single
						// block
						while (stackBlocks.search(openGate) > 2) {
							SequenceBlock newSequenceBlock = processSchema
									.newSequenceBlock("SEQBlock" + this.seqBlockCounter);
							newSequenceBlock.addSecondBlock(stackBlocks.pop());// add second block
							newSequenceBlock.addFirstBlock(stackBlocks.pop());// add first block
							stackBlocks.push(newSequenceBlock);
						}
						String condition = currentNode.getAttribute("condition");
//						ExtensionElements extensionElements = currentNode.getExtensionElements();
						if (condition.isEmpty())
							throw new Exception("The condition of the XOR split " + currentNode.getId() + " is empty!");
						XORLoopGate gate = new XORLoopGate(currentNode.getId(),
								GatewayConditionParser.parseGatewayCondition(condition, this.dataSchema));
						visitedLoopGates.push(gate);
						stackBlocks.push(gate);// pushing the second fork on the stack of blocks
						// newFrontier.stream().forEach(c -> bpmnNodeQueue.addFirst(c));
					}
					continue;
				}
				// ****************************************

				 //****************************************
                //process a POSSIBLE COMPLETION block
                //****************************************
				if (currentNode.getElementNode().getLocalName().equals(BPMNTypes.EXCLUSIVE_GATEWAY)
						&& currentNode.getIngoingSequenceFlows().size() == 1 
						&& currentNode.getIngoingSequenceFlows().stream()
						.anyMatch((sequence) -> sequence.getTargetElement().getElementNode().getLocalName()
								.equals(BPMNTypes.END_EVENT)) == true) {
					
					
				
//                if (currentNode instanceof ExclusiveGateway && currentNode.getIncoming().size() == 1 && !currentNode.getSucceedingNodes().filterByType(EndEvent.class).list().isEmpty()) {
                    System.out.println("--------[Possible COMPLETION XOR] : " + currentNode.getId()+" - " + currentNode.getName());

//                    EndEvent end = currentNode.getSucceedingNodes().filterByType(EndEvent.class).list().get(0);
                    BPMNElementNode end = currentNode.getIngoingSequenceFlows().stream().filter((sequence) -> sequence.getTargetElement().getElementNode().getLocalName()
								.equals(BPMNTypes.END_EVENT)).collect(Collectors.toList()).get(0).getTargetElement();
                    //check if there are no potential situations in which we have a loop instead of the completion block
                    if (lookAheadLoop(newFrontier, this.visitedOpenXORLoopGatesNew) == null) {
                        PossibleCompletion newPossibleCompletionBlock = null;
//                        ExtensionElements extensionElements = currentNode.getExtensionElements();
                        String condition = currentNode.getAttribute("condition");
                        if (condition.isEmpty())
                            //throw new Exception("The condition of the POSSIBLE COMPLETION block " + currentNode.getId() + " is empty!");
                            newPossibleCompletionBlock = processSchema.newPossibleCompletion(currentNode.getId());
                        else
                            newPossibleCompletionBlock = processSchema.newPossibleCompletion(currentNode.getId(), GatewayConditionParser.parseGatewayCondition(condition, this.dataSchema));
                        //newPossibleCompletionBlock.add(end.getId());
                        //extract the condition from the XOR gate
                        newFrontier.remove(end);
                        newPossibleCompletionBlock.addMainProcessLifecycleVariable(this.dabProcess.getLifeCycleVariable());//add lifecyle variable of the main process
                        stackBlocks.push(newPossibleCompletionBlock);
                        this.bpmnNodeQueueNew.addFirst(newFrontier.get(0));
                    }
                }
                //****************************************
				
				   //**************************************************
                //END EVENT + RECURSIVE CREATION OF SEQUENCE BLOCKS
                //**************************************************
                //do here the creation of 2-sized sequence block! when you arrive to the end, do a traversal
                //and create a sequence of chained pairs
				if (currentNode.getElementNode().getLocalName().equals(BPMNTypes.END_EVENT)
						&& stackBlocks.size() > 1) {
					
				
//                if (currentNode instanceof EndEvent && stackBlocks.size() > 1) {
                    while (stackBlocks.size() > 1) {
                        SequenceBlock newSequenceBlock = processSchema.newSequenceBlock("SEQBlock" + this.seqBlockCounter);
                        newSequenceBlock.addSecondBlock(stackBlocks.pop());//add second block
                        newSequenceBlock.addFirstBlock(stackBlocks.pop());//add first block
                        stackBlocks.push(newSequenceBlock);
                        this.seqBlockCounter++;
                    }
                }
			}
		}
	}

	// return a flow node that ends up in a loop
	private static BPMNElementNode lookAheadLoop(List<BPMNElementNode> frontier, Stack<BPMNElementNode> visited) {
		for (BPMNElementNode node : frontier)
			if (containsVisitedXOR(visited, node))
				return node;
		return null;
	}

	private static boolean containsVisitedXOR(Stack<BPMNElementNode> visited, BPMNElementNode start) {
		// check for the case when the next node is already the closing loop node
		if (visited.contains(start))
			return true;

		Set<BPMNElementNode> localVisited = new HashSet<BPMNElementNode>();
		LinkedList<BPMNElementNode> queue = new LinkedList<BPMNElementNode>();
		localVisited.add(start);
		queue.add(start);

		BPMNElementNode current;
		while (!queue.isEmpty()) {
			current = queue.removeFirst();
			for (BPMNElementNode node : current.getOutgoingSequenceFlows().stream().map((sq) -> sq.getTargetElement())
					.collect(Collectors.toList())) {
				if (!localVisited.contains(node))
					queue.add(node);
				if (node.getElementNode().getLocalName().equals(BPMNTypes.EXCLUSIVE_GATEWAY))
					if (visited.contains(node))
						return true;
					else
						return false;// this is used to check cases in which a possible completion block is within
										// the loop block
			}
		}
		return false;
	}

	private static boolean isExclusiveChoiceXORJoin(BPMNElementNode node, Deque<BPMNElementNode> bpmnNodeQueue,
			Set<BPMNElementNode> visitedNodes, Stack<XORSplitGate> visitedXORSplitGates, BPMNModel modelInstance)
			throws Exception {
		List<BPMNElementNode> previousNodes = node.getIngoingSequenceFlows().stream().map((sq) -> sq.getSourceElement())
				.collect(Collectors.toList());
		if (previousNodes.size() > 2)
			throw new Exception("Too many previous nodes for node " + node.getId());
		if (visitedNodes.containsAll(previousNodes)) // we have visited all the previous nodes of the XOR join
			return true;
		if (!visitedXORSplitGates.empty()) { // we have visited only one previous node of the XOR join
			XORSplitGate gate = visitedXORSplitGates.peek();// there must be somewhere an open XOR gate
			BPMNElementNode XOR = modelInstance.openDefaultProces().findElementNodeById(gate.getId());// find it and
																										// check whether
																										// one of its
			// successors is still in the queue
			List<BPMNElementNode> nextNodes = XOR.getOutgoingSequenceFlows().stream().map((sq) -> sq.getTargetElement())
					.collect(Collectors.toList());

			if (visitedNodes.contains(previousNodes.get(0)) || visitedNodes.contains(previousNodes.get(1)))
				if (bpmnNodeQueue.contains(nextNodes.get(0)) || bpmnNodeQueue.contains(nextNodes.get(1)))
					return true;
		}
		return false;
	}

	private static int assembleFirstXORBranch(XORSplitGate startingXORGate, int seqBlockCounter,
			Stack<Block> stackBlocks, ProcessSchema processSchema) {
		while (stackBlocks.search(startingXORGate) > 2) {
			SequenceBlock newSequenceBlock = processSchema.newSequenceBlock("SEQblock" + seqBlockCounter);
			newSequenceBlock.addSecondBlock(stackBlocks.pop());// add second block
			newSequenceBlock.addFirstBlock(stackBlocks.pop());// add first block
			stackBlocks.push(newSequenceBlock);
			seqBlockCounter++;
		}
		return seqBlockCounter;
	}

	private static int assembleSecondXORBranch(XORSplitGate startingXORGate, int seqBlockCounter,
			Stack<Block> stackBlocks, ProcessSchema processSchema) {
		Block pointerBlock = stackBlocks.get(stackBlocks.size() - stackBlocks.search(startingXORGate) + 1);// find a
																											// block
																											// going
																											// after the
																											// XOR split
		while (stackBlocks.search(pointerBlock) > 2) {
			SequenceBlock newSequenceBlock = processSchema.newSequenceBlock("SEQblock" + seqBlockCounter);
			newSequenceBlock.addSecondBlock(stackBlocks.pop());// add second block
			newSequenceBlock.addFirstBlock(stackBlocks.pop());// add first block
			stackBlocks.push(newSequenceBlock);
			seqBlockCounter++;
		}
		return seqBlockCounter;
	}

	private static int assembleFirstANDBranch(DeferredANDSplitGate startingANDGate, int seqBlockCounter,
			Stack<Block> stackBlocks, ProcessSchema processSchema) {
		while (stackBlocks.search(startingANDGate) > 2) {
			SequenceBlock newSequenceBlock = processSchema.newSequenceBlock("SEQblock" + seqBlockCounter);
			newSequenceBlock.addSecondBlock(stackBlocks.pop());// add second block
			newSequenceBlock.addFirstBlock(stackBlocks.pop());// add first block
			stackBlocks.push(newSequenceBlock);
			seqBlockCounter++;
		}
		return seqBlockCounter;
	}

	private static int assembleSecondANDBranch(DeferredANDSplitGate startingANDGate, int seqBlockCounter,
			Stack<Block> stackBlocks, ProcessSchema processSchema) {
		Block pointerBlock = stackBlocks.get(stackBlocks.size() - stackBlocks.search(startingANDGate) + 1);// find a
																											// block
																											// going
																											// after the
																											// XOR split
		while (stackBlocks.search(pointerBlock) > 2) {
			SequenceBlock newSequenceBlock = processSchema.newSequenceBlock("SEQblock" + seqBlockCounter);
			newSequenceBlock.addSecondBlock(stackBlocks.pop());// add second block
			newSequenceBlock.addFirstBlock(stackBlocks.pop());// add first block
			stackBlocks.push(newSequenceBlock);
			seqBlockCounter++;
		}
		return seqBlockCounter;
	}
}
