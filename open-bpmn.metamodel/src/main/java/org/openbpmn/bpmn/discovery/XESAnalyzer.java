package org.openbpmn.bpmn.discovery;

import org.deckfour.xes.in.XesXmlParser;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.openbpmn.bpmn.discovery.model.*;
import org.openbpmn.bpmn.exceptions.BPMNModelException;

import io.process.analytics.tools.bpmn.generator.App;

import java.io.File;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class XESAnalyzer {
	
	//TODO: error in loops!
	// loop position=> maybe should merge the list of loop based on the target to know where we should add the loop gateway (split gateway of loop)
	// some relations are removed when there is a loop in this treatment of data, we should take care to how I treat the data
	public static void main(String[] args) {
		try {
			// Start timing
			long startTime = System.nanoTime();
			XLog log = new XESAnalyzer().readLog("C:\\Users\\AliNourEldin\\Downloads\\splitminerwg\\diagram.xes");
			DependencyGraph dependencyGraph = generateDependencyGraph(log);
//			dependencyGraph.findLoopsAndParrallelism();
			System.out.println(dependencyGraph.loops);
			System.out.println(dependencyGraph.parallelism);

			//get parallelism
			ParallelismMerger parallelismMerger = new ParallelismMerger(dependencyGraph.parallelism,
					dependencyGraph.dependencyGraph);
			LinkedList<LinkedList<String>> parallelRelations = parallelismMerger.getParallelims();
			System.out.println(parallelismMerger.getParallelims());


			//get decisions
			DecisionMerger decisionMerger = new DecisionMerger(dependencyGraph.exlusive, dependencyGraph.dependencyGraph);
			LinkedList<LinkedList<String>> decisionRelations = decisionMerger.getDecisions();
			System.out.println(decisionRelations);
			
			
//			
//			Set<String> startActivities = findStartActivities(traces);
//			Set<String> endActivities = findEndActivities(traces);
//			
//
//			Map<String, List<String>> dependencyRelations = RelationConverter
//					.relationsToMap(new ArrayList(dependencyGraph));		
//			System.out.println(dependencyRelations);
//
//			Set<Set<String>> loops = findLoop(dependencyRelations, traces);
//			System.out.println("After:");
//			System.out.println(dependencyRelations);
//
//			Set<Set<String>> parallels = findParallelPairs(dependencyRelations, traces);
//
//			// remove loop
////			removeLoops(dependencyRelations, loops, traces);
//
//			LinkedList<Set<String>> decisions = findDecisions(parallels, dependencyRelations);
//
//			LinkedList<Set<String>> mergeParralelism = mergeParallelism(parallels,dependencyRelations, traces);
//			System.out.println("Dependency Relations: " + dependencyRelations);
//			System.out.println("Loop Events: " + loops);
//			System.out.println("Parallel Events: " + mergeParralelism);
//			System.out.println("Decisions: " + decisions);
//
//			LinkedList<String> relations = mapToListOfRelations(dependencyRelations);
//			List<String> startEvents = new ArrayList();
//			List<String> endEvents = new ArrayList();
//
//			System.out.println("Relations: " + relations);
//			for (String str : startActivities) {
//				String startEvent = "start_" + str;
//				startEvents.add(startEvent);
//				relations.add(startEvent + "->" + str);
//			}
//
//			for (String str : endActivities) {
//				String endEvent = "end_" + str;
//				endEvents.add(endEvent);
//				relations.add(str + "->" + endEvent);
//			}
//			System.out.println("Dependency Relations with events: " + relations);
//			LinkedList<String> orderedEvents = DepthFirstSearch.DFSToList(relations, startEvents.iterator().next());
//
//			Map<String, LinkedList<LinkedList<String>>> relations1 = new LinkedHashMap<>();
//			relations1.put(BPMNDiscovery.DECISION, convertToListOfLists(decisions));
//			relations1.put(BPMNDiscovery.PARALLEL, convertToListOfLists(mergeParralelism));
//			try {
//				BPMNDiscovery bpmnDiscovery = new BPMNDiscovery(startEvents, endEvents, orderedEvents, relations1);
//				bpmnDiscovery.DependencyGraphToBPMN();
//				long endTime = System.nanoTime();
//				long duration = (endTime - startTime) / 1_000_000; // Convert nanoseconds to milliseco
//				System.out.println("Execution time: " + duration + " ms");
//				bpmnDiscovery.saveMode("C:\\Users\\AliNourEldin\\Desktop\\bpmn-layout\\bpmn-auto-layout\\test\\fixtures\\testt.bpmn");
//
//			} catch (BPMNModelException e) {
//				e.printStackTrace();
//			}
//			// Stop timing
//			long endTime = System.nanoTime();
//			// Calculate execution time in milliseconds
//			long duration = (endTime - startTime) / 1_000_000; // Convert nanoseconds to milliseconds
//
//			System.out.println("Execution time: " + duration + " ms");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	static void removeLoops(Map<String, List<String>> dependencyRelations, Set<Set<String>> loops,
			Set<List<String>> traces) {
		for (Set<String> pair : loops) {
			// self-loop
			if (pair.size() == 1) {
				Iterator<String> iter = pair.iterator();
				String first = iter.next();
				dependencyRelations.get(first).remove(first);
				continue;
			}
		}
	}

	public static LinkedList<LinkedList<String>> convertToListOfLists(LinkedList<Set<String>> listOfSets) {
		LinkedList<LinkedList<String>> resultList = new LinkedList<>();
		for (Set<String> set : listOfSets) {
			LinkedList<String> newList = new LinkedList<>(set); // Convert Set to LinkedList
			resultList.add(newList);
		}
		return resultList;
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

	public static DependencyGraph generateDependencyGraph(XLog log) {
		DependencyGraph graph = new DependencyGraph();
		for (XTrace trace : log) {
			String lastEvent = null;
			for (XEvent event : trace) {
				String eventName = event.getAttributes().get("concept:name").toString();
				graph.addVertex(eventName);
				
				if(lastEvent!=null) {
					graph.addEdge(lastEvent, eventName);
				}
				lastEvent = eventName;
			}
			graph.startActivities.add(trace.get(0).getAttributes().get("concept:name").toString());
			graph.endActivities.add(trace.get(trace.size()-1).getAttributes().get("concept:name").toString());
		}
		return graph;
	}

	
	

	public static LinkedList<Set<String>> findDecisions(Set<Set<String>> paralellism,
			Map<String, List<String>> dependencyRelations) {

		// get pair decisions
		Set<Set<String>> pairDecisionPoints = new HashSet<>();
		for (Map.Entry<String, List<String>> entry : dependencyRelations.entrySet()) {
			List<String> targets = entry.getValue();
			if (targets.size() > 1) {
				for (int i = 0; i < targets.size() - 1; i++) {
					for (int j = i + 1; j < targets.size(); j++) {
						Set<String> compatible = new HashSet<>();
						String element1 = targets.get(i);
						String element2 = targets.get(j);

						if (!paralellism.stream()
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
			for (String element : pair.stream().collect(Collectors.toList())) {
				for (Map.Entry<String, List<String>> entry : dependencyRelations.entrySet()) {
					if (entry.getValue().contains(element)) {
						String sourceElement = entry.getKey();
						if (sortedBySource.containsKey(sourceElement)) {
							if (!sortedBySource.get(sourceElement).containsKey(element)) {
								sortedBySource.get(sourceElement).putIfAbsent(element, new HashSet<String>());
							}
						} else {
							sortedBySource.putIfAbsent(entry.getKey(), new HashMap());
						}
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
							if (dependencyRelations.get(source.getKey()).contains(activity)
									&& !activity.contentEquals(sourceConnections.getKey())) {
								sourceConnections.getValue().add(activity);
							}
						}
					}
				}
			}
		}
		System.out.println(sortedBySource);
		LinkedList<Set<String>> finalDecisionList = new LinkedList();

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
		return finalDecisionList;
	}

	public static Set<Set<String>> findLoop(Map<String, List<String>> dependencyRelations, Set<List<String>> traces) {
		Set<Set<String>> paralellPairs = new HashSet<>();

		// Check for mutual follows, and frequency >1
		for (Map.Entry<String, List<String>> source : dependencyRelations.entrySet()) {
			String sourceActivity = source.getKey();
			for (String follower : source.getValue()) {
				// Check if follower also directly follows the current event
				if (dependencyRelations.get(follower) != null
						&& dependencyRelations.get(follower).contains(sourceActivity)) {
					// check if sourceAcitivity and follower has the same source
					if (dependencyRelations.values().stream()
							.anyMatch(sublist -> sublist.contains(follower) && sublist.contains(sourceActivity))) {
						Set<String> pair = new TreeSet<>(); // Using TreeSet to keep the order consistent
						pair.add(sourceActivity);
						pair.add(follower);
						paralellPairs.add(pair);
					}
				}
			}
		}

		Set<Set<String>> loopPairs = new HashSet<>();
		for (Set<String> pair : paralellPairs) {
			// self-loop
			if (pair.size() == 1) {
				loopPairs.add(pair);
				Iterator<String> iter = pair.iterator();
				String first = iter.next();
				// remove loop
//				dependencyRelations.get(first).remove(first);
				continue;
			}
			Iterator<String> iter = pair.iterator();
			String first = iter.next();
			String second = iter.next();
			boolean isLoop = false;
			for (List<String> trace : traces) {
				if (trace.stream().filter(event -> event.contentEquals(first)).count() > 1
						&& trace.stream().filter(event -> event.contentEquals(second)).count() > 1) {
					isLoop = true;
					loopPairs.add(pair);
					break;
				}
			}
			if (isLoop) {
				boolean isFirstElementBeforeSecond = true;
				for (List<String> trace : traces) {
					if (trace.indexOf(first) > trace.indexOf(second)) {
						isFirstElementBeforeSecond = false;
						break;
					}

				}
				String source;
				String target;
				if (isFirstElementBeforeSecond) {
					source = first;
					target = second;
				} else {
					source = second;
					target = first;
				}
				for (Map.Entry<String, List<String>> relations : dependencyRelations.entrySet()) {
					if (!relations.getKey().contentEquals(source)) {
						// remove loop
//						relations.getValue().remove(target);
					}
				}
				// remove loop
//				dependencyRelations.get(target).removeAll(dependencyRelations.get(source));
			}
		}

		return loopPairs;
	}

	public static Set<Set<String>> findParallelPairs(Map<String, List<String>> dependencyRelations,
			Set<List<String>> traces) {
		Set<Set<String>> parallelPairs = new HashSet<>();

		// Check for mutual follows, indicating parallelism
		for (Map.Entry<String, List<String>> source : dependencyRelations.entrySet()) {
			String sourceActivity = source.getKey();
			for (String follower : source.getValue()) {
				// Check if follower also directly follows the current event
				if (dependencyRelations.get(follower) != null
						&& dependencyRelations.get(follower).contains(sourceActivity)) {
					// check if sourceAcitivity and follower has the same source
					if (dependencyRelations.values().stream()
							.anyMatch(sublist -> sublist.contains(follower) && sublist.contains(sourceActivity))) {
						// self-loop
						if (!sourceActivity.contentEquals(follower)) {
							Set<String> pair = new TreeSet<>(); // Using TreeSet to keep the order consistent
							pair.add(sourceActivity);
							pair.add(follower);
							parallelPairs.add(pair);
						}

					}
				}
			}
		}

		removeParallelismFromDependecies(parallelPairs, dependencyRelations);

		return parallelPairs;
	}

	private static void removeParallelismFromDependecies(Set<Set<String>> parallelPairs,
			Map<String, List<String>> dependencies) {
		System.out.println(parallelPairs);
		Iterator<Set<String>> iterator = parallelPairs.iterator();
		while (iterator.hasNext()) {
			Set<String> set = iterator.next();
			Iterator<String> iter = set.iterator();
			String first = iter.next();
			String second = iter.next();
			// Remove second from the list of dependencies of first, if present
			if (dependencies.containsKey(first) && dependencies.get(first).contains(second)) {
				dependencies.get(first).remove(second);
			}

			// Remove first from the list of dependencies of second, if present
			if (dependencies.containsKey(second) && dependencies.get(second).contains(first)) {
				dependencies.get(second).remove(first);
			}
		}
	}

	private static boolean isInvalidPair(Set<String> pair, Map<String, List<String>> dependencies) {
		Set<String> checkSet = new HashSet<>(pair); // Create a set from the pair for easy comparison
		int count = 0;
		for (List<String> depList : dependencies.values()) {
			// Create a set from the dependency list
			Set<String> depSet = new HashSet<>(depList);
			if (depSet.containsAll(checkSet)) {
				count++;
			}
		}
		if (count > 0) {
			return false;
		}
		return true;
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

//        // Check if each event in the set really ends all traces it appears in
//        Set<String> verifiedEndEvents = new HashSet<>(endEvents);
//        for (String event : endEvents) {
//            for (List<String> trace : traces) {
//                if (!trace.get(trace.size() - 1).equals(event)) {
//                    verifiedEndEvents.remove(event);
//                    break;
//                }
//            }
//        }

		return endEvents;
	}
}
