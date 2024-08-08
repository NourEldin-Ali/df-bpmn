package org.openbpmn.bpmn.discovery.model;

import java.awt.*;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.Graphs;
import org.jgrapht.alg.cycle.JohnsonSimpleCycles;
import org.jgrapht.alg.cycle.SzwarcfiterLauerSimpleCycles;
import org.jgrapht.alg.cycle.TarjanSimpleCycles;
import org.jgrapht.alg.cycle.TiernanSimpleCycles;
import org.jgrapht.alg.lca.NaiveLCAFinder;
import org.jgrapht.alg.shortestpath.AllDirectedPaths;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.DirectedWeightedPseudograph;

public class DependencyGraph {
    public DirectedWeightedPseudograph<String, DefaultWeightedEdge> dependencyGraph;
    public DirectedWeightedPseudograph<String, DefaultWeightedEdge> dependencyGraphWithLoop;
    public List<String> startActivities;
    public List<String> endActivities;
    public Map<String, Map<String, String>> elementInformations;
    public Map<String, String> elementsName;
    public Set<List<String>> loops;
    public Set<Set<String>> parallelism;
    public Set<Set<String>> exlusive;
    public Set<Set<String>> inclusive;

    public Map<String, Integer> vertexWeights;

    public DependencyGraph() {
        dependencyGraph = new DirectedWeightedPseudograph<>(DefaultWeightedEdge.class);
        dependencyGraphWithLoop = new DirectedWeightedPseudograph<>(DefaultWeightedEdge.class);
        startActivities = new ArrayList<>();
        endActivities = new ArrayList<>();
        loops = new HashSet<>();
        parallelism = new HashSet<>();
        inclusive = new HashSet<>();
        exlusive = new HashSet<>();
        vertexWeights = new HashMap<>();
        elementInformations = new HashMap<>();
        elementsName = new HashMap<>();
    }

    /**
     * used during the extraction of traces from the event log
     *
     * @param v1
     */
    public void addVertex(String v1) {
        if (!dependencyGraph.vertexSet().contains(v1)) {
            dependencyGraph.addVertex(v1);
            dependencyGraphWithLoop.addVertex(v1);
            vertexWeights.put(v1, 1);
        } else {
            vertexWeights.put(v1, vertexWeights.get(v1) + 1);
        }
    }

    /**
     * used during the extraction of traces from the event log
     *
     * @param v1
     * @param v2
     */
    public void addEdge(String v1, String v2) {
        DefaultWeightedEdge edge = dependencyGraph.getEdge(v1, v2);
        if (edge == null) {
            // Edge does not exist, add it with initial weight 1
            edge = dependencyGraph.addEdge(v1, v2);
            dependencyGraph.setEdgeWeight(edge, 1);
            DefaultWeightedEdge edge1 = dependencyGraphWithLoop.addEdge(v1, v2);
            dependencyGraphWithLoop.setEdgeWeight(edge1, 1);
        } else {
            // Edge exists, increment its weight
            dependencyGraph.setEdgeWeight(edge, dependencyGraph.getEdgeWeight(edge) + 1);
            DefaultWeightedEdge edge1 = dependencyGraphWithLoop.getEdge(v1, v2);
            dependencyGraphWithLoop.setEdgeWeight(edge1, dependencyGraphWithLoop.getEdgeWeight(edge1) + 1);
        }
    }

    public void findParallelism() {

    }

    //this function should be used after extract the parallelism
    public void findInclusive() {

    }

    //this function should be used after extract the parallelism and the inclusive and after removing the loops
    public void findExculisve() {
        // get pair decisions
        Set<Set<String>> pairDecisionPoints = new HashSet<>();
        for (String activity : dependencyGraph.vertexSet()) {
            Set<DefaultWeightedEdge> outgoingEdgeSource = dependencyGraph.outgoingEdgesOf(activity);
            List<String> targetActivities = new ArrayList();
            outgoingEdgeSource.stream().forEach(edge -> targetActivities.add(dependencyGraph.getEdgeTarget(edge)));
            if (targetActivities.size() > 1) {
                for (int i = 0; i < targetActivities.size() - 1; i++) {
                    for (int j = i + 1; j < targetActivities.size(); j++) {
                        Set<String> compatible = new HashSet<>();
                        String element1 = targetActivities.get(i);
                        String element2 = targetActivities.get(j);

                        if (parallelism.stream()
                                .noneMatch(sublist -> sublist.contains(element1) && sublist.contains(element2)) &&
                                inclusive.stream()
                                        .noneMatch(sublist -> sublist.contains(element1) && sublist.contains(element2))) {
                            compatible.add(element1);
                            compatible.add(element2);
                            pairDecisionPoints.add(compatible);
                        }
                    }
                }
            }
        }
        exlusive = pairDecisionPoints;
    }

    /**
     * This function is used when we are sure that the dependency graph does not
     * contain parallelism, and only contains loop Used to take input from LLM
     * <p>
     * To use this function we should remove all the parallelism and the inclusive from the dependency graph
     */
    public void findAndRemoveLoops() {
        if (dependencyGraph != null) {
            AllDirectedPaths<String, DefaultWeightedEdge> allPaths = new AllDirectedPaths<>(dependencyGraphWithLoop);
            for (String startNode : startActivities) {
                for (String endNode : endActivities) {
                    List<GraphPath<String, DefaultWeightedEdge>> paths = allPaths.getAllPaths(startNode, endNode, true, null);
                    for (GraphPath<String, DefaultWeightedEdge> grapthpath : paths) {
                        List<String> node = grapthpath.getVertexList();
                        for (int i = 0; i < node.size(); i++) {
                            for (int j = i; j < node.size(); j++) {
                                if (dependencyGraphWithLoop.containsEdge(node.get(j), node.get(i))) {
                                    loops.add(new ArrayList<>(Arrays.asList(node.get(j), node.get(i))));
                                }
                            }
                        }

                    }
                }
            }

            // remove loops from dependency graph
            dependencyGraph = (DirectedWeightedPseudograph<String, DefaultWeightedEdge>) dependencyGraphWithLoop.clone();
            loops.stream().forEach(edge -> {
                dependencyGraph.removeEdge(edge.get(0), edge.get(1));
            });


//			List<List<String>> tempLoops = new ArrayList<>();
            DirectedWeightedPseudograph<String, DefaultWeightedEdge> tempGraph = new DirectedWeightedPseudograph<String, DefaultWeightedEdge>(
                    DefaultWeightedEdge.class);
            Set<DefaultWeightedEdge> edges = dependencyGraph.edgeSet();
            allPaths = new AllDirectedPaths<>(tempGraph);

            for (DefaultWeightedEdge edge : edges) {
                String source = dependencyGraph.getEdgeSource(edge);
                String target = dependencyGraph.getEdgeTarget(edge);

                if (tempGraph.containsVertex(source) && tempGraph.containsVertex(target)) {

                    List<GraphPath<String, DefaultWeightedEdge>> paths = allPaths.getAllPaths(target, source, true,
                            null);

                    if (!paths.isEmpty() || source.contentEquals(target)
                            || dependencyGraph.getEdge(target, source) != null) {
                        loops.add(new ArrayList<>(Arrays.asList(source, target)));
                    } else {
                        tempGraph.addVertex(source);
                        tempGraph.addVertex(target);
                        tempGraph.addEdge(source, target);
                    }
                } else {

                    tempGraph.addVertex(source);
                    tempGraph.addVertex(target);
                    tempGraph.addEdge(source, target);

                }
            }
            // remove loops from dependency graph
            loops.stream().forEach(edge -> {
                dependencyGraph.removeEdge(edge.get(0), edge.get(1));
            });
        }


    }

    public void findLoopsAndParrallelism() {
        Set<List<String>> tempLoop = new HashSet();
        if (dependencyGraphWithLoop != null) {
            // TODO: error in loop detections
            // self-loop, loops, parallelism detections
            TiernanSimpleCycles<String, DefaultWeightedEdge> cycleDetector = new TiernanSimpleCycles<>(
                    dependencyGraphWithLoop);
            boolean hasCycle = cycleDetector.findSimpleCycles().size() > 0;
            if (hasCycle) {
                List<List<String>> cyclesList = cycleDetector.findSimpleCycles();
                for (List<String> cycle : cyclesList) {
                    // in case of self-loop or loop
                    if (cycle.size() == 1) {
                        loops.add(new ArrayList(Arrays.asList(cycle.get(0), cycle.get(0))));

                    } else if (cycle.size() > 2) {
                        String element1 = cycle.get(0);
                        String element2 = cycle.get(cycle.size() - 1);
                        loops.add(new ArrayList(Arrays.asList(element1, element2)));
                    } else {
                        String element1 = cycle.get(0);
                        String element2 = cycle.get(cycle.size() - 1);
                        tempLoop.add(new ArrayList(Arrays.asList(element1, element2)));
                    }
                }
            }
        }
        // remove loops from dependency graph
        dependencyGraph = (DirectedWeightedPseudograph<String, DefaultWeightedEdge>) dependencyGraphWithLoop.clone();

        loops.stream().forEach(edge -> {
            dependencyGraph.removeEdge(edge.get(0), edge.get(1));

        });

        for (List<String> cycle : tempLoop) {
            if (cycle.size() == 2) {
                String element1 = cycle.get(0);
                String element2 = cycle.get(1);
                if (element1.contentEquals(element2)) {
                    continue;
                }

                // get source of element1
                Set<DefaultWeightedEdge> incomingEdgeElement1 = dependencyGraph.incomingEdgesOf(element1);
                Set<String> sourceElement1 = new HashSet<>();
                incomingEdgeElement1.stream().forEach(edge -> sourceElement1.add(dependencyGraph.getEdgeSource(edge)));
                // get source of element2
                Set<DefaultWeightedEdge> incomingEdgeElement2 = dependencyGraph.incomingEdgesOf(element2);
                Set<String> sourceElement2 = new HashSet<>();
                incomingEdgeElement2.stream().forEach(edge -> sourceElement2.add(dependencyGraph.getEdgeSource(edge)));

                if (sourceElement2.contains(element1) && sourceElement1.contains(element2)) {
                    parallelism.add(new HashSet<String>(Arrays.asList(element2, element1)));
                }
            }
        }

        parallelism.stream().forEach(edge -> {
            Iterator<String> edges = edge.iterator();
            String element1 = edges.next();
            String element2 = edges.next();
            dependencyGraph.removeEdge(element2, element1);
            dependencyGraph.removeEdge(element1, element2);

        });

        // remove parallel from temp loop
        Set<List<String>> resultSet = new HashSet<>();
        for (List<String> list : tempLoop) {
            Set<String> tempSet = new HashSet<>(list);
            if (!parallelism.contains(tempSet)) {
                resultSet.add(list);
            }
        }

        loops.addAll(resultSet);
        loops.stream().forEach(edge -> {
            dependencyGraph.removeEdge(edge.get(0), edge.get(1));
        });
        System.out.println(parallelism);
    }

    public void findLoops() {

    }

    //	public LinkedList<Pair<List<String>, List<String>>> getLoops() {
//		Map<String, List<List<String>>> mergedLoop = mergeLoop();
//		LinkedList<Pair<List<String>, List<String>>> pairLoops = new LinkedList<>();
//
//		for (Entry<String, List<List<String>>> entryKeys : mergedLoop.entrySet()) {
//			String Key = entryKeys.getKey();
//			for (List<String> lst : entryKeys.getValue()) {
//				pairLoops.add(new Pair<List<String>, List<String>>(Arrays.asList(Key), lst));
//			}
//		}
//
//		// Map to hold merged sources based on unique targets
//		Map<List<String>, List<String>> map = new HashMap<>();
//
//		// Iterate over the LinkedList
//		for (Pair<List<String>, List<String>> pair : pairLoops) {
//			List<String> source = pair.getSource();
//			List<String> target = pair.getTarget();
//
//			// Merge sources if target already exists
//			if (map.containsKey(target)) {
//				map.get(target).addAll(source); // Merge the source lists
//			} else {
//				map.put(target, new ArrayList<>(source)); // Add new entry if target is not found
//			}
//		}
//
//		// Rebuild the LinkedList from the map
//		LinkedList<Pair<List<String>, List<String>>> mergedList = new LinkedList<>();
//		for (Map.Entry<List<String>, List<String>> entry : map.entrySet()) {
//			mergedList.add(new Pair<>(entry.getKey(), entry.getValue()));
//		}
//
//		// Output the merged list
////		for (Pair<List<String>, List<String>> pair : mergedList) {
////			System.out.println("Source: " + pair.getSource() + ", Target: " + pair.getTarget());
////		}
//		sortLoop(mergedList);
//		return mergedList;
//	}
    public LinkedList<Pair<Set<String>, Set<String>>> getLoops() {
        LinkedList<Pair<Set<String>, Set<String>>> mergedList = new LinkedList<>();
        sortLoop(mergedList);
        return mergedList;
    }

//	public Set<>

    public void sortLoop(LinkedList<Pair<Set<String>, Set<String>>> loop) {
        Collections.sort(loop, new Comparator<Pair<Set<String>, Set<String>>>() {
            @Override
            public int compare(Pair<Set<String>, Set<String>> p1, Pair<Set<String>, Set<String>> p2) {
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
    }

    public Map<String, List<List<String>>> mergeLoop() {
        // Get the loop relations from the dependency graph
        Set<List<String>> loopRelations = loops;
        // Get all targets of loop relations
        Map<String, List<String>> sortedByTarget = new HashMap<>();
        for (List<String> pair : loopRelations) {
            String element2 = pair.get(0);
            sortedByTarget.computeIfAbsent(element2, k -> new ArrayList<>()).add(pair.get(1));
        }
        // Merge loop relations
        Map<String, List<List<String>>> mergeByTarget = new HashMap<>();

        for (Entry<String, List<String>> entry : sortedByTarget.entrySet()) {
            String target = entry.getKey();
            List<String> loopList = entry.getValue();
            List<List<String>> resultList = new ArrayList<>();
            List<String> visited = new ArrayList<>();
            for (int i = 0; i < loopList.size(); i++) {
                String element1 = loopList.get(i);
                if (visited.contains(element1)) {
                    continue;
                }
                visited.add(element1);
                // Self-loop
                if (element1.equals(target)) {
                    List<String> mergeElements = new ArrayList<>();
                    mergeElements.add(element1);
                    resultList.add(mergeElements);
                    continue;
                } else {

                    List<String> mergeElements = new ArrayList<>();
                    mergeElements.add(element1);

                    for (int j = 0; j < loopList.size(); j++) {
//						
                        String element2 = loopList.get(j);
                        if (visited.contains(element2)) {
                            continue;
                        }
                        // Check if the elements have the same successors
                        Set<DefaultWeightedEdge> outgoingEdgeSource = dependencyGraph.outgoingEdgesOf(element1);
                        List<String> l1 = new ArrayList<String>();
                        outgoingEdgeSource.stream().forEach(edge -> l1.add(dependencyGraph.getEdgeTarget(edge)));

                        outgoingEdgeSource = dependencyGraph.outgoingEdgesOf(element2);
                        List<String> l2 = new ArrayList<String>();
                        outgoingEdgeSource.stream().forEach(edge -> l2.add(dependencyGraph.getEdgeTarget(edge)));

                        if (l1.equals(l2)) {
                            mergeElements.add(element2);
                            visited.add(loopList.get(j));
                        }
                    }

                    // Remove merged list from initial loop relation
//					loopList.removeAll(listToRemove);
                    resultList.add(mergeElements);
                }
            }
            mergeByTarget.put(target, resultList);
        }
        return mergeByTarget;
    }


    public String getLCA(String element1, String element2) {
        NaiveLCAFinder<String, DefaultWeightedEdge> lcaFinder = new NaiveLCAFinder<>(this.dependencyGraph);
        return lcaFinder.getLCA(element1, element2);
    }

    public static void regex(List<String> list) {
        list.replaceAll(s -> regex(s));
    }

    public static void regex(Set<String> set) {
        Set<String> updatedSet = new HashSet<>();
        for (String s : set) {
            updatedSet.add(regex(s)); // Assume regex(s) is a method that processes the string `s`
        }
        set.clear();
        set.addAll(updatedSet);
    }

    public static String regex(String str) {
        return str.trim().replaceAll("[^A-Za-z0-9]", "-").toLowerCase();
    }

//	/**
//	 * Get all dependencies including loops
//	 *
//	 * @return
//	 */
//	public List<String> getFullDependenciesDFA() {
//		LinkedList<String> dependencyRelation = new LinkedList<>();
//		dependencyGraphWithLoop.edgeSet().forEach(edge -> {
//			String source = dependencyGraphWithLoop.getEdgeSource(edge).trim();
//			String target = dependencyGraphWithLoop.getEdgeTarget(edge).trim();
//			dependencyRelation.add(source + "->" + target);
//		});
//
//		return DepthFirstSearch.DFSToList(dependencyRelation, startActivities.get(0).trim());
//
//	}
//
//	public void vertixLevel() {
//		// Step 2: Apply Bellman-Ford
////        BellmanFordShortestPath<String, DefaultWeightedEdge> bellmanFord = new BellmanFordShortestPath<>(dependencyGraphWithLoop);
////        String sourceVertex = startActivities.get(0);
////
////        // Print shortest path "levels" from source vertex
////        dependencyGraphWithLoop.vertexSet().forEach(vertex -> {
////            double pathCost = bellmanFord.getPathWeight(sourceVertex,vertex);
////            System.out.println("Vertex: " + vertex + ", Level (Shortest Path Cost from '" + sourceVertex + "'): " + pathCost);
////        });
//
//		List<String> requiredVertices = List.of("h", "k", "c");
//
//		// Create a BFS iterator from vertex1
//
//		Set<GraphPath<String, DefaultWeightedEdge>> selectedPath = new HashSet();
//		// Find all paths from start to end vertex, consider all combinations of
//		// required vertices
//		AllDirectedPaths<String, DefaultWeightedEdge> allPaths = new AllDirectedPaths<>(dependencyGraphWithLoop);
//		for (int i = 0; i < endActivities.size(); i++) {
//			String end = endActivities.get(i);
//
//			List<GraphPath<String, DefaultWeightedEdge>> paths = allPaths.getAllPaths(startActivities.get(0), end, true,
//					null);
//
//			// Filter paths to include all required vertices
//			List<GraphPath<String, DefaultWeightedEdge>> filteredPaths = paths.stream()
//					.filter(path -> requiredVertices.stream().allMatch(path.getVertexList()::contains))
//					.collect(Collectors.toList());
//			// Print the paths
////                filteredPaths.forEach(path -> System.out.println(path.getVertexList()));
//			selectedPath.addAll(filteredPaths);
//
//		}
//		System.out.println(selectedPath);
//		if (selectedPath != null) {
//
//			for (GraphPath<String, DefaultWeightedEdge> path : selectedPath) {
//				List<String> vertices = path.getVertexList();
//				List<DefaultWeightedEdge> edges = path.getEdgeList();
//
//				System.out.println("Vertices in path:");
//				for (int i = 0; i < vertices.size(); i++) {
//					System.out.println("Position " + (i + 1) + ": Vertex " + vertices.get(i));
//				}
//
//				System.out.println("Edges in path:");
//				for (int i = 0; i < edges.size(); i++) {
//					System.out.println("Position " + (i + 1) + ": Edge " + edges.get(i));
//				}
//			}
//		}
//	}

    /**
     * Get dependencies without loops and parallelism
     *
     * @return
     */
    public List<String> getDependenciesDFA() {

        List<List<String>> allPaths = new ArrayList<>();

        AllDirectedPaths<String, DefaultWeightedEdge> allDirectedPaths = new AllDirectedPaths<>(dependencyGraph);

        Set<String> endEvent = findEndNodes(dependencyGraph);
        for (String act : endActivities) {
            if (dependencyGraph.containsVertex(act)) {
                endEvent.add(act);
            }
        }

        Set<String> startEvent = findStartNodes(dependencyGraph);
        for (String act : startActivities) {
            if (dependencyGraph.containsVertex(act)) {
                startEvent.add(act);
            }
        }
        for (String startNode : startEvent) {
            for (String endNode : endEvent) {
                allDirectedPaths.getAllPaths(startNode.trim(), endNode.trim(), true, null)
                        .forEach(path -> allPaths.add(path.getVertexList()));
            }
        }
        System.out.println(allPaths);
        Collections.sort(allPaths, new Comparator<List<String>>() {
            @Override
            public int compare(List<String> o1, List<String> o2) {
                return Integer.compare(o2.size(), o1.size());
            }
        });

        // Print paths
        // allPaths.forEach(System.out::println);
        LinkedList<String> dependencyRelation = new LinkedList<>();
        for (List<String> path : allPaths) {
            for (int i = 0; i < path.size() - 1; i++) {
                String source = path.get(i);
                String target = path.get(i + 1);
                String relation = source + "->" + target;

                if (!dependencyRelation.contains(relation)) {
                    dependencyRelation.add(relation);
                }
            }
        }
        return DepthFirstSearch.DFSToList(dependencyRelation, startActivities.get(0));

    }

    public static Set<String> findEndNodes(Graph<String, DefaultWeightedEdge> graph) {
        Set<String> endNodes = new HashSet<>();
        for (String vertex : graph.vertexSet()) {
            if (graph.outgoingEdgesOf(vertex).isEmpty()) { // Check if there are no outgoing edges
                endNodes.add(vertex);
            }
        }
        return endNodes;
    }

    public static Set<String> findStartNodes(Graph<String, DefaultWeightedEdge> graph) {
        Set<String> startNodes = new HashSet<>();
        for (String vertex : graph.vertexSet()) {
            if (graph.incomingEdgesOf(vertex).isEmpty()) { // Check if there are no outgoing edges
                startNodes.add(vertex);
            }
        }
        return startNodes;
    }

    public void changeVertexNameToRegex() {

        Set<String> vertix = new HashSet<>(dependencyGraph.vertexSet());
        for (String v : vertix) {
            changeVertexName(dependencyGraph, v, regex(v));
            changeVertexName(dependencyGraphWithLoop, v, regex(v));
        }
    }

    public void changeVertexName(String oldName, String newName) {
        changeVertexName(dependencyGraph, oldName, newName);
        changeVertexName(dependencyGraphWithLoop, oldName, newName);
    }

    public void changeVertexName(Graph<String, DefaultWeightedEdge> graph, String oldName, String newName) {
        if (!graph.containsVertex(oldName)) {
            System.out.println("Vertex '" + oldName + "' not found.");
            return;
        }

        // Copy edges
        Set<DefaultWeightedEdge> incomingEdges = new HashSet<>(graph.incomingEdgesOf(oldName));
        Set<DefaultWeightedEdge> outgoingEdges = new HashSet<>(graph.outgoingEdgesOf(oldName));

        // Remove the old vertex
        graph.removeVertex(oldName);

        // Add the new vertex
        graph.addVertex(newName);

        // Re-create edges
        for (DefaultWeightedEdge edge : incomingEdges) {
            String sourceVertex = graph.getEdgeSource(edge);
            graph.addEdge(sourceVertex, newName);
        }
        for (DefaultWeightedEdge edge : outgoingEdges) {
            String targetVertex = graph.getEdgeTarget(edge);
            graph.addEdge(newName, targetVertex);
        }
    }

    public void regexOnElementInfo() {
        regex(startActivities);
        regex(endActivities);

        Iterator<List<String>> loopIter = loops.iterator();
        while (loopIter.hasNext()) {
            List<String> elements = loopIter.next();
            regex(elements);
        }

        Iterator<Set<String>> paramItem = parallelism.iterator();
        while (paramItem.hasNext()) {
            Set<String> elements = paramItem.next();
            regex(elements);

        }
        System.out.println(parallelism);
        // Creating a new map to hold keys without spaces
        Map<String, Map<String, String>> newMap = new HashMap<>();

        // Iterating over the original map to remove spaces from keys
        for (Entry<String, Map<String, String>> entry : elementInformations.entrySet()) {
            String newKey = regex(entry.getKey()); // Removing spaces
            newMap.put(newKey, entry.getValue()); // Putting the new key and value in the new map
        }

        // Optional: If the original map is no longer needed, you can replace it with
        // the new map
        elementInformations = newMap;
    }

    public static boolean haveSameSuccessors(DirectedWeightedPseudograph<String, DefaultWeightedEdge> graph, String vertex1, String vertex2) {
        // Get successor lists for both vertices
        var successorsV1 = Graphs.successorListOf(graph, vertex1);
        var successorsV2 = Graphs.successorListOf(graph, vertex2);

        // Check if the successor lists are equal
        return successorsV1.containsAll(successorsV2) && successorsV2.containsAll(successorsV1);

    }

    public static boolean haveSamePredecessors(DirectedWeightedPseudograph<String, DefaultWeightedEdge> graph, String vertex1, String vertex2) {
        // Get predecessor lists for both vertices
        var predecessorsV1 = Graphs.predecessorListOf(graph, vertex1);
        var predecessorsV2 = Graphs.predecessorListOf(graph, vertex2);
//        System.out.println("haveSamePredecessors: " + vertex1 + " and " + vertex2);
//        System.out.println("Predecessors of " + vertex1 + ": " + predecessorsV1);
//        System.out.println("Predecessors of " + vertex2 + ": " + predecessorsV2);
//        System.out.println("******");
        // Check if the predecessor lists are equal
        return predecessorsV1.containsAll(predecessorsV2) && predecessorsV2.containsAll(predecessorsV1);

    }

    public static boolean haveIntersectSuccessors(DirectedWeightedPseudograph<String, DefaultWeightedEdge> graph, String vertex1, String vertex2) {
        // Get successor lists for both vertices
        var successorsV1 = Graphs.successorListOf(graph, vertex1);
        var successorsV2 = Graphs.successorListOf(graph, vertex2);

        // Check if the lists are equal
        boolean areEqual = successorsV1.containsAll(successorsV2) && successorsV2.containsAll(successorsV1);

        // Check if one list is a subset of the other
        boolean isSubsetV1inV2 = successorsV2.containsAll(successorsV1);
        boolean isSubsetV2inV1 = successorsV1.containsAll(successorsV2);

        // Create a copy of successorsV1 to avoid modifying the original list
        List<String> intersection = new ArrayList<>(successorsV1);

        // Retain only the elements that are also in successorsV2
        intersection.retainAll(successorsV2);

        // Check if either the lists are equal or one is a subset of the other
        boolean result = areEqual || isSubsetV1inV2 || isSubsetV2inV1 | !intersection.isEmpty();
        return result;
    }

    public static boolean haveIntersectPredecessors(DirectedWeightedPseudograph<String, DefaultWeightedEdge> graph, String vertex1, String vertex2) {
        // Get predecessor lists for both vertices
        var predecessorsV1 = Graphs.predecessorListOf(graph, vertex1);
        var predecessorsV2 = Graphs.predecessorListOf(graph, vertex2);
//        System.out.println("haveSamePredecessors: " + vertex1 + " and " + vertex2);
//        System.out.println("Predecessors of " + vertex1 + ": " + predecessorsV1);
//        System.out.println("Predecessors of " + vertex2 + ": " + predecessorsV2);
//        System.out.println("******");

        // Check if the lists are equal
        boolean areEqual = predecessorsV1.containsAll(predecessorsV2) && predecessorsV2.containsAll(predecessorsV1);

        // Check if one list is a subset of the other
        boolean isSubsetV1inV2 = predecessorsV2.containsAll(predecessorsV1);
        boolean isSubsetV2inV1 = predecessorsV1.containsAll(predecessorsV2);

        // Create a copy of successorsV1 to avoid modifying the original list
        List<String> intersection = new ArrayList<>(predecessorsV1);

        // Retain only the elements that are also in successorsV2
        intersection.retainAll(predecessorsV2);

        // Check if either the lists are equal or one is a subset of the other
        boolean result = areEqual || isSubsetV1inV2 || isSubsetV2inV1 | !intersection.isEmpty();
        return result;

    }
}
