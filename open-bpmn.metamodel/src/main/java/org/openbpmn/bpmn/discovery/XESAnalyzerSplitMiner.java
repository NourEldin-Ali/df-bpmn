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
            String fileName = "M3";
            String pathLog = "C:\\Users\\AliNourEldin\\Desktop\\da-bpmn\\generated-BPMN\\event_logs\\" + fileName + ".xes";
            String outputpath = "C:\\Users\\AliNourEldin\\Desktop\\da-bpmn\\generated-BPMN\\our\\" + fileName + ".bpmn";
            Double epsilom = 1.0;
            Double frequency = 0.0;


            long startTime = System.nanoTime();

            XLog log = new XESAnalyzerSplitMiner().readLog(pathLog);

            Map<String, Integer> traces = new HashMap<>();
            Map<String, List<String>> tracesList = new HashMap<>();

            DependencyGraph dependencyGraph = generateDependencyGraph(log, traces, tracesList);

            //get names
            dependencyGraph.dependencyGraph.vertexSet().forEach(vertex -> {
                dependencyGraph.elementsName.put(vertex.trim(), vertex);
            });

            List<String> startActivities = new ArrayList<>(dependencyGraph.startActivities);
            List<String> endActivities = new ArrayList<>(dependencyGraph.endActivities);
            dependencyGraph.startActivities.clear();
            dependencyGraph.endActivities.clear();
            for (String str : startActivities) {
                String startEvent = "start_" + str;
                dependencyGraph.addVertex(startEvent);
                dependencyGraph.addEdge(startEvent, str);
                dependencyGraph.elementInformations.put(startEvent, new HashMap<String, String>() {{
                    put("type", "start");
                }});
                dependencyGraph.elementsName.put(startEvent.trim(), startEvent);
                dependencyGraph.startActivities.add(startEvent);
            }
            String endEvent = "end";
            dependencyGraph.addVertex(endEvent);
            dependencyGraph.elementInformations.put(endEvent, new HashMap<String, String>() {{
                put("type", "end");
            }});
            dependencyGraph.elementsName.put(endEvent.trim(), endEvent);
            dependencyGraph.endActivities.add(endEvent);

            for (String str : endActivities) {
                dependencyGraph.addEdge(str, endEvent);
                dependencyGraph.endActivities.remove(str);
            }

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
                        System.out.println(loop2score);

                        if (loop2score != 0) {
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

            //get parallelism
            System.out.println("Parallelism: ");
            startTime_1 = System.nanoTime();

            HashSet<DefaultWeightedEdge> removableEdge = new HashSet<>();

            dependencyGraph.dependencyGraph.edgeSet().forEach(edge -> {
                String source = dependencyGraph.dependencyGraph.getEdgeSource(edge);
                String target = dependencyGraph.dependencyGraph.getEdgeTarget(edge);

                boolean priorityCheck = !dependencyGraph.loops.contains(new ArrayList<String>() {{
                    add(source);
                    add(target);
                }}) || true;

                if (dependencyGraph.dependencyGraph.containsEdge(target, source) && !removableEdge.contains(edge) && priorityCheck && target != source) {
                    DefaultWeightedEdge edge2 = dependencyGraph.dependencyGraph.getEdge(target, source);
                    double src2tgt_frequency = dependencyGraph.dependencyGraph.getEdgeWeight(edge);
                    double tgt2src_frequency = dependencyGraph.dependencyGraph.getEdgeWeight(edge2);
                    double parallelismScore = (double) (src2tgt_frequency - tgt2src_frequency) / (src2tgt_frequency + tgt2src_frequency);
                    if (Math.abs(parallelismScore) < epsilom) {
                        removableEdge.add(edge);
                        removableEdge.add(edge2);
                        dependencyGraph.parallelism.add(new HashSet<String>() {{
                            add(source);
                            add(target);
                        }});
                    } else {
                        if (parallelismScore > 0) removableEdge.add(edge2);
                        else removableEdge.add(edge);
                    }
                }

            });
            dependencyGraph.dependencyGraph.removeAllEdges(removableEdge);
            dependencyGraph.dependencyGraphWithLoop.removeAllEdges(removableEdge);

            System.out.println(dependencyGraph.parallelism);
            dependencyGraph.removeParallelismDiscovery();
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


            //continue loop detection
            System.out.println("Loop2: ");
            startTime_1 = System.nanoTime();
            System.out.println(dependencyGraph.loops);
//            dependencyGraph.getLongLoopDiscovery();
            dependencyGraph.findAndRemoveLoops();
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
}
