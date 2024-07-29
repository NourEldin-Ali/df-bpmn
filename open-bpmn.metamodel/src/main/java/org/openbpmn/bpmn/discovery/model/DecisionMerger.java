package org.openbpmn.bpmn.discovery.model;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.DirectedWeightedPseudograph;

import java.util.*;
import java.util.stream.Collectors;

public class DecisionMerger {
    Set<Set<String>> decisions;
    DirectedWeightedPseudograph<String, DefaultWeightedEdge> dependencyGraph;
 public DecisionMerger(Set<Set<String>> decisions, DirectedWeightedPseudograph<String, DefaultWeightedEdge> dependencyGraph) {
        this.decisions = decisions;
        this.dependencyGraph = dependencyGraph;
    }

    public LinkedList<LinkedList<String>> getDecisions() {


        // get all sources
        Map<String, Map<String, Set<String>>> sortedBySource = new HashMap<>();
        for (Set<String> pair : decisions.stream().collect(Collectors.toList())) {
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
                for (Set<String> pair : decisions.stream().collect(Collectors.toList())) {
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
                for (Map.Entry<String, Set<String>> entry : sourceValue.entrySet()) {
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
                    for (Map.Entry<String, Set<String>> entry : sourceValue.entrySet()) {
                        entry.getValue().remove(frequentElement);
                    }
                } else {
                    source.getValue().remove(frequentElement);
                }
            }
        }

        // Convert Set<Set<String>> to LinkedList<LinkedList<String>>
        LinkedList<LinkedList<String>> result = new LinkedList<>();
        for (Set<String> innerSet : finalDecisionList) {
            LinkedList<String> innerList = new LinkedList<>(innerSet);
            result.add(innerList);
        }

        return result;
    }
}
