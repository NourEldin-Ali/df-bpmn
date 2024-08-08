package org.openbpmn.bpmn.discovery.model;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.DirectedWeightedPseudograph;

import java.util.*;
import java.util.stream.Collectors;

public class ParallelismMerger {
    Set<Set<String>> parallelism;
    DirectedWeightedPseudograph<String, DefaultWeightedEdge> dependencyGraph;

    public ParallelismMerger(Set<Set<String>> parallelism, DirectedWeightedPseudograph<String, DefaultWeightedEdge> dependencyGraph) {
        this.parallelism = parallelism;
        this.dependencyGraph = dependencyGraph;
    }


    public LinkedList<LinkedList<String>> getParallelims() {

        // Converting set back to list
        LinkedList<LinkedList<String>> result = new LinkedList<>();
        Set<Set<String>> mergedPara = mergeParallelism();
        // TODO: check why there exists a missing data?
        // check if any missing data, and add
        for (Set<String> check : parallelism) {
            boolean shouldAdd = true;
            for (Set<String> megrgedParallelism : mergedPara) {
                if (megrgedParallelism.containsAll(check)) {
                    shouldAdd = false;
                    break;

                }
            }
            if (shouldAdd) {
                mergedPara.add(check);
            }
        }
        for (Set<String> innerSet : mergedPara) {
            LinkedList<String> innerList = new LinkedList<>(innerSet);
            result.add(innerList);
        }

        return result;
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
                    sortedBySource.putIfAbsent(source, new HashSet<>());
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
                    source.getValue().add(new HashSet<>(pair));
                }
            }
        }
        // Step 2 & 3: Calculate frequency and identify the highest frequency element
        String frequentElement = "";
        Set<Set<String>> maxFreqElements = new HashSet<>();
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
//        System.out.println("print test sortedBySource: ");
//        System.out.println(sortedBySource);
        // union
        Set<Set<String>> finalParalellList = new LinkedHashSet<>();
        for (Map.Entry<String, Set<Set<String>>> source : sortedBySource.entrySet()) {
//            System.out.println(source.getKey());
            finalParalellList.addAll(unionSetsWithCommonElements(new ArrayList<>(source.getValue())));
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
//        System.out.println("test unionSetsWithCommonElements: :");
//        System.out.println(originalSets);
        return originalSets;
    }

}
