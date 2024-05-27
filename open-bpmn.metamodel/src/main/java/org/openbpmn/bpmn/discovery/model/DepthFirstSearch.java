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
	    
	    
	    
//	    public static void main(String[] args) {
//	       LinkedList<String> events = new LinkedList();
//			events.add("start->a");
//			events.add("a->b");
//			events.add("b->end");
//
//			events.add("a->c");
//			events.add("c->h");
//			events.add("h->end1");
//
//			events.add("a->d");
//			events.add("d->h");
//
//			events.add("a->e");
//			events.add("e->h");
//
//			events.add("a->f");
//			events.add("f->h");
//
//			events.add("a->g");
//			events.add("g->h");
//
//			
//	        List<Pair> pairs = dfs(RelationConverter.relationsToMap(events), "start");
//	        for (Pair pair : pairs) {
//	            System.out.println(pair.toString());
//	        }
//	    }
}
