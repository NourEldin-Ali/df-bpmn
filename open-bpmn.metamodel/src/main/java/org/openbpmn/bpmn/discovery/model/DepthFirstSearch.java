package org.openbpmn.bpmn.discovery.model;

import java.util.*;

public class DepthFirstSearch {
	 public static List<Pair> dfs(Map<String, List<String>> graph, String start) {
	        Set<String> visited = new HashSet<>();
	        List<Pair> pairs = new ArrayList<>();
	        dfsHelper(graph, start, visited, pairs);
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
