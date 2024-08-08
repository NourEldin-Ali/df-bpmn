package org.openbpmn.bpmn.discovery.model;

import java.util.*;

public class DecisionForLoop {
    Set<Set<String>> decisions;
    Set<Set<String>> parallel;
    Set<Set<String>> inclusive;
    List<Pair<Set<String>, Set<String>>> mergedLoop;

    public DecisionForLoop(Set<Set<String>> decisions,
                           Set<Set<String>> parallel,
                           Set<Set<String>> inclusive,
                           List<Pair<Set<String>, Set<String>>> mergedLoop) {
        this.decisions = decisions;
        this.parallel = parallel;
        this.inclusive = inclusive;
        this.mergedLoop = mergedLoop;
    }

    public void appendDecisions() {
        for (Pair<Set<String>, Set<String>> loop : mergedLoop) {
            if (loop.getTarget().size() > 1) {
                Set<Set<String>> combinations = generateCombinations(loop.getTarget());
                for (Set<String> combination : combinations) {
                    System.out.println("Combination: " + combination);
                    boolean isExclusive = true;
                    if (parallel.contains(combination)) {
                        isExclusive = false;
                    } else if (inclusive.contains(combination)) {
                        isExclusive = false;
                    }

                    if (isExclusive) {
                        System.out.println("Decision: " + combination);
                        decisions.add(combination);
                    }
                }
            }
        }
    }

    private Set<Set<String>> generateCombinations(Set<String> originalSet) {
        Set<Set<String>> combinations = new HashSet<>();

        // Convert the set to an array for easy indexing
        String[] elements = originalSet.toArray(new String[0]);

        // Iterate over all pairs of elements
        for (int i = 0; i < elements.length; i++) {
            for (int j = i + 1; j < elements.length; j++) {
                Set<String> combination = new HashSet<>();
                combination.add(elements[i]);
                combination.add(elements[j]);
                combinations.add(combination);
            }
        }

        return combinations;
    }
}
