package org.openbpmn.discovery;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import org.openbpmn.bpmn.discovery.BPMNDiscovery;
import org.openbpmn.bpmn.discovery.model.DependencyGraph;
import org.openbpmn.bpmn.exceptions.BPMNModelException;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class LogInspector {
	private static Logger logger = Logger.getLogger(LogInspector.class.getName());

	/**
	 * This test creates an empty BPMN model instance
	 * 
	 * @throws BPMNModelException
	 * @throws CloneNotSupportedException
	 */
	@Test
	public void loopInput() throws BPMNModelException, CloneNotSupportedException {
		String path = "src/test/resources/discovery/test.bpmn";
		String startEventString = "Order read";
		String dependencyRelationsString = "[\"Order read -> Read order from automatic order management system\", \"Read order from automatic order management system -> Check if first product is in stock\", \"Check if first product is in stock -> Withdraw product from warehouse\", \"Check if first product is in stock -> Reorder product from wholesaler\", \"Withdraw product from warehouse -> Register ordered product in stock management system\", \"Reorder product from wholesaler -> Demand delivery delay penalty\", \"Demand delivery delay penalty -> Register ordered product in stock management system\", \"Register ordered product in stock management system -> Verify if entire order is ready for shipment\", \"Verify if entire order is ready for shipment -> Request courier\", \"Verify if entire order is ready for shipment -> Pack products\", \"Request courier -> Ship products\", \"Pack products -> Ship products\", \"Ship products -> Products shipped\", \"Demand delivery delay penalty -> Delivery delay penalty demanded\", \"Verify if entire order is ready for shipment -> Check if first product is in stock\"]";
		String parallelRelationString = "[[\"Request courier\", \"Pack products\"]]";
		String elementInfoString = "{\"Order read\": {\"type\": \"start\", \"participant\": \"Automatic Order Management System\"}, \"Products shipped\": {\"type\": \"end\", \"participant\": \"Courier\"}, \"Delivery delay penalty demanded\": {\"type\": \"end\", \"participant\": \"Wholesaler\"}, \"Read order from automatic order management system\": {\"type\": \"service\", \"participant\": \"Automatic Order Management System\"}, \"Check if first product is in stock\": {\"type\": \"service\", \"participant\": \"Warehouse\"}, \"Withdraw product from warehouse\": {\"type\": \"service\", \"participant\": \"Warehouse\"}, \"Reorder product from wholesaler\": {\"type\": \"service\", \"participant\": \"Wholesaler\"}, \"Demand delivery delay penalty\": {\"type\": \"service\", \"participant\": \"Wholesaler\"}, \"Register ordered product in stock management system\": {\"type\": \"service\", \"participant\": \"Stock Management System\"}, \"Verify if entire order is ready for shipment\": {\"type\": \"service\", \"participant\": \"\"}, \"Request courier\": {\"type\": \"service\", \"participant\": \"Courier\"}, \"Pack products\": {\"type\": \"service\", \"participant\": \"\"}, \"Ship products\": {\"type\": \"service\", \"participant\": \"\"}}";

		List<String> startsEvents = new ArrayList<>();
		startsEvents.add(startEventString);

//		List<String> endEvents = stringToList(endEventsString);
//		LinkedList<String> events = stringToList(dependencyRelationsString);
//		Set<Set<String>> parallelRelations = transformStringToList(parallelRelationString);

		Gson gson = new Gson();

		// Define the type of the data structure for deserialization
		Type listOfListsType = new TypeToken<Set<Set<String>>>() {
		}.getType();
		Type listOfStringsType = new TypeToken<List<String>>() {
		}.getType();
		Type listOfMapType = new TypeToken<Map<String, Map<String, String>>>() {
		}.getType();
		// Deserialize JSON to Java objects
		List<String> events = gson.fromJson(dependencyRelationsString, listOfStringsType);
		Set<Set<String>> parallelRelations = gson.fromJson(parallelRelationString, listOfListsType);
		Map<String, Map<String, String>> elementsInfo = gson.fromJson(elementInfoString, listOfMapType);
		List<String> endEvents = new LinkedList();
		for (Map.Entry<String, Map<String, String>> entry : elementsInfo.entrySet()) {
			if (entry.getValue().get("type").contentEquals("end")) {
				endEvents.add(entry.getKey());
			}
		}
		// Initialize the HashMap
		Map<String, String> elementsName = new HashMap();
		DependencyGraph bpmnTransformation = new DependencyGraph();
		for (String dependency : events) {
			// Split the dependency string into source and target
			String[] parts = dependency.split("->");

			// Extract source and target
			String sourceId = DependencyGraph.regex(parts[0]);
			String targetId = DependencyGraph.regex(parts[1]);

			elementsName.put(sourceId, parts[0]);
			elementsName.put(targetId, parts[1]);

			bpmnTransformation.addVertex(sourceId);
			bpmnTransformation.addVertex(targetId);
			bpmnTransformation.addEdge(sourceId, targetId);
		}
		System.out.println(events);
		bpmnTransformation.startActivities = startsEvents;
		bpmnTransformation.endActivities = endEvents;
		bpmnTransformation.parallelism = parallelRelations;
		bpmnTransformation.elementInformations = elementsInfo;
		bpmnTransformation.elementsName = elementsName;
		bpmnTransformation.changeVertexNameToRegex();
		bpmnTransformation.regexOnElementInfo();
		bpmnTransformation.findAndRemoveLoops();
		System.out.println(bpmnTransformation.loops);
		System.out.println(bpmnTransformation.getLoops());

		BPMNDiscovery bpmnDiscovery = new BPMNDiscovery(bpmnTransformation);
		bpmnDiscovery.DependencyGraphToBPMN();
		bpmnDiscovery.saveMode(path);

	}
}