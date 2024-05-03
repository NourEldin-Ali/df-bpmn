package org.openbpmn.bpmn.discovery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * This function is to convert list of relations to map of relations, for
 * example: input: ["a->b", "a->c", "d->e", "b->k", "c->e", "a->d"] output:
 * {'a': ['b', 'c', 'd'], 'd': ['e'], 'b': ['k'], 'c': ['e']}
 * 
 * @author Ali Nour Eldin
 *
 */
public class RelationConverter {
	// Method to reorder LinkedHashMap
	private static void reorderLinkedHashMap(LinkedHashMap<String, List<String>> linkedHashMap,
			String keyToMoveToFront) {
		LinkedHashMap<String, List<String>> relationsMap = new LinkedHashMap<>();
		
		if (linkedHashMap.containsKey(keyToMoveToFront)) {
			List<String> value = linkedHashMap.remove(keyToMoveToFront); // Remove the key-value pair
			relationsMap.put(keyToMoveToFront, value); // Reinsert the key-value pair to the front
		}
		for(String key: linkedHashMap.keySet()) {
			relationsMap.put(key, linkedHashMap.get(key));
		}
		linkedHashMap.clear();
		linkedHashMap.putAll(relationsMap);
	}

	public static Map<String, List<String>> relationsToMap(List<String> relations, String startEvent) {
		LinkedHashMap<String, List<String>> relationsMap = new LinkedHashMap<>();
		for (String relation : relations) {
			String[] parts = relation.split("->");
			String start = parts[0];
			String end = parts[1];
			if (!relationsMap.containsKey(start)) {
				relationsMap.put(start, new ArrayList<>());
			}
			relationsMap.get(start).add(end);
		}
		reorderLinkedHashMap(relationsMap, startEvent);
		return relationsMap;
	}
	
	public static Map<String, List<String>> relationsToMap(List<String> relations) {
		Map<String, List<String>> graph = new HashMap<>();
		for (String relation : relations) {
			String[] parts = relation.split("->");
			String start = parts[0];
			String end = parts[1];
			if (!graph.containsKey(start)) {
				 graph.put(start, new ArrayList<>());
			}
			graph.get(start).add(end);
		}
		return graph;
	}
}
