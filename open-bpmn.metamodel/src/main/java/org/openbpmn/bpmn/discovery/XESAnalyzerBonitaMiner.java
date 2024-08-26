package org.openbpmn.bpmn.discovery;

import org.apache.commons.lang3.StringUtils;
import org.deckfour.xes.in.XesXmlParser;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.DirectedWeightedPseudograph;
import org.openbpmn.bpmn.discovery.model.DependencyGraph;
import org.openbpmn.bpmn.discovery.model.LoopMerger;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class XESAnalyzerBonitaMiner {

    public static void main(String[] args) {
        try {
            // Start timing
            String fileName = "M2";
            String pathLog = "C:\\Users\\AliNourEldin\\Desktop\\da-bpmn\\generated-BPMN\\event_logs\\" + fileName + ".xes";
            String outputpath = "C:\\Users\\AliNourEldin\\Desktop\\da-bpmn\\generated-BPMN\\our\\" + fileName + ".bpmn";
            Double epsilom = 1.0;
            Double frequencyNoiseRemove = 0.0;
            Double frequency = 0.0;
            long startTime = System.nanoTime();

            XLog log = new XESAnalyzerBonitaMiner().readLog(pathLog);

            Map<String, Integer> traces = new HashMap<>();
            Map<String, List<String>> tracesList = new HashMap<>();

            DependencyGraph dependencyGraph = generateDependencyGraph(log, traces, tracesList);

            double maxWeight = getMaxEdgeWeight(dependencyGraph.dependencyGraph);


            //loop detection
            loopDetection(dependencyGraph, traces);

            //get parallelism
            DependencyGraph dependencyGraphNew = generateDependencyGraph(log, traces, tracesList);

            dependencyGraphNew.loops.addAll(dependencyGraph.loops);
            parallelDetection(dependencyGraphNew, epsilom);
            dependencyGraph.parallelism.addAll(dependencyGraphNew.parallelism);


            // add loops to dependency graph with loop
            dependencyGraph.loops.forEach(loop -> {
                dependencyGraph.dependencyGraphWithLoop.addEdge(loop.get(0), loop.get(1));
            });
            DependencyGraph dependencyGraphNew2 = generateDependencyGraph(log, traces, tracesList);

            //should check if there is activities not connected
            addDisconnectedActivities(dependencyGraph, dependencyGraphNew2);

            //extra loop
            extraLoop(dependencyGraph, dependencyGraphNew2);

            // extra parallelism
            extraParallelism(dependencyGraph, dependencyGraphNew2);


//            long startTime_1;
            //get inclusive
//            System.out.println("Inclusive: ");
//            startTime_1 = System.nanoTime();
            dependencyGraph.findInclusive();
//            System.out.println(dependencyGraph.inclusive);
//            printOutTime(startTime_1);
//            System.out.println("END Inclusive");

            //get exclusive
//            System.out.println("Decision: ");
//            startTime_1 = System.nanoTime();
            dependencyGraph.findExculisve();
//            System.out.println(dependencyGraph.exlusive);
//            DecisionMerger decisionMerger = new DecisionMerger(dependencyGraph.exlusive, dependencyGraph.dependencyGraphWithLoop);
//            LinkedList<LinkedList<String>> decisionRelations = decisionMerger.getDecisions();
//            System.out.println("Decision Relations: ");
//            System.out.println(decisionRelations);
//            printOutTime(startTime_1);
//            System.out.println("End Decision");
//            System.out.println(dependencyGraph.dependencyGraphWithLoop.toString());
//            System.out.println(dependencyGraph.dependencyGraph.toString());

//            get sequence
            BPMNDiscovery bpmnDiscovery = new BPMNDiscovery(dependencyGraph);
            bpmnDiscovery.DependencyGraphToBPMN();
            bpmnDiscovery.saveMode(outputpath);
            printOutTime(startTime);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void addDisconnectedActivities(DependencyGraph dependencyGraph, DependencyGraph dependencyGraphNew2) {
        dependencyGraph.dependencyGraphWithLoop.vertexSet().forEach(vertex -> {
            if(dependencyGraph.startActivities.contains(vertex) || dependencyGraph.endActivities.contains(vertex)){
                return;
            }

            if (dependencyGraph.dependencyGraphWithLoop.incomingEdgesOf(vertex).size() == 0) {
                //get the max weth comming to this source
                final DefaultWeightedEdge[] e = {null};
                final double[] max = {0};
                dependencyGraphNew2.dependencyGraph.incomingEdgesOf(vertex).forEach(edge -> {
                    double weight = dependencyGraphNew2.dependencyGraph.getEdgeWeight(edge);
                    if (weight> max[0]) {
                        e[0] = edge;
                        max[0] = weight;
                    }
                });
                if(e[0] == null){
                    return;
                }
                String source = dependencyGraphNew2.dependencyGraph.getEdgeSource(e[0]);
                DefaultWeightedEdge edge =  dependencyGraph.dependencyGraphWithLoop.addEdge(source, vertex);
                dependencyGraph.dependencyGraphWithLoop.setEdgeWeight(edge, max[0]);
                DefaultWeightedEdge edge1 =  dependencyGraph.dependencyGraph.addEdge(source, vertex);
                dependencyGraph.dependencyGraph.setEdgeWeight(edge1, max[0]);

//                    dependencyGraph.dependencyGraphWithLoop.addEdge("start", vertex);
            } else if (dependencyGraph.dependencyGraphWithLoop.outgoingEdgesOf(vertex).size() == 0) {
//                    dependencyGraph.dependencyGraphWithLoop.addEdge(vertex, "end");
                final DefaultWeightedEdge[] e = {null};
                final double[] max = {-1};
                //get max weigth edge
                dependencyGraphNew2.dependencyGraph.outgoingEdgesOf(vertex).forEach(edge -> {
                    double weight = dependencyGraphNew2.dependencyGraph.getEdgeWeight(edge);
                    System.out.println(weight);
                    if (weight> max[0]) {
                        e[0] = edge;
                        max[0] = weight;
                    }
                });

                if(e[0] == null){
                    return;
                }
                String target = dependencyGraphNew2.dependencyGraph.getEdgeTarget(e[0]);
                DefaultWeightedEdge edge =  dependencyGraph.dependencyGraphWithLoop.addEdge(vertex,target);
                dependencyGraph.dependencyGraphWithLoop.setEdgeWeight(edge, max[0]);
                DefaultWeightedEdge edge1 =  dependencyGraph.dependencyGraph.addEdge(vertex,target);
                dependencyGraph.dependencyGraph.setEdgeWeight(edge1, max[0]);
            }
        });
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

        //get names
        graph.dependencyGraph.vertexSet().forEach(vertex -> {
            graph.elementsName.put(vertex.trim(), vertex);
        });

        List<String> startActivities = new ArrayList<>(graph.startActivities);
        List<String> endActivities = new ArrayList<>(graph.endActivities);
        graph.startActivities.clear();
        graph.endActivities.clear();
        String startEvent = "start";
        graph.addVertex(startEvent);
        graph.elementInformations.put(startEvent, new HashMap<String, String>() {{
            put("type", "start");
        }});
        graph.elementsName.put(startEvent.trim(), startEvent);

        for (String str : startActivities) {
            graph.addEdge(startEvent, str);
        }

        startActivities.clear();
        startActivities.add(startEvent);
        graph.startActivities.add(startEvent);


        String endEvent = "end";
        graph.addVertex(endEvent);
        graph.elementInformations.put(endEvent, new HashMap<String, String>() {{
            put("type", "end");
        }});
        graph.elementsName.put(endEvent.trim(), endEvent);

        for (String str : endActivities) {
            graph.addEdge(str, endEvent);
//                dependencyGraph.endActivities.remove(str);
        }
        endActivities.clear();
        endActivities.add(endEvent);
        graph.endActivities.add(endEvent);
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


    public static void parallelDetection(DependencyGraph dependencyGraph, Double epsilom) {
        //remove looping edge (we dont remove a->, b->a)
        Set<List<String>> edgesToRemove = new HashSet<>();
        dependencyGraph.loops.forEach(loop -> {
            String source = loop.get(0);
            String target = loop.get(1);

            if (!(dependencyGraph.loops.stream().anyMatch(list -> list.contains(target) && list.contains(source))
                    && dependencyGraph.loops.stream().anyMatch(list -> list.contains(source) && list.contains(target))) || source.contentEquals(target)) {
                edgesToRemove.add(new ArrayList<>() {{
                    add(source);
                    add(target);
                }});
            }
        });

        edgesToRemove.forEach(edge -> {
            Iterator<String> parallel = edge.iterator();
            String source = parallel.next();
            String target = parallel.next();
            dependencyGraph.dependencyGraph.removeEdge(source, target);
            dependencyGraph.dependencyGraph.removeEdge(target, source);
            dependencyGraph.dependencyGraphWithLoop.removeEdge(source, target);
            dependencyGraph.dependencyGraphWithLoop.removeEdge(target, source);
        });

        dependencyGraph.dependencyGraph.edgeSet().forEach(edge -> {
            String source = dependencyGraph.dependencyGraph.getEdgeSource(edge);
            String target = dependencyGraph.dependencyGraph.getEdgeTarget(edge);

            Set<String> parallel = new HashSet<>();
            parallel.add(source);
            parallel.add(target);

            if (!source.equals(target)) {
                if (!dependencyGraph.parallelism.contains(parallel) && dependencyGraph.dependencyGraph.containsEdge(target, source)) {
                    Set<String> p = new HashSet<>();
                    p.add(target);
                    p.add(source);
                    dependencyGraph.parallelism.add(p);
                }
            }
        });
        System.out.println(dependencyGraph.parallelism);
        Set<Set<String>> tempParallelism = new HashSet<>(dependencyGraph.parallelism);
        dependencyGraph.parallelism.clear();
        tempParallelism.stream().forEach(parallel -> {
            Iterator<String> it = parallel.iterator();
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
        dependencyGraph.removeParallelism();
        dependencyGraph.dependencyGraph.vertexSet().forEach(vertex -> {
            if (dependencyGraph.dependencyGraph.incomingEdgesOf(vertex).size() == 0) {

            } else if (dependencyGraph.dependencyGraph.outgoingEdgesOf(vertex).size() == 0) {

            }
            ;
        });
        dependencyGraph.filerParallelism();
    }


    public static void loopDetection(DependencyGraph dependencyGraph, Map<String, Integer> traces) {
        //self loop detection
        Set<List<String>> selfLoops = new HashSet<>();
        dependencyGraph.dependencyGraph.edgeSet().forEach(edge -> {
            String source = dependencyGraph.dependencyGraph.getEdgeSource(edge);
            String target = dependencyGraph.dependencyGraph.getEdgeTarget(edge);
            List<String> loop = new ArrayList<>();
            loop.add(source);
            loop.add(target);
            if (source.equals(target)) {
                selfLoops.add(loop);
            }
        });


        //remove self loop
        selfLoops.forEach(loop -> {
            dependencyGraph.dependencyGraphWithLoop.removeEdge(loop.get(0), loop.get(1));
            dependencyGraph.dependencyGraph.addEdge(loop.get(0), loop.get(1));
        });

        //detect short loop
        Set<List<String>> shortLoops = new HashSet<>();

        dependencyGraph.dependencyGraph.edgeSet().forEach(edge -> {
            String source = dependencyGraph.dependencyGraph.getEdgeSource(edge);
            String target = dependencyGraph.dependencyGraph.getEdgeTarget(edge);

            List<String> loop = new ArrayList<>();
            loop.add(source);
            loop.add(target);

            if (!shortLoops.contains(loop) && dependencyGraph.dependencyGraph.containsEdge(target, source) &&
                    selfLoops.stream().noneMatch(list -> list.contains(source) || list.contains(target))) {
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
                    List<String> loop2 = new ArrayList<>();
                    loop2.add(target);
                    loop2.add(source);

                    shortLoops.add(loop);
                    shortLoops.add(loop2);
                }
            }


        });


        //check if any self loop has a loop based on the sources of all the self loop
        selfLoops.forEach(loop -> {
            selfLoops.forEach(loop2 -> {
                if (loop != loop2) {
                    String source = loop.get(0);
                    String target = loop2.get(0);
                    if (dependencyGraph.dependencyGraph.containsEdge(source, target)
                            && dependencyGraph.dependencyGraph.containsEdge(target, source)
                            && !shortLoops.contains(new ArrayList<>(Arrays.asList(source, target)))
                    ) {
                        List<String> loop3 = new ArrayList<>();
                        loop3.add(source);
                        loop3.add(target);
                        shortLoops.add(loop3);

                        loop3.clear();
                        loop3.add(target);
                        loop3.add(source);
                        shortLoops.add(loop3);
                    }
                }
            });
        });

        //remove all a->b, b->a from the graph, and add all the loops to it
        Set<DefaultWeightedEdge> edgesToRemove = new HashSet<>();
        dependencyGraph.dependencyGraph.edgeSet().forEach(edge -> {
            String source = dependencyGraph.dependencyGraph.getEdgeSource(edge);
            String target = dependencyGraph.dependencyGraph.getEdgeTarget(edge);
            if (dependencyGraph.dependencyGraph.containsEdge(target, source) &&
                    !(shortLoops.contains(new ArrayList<>(Arrays.asList(source, target)))
                            || shortLoops.contains(new ArrayList<>(Arrays.asList(target, source))))) {
                edgesToRemove.add(edge);
            }
        });
        edgesToRemove.forEach(edge -> {
            String source = dependencyGraph.dependencyGraph.getEdgeSource(edge);
            String target = dependencyGraph.dependencyGraph.getEdgeTarget(edge);
            dependencyGraph.dependencyGraph.removeEdge(source, target);
            dependencyGraph.dependencyGraphWithLoop.removeEdge(source, target);
        });

        //detect looping edge
        dependencyGraph.findAndRemoveLoops2(traces);
        dependencyGraph.loops.addAll(selfLoops);
    }

    public static List<Integer> getAllIndexes(String str, String subStr) {
        List<Integer> indexes = new ArrayList<>();
        int index = str.indexOf(subStr);
        while (index >= 0) {
            indexes.add(index);
            index = str.indexOf(subStr, index + 1);
        }
        return indexes;
    }

    public static void extraLoop(DependencyGraph dependencyGraph, DependencyGraph dependencyGraphNew2) {
        Set<List<String>> toAddToLoop = new HashSet<>();
        dependencyGraph.loops.forEach(edge1 -> {
            dependencyGraph.loops.forEach(edge2 -> {
                if (edge1 != edge2 && !edge1.get(0).equals(edge1.get(1)) && !edge2.get(0).equals(edge2.get(1))) {
                    if (dependencyGraph.parallelism.stream().anyMatch(parallel -> parallel.contains(edge1.get(1)) && parallel.contains(edge2.get(1)))) {

                        if (dependencyGraphNew2.dependencyGraph.containsEdge(edge1.get(1), edge2.get(0)) &&
                                dependencyGraphNew2.dependencyGraph.containsEdge(edge2.get(1), edge1.get(0))) {
                            List<String> loop = new ArrayList<>();
                            loop.add(edge2.get(0));
                            loop.add(edge1.get(1));
                            toAddToLoop.add(loop);
                            dependencyGraph.dependencyGraphWithLoop.addEdge(loop.get(0), loop.get(1));
                        }
                    }
                }
            });
        });
        dependencyGraph.loops.addAll(toAddToLoop);
        dependencyGraph.loops.forEach(loop -> {
            dependencyGraph.dependencyGraphWithLoop.addEdge(loop.get(0), loop.get(1));
        });
    }

    public static void extraParallelism(DependencyGraph dependencyGraph, DependencyGraph dependencyGraphNew2) {

        LoopMerger loopMerger = new LoopMerger(dependencyGraph.loops, dependencyGraph.dependencyGraphWithLoop);
        //step 1 to get the parallelism from the loops
        //step 2 to check if all the elements in the parallelism are parallel (based on the frequency)

        //step 1
        loopMerger.getMergedLoop().forEach(loop -> {
            Set<Set<String>> allCombination = new HashSet<>();
            loop.getTarget().forEach(element -> {
                loop.getTarget().forEach(element2 -> {
                    Set<String> combination = new HashSet<>();
                    if (!element.equals(element2)) {
                        combination.add(element);
                        combination.add(element2);
                        allCombination.add(combination);
                    }
                });
            });
            //check if the loop is parallel
            allCombination.forEach(para -> {
                if (!dependencyGraph.parallelism.contains(para)) {
                    Iterator<String> it = para.iterator();
                    String source = it.next();
                    String target = it.next();
                    if (dependencyGraphNew2.dependencyGraph.containsEdge(source, target) && dependencyGraphNew2.dependencyGraph.containsEdge(target, source)) {
                        dependencyGraph.parallelism.add(para);
                    }
                }
            });
        });

        // step 2
        Set<Set<String>> paraToDelete = new HashSet<>();
        dependencyGraph.parallelism.forEach(para -> {
            Iterator<String> it = para.iterator();
            String source = it.next();
            String target = it.next();

            Set<String> sourceConnection = dependencyGraph.parallelism.stream().filter(parallel -> parallel.contains(source)).map(s -> {
                Iterator<String> it1 = s.iterator();
                String k = it1.next();
                if (k.contentEquals(source)) {
                    k = it1.next();
                }
                return k;
            }).collect(Collectors.toSet());

            Set<String> targetConnection = dependencyGraph.parallelism.stream().filter(parallel -> parallel.contains(target)).map(s -> {
                Iterator<String> it1 = s.iterator();
                String k = it1.next();
                if (k.contentEquals(target)) {
                    k = it1.next();
                }
                return k;
            }).collect(Collectors.toSet());

            double sumOfFrequencyOfSourceConnections = sourceConnection.stream().mapToDouble(s -> dependencyGraph.vertexWeights.get(s)).sum();
            double sumOfFrequencyOfTargetConnections = targetConnection.stream().mapToDouble(s -> dependencyGraph.vertexWeights.get(s)).sum();
//            System.out.println(source + " " + sourceConnection);
//            System.out.println(dependencyGraph.vertexWeights.get(source));
//            System.out.println(sumOfFrequencyOfSourceConnections);
//
//            System.out.println(target + " " + targetConnection);
//            System.out.println(dependencyGraph.vertexWeights.get(target));
//            System.out.println(sumOfFrequencyOfTargetConnections);

            if (!(
                    dependencyGraph.vertexWeights.get(source).doubleValue() == dependencyGraph.vertexWeights.get(target).doubleValue())
                    && !((sourceConnection.size() != 1 ||
                    targetConnection.size() != 1) &&
                    (sumOfFrequencyOfSourceConnections == dependencyGraph.vertexWeights.get(target)
                            || sumOfFrequencyOfTargetConnections == dependencyGraph.vertexWeights.get(source)))
            ) {
                paraToDelete.add(para);
            }
        });
        System.out.println(paraToDelete);
        dependencyGraph.parallelism.removeAll(paraToDelete);
    }
}
