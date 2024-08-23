package org.openbpmn.bpmn.discovery;

import org.apache.commons.lang3.StringUtils;
import org.deckfour.xes.in.XesXmlParser;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.DirectedWeightedPseudograph;
import org.openbpmn.bpmn.discovery.model.DecisionMerger;
import org.openbpmn.bpmn.discovery.model.DependencyGraph;
import org.openbpmn.bpmn.discovery.model.LoopMerger;
import org.openbpmn.bpmn.discovery.model.ParallelismMerger;

import java.io.File;
import java.util.*;

public class XESAnalyzerSplitMiner {

    public static void main(String[] args) {
        try {
            // Start timing
            String fileName = "S0";
            String pathLog = "C:\\Users\\AliNourEldin\\Desktop\\da-bpmn\\generated-BPMN\\event_logs\\" + fileName + ".xes";
            String outputpath = "C:\\Users\\AliNourEldin\\Desktop\\da-bpmn\\generated-BPMN\\our\\" + fileName + ".bpmn";
            Double epsilom = 1.0;
            Double frequencyNoiseRemove = 0.05;
            Double frequency = 0.0;


            long startTime = System.nanoTime();

            XLog log = new XESAnalyzerSplitMiner().readLog(pathLog);

            Map<String, Integer> traces = new HashMap<>();
            Map<String, List<String>> tracesList = new HashMap<>();

            DependencyGraph dependencyGraph = generateDependencyGraph(log, traces, tracesList);
            double maxWeight = getMaxEdgeWeight(dependencyGraph.dependencyGraph);
            System.out.println("Max Weight: " + maxWeight);


            //get names
            dependencyGraph.dependencyGraph.vertexSet().forEach(vertex -> {
                dependencyGraph.elementsName.put(vertex.trim(), vertex);
            });

            List<String> startActivities = new ArrayList<>(dependencyGraph.startActivities);
            List<String> endActivities = new ArrayList<>(dependencyGraph.endActivities);
            dependencyGraph.startActivities.clear();
            dependencyGraph.endActivities.clear();
            String startEvent = "start";
            dependencyGraph.addVertex(startEvent);
            dependencyGraph.elementInformations.put(startEvent, new HashMap<String, String>() {{
                put("type", "start");
            }});
            dependencyGraph.elementsName.put(startEvent.trim(), startEvent);

            for (String str : startActivities) {
                dependencyGraph.addEdge(startEvent, str);
            }

            startActivities.clear();
            startActivities.add(startEvent);
            dependencyGraph.startActivities.add(startEvent);


            String endEvent = "end";
            dependencyGraph.addVertex(endEvent);
            dependencyGraph.elementInformations.put(endEvent, new HashMap<String, String>() {{
                put("type", "end");
            }});
            dependencyGraph.elementsName.put(endEvent.trim(), endEvent);

            for (String str : endActivities) {
                dependencyGraph.addEdge(str, endEvent);
//                dependencyGraph.endActivities.remove(str);
            }
            endActivities.clear();
            endActivities.add(endEvent);
            dependencyGraph.endActivities.add(endEvent);

//            for (String end : endActivities) {
//                String endEvent = "end_" + end;
//                dependencyGraph.addVertex(endEvent);
//                dependencyGraph.addEdge(end, endEvent);
//                dependencyGraph.elementsName.put(endEvent.trim(), endEvent);
//                dependencyGraph.elementInformations.put(endEvent, new HashMap<String, String>() {{
//                    put("type", "end");
//                }});
//                dependencyGraph.endActivities.add(endEvent);
//            }
            System.out.println(dependencyGraph.dependencyGraph.toString());


            long startTime_1;
            //get loop
            System.out.println("Loop: ");
            startTime_1 = System.nanoTime();
            System.out.println(dependencyGraph.loops);
            dependencyGraph.dependencyGraph.edgeSet().forEach(edge -> {
                String source = dependencyGraph.dependencyGraph.getEdgeSource(edge);
                String target = dependencyGraph.dependencyGraph.getEdgeTarget(edge);

                List<String> loop = new ArrayList<>();
                loop.add(source);
                loop.add(target);

                if (source.equals(target)) {

                    dependencyGraph.loops.add(loop);
                } else {
                    if (!dependencyGraph.loops.contains(loop) && dependencyGraph.dependencyGraph.containsEdge(target, source)) {
                        String src2tgt_loop2Pattern = "::" + source + "::" + target + "::" + source + "::";
                        String tgt2src_loop2Pattern = "::" + target + "::" + source + "::" + target + "::";
                        double src2tgt_loop2Frequency = 0;
                        double tgt2src_loop2Frequency = 0;
                        for (String trace : traces.keySet()) {
                            src2tgt_loop2Frequency += (StringUtils.countMatches(trace, src2tgt_loop2Pattern) * traces.get(trace));
                            tgt2src_loop2Frequency += (StringUtils.countMatches(trace, tgt2src_loop2Pattern) * traces.get(trace));
                        }
                        double loop2score = src2tgt_loop2Frequency + tgt2src_loop2Frequency;
                        if (loop2score > 0) {
//                        if(src2tgt_loop2Frequency>0 && tgt2src_loop2Frequency>0){
                            List<String> loop2 = new ArrayList<>();
                            loop2.add(target);
                            loop2.add(source);

                            dependencyGraph.loops.add(loop);
                            dependencyGraph.loops.add(loop2);
                        }
                    }

                }
            });
            System.out.println(dependencyGraph.loops);
            printOutTime(startTime_1);
            System.out.println("END Loop");

            //remove self loop
            dependencyGraph.loops.forEach(loop -> {
                if (loop.get(0).equals(loop.get(1))) {
                    dependencyGraph.loopsL1.add(loop);
                    dependencyGraph.dependencyGraphWithLoop.removeEdge(loop.get(0), loop.get(0));
                    dependencyGraph.dependencyGraph.addEdge(loop.get(0), loop.get(1));
                }
            });


            //get parallelism
            System.out.println("Parallelism: ");
            startTime_1 = System.nanoTime();
            dependencyGraph.dependencyGraph.edgeSet().forEach(edge -> {
                String source = dependencyGraph.dependencyGraph.getEdgeSource(edge);
                String target = dependencyGraph.dependencyGraph.getEdgeTarget(edge);

                Set<String> loop = new HashSet<>();
                loop.add(source);
                loop.add(target);

                if (!source.equals(target)) {

                    if (!dependencyGraph.parallelism.contains(loop) && dependencyGraph.dependencyGraph.containsEdge(target, source)) {
//                        String src2tgt_loop2Pattern = "::" + source + "::" + target + "::" + source + "::";
//                        String tgt2src_loop2Pattern = "::" + target + "::" + source + "::" + target + "::";
//                        double src2tgt_loop2Frequency = 0;
//                        double tgt2src_loop2Frequency = 0;
//                        for (String trace : traces.keySet()) {
//                            src2tgt_loop2Frequency += (StringUtils.countMatches(trace, src2tgt_loop2Pattern) * traces.get(trace));
//                            tgt2src_loop2Frequency += (StringUtils.countMatches(trace, tgt2src_loop2Pattern) * traces.get(trace));
//                        }
//                        double loop2score = src2tgt_loop2Frequency + tgt2src_loop2Frequency;
//                        if(loop2score==0){
                        Set<String> loop2 = new HashSet<>();
                        loop2.add(target);
                        loop2.add(source);
                        dependencyGraph.parallelism.add(loop2);
                    }
//                    }

                }
            });

            Set<Set<String>> tempParallelism = new HashSet<>(dependencyGraph.parallelism);
            dependencyGraph.parallelism.clear();
            tempParallelism.stream().forEach(loop -> {
                Iterator<String> it = loop.iterator();
                String source = it.next();
                String target = it.next();
                DefaultWeightedEdge edge = dependencyGraph.dependencyGraph.getEdge(source, target);


                if (dependencyGraph.dependencyGraph.containsEdge(target, source)) {
                    DefaultWeightedEdge edge2 = dependencyGraph.dependencyGraph.getEdge(target, source);
                    double src2tgt_frequency = dependencyGraph.dependencyGraph.getEdgeWeight(edge);
                    double tgt2src_frequency = dependencyGraph.dependencyGraph.getEdgeWeight(edge2);
//                    double parallelismScore = (double) (src2tgt_frequency - tgt2src_frequency) / (src2tgt_frequency + tgt2src_frequency);
                    double parallelismScore = (double) (src2tgt_frequency - tgt2src_frequency) / Math.max(src2tgt_frequency, tgt2src_frequency);
                    if (Math.abs(parallelismScore) < epsilom) {
                        dependencyGraph.parallelism.add(new HashSet<String>() {{
                            add(source);
                            add(target);
                        }});
                    }

                }

            });


            System.out.println(dependencyGraph.parallelism);


            // Remove edges with frequency less than the threshold
            Map<String, Double> removableEdge = removeEdgesBasedOnFrequency(dependencyGraph, frequencyNoiseRemove, maxWeight);
            removableEdge.putAll(removeEdgesBasedOnFrequency(dependencyGraph, frequency, maxWeight));


            dependencyGraph.loops.clear();

            //get loop
            System.out.println("Loop: ");
            startTime_1 = System.nanoTime();
            System.out.println(dependencyGraph.loops);
            dependencyGraph.dependencyGraph.edgeSet().forEach(edge -> {
                String source = dependencyGraph.dependencyGraph.getEdgeSource(edge);
                String target = dependencyGraph.dependencyGraph.getEdgeTarget(edge);

                List<String> loop = new ArrayList<>();
                loop.add(source);
                loop.add(target);

                if (source.equals(target)) {

                    dependencyGraph.loops.add(loop);
                } else {
                    if (!dependencyGraph.loops.contains(loop) && dependencyGraph.dependencyGraph.containsEdge(target, source)) {
                        String src2tgt_loop2Pattern = "::" + source + "::" + target + "::" + source + "::";
                        String tgt2src_loop2Pattern = "::" + target + "::" + source + "::" + target + "::";
                        double src2tgt_loop2Frequency = 0;
                        double tgt2src_loop2Frequency = 0;
                        for (String trace : traces.keySet()) {
                            src2tgt_loop2Frequency += (StringUtils.countMatches(trace, src2tgt_loop2Pattern) * traces.get(trace));
                            tgt2src_loop2Frequency += (StringUtils.countMatches(trace, tgt2src_loop2Pattern) * traces.get(trace));
                        }
                        double loop2score = src2tgt_loop2Frequency + tgt2src_loop2Frequency;
                        if (loop2score > 0) {
//                        if(src2tgt_loop2Frequency>0 && tgt2src_loop2Frequency>0){
                            List<String> loop2 = new ArrayList<>();
                            loop2.add(target);
                            loop2.add(source);

                            dependencyGraph.loops.add(loop);
                            dependencyGraph.loops.add(loop2);
                        }
                    }

                }
            });
            System.out.println(dependencyGraph.loops);
            printOutTime(startTime_1);
            System.out.println("END Loop");

            removableEdge.keySet().forEach(edge -> {
                String[] edgeVertices = edge.split("::");
                dependencyGraph.dependencyGraph.addEdge(edgeVertices[0], edgeVertices[1]);
                dependencyGraph.dependencyGraph.setEdgeWeight(edgeVertices[0], edgeVertices[1], removableEdge.get(edge));
                dependencyGraph.dependencyGraphWithLoop.addEdge(edgeVertices[0], edgeVertices[1]);
                dependencyGraph.dependencyGraphWithLoop.setEdgeWeight(edgeVertices[0], edgeVertices[1], removableEdge.get(edge));
            });

            dependencyGraph.removeAllParallelismDiscovery(tempParallelism);

            removableEdge.clear();

            // Remove edges with frequency less than the threshold
            removableEdge.putAll(removeEdgesBasedOnFrequency(dependencyGraph, frequencyNoiseRemove, maxWeight));
            removableEdge.putAll(removeEdgesBasedOnFrequency(dependencyGraph, frequency, maxWeight));

            System.out.println(dependencyGraph.parallelism);
            dependencyGraph.filerParallelism();
            System.out.println(dependencyGraph.parallelism);
            ParallelismMerger parallelismMerger = new ParallelismMerger(dependencyGraph.parallelism,
                    dependencyGraph.dependencyGraphWithLoop);
            LinkedList<LinkedList<String>> parallelMergeRelations = parallelismMerger.getParallelims();
            System.out.println("Parallel Merge Relations: ");
            System.out.println(parallelMergeRelations);
            printOutTime(startTime_1);
            System.out.println("END Parallelism");


            //check if there is not connected vertices in the outgoing or incomming, should add the edge from the removable edge based on teh high frequency of this edge
            dependencyGraph.dependencyGraph.vertexSet().forEach(vertex -> {
                if (dependencyGraph.loops.stream().noneMatch(list -> list.contains(vertex))) {
                    Set<DefaultWeightedEdge> incomingEdges = dependencyGraph.dependencyGraph.incomingEdgesOf(vertex);
                    Set<DefaultWeightedEdge> outgoingEdges = dependencyGraph.dependencyGraph.outgoingEdgesOf(vertex);
                    if (incomingEdges.isEmpty()) {
                        System.out.println("Incoming: " + vertex);
                        String maxEdge = "";
                        double maxFrequency = 0;
                        for (String edge : removableEdge.keySet()) {
                            String[] edgeVertices = edge.split("::");
                            if (edgeVertices[1].equals(vertex)) {
                                if (removableEdge.get(edge) > maxFrequency) {
                                    maxFrequency = removableEdge.get(edge);
                                    maxEdge = edge;
                                }
                            }
                        }
                        if (!maxEdge.isEmpty()) {
                            String[] edgeVertices = maxEdge.split("::");
                            dependencyGraph.dependencyGraph.addEdge(edgeVertices[0], edgeVertices[1]);
                            dependencyGraph.dependencyGraphWithLoop.addEdge(edgeVertices[0], edgeVertices[1]);
                        }
                    }

                    if (outgoingEdges.isEmpty()) {
                        System.out.println("Outgoing: " + vertex);
                        String maxEdge = "";
                        double maxFrequency = 0;
                        for (String edge : removableEdge.keySet()) {
                            String[] edgeVertices = edge.split("::");
                            if (edgeVertices[0].equals(vertex)) {
                                if (removableEdge.get(edge) > maxFrequency) {
                                    maxFrequency = removableEdge.get(edge);
                                    maxEdge = edge;
                                }
                            }
                        }
                        if (!maxEdge.isEmpty()) {
                            String[] edgeVertices = maxEdge.split("::");
                            dependencyGraph.dependencyGraph.addEdge(edgeVertices[0], edgeVertices[1]);
                            dependencyGraph.dependencyGraphWithLoop.addEdge(edgeVertices[0], edgeVertices[1]);
                        }
                    }
                }
            });


            //continue loop detection
            System.out.println("Loop2: ");
            startTime_1 = System.nanoTime();
            System.out.println(dependencyGraph.loops);
//            dependencyGraph.getLongLoopDiscovery();
//            dependencyGraph.findAndRemoveLoops();

            dependencyGraph.findAndRemoveLoops2(traces);
            System.out.println(dependencyGraph.loops);
            LoopMerger loopMerger = new LoopMerger(dependencyGraph.loops, dependencyGraph.dependencyGraphWithLoop);
            System.out.println(loopMerger.getMergedLoop());
            printOutTime(startTime_1);
            System.out.println("END Loop2");
//
//
            //get inclusive
            System.out.println("Inclusive: ");
            startTime_1 = System.nanoTime();
            dependencyGraph.findInclusive();
            System.out.println(dependencyGraph.inclusive);
            printOutTime(startTime_1);
            System.out.println("END Inclusive");

            //get exclusive
            System.out.println("Decision: ");
            startTime_1 = System.nanoTime();
            dependencyGraph.findExculisve();
            System.out.println(dependencyGraph.exlusive);
            DecisionMerger decisionMerger = new DecisionMerger(dependencyGraph.exlusive, dependencyGraph.dependencyGraphWithLoop);
            LinkedList<LinkedList<String>> decisionRelations = decisionMerger.getDecisions();
            System.out.println("Decision Relations: ");
            System.out.println(decisionRelations);
            printOutTime(startTime_1);
            System.out.println("End Decision");
            System.out.println(dependencyGraph.dependencyGraphWithLoop.toString());
            System.out.println(dependencyGraph.dependencyGraph.toString());

            //get sequence
            BPMNDiscovery bpmnDiscovery = new BPMNDiscovery(dependencyGraph);
            bpmnDiscovery.DependencyGraphToBPMN();
            bpmnDiscovery.saveMode(outputpath);
            printOutTime(startTime);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void printOutTime(long startTime) {

        // Stop timing
        long endTime = System.nanoTime();
        // Calculate execution time in milliseconds
        long duration = (endTime - startTime) / 1_000_000; // Convert nanoseconds to milliseconds
        System.out.println("Execution time: " + duration + " ms");
    }

    public XLog readLog(String filePath) throws Exception {
        XesXmlParser parser = new XesXmlParser();
        if (parser.canParse(new File(filePath))) {
            List<XLog> logs = parser.parse(new File(filePath));
            if (!logs.isEmpty()) {
                return logs.get(0); // Return the first log
            }
        }
        throw new Exception("Unable to parse log");
    }

    public static DependencyGraph generateDependencyGraph(XLog log, Map<String, Integer> traces, Map<String, List<String>> tracesList) {
        DependencyGraph graph = new DependencyGraph();
        for (XTrace trace : log) {
            List<String> lst = new ArrayList<>();
            String traceName = "::";
            String lastEvent = null;
            for (XEvent event : trace) {
                String eventName = event.getAttributes().get("concept:name").toString();
                graph.addVertex(eventName);


                if (lastEvent != null) {
                    graph.addEdge(lastEvent, eventName);
                }
                lastEvent = eventName;
                traceName += DependencyGraph.regex(eventName) + "::";
                lst.add(DependencyGraph.regex(eventName));
            }
            if (traces.containsKey(traceName)) {
                traces.put(traceName, traces.get(traceName) + 1);
            } else {
                traces.put(traceName, 1);
                tracesList.put(traceName, lst);
            }
            graph.startActivities.add(trace.get(0).getAttributes().get("concept:name").toString());
            graph.endActivities.add(trace.get(trace.size() - 1).getAttributes().get("concept:name").toString());
        }
        return graph;
    }


    public static Set<List<String>> extractTracePairs(XLog log) {
        Set<List<String>> tracePairs = new HashSet<>();
        for (XTrace trace : log) {
            List<String> events = new ArrayList<>();
            for (XEvent event : trace) {
                String eventName = event.getAttributes().get("concept:name").toString();
                events.add(eventName);
            }
            tracePairs.add(new ArrayList<>(events)); // Add a copy for immutability concerns
        }
        return tracePairs;
    }

    public static Set<String> findStartActivities(Set<List<String>> traces) {
        Set<String> startEvents = new HashSet<>();
        // Add the first event of each trace to the set
        for (List<String> trace : traces) {
            startEvents.add(trace.get(0));
        }

        // Check if each event in the set really starts all traces it appears in
        Set<String> verifiedStartEvents = new HashSet<>(startEvents);
        for (String event : startEvents) {
            for (List<String> trace : traces) {
                if (!trace.get(0).equals(event)) {
                    verifiedStartEvents.remove(event);
                    break;
                }
            }
        }

        return verifiedStartEvents;
    }

    public static Set<String> findEndActivities(Set<List<String>> traces) {
        Set<String> endEvents = new HashSet<>();
        // Add the last event of each trace to the set
        for (List<String> trace : traces) {
            endEvents.add(trace.get(trace.size() - 1));
        }


        return endEvents;
    }

    public static boolean areTasksConcurrent(DirectedWeightedPseudograph<String, DefaultWeightedEdge> graph, String a, String b, double epsilon) {

        DefaultWeightedEdge edgeAtoB = graph.getEdge(a, b);
        DefaultWeightedEdge edgeBtoA = graph.getEdge(b, a);

        // Condition 3: Basic requirement for concurrency
        if (edgeAtoB == null || edgeBtoA == null)
            return false;
        double weightAtoB = graph.getEdgeWeight(edgeAtoB);
        double weightBtoA = graph.getEdgeWeight(edgeBtoA);
        if (weightAtoB <= 0 || weightBtoA <= 0)
            return false;


        // Adjusting for frequency-based epsilon check
        double max = Math.max(weightAtoB, weightBtoA);
        double sum = weightAtoB + weightBtoA;
        double diff = Math.abs(weightAtoB - weightBtoA) / max;  // Normalize the difference by the maximum weight
        if (diff >= epsilon)
            return false;


        return true;
    }

    public static Set<Set<String>> findAllConcurrentPairs(DirectedWeightedPseudograph<String, DefaultWeightedEdge> graph, double epsilon) {
        Set<Set<String>> concurrentPairs = new HashSet<>();
        List<String> allVertices = new ArrayList<>(graph.vertexSet());

        for (int i = 0; i < allVertices.size(); i++) {
            for (int j = i + 1; j < allVertices.size(); j++) {
                String taskA = allVertices.get(i);
                String taskB = allVertices.get(j);
                if (areTasksConcurrent(graph, taskA, taskB, epsilon)) {
                    Set<String> pair = new HashSet<>();
                    pair.add(taskA);
                    pair.add(taskB);
                    concurrentPairs.add(pair);
                }
            }
        }

        return concurrentPairs;
    }

    public static double getMaxEdgeWeight(DirectedWeightedPseudograph<String, DefaultWeightedEdge> graph) {
        double maxWeight = Double.NEGATIVE_INFINITY;
        for (DefaultWeightedEdge edge : graph.edgeSet()) {
            double weight = graph.getEdgeWeight(edge);
            if (weight > maxWeight) {
                maxWeight = weight;
            }
        }
        return maxWeight;
    }

    public static Map<String, Double> removeEdgesBasedOnFrequency(DependencyGraph dependencyGraph, double frequency, Double maxWeight) {

        Map<String, Double> removableList = new HashMap<>();
        Set<DefaultWeightedEdge> edgesToRemove = new HashSet<>();

        for (DefaultWeightedEdge edge : dependencyGraph.dependencyGraph.edgeSet()) {
            double weight = dependencyGraph.dependencyGraph.getEdgeWeight(edge);
            if (weight / maxWeight < frequency) {
                edgesToRemove.add(edge);
            }
        }

        for (DefaultWeightedEdge edge : edgesToRemove) {
            String source = dependencyGraph.dependencyGraph.getEdgeSource(edge);
            String target = dependencyGraph.dependencyGraph.getEdgeTarget(edge);
            removableList.put(source + "::" + target, dependencyGraph.dependencyGraph.getEdgeWeight(edge));
            dependencyGraph.dependencyGraph.removeEdge(source, target);
            dependencyGraph.dependencyGraphWithLoop.removeEdge(source, target);
        }
        return removableList;
    }

}
