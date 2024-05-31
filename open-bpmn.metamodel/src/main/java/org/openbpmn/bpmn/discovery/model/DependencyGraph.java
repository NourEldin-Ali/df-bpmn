package org.openbpmn.bpmn.discovery.model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import java.util.Optional;
import java.util.Queue;
import java.util.stream.Collectors;

import org.jgrapht.Graph;
import org.jgrapht.alg.cycle.CycleDetector;
import org.jgrapht.alg.cycle.JohnsonSimpleCycles;
import org.jgrapht.alg.cycle.StackBFSFundamentalCycleBasis;
import org.jgrapht.alg.cycle.SzwarcfiterLauerSimpleCycles;
import org.jgrapht.alg.cycle.TarjanSimpleCycles;
import org.jgrapht.alg.cycle.TiernanSimpleCycles;
import org.jgrapht.alg.lca.HeavyPathLCAFinder;
import org.jgrapht.alg.lca.NaiveLCAFinder;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.DirectedWeightedPseudograph;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.traverse.BreadthFirstIterator;

public class DependencyGraph {
	public DirectedWeightedPseudograph<String, DefaultWeightedEdge> dependencyGraph;
	public DirectedWeightedPseudograph<String, DefaultWeightedEdge> dependencyGraphWithLoop;
	public Set<String> startActivities;
	public Set<String> endActivities;

	public Set<List<String>> loops;
	public Set<Set<String>> parallelism;
	public Set<Set<String>> decisions;

	public DependencyGraph() {
		dependencyGraph = new DirectedWeightedPseudograph<>(DefaultWeightedEdge.class);
		dependencyGraphWithLoop = new DirectedWeightedPseudograph<>(DefaultWeightedEdge.class);
		startActivities = new HashSet<>();
		endActivities = new HashSet<>();
		loops = new HashSet<>();
		parallelism = new HashSet<>();
		decisions = new HashSet<>();
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

	public void findLoopsAndParrallelism() {
		Set<List<String>> tempLoop = new HashSet();
		if (dependencyGraphWithLoop != null) {
			// TODO: error in loop detections
			// self-loop, loops, parallelism detections
			SzwarcfiterLauerSimpleCycles<String, DefaultWeightedEdge> cycleDetector = new SzwarcfiterLauerSimpleCycles<>(
					dependencyGraphWithLoop);
			boolean hasCycle = cycleDetector.findSimpleCycles().size() > 0;
			if (hasCycle) {
				List<List<String>> cyclesList = cycleDetector.findSimpleCycles();
				for (List<String> cycle : cyclesList) {
					// in case of self-loop or loop
					if (cycle.size() == 1) {
						loops.add(new ArrayList(Arrays.asList(cycle.get(0), cycle.get(0))));
						tempLoop.add(new ArrayList(Arrays.asList(cycle.get(0), cycle.get(0))));
					} else {
						String element1 = cycle.get(0);
						String element2 = cycle.get(cycle.size() - 1);
						tempLoop.add(new ArrayList(Arrays.asList(element2, element1)));
					}
				}
			}
		}
		// remove loops from dependency graph
		dependencyGraph = (DirectedWeightedPseudograph<String, DefaultWeightedEdge>) dependencyGraphWithLoop.clone();
//		System.out.println(loops);
		loops.stream().forEach(edge -> {
			dependencyGraph.removeEdge(edge.get(0), edge.get(1));

		});

		JohnsonSimpleCycles<String, DefaultWeightedEdge> cycleDetector = new JohnsonSimpleCycles<>(
				dependencyGraphWithLoop);
		boolean hasCycle = cycleDetector.findSimpleCycles().size() > 0;
		if (hasCycle) {
			List<List<String>> cyclesList = cycleDetector.findSimpleCycles();
			for (List<String> cycle : cyclesList) {
				if (cycle.size() == 2) {
					String element1 = cycle.get(0);
					String element2 = cycle.get(1);

					// get source of element1
					Set<DefaultWeightedEdge> incomingEdgeElement1 = dependencyGraph.incomingEdgesOf(element1);
					Set<String> sourceElement1 = new HashSet<>();
					incomingEdgeElement1.stream()
							.forEach(edge -> sourceElement1.add(dependencyGraph.getEdgeSource(edge)));

					// get source of element2
					Set<DefaultWeightedEdge> incomingEdgeElement2 = dependencyGraph.incomingEdgesOf(element1);
					Set<String> sourceElement2 = new HashSet<>();
					incomingEdgeElement2.stream()
							.forEach(edge -> sourceElement2.add(dependencyGraph.getEdgeSource(edge)));

					sourceElement1.remove(element1);
					sourceElement1.remove(element2);
					sourceElement2.remove(element1);
					sourceElement2.remove(element2);
					// if the pairs has the same source => parallelism
					Set<String> intersection = new HashSet<>(sourceElement1); // Make a copy of set1
					intersection.retainAll(sourceElement2); // Retain only elements that are also in set2
					if (!intersection.isEmpty()) {
						parallelism.add(new HashSet<String>(Arrays.asList(element2, element1)));
					}
				}
			}
		}
//		System.out.println(parallelism);
		parallelism.stream().forEach(edge -> {
			Iterator<String> edges = edge.iterator();
			String element1 = edges.next();
			String element2 = edges.next();
			dependencyGraph.removeEdge(element2, element1);
			dependencyGraph.removeEdge(element1, element2);

		});

		//remove parallel from temp loop
		Set<List<String>> resultSet = new HashSet<>();
		for (List<String> list : tempLoop) {
			Set<String> tempSet = new HashSet<>(list);
			if (!parallelism.contains(tempSet)) {
				resultSet.add(list);
			}
		}
		loops = resultSet;
		loops.stream().forEach(edge -> {
			dependencyGraph.removeEdge(edge.get(0), edge.get(1));
		});
	}

	public Set<List<String>> getLoops() {
		return loops;
	}

	// TODO: valide it, it was generated using LLM
	public Map<String, List<List<String>>> mergeLoop() {
		// Get the loop relations from the dependency graph
		Set<List<String>> loopRelations = loops;
//		System.out.println(loopRelations);

		// Get all targets of loop relations
		Map<String, List<List<String>>> sortedByTarget = new HashMap<>();
		for (List<String> pair : loopRelations) {
			String element2 = pair.get(1);
			sortedByTarget.computeIfAbsent(element2, k -> new ArrayList<>()).add(pair);
		}

//		System.out.println(sortedByTarget);
		// Merge loop relations
		Map<String, List<List<String>>> mergeByTarget = new HashMap<>();

		for (Entry<String, List<List<String>>> entry : sortedByTarget.entrySet()) {
			String target = entry.getKey();
			List<List<String>> loopList = entry.getValue();
			List<List<String>> resultList = new ArrayList<>();

			for (int i = 0; i < loopList.size(); i++) {
				if (i >= loopList.size()) {
					break;
				}
				// Self-loop
				if (loopList.get(i).get(0).equals(loopList.get(i).get(1))) {
					resultList.add(Collections.singletonList(loopList.get(i).get(0)));
					continue;
				} else {
					List<List<String>> listToRemove = new ArrayList<>();
					List<String> mergeElements = new ArrayList<>();
					String element1 = loopList.get(i).get(0);
					mergeElements.add(element1);

					for (int j = i + 1; j < loopList.size(); j++) {
						if (j >= loopList.size()) {
							break;
						}
						String element2 = loopList.get(j).get(0);

						// Check if the elements have the same successors
						Set<DefaultWeightedEdge> outgoingEdgeSource = dependencyGraph.outgoingEdgesOf(element1);
						List<String> l1 = new ArrayList<String>();
						outgoingEdgeSource.stream().forEach(edge -> l1.add(dependencyGraph.getEdgeTarget(edge)));

						outgoingEdgeSource = dependencyGraph.outgoingEdgesOf(element2);
						List<String> l2 = new ArrayList<String>();
						outgoingEdgeSource.stream().forEach(edge -> l2.add(dependencyGraph.getEdgeTarget(edge)));

						if (l1.equals(l2)) {
							mergeElements.add(element2);
							listToRemove.add(loopList.get(j));
						}
					}

					// Remove merged list from initial loop relation
					loopList.removeAll(listToRemove);
					resultList.add(mergeElements);
				}
			}
			mergeByTarget.put(target, resultList);
		}
		return mergeByTarget;
	}

	public List<List<String>> getParallelims() {
//		Set<List<String>> setWithoutDuplicates = new LinkedHashSet<>(mergeParallelism());
		// Converting set back to list
		List<List<String>> listWithoutDuplicates = new LinkedList(mergeParallelism());
		return listWithoutDuplicates;
	}

	public List<List<String>> getDecisions() {

		// get pair decisions
		Set<Set<String>> pairDecisionPoints = new HashSet<>();
		for (String activity : dependencyGraph.vertexSet()) {
			Set<DefaultWeightedEdge> outgoingEdgeSource = dependencyGraph.outgoingEdgesOf(activity);
			List<String> targetActivities = new ArrayList();
			outgoingEdgeSource.stream().forEach(edge -> targetActivities.add(dependencyGraph.getEdgeTarget(edge)));
//			System.out.println(activity);
//			System.out.println(targetActivities);
			if (targetActivities.size() > 1) {
				for (int i = 0; i < targetActivities.size() - 1; i++) {
					for (int j = i + 1; j < targetActivities.size(); j++) {
						Set<String> compatible = new HashSet<>();
						String element1 = targetActivities.get(i);
						String element2 = targetActivities.get(j);

						if (!parallelism.stream()
								.anyMatch(sublist -> sublist.contains(element1) && sublist.contains(element2))) {
							compatible.add(element1);
							compatible.add(element2);
							pairDecisionPoints.add(compatible);
						}
					}
				}
			}

		}

		// get all sources
		Map<String, Map<String, Set<String>>> sortedBySource = new HashMap<>();
		for (Set<String> pair : pairDecisionPoints.stream().collect(Collectors.toList())) {
			for (String activity : pair.stream().collect(Collectors.toList())) {
				// get all source of activity
				Set<DefaultWeightedEdge> incomingEdgeActivity = dependencyGraph.incomingEdgesOf(activity);
				Set<String> sourcesActivity = new HashSet<>();
				incomingEdgeActivity.stream().forEach(edge -> sourcesActivity.add(dependencyGraph.getEdgeSource(edge)));
				for (String sourceElement : sourcesActivity) {
					if (sortedBySource.containsKey(sourceElement)) {
						if (!sortedBySource.get(sourceElement).containsKey(activity)) {
							sortedBySource.get(sourceElement).putIfAbsent(activity, new HashSet<String>());
						}
					} else {
						sortedBySource.putIfAbsent(sourceElement, new HashMap());
					}
				}
			}
		}

		// fill activity in the sources
		for (Map.Entry<String, Map<String, Set<String>>> source : sortedBySource.entrySet()) {
			for (Map.Entry<String, Set<String>> sourceConnections : source.getValue().entrySet()) {
				for (Set<String> pair : pairDecisionPoints.stream().collect(Collectors.toList())) {
					if (pair.contains(sourceConnections.getKey())) {
						for (String activity : pair.stream().collect(Collectors.toList())) {
							if (dependencyGraph.getEdge(source.getKey(), activity) != null
									&& !activity.contentEquals(sourceConnections.getKey())) {
								sourceConnections.getValue().add(activity);
							}
						}
					}
				}
			}
		}

		Set<Set<String>> finalDecisionList = new HashSet();

		// get most frequent element
		for (Map.Entry<String, Map<String, Set<String>>> source : sortedBySource.entrySet()) {
			while (!source.getValue().isEmpty()) {
				String frequentElement = "";
				Set<String> maxFreqElements = new HashSet<String>();
				Map<String, Set<String>> sourceValue = source.getValue();
				for (Entry<String, Set<String>> entry : sourceValue.entrySet()) {
					if (frequentElement.isEmpty()) {
						frequentElement = entry.getKey();
						maxFreqElements = entry.getValue();
					} else {
						if (entry.getValue().size() > maxFreqElements.size()) {
							frequentElement = entry.getKey();
							maxFreqElements = entry.getValue();
						}
					}
				}
				if (maxFreqElements.size() > 0) {
					maxFreqElements.add(frequentElement);
					finalDecisionList.add(maxFreqElements);
					source.getValue().remove(frequentElement);
					for (Entry<String, Set<String>> entry : sourceValue.entrySet()) {
						entry.getValue().remove(frequentElement);
					}
				} else {
					source.getValue().remove(frequentElement);
				}
			}
		}

		return new LinkedList(finalDecisionList);
	}

	/**
	 * This function to merge all the parallel relation to be in transativity
	 * relation (all relation after activity after the same gateway)
	 * 
	 * @return
	 */
	public Set<Set<String>> mergeParallelism() {
		// get all sources
		Map<String, Set<Set<String>>> sortedBySource = new HashMap<>();
		for (Set<String> pair : parallelism.stream().collect(Collectors.toList())) {
			for (String activity : pair.stream().collect(Collectors.toList())) {
				// get source of activity
				Set<DefaultWeightedEdge> incomingEdgeActivity = dependencyGraph.incomingEdgesOf(activity);
				Set<String> sourceActivity = new HashSet<>();
				incomingEdgeActivity.stream().forEach(edge -> sourceActivity.add(dependencyGraph.getEdgeSource(edge)));
				sourceActivity.stream().forEach(source -> {
					sortedBySource.putIfAbsent(source, new HashSet());
				});
			}
		}

		// get sources connections
		for (Map.Entry<String, Set<Set<String>>> source : sortedBySource.entrySet()) {
			// get target of sources
			Set<DefaultWeightedEdge> outgoingEdgeSource = dependencyGraph.outgoingEdgesOf(source.getKey());
			Set<String> targetActivities = new HashSet<>();
			outgoingEdgeSource.stream().forEach(edge -> targetActivities.add(dependencyGraph.getEdgeTarget(edge)));
			for (Set<String> pair : parallelism.stream().collect(Collectors.toList())) {
				boolean isAllSameSource = true;
				for (String activity : pair.stream().collect(Collectors.toList())) {
					if (!targetActivities.contains(activity)) {
						isAllSameSource = false;
					}
				}
				if (isAllSameSource) {
					source.getValue().add(new HashSet(pair));
				}
			}
		}
		// Step 2 & 3: Calculate frequency and identify the highest frequency element
		String frequentElement = "";
		Set<Set<String>> maxFreqElements = new HashSet();
		for (Map.Entry<String, Set<Set<String>>> entry : sortedBySource.entrySet()) {
			if (frequentElement.isEmpty()) {
				frequentElement = entry.getKey();
				maxFreqElements = entry.getValue();
			} else {
				if (entry.getValue().size() > maxFreqElements.size()) {
					frequentElement = entry.getKey();
					maxFreqElements = entry.getValue();
				}
			}

		}
		// Step 4: Remove elements with the highest frequency from all entries (not
		// included highest one)
		for (Map.Entry<String, Set<Set<String>>> entry : sortedBySource.entrySet()) {
			if (!frequentElement.contentEquals(entry.getKey())) {
				Iterator<Set<String>> sublistIter = entry.getValue().iterator();
				while (sublistIter.hasNext()) {
					Set<String> sublist = sublistIter.next();
					if (maxFreqElements.contains(sublist)) {
						sublistIter.remove();
					}
				}
			}
		}

		// union
		Set<Set<String>> finalParalellList = new LinkedHashSet<>();
		for (Map.Entry<String, Set<Set<String>>> source : sortedBySource.entrySet()) {
			finalParalellList.addAll(unionSetsWithCommonElements(new ArrayList(source.getValue())));
		}
		return finalParalellList;
	}

	/**
	 * This function is used to merge lists with common elements
	 * 
	 * @param originalSets
	 * @return
	 */
	public static List<Set<String>> unionSetsWithCommonElements(List<Set<String>> originalSets) {
		boolean mergeOccurred;

		do {
			mergeOccurred = false;
			List<Set<String>> newSets = new ArrayList<>();

			while (!originalSets.isEmpty()) {
				Set<String> current = originalSets.remove(0);
				int i = 0;
				// Find the first set that intersects with the current set
				while (i < newSets.size()) {
					if (!Collections.disjoint(newSets.get(i), current)) {
						newSets.get(i).addAll(current); // Merge the current set with the matching set
						mergeOccurred = true;
						break;
					}
					i++;
				}

				// If no intersecting set was found, add the current set as a new set
				if (i == newSets.size()) {
					newSets.add(current);
				}
			}

			originalSets = newSets; // Update the list for the next iteration
		} while (mergeOccurred);
		return originalSets;
	}

	public String findLCA(String v1, String v2) {
		Set<String> ancestors1 = findAllAncestors(v1);
		Set<String> ancestors2 = findAllAncestors(v2);

		// Find common ancestors
		ancestors1.retainAll(ancestors2);

		// Optionally, determine the "lowest" or "closest" common ancestor if needed
		// This part is tricky without specific rules on how to select the LCA if
		// multiple are found

		return ancestors1.stream().findFirst().orElse(null); // Simplistic approach: just find any common ancestor
	}

	private Set<String> findAllAncestors(String start) {
		Set<String> visited = new HashSet<>();
		Deque<String> stack = new ArrayDeque<>();
		stack.push(start);

		while (!stack.isEmpty()) {
			String vertex = stack.pop();
			for (DefaultWeightedEdge edge : dependencyGraph.incomingEdgesOf(vertex)) {
				String source = dependencyGraph.getEdgeSource(edge);
				if (visited.add(source)) {
					stack.push(source);
				}
			}
		}

		return visited;
	}

	public String getLCA(String element1, String element2) {
		NaiveLCAFinder<String, DefaultWeightedEdge> lcaFinder = new NaiveLCAFinder<>(this.dependencyGraph);
		return  lcaFinder.getLCA(element1, element2);
	}
	public static void main(String[] args) {
		DependencyGraph graph = new DependencyGraph();
		// Create a directed graph

		// Add vertices
		graph.addVertex("a");
		graph.addVertex("b");
		graph.addVertex("c");
		graph.addVertex("d");
		graph.addVertex("f");
		graph.addVertex("e");
		graph.addVertex("g");
		graph.addVertex("k");

		// Add edges
		graph.addEdge("a", "b");
		graph.addEdge("a", "c");
		graph.addEdge("b", "c");
		graph.addEdge("c", "b");
//		graph.addEdge("c", "a"); // This creates a cycle: a -> b -> c -> a
		graph.addEdge("a", "d"); // This creates a cycle
		graph.addEdge("d", "e");
		graph.addEdge("e", "f");
		graph.addEdge("e", "g");
		graph.addEdge("g", "k");
		graph.addEdge("f", "k");
//		graph.addEdge("d", "d"); // This is a self-loop

		graph.findLoopsAndParrallelism();
//		graph.dependencyGraph.get
		System.out.println(graph.dependencyGraph.toString());
//		System.out.println(graph.getLoops());
//		System.out.println(graph.getParallelims());
//		System.out.println(graph.getDecisions());
//		String root = graph.findLCA("f", "g");
//		System.out.println(root);
		// Detect cycles
		NaiveLCAFinder<String, DefaultWeightedEdge> lcaFinder = new NaiveLCAFinder<>(graph.dependencyGraph);
		String lca = lcaFinder.getLCA("c", "k");

		System.out.println("The LCA of 4 and 5 is: " + lca);

	}

}
