package org.openbpmn.bpmn.discovery.model;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.DirectedWeightedPseudograph;

import java.util.*;
import java.util.stream.Collectors;

public class LoopMerger {
    private Set<List<String>> loops;
    private DirectedWeightedPseudograph<String, DefaultWeightedEdge> dependencyGraph;

    public LoopMerger(Set<List<String>> loops, DirectedWeightedPseudograph<String, DefaultWeightedEdge> dependencyGraph) {
        this.loops = loops;
        this.dependencyGraph = dependencyGraph;
    }

    public List<Pair<Set<String>, Set<String>>> getMergedLoop(){
//        System.out.println("before merge");
//        System.out.println(this.loops);
        //merge loops by source
        List<Pair<String, Set<String>>> mergetBySource = mergeLoopsBySource();
//        System.out.println("after  by source");
//        System.out.println(mergetBySource);
        //merge loops by target
        List<Pair<Set<String>, Set<String>>> mergetloop = mergeLoopsByTarget(mergetBySource);
//        System.out.println(mergetloop);
        //sort the loops
        sortLoop(mergetloop);
        return mergetloop;
    }



    public List<Pair<String, Set<String>>> mergeLoopsBySource() {
        List<Pair<String, Set<String>>> mergedLoops = new ArrayList<>();
        Set<List<String>> loopsCopy = new HashSet<>(loops); // Make a copy to avoid concurrent modification


        for (List<String> loop : loops) {
            if (!loopsCopy.contains(loop)) {
                continue; // Skip if already processed
            }

            String source = loop.get(0); // Assuming the first element is the source
            String target = loop.get(1); // Assuming the first element is the target
            Set<String> targets = new HashSet<>();
            targets.add(target); // Assuming the last element is the target

            Iterator<List<String>> it = loopsCopy.iterator();
            while (it.hasNext()) {
                List<String> otherLoop = it.next();
                if (otherLoop == loop) {
                    continue; // Skip the same loop
                }

                String otherSource = otherLoop.get(0);
                String otherTarget = otherLoop.get(1);

                if (source.contentEquals(otherSource) && DependencyGraph.haveIntersectPredecessors(dependencyGraph, target, otherTarget)) {
                    targets.add(otherTarget);
                    it.remove(); // Remove the merged loop
                }
            }

            mergedLoops.add(new Pair<>(source, targets));
            loopsCopy.remove(loop); // Remove the processed loop
        }

        return mergedLoops;
    }


    public List<Pair<Set<String>,Set<String>>> mergeLoopsByTarget(List<Pair<String, Set<String>>> mergedLoopBySource) {
        List<Pair<Set<String>, Set<String>>> mergedLoops = new ArrayList<>();
        while (!mergedLoopBySource.isEmpty()) {
            Pair<String, Set<String>> l = mergedLoopBySource.get(0);
            Set<String> sources = new HashSet<>();
            sources.add(l.getSource());
            if(l.getTarget().size()==1 && l.getSource().contentEquals(l.getTarget().iterator().next())){
                mergedLoops.add(Pair.of(sources, l.getTarget()));
                mergedLoopBySource.remove(l); // Remove l from mergedLoopBySource
                continue;
            }
            Iterator<Pair<String, Set<String>>> it = mergedLoopBySource.iterator();
            while (it.hasNext()) {
                Pair<String, Set<String>> lPrime = it.next();
                if (l == lPrime) continue; // Skip the same element

                if (l.getTarget().equals(lPrime.getTarget()) && DependencyGraph.haveIntersectSuccessors(dependencyGraph,l.getSource(), lPrime.getSource())) {
                    sources.add(lPrime.getSource());
                    it.remove(); // Remove l' from mergedLoopBySource
                }
            }

            mergedLoops.add(Pair.of(sources, l.getTarget()));
            mergedLoopBySource.remove(l); // Remove l from mergedLoopBySource
        }

        return mergedLoops;
    }

    public void sortLoop(List<Pair<Set<String>, Set<String>>> loop) {
        loop.sort(new Comparator<Pair<Set<String>, Set<String>>>() {
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

}
