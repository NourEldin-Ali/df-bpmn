package org.openbpmn.bpmn.discovery.model;

import java.util.*;

public class DepthFirstSearch {
	 public static List<Pair> dfs(Map<String, List<String>> graph, String start) {
	        Set<String> visited = new HashSet<>();
	        List<Pair> pairs = new ArrayList<>();
	        
	        // Ensure all nodes are included, even those not in the key set but present in values
	        Set<String> allNodes = new HashSet<>(graph.keySet());
	        for (List<String> neighbors : graph.values()) {
	            for (String neighbor : neighbors) {
	                allNodes.add(neighbor);
	            }
	        }
	        
	        dfsHelper(graph, start, visited, pairs);
	        // Start DFS from each unvisited node
	        for (String node : allNodes) {
	            if (!visited.contains(node)) {
	                dfsHelper(graph, node, visited, pairs);
	            }
	        }
	        return pairs;
	    }

	    private static void dfsHelper(Map<String, List<String>> graph, String node, Set<String> visited, List<Pair> pairs) {
	        visited.add(node);
	        List<String> neighbors = graph.get(node);
	        if (neighbors != null) {
	            for (String neighbor : neighbors) {
	                pairs.add(new Pair(node, neighbor));
	                if (!visited.contains(neighbor)) {
	                    dfsHelper(graph, neighbor, visited, pairs);
	                }
	            }
	        }
	    }
	    
	    
	    


	    static class Pair {
	    	String first;
	    	String second;

	        Pair(String first, String second) {
	            this.first = first;
	            this.second = second;
	        }

	        @Override
	        public String toString() {
	            return "(" + first + ", " + second + ")";
	        }
	    }
	    
	    
	    public static  LinkedList<String> DFSToList(LinkedList<String> events, String startEvent){
	    	LinkedList<String> result = new LinkedList();
	    	List<Pair> pairs = dfs(RelationConverter.relationsToMap(events),startEvent);
	        for (Pair pair : pairs) {
	        	result.add(pair.first+"->"+pair.second);
	        }
	        return result;
	    }
	    
	    
}
