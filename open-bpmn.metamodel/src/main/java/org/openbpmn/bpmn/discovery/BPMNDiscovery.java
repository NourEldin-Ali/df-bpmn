package org.openbpmn.bpmn.discovery;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.AllDirectedPaths;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.openbpmn.bpmn.BPMNModel;
import org.openbpmn.bpmn.BPMNTypes;
import org.openbpmn.bpmn.discovery.model.*;
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

//TODO: clean the code
//TODO: to validate in split add target as successor
//TODO: Validate loop block construction (target !=1) why create always new block??
//TODO: post processing to avoid deadlock
//TODO: post processing should not merge all the gateways (only loops?)
//TODO: bug in loop: results_part_3/doc-5.4-gpt-4o-2.json 
public class BPMNDiscovery {
    private static Logger logger = Logger.getLogger(BPMNDiscovery.class.getName());
    public static String PARALLEL = "parallel";
    public static String DECISION = "decision";
    public static String INCLUSIVE = "inclusive";

    static String GATEWAY_TYPE = "gatewayType";
    static String GATEWAY_NUM = "num";
    public BPMNModel model;
    public List<String> startsEvent;
    public List<String> endsEvent;
    public LinkedList<String> dependencies;
    public Map<String, LinkedList<LinkedList<String>>> relations;
    public List<Pair<Set<String>, Set<String>>> loops;
    public DependencyGraph dependenciesGraph = new DependencyGraph();
    private Integer seqenceFlowId = 0;
    private Integer gatewayId = 0;
    private Gateway splitGateway = null;
    private Boolean isAdded = false;

    public BPMNDiscovery(DependencyGraph dependenciesGraph) {
        logger.info("...Creating BPMN stated");
        model = BPMNModelFactory.createInstance("demo", "1.0.0", "http://org.openbpmn");


        this.startsEvent = dependenciesGraph.startActivities;
        this.endsEvent = dependenciesGraph.endActivities;

        DependencyGraph.regex(startsEvent);
        DependencyGraph.regex(endsEvent);

        this.dependenciesGraph = dependenciesGraph;
        this.dependenciesGraph.changeVertexNameToRegex();

        this.dependencies = (LinkedList<String>) dependenciesGraph.getDependenciesDFA();


        LoopMerger loopMerger = new LoopMerger(this.dependenciesGraph.loops, dependenciesGraph.dependencyGraphWithLoop);
        this.loops = loopMerger.getMergedLoop();
        System.out.println("loops");
        System.out.println(this.loops);

        System.out.println(this.dependenciesGraph.elementInformations);
        System.out.println(this.dependenciesGraph.elementsName);
        //get missing exclusive for the loops
        DecisionForLoop decisionForLoop = new DecisionForLoop(dependenciesGraph.exlusive, dependenciesGraph.parallelism,
                dependenciesGraph.inclusive, this.loops);
//        decisionForLoop.appendDecisions();

        System.out.println(dependenciesGraph.dependencyGraph.toString());
        //get exclusive
        DecisionMerger decisionMerger = new DecisionMerger(dependenciesGraph.exlusive, dependenciesGraph.dependencyGraph);
        LinkedList<LinkedList<String>> decisionRelations = decisionMerger.getDecisions();

        System.out.println("decisionRelations1");
        System.out.println(decisionRelations);
        //get parallelism
        ParallelismMerger parallelismMerger = new ParallelismMerger(dependenciesGraph.parallelism,
                dependenciesGraph.dependencyGraph);
        LinkedList<LinkedList<String>> parallelRelations = parallelismMerger.getParallelims();
        System.out.println("parallelRelations1");
        System.out.println(parallelRelations);

        //get inclusive
        ParallelismMerger inclusiveMerger = new ParallelismMerger(dependenciesGraph.inclusive, dependenciesGraph.dependencyGraphWithLoop);
        LinkedList<LinkedList<String>> inclusiveRelations = inclusiveMerger.getParallelims();


        this.relations = new LinkedHashMap<>();
        this.relations.put(BPMNDiscovery.DECISION, decisionRelations);
        this.relations.put(BPMNDiscovery.PARALLEL, parallelRelations);
        this.relations.put(BPMNDiscovery.INCLUSIVE, inclusiveRelations);

        relations.values().forEach(elements -> {
            elements.forEach(DependencyGraph::regex);
        });

        Iterator<Pair<Set<String>, Set<String>>> loopIter = loops.iterator();
        while (loopIter.hasNext()) {
            Pair<Set<String>, Set<String>> elements = loopIter.next();
            DependencyGraph.regex(elements.getSource());
            DependencyGraph.regex(elements.getTarget());
        }


    }

    public BPMNDiscovery(List<String> startsEvent, List<String> endsEvent, LinkedList<String> dependencies,
                         Map<String, LinkedList<LinkedList<String>>> relations, LinkedList<Pair<Set<String>, Set<String>>> loops) {
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
            elements.forEach(DependencyGraph::regex);
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
            elements.forEach(DependencyGraph::regex);
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
        LoopMerger loopMerger = new LoopMerger(dependenciesGraph.loops, dependenciesGraph.dependencyGraph);
        this.loops = loopMerger.getMergedLoop();

        this.dependencies = DepthFirstSearch.DFSToList(dependencies, startsEvent.get(0));
    }

    /**
     * This function is to convert Dependency graph to BPMN
     * <p>
     * /@param startsEvent
     * /@param endsEvent
     * /@param dependencies the relation between pair of element
     * /@param relations    it contains the decision and parallel relations as list
     * /@throws BPMNModelException
     * /@throws CloneNotSupportedException
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
                                                .getAllSuccesssors(precedingOfTarget).stream().map(BPMNElement::getId)
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
        for (Pair<Set<String>, Set<String>> loop : loops) {
            ArrayList<BPMNElementNode> sourceElements = new ArrayList<>();
            ArrayList<BPMNElementNode> targetElements = new ArrayList<>();

            for (String sourceId : loop.getSource()) {
                BPMNElementNode sourceElement = (BPMNElementNode) process.findElementById(sourceId);
//				System.out.println(sourceElement.getId());
                sourceElements.add(sourceElement);
            }

            for (String targetId : loop.getTarget()) {
                BPMNElementNode targetElement = (BPMNElementNode) process.findElementById(targetId);
//				System.out.println(targetElement.getId());
                targetElements.add(targetElement);
            }
            try {
                loopBlockConstruction(sourceElements, targetElements);
            } catch (Exception e) {
                System.out.println("########### LOOP CONSTRACTION ERROR ##################");
                System.out.println(e.getMessage());
            }
        }

        //TODO : check error -------- important
        // add loops
        for (Pair<Set<String>, Set<String>> loop : loops) {
            ArrayList<BPMNElementNode> sourceElements = new ArrayList<>();
            for (String sourceId : loop.getSource()) {
                BPMNElementNode sourceElement = (BPMNElementNode) process.findElementById(sourceId);
                sourceElements.add(sourceElement);
            }

            ArrayList<BPMNElementNode> targetElements = new ArrayList<>();
            for (String targetId : loop.getTarget()) {
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
//		if (splitGateway == null) {
        gatewayType = BPMNTypes.EXCLUSIVE_GATEWAY;
        gatewayNum = "-3";
//		}
//		else {
//			gatewayType = splitGateway.getType();
//			gatewayNum = splitGateway.getAttribute(GATEWAY_NUM);
//		}

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
                if (!isAdded) {
                    System.out.println("add new gateway before the existing gateway(" + probablySelectedGateway.getId()
                            + "): gt-" + gatewayId.toString());
//					System.out.println(probablyRelationType.get(GATEWAY_TYPE));
//					System.out.println(probablySelectedGateway.getIngoingSequenceFlows().size());
//					System.out.println(probablySelectedGateway.getOutgoingSequenceFlows().size());
                    // print probablyRelationType.get(GATEWAY_TYPE)
//					System.out.println(probablyRelationType);


                    String gatewayType = probablyRelationType.get(GATEWAY_TYPE);

                    if (gatewayType == null) {
                        gatewayType = BPMNTypes.EXCLUSIVE_GATEWAY;
                    }


                    // add gateway before probablySelectedGateway
                    Gateway newGateway = process.addGateway("gt-" + gatewayId.toString(), "",
                            gatewayType);
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
            // TODO: should add the target as suction not the selectedSequence flow. getTargetElement
            // process.addSequenceFlow("sq-" + seqenceFlowId.toString(), selectedGateway.getId(),
            // selectedSequence.getTargetElement().getId());
            process.addSequenceFlow("sq-" + seqenceFlowId.toString(), selectedGateway.getId(),
                    targetElement.getId());
            seqenceFlowId++;
            splitGateway = selectedGateway;

        } else if (acceptedSequenceFlows.size() >= 1) {
            isAdded = true;
            System.out.println("Add new gateway after existing gateway (" + selectedGateway.getId() + "): gt-"
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
                        } else if (entry.getKey().contentEquals(INCLUSIVE)) {
                            gatewayType = BPMNTypes.INCLUSIVE_GATEWAY;
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
     * <p>
     * /@param succeesor
     * /@param target
     *
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
                    .map(BPMNElement::getId).collect(Collectors.toSet());
            // get source of element1
            Set<DefaultWeightedEdge> targetsEdgeElementEdge = dependenciesGraph.dependencyGraph
                    .outgoingEdgesOf(sourceElement.getId());
            Set<String> targetsElement = new HashSet<>();
            targetsEdgeElementEdge.stream()
                    .forEach(edge -> targetsElement.add(dependenciesGraph.dependencyGraph.getEdgeTarget(edge)));
//            System.out.println(targetsElementFromBPMN);
//            System.out.println(targetsElement);
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

        //check if we have multiple source elements
        if (sourceElements.size() > 1) {
//            int countSqOfsSource = 0;
//            for (BPMNElementNode element : sourceElements) {
//                if (!element.getOutgoingSequenceFlows().isEmpty()) {
//                    countSqOfsSource++;
//                }
//            }

//            if (countSqOfsSource != sourceElements.size()) {

            Set<BPMNElementNode> gatewaysofSource = sourceElements.stream()
                    .flatMap(element -> process.getAllForwardNestedGateways(element).stream())
                    .collect(Collectors.toSet());

            List<BPMNElementNode> acceptedGateway = new ArrayList<>();
            for (BPMNElementNode bpmnElementNode : gatewaysofSource) {
                List<BPMNElementNode> predeccessors = process.getAllPredeccessors(bpmnElementNode);
                if (predeccessors.containsAll(sourceElements)) {
                    acceptedGateway.add(bpmnElementNode);
                }
            }
            AtomicReference<BPMNElementNode> sourceElement = new AtomicReference<>(acceptedGateway.stream().filter(gateway ->
                            gateway.getIngoingSequenceFlows().stream().
                                    allMatch(sequenceFlow -> !acceptedGateway.contains(sequenceFlow.getSourceElement())))
                    .findAny().orElse(null));
            System.out.println("LOOP BLOCK CONSTRUCTION: "
                    + sourceElements.stream().map(BPMNElement::getId).collect(Collectors.toList()));
            if (sourceElement.get() != null) {
                sourceElements.stream().flatMap(element -> element.getOutgoingSequenceFlows().stream())
                        .forEach(sq -> {
                            if (sq.getTargetElement() instanceof Gateway) {
                                if(sq.getTargetElement().getIngoingSequenceFlows().size()>1){
                                    sourceElement.set(null);
                                    return;
                                }
                            }else{
                                sourceElement.set(null);
                                return;
                            }
                        });
            }
            if (sourceElement.get() == null) {

                System.out.println("LOOP BLOCK CONSTRUCTION: "
                        + sourceElements.stream().map(BPMNElement::getId).collect(Collectors.toList()));
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
                    List<String> sourcesId = listOfPaths.stream().map(GraphPath::getStartVertex)
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
        }



        if (targetElements.size() > 1) {

//            // check if there exists a relation between the targets (relation = gateway)
//            List<String> targetIds = targetElements.stream().map(BPMNElement::getId).collect(Collectors.toList());
//
//            boolean shouldConstruct = true;
//            Set<BPMNElementNode> gatewaysofSource = targetElements.stream()
//                    .flatMap(element -> process.getAllBackwardNestedGateways(element).stream())
//                    .collect(Collectors.toSet());
//
//
//            for (BPMNElementNode bpmnElementNode : gatewaysofSource) {
//                List<BPMNElementNode> sucessors = process.getAllSuccesssors(bpmnElementNode);
//                if (sucessors.containsAll(targetElements)) {
//                    shouldConstruct = false;
//                    break;
//                }
//            }

            AtomicReference<BPMNElementNode> targetElementTemp = new AtomicReference<>();
            // get root gateway based on the relation
            List<String> targetIds = targetElements.stream().map(BPMNElement::getId).collect(Collectors.toList());
            int num = getGatewayNum(targetIds);
            StringBuffer gatewayNum = new StringBuffer();
            gatewayNum.append(num);
            if (!gatewayNum.toString().contentEquals("-1")) {
                List<BPMNElementNode> acceptedGateway = new ArrayList<>();
                acceptedGateway =
                        process.getGateways().stream()
                                .filter(gateway -> gateway.getAttribute(GATEWAY_NUM).contentEquals(gatewayNum) && gateway.getIngoingSequenceFlows().size() <= 1).collect(Collectors.toList());
                targetElementTemp.set(acceptedGateway.stream().filter(gateway ->
                        process.getAllSuccesssors(gateway).size() == targetElements.size()).findAny().orElse(null));

            } else {
                Set<BPMNElementNode> gatewaysofSource = sourceElements.stream()
                        .flatMap(element -> process.getAllBackwardNestedGateways(element).stream())
                        .collect(Collectors.toSet());

                List<BPMNElementNode> acceptedGateway = new ArrayList<>();
                for (BPMNElementNode bpmnElementNode : gatewaysofSource) {
                    List<BPMNElementNode> sucessors = process.getAllSuccesssors(bpmnElementNode);
                    if (sucessors.containsAll(targetElements)) {
                        acceptedGateway.add(bpmnElementNode);
                    }
                }
                targetElementTemp.set(acceptedGateway.stream().filter(gateway ->
                                gateway.getOutgoingSequenceFlows().stream().
                                        allMatch(sequenceFlow -> !acceptedGateway.contains(sequenceFlow.getTargetElement())))
                        .findAny().orElse(null));
//                targetElement = bpmnElementNode;

            }

            if (targetElementTemp.get() != null) {
                targetElements.stream().flatMap(element -> element.getIngoingSequenceFlows().stream())
                        .forEach(sq -> {
                            if (sq.getSourceElement() instanceof Gateway) {
                                if(sq.getSourceElement().getIngoingSequenceFlows().size()>1){
                                    targetElementTemp.set(null);
                                    return;
                                }
                            }else{
                                targetElementTemp.set(null);
                                return;
                            }
                        });
            }

            // if no gateway added to the source elements
            if (targetElementTemp.get() ==null) {

                System.out.println("LOOP BLOCK TARGET CONSTRUCTION: "
                        + targetElements.stream().map(BPMNElement::getId).collect(Collectors.toList()));
                //add temp activity
                String tempActivity = "___temp___";
                BPMNElementNode tempElement = process.addTask(tempActivity, tempActivity, BPMNTypes.TASK);
                //use split to add relation between the temp and the targets to add the split gateway (to add the relation correcte relation between them)
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
                //delete the temp activity
                for (SequenceFlow sq : tempElement.getOutgoingSequenceFlows()) {
                    process.deleteSequenceFlow(sq.getId());
                }
                process.deleteElementNode(tempElement.getId());
                // in case an activity has more that on incoming sequence flow
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

    }

    private void addLoopGateway(List<BPMNElementNode> sourceElements, List<BPMNElementNode> targetElements)
            throws BPMNModelException, CloneNotSupportedException {
        System.out.println("LOOP: " + sourceElements.stream().map(BPMNElement::getId).collect(Collectors.toList())
                + "->" + targetElements.stream().map(BPMNElement::getId).collect(Collectors.toList()));
        BPMNProcess process = model.openDefaultProces();
        BPMNElementNode targetElement = null;
        if (targetElements.size() == 1) {
            targetElement = targetElements.get(0);
        } else {

//            // get root gateway based on the relation
//            List<String> targetIds = targetElements.stream().map(BPMNElement::getId).collect(Collectors.toList());
//            int num = getGatewayNum(targetIds);
//            StringBuffer gatewayNum = new StringBuffer();
//            gatewayNum.append(num);
//            if (!gatewayNum.toString().contentEquals("-1")) {
//                List<Gateway> acceptedGateway =
//                process.getGateways().stream()
//                        .filter(gateway -> gateway.getAttribute(GATEWAY_NUM).contentEquals(gatewayNum) && gateway.getIngoingSequenceFlows().size() <= 1).collect(Collectors.toList());
//
//                if(acceptedGateway.isEmpty()){
//                    System.out.println("Error in detect loop targets relation (gateway)");
//                    return;
//                }else{
//                    targetElement = acceptedGateway.stream().filter(gateway ->
//                            gateway.getOutgoingSequenceFlows().size() == targetElements.size()).findAny().orElse(null);
//                    if(targetElement==null){
//                        System.out.println("Error in detect loop targets relation");
//                        return;
//                    }
//                }
//            } else {
//                Set<BPMNElementNode> gatewaysofSource = sourceElements.stream()
//                        .flatMap(element -> process.getAllBackwardNestedGateways(element).stream())
//                        .collect(Collectors.toSet());
//
//                List<BPMNElementNode> acceptedGateway = new ArrayList<>();
//                for (BPMNElementNode bpmnElementNode : gatewaysofSource) {
//                    List<BPMNElementNode> sucessors = process.getAllSuccesssors(bpmnElementNode);
//                    if (sucessors.containsAll(targetElements)) {
//                        acceptedGateway.add(bpmnElementNode);
//                    }
//                }
//
//
//                List<BPMNElementNode> acceptedGateway2 =  acceptedGateway.stream().filter(gateway ->
//                        gateway.getOutgoingSequenceFlows().stream().
//                                allMatch(sequenceFlow -> !acceptedGateway.contains(sequenceFlow.getTargetElement()))).collect(Collectors.toList());
//
//                if(acceptedGateway2.isEmpty()){
//                    System.out.println("Error in detect loop targets relation-0 (gateway)");
//                    return;
//                }else{
//                    targetElement = acceptedGateway2.stream().filter(gateway ->
//                            gateway.getOutgoingSequenceFlows().size() == targetElements.size()).findAny().orElse(null);
//                    if(targetElement==null){
//                        System.out.println("Error in detect loop targets relation-0");
//                        return;
//                    }
//                }
//
////                targetElement = bpmnElementNode;
//                if (targetElement == null) {
//                    System.out.println("Error in detect loop targets relation-1");
//                    return;
//                }
//            }

            AtomicReference<BPMNElementNode> targetElementTemp = new AtomicReference<>();
            // get root gateway based on the relation
            List<String> targetIds = targetElements.stream().map(BPMNElement::getId).collect(Collectors.toList());
            int num = getGatewayNum(targetIds);
            StringBuffer gatewayNum = new StringBuffer();
            gatewayNum.append(num);
            if (!gatewayNum.toString().contentEquals("-1")) {
                List<BPMNElementNode> acceptedGateway = new ArrayList<>();

                acceptedGateway =
                        process.getGateways().stream()
                        .filter(gateway -> gateway.getAttribute(GATEWAY_NUM).contentEquals(gatewayNum) && gateway.getIngoingSequenceFlows().size() <= 1).collect(Collectors.toList());
                targetElementTemp.set(acceptedGateway.stream().filter(gateway ->
                        process.getAllSuccesssors(gateway).size() == targetElements.size()).findAny().orElse(null));


            } else {
                Set<BPMNElementNode> gatewaysofSource = sourceElements.stream()
                        .flatMap(element -> process.getAllBackwardNestedGateways(element).stream())
                        .collect(Collectors.toSet());

                List<BPMNElementNode> acceptedGateway = new ArrayList<>();
                for (BPMNElementNode bpmnElementNode : gatewaysofSource) {
                    List<BPMNElementNode> sucessors = process.getAllSuccesssors(bpmnElementNode);
                    if (sucessors.containsAll(targetElements)) {
                        acceptedGateway.add(bpmnElementNode);
                    }
                }
                targetElementTemp.set(acceptedGateway.stream().filter(gateway ->
                                gateway.getOutgoingSequenceFlows().stream().
                                        allMatch(sequenceFlow -> !acceptedGateway.contains(sequenceFlow.getTargetElement())))
                        .findAny().orElse(null));
//                targetElement = bpmnElementNode;

            }

            if(targetElementTemp.get() == null){
                System.out.println("Error in detect loop targets relation");
                return;
            }else{
                targetElement = targetElementTemp.get();
            }
        }




        BPMNElementNode sourceElement = null;
        if (sourceElements.size() == 1) {
            sourceElement = sourceElements.get(0);
        } else {


            Set<BPMNElementNode> gatewaysofSource = sourceElements.stream()
                    .flatMap(element -> process.getAllForwardNestedGateways(element).stream())
                    .collect(Collectors.toSet());

            List<BPMNElementNode> acceptedGateway = new ArrayList<>();
            for (BPMNElementNode bpmnElementNode : gatewaysofSource) {
                List<BPMNElementNode> predeccessors = process.getAllPredeccessors(bpmnElementNode);
                if (predeccessors.containsAll(sourceElements)) {
                    acceptedGateway.add(bpmnElementNode);
                }
            }
            sourceElement = acceptedGateway.stream().filter(gateway ->
                            gateway.getIngoingSequenceFlows().stream().
                                    allMatch(sequenceFlow -> !acceptedGateway.contains(sequenceFlow.getSourceElement())))
                    .findAny().orElse(null);
//            for (BPMNElementNode target : gatewaysofSource) {
//                int countCanAccess = 0;
//                for (BPMNElementNode src : sourceElements) {
//                    if (process.isPreceding(src, target, target)) {
//                        countCanAccess++;
//                    }
//                }
//                if (countCanAccess == sourceElements.size()) {
//                    sourceElement = target;
//                    break;
//                }
//            }

            if (sourceElement == null) {
                System.out.println("Error in detect loop sources relation");
                return;
            }
        }

        if (sourceElement.getOutgoingSequenceFlows().size() == 0) {
            if (targetElement.getIngoingSequenceFlows().size() > 0) {
                // add only one gateway before the target
                System.out.println("add new loop gateway before the target (" + targetElement.getId() + "): gt-"
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
                        + targetElement.getId() + "): gt-"
                        + gatewayId.toString());
                // add new sequence flow
                process.addSequenceFlow("sq-" + seqenceFlowId.toString(), sourceElement.getId(), targetElement.getId());
                seqenceFlowId++;

            }
        } else {
            if (targetElement.getIngoingSequenceFlows().size() > 0) {
                System.out.println("add new loop gateways (2) before the target("
                        + targetElement.getId()
                        + "), and after source (" + sourceElement.getId()
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
                        + sourceElement.getId()
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
