package org.openbpmn.bpmn.discovery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openbpmn.bpmn.discovery.model.DependencyGraph;
import org.openbpmn.bpmn.discovery.model.DepthFirstSearch;
import org.openbpmn.bpmn.discovery.model.Pair;
import org.openbpmn.bpmn.elements.core.BPMNElementNode;
import org.openbpmn.bpmn.exceptions.BPMNModelException;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

public class BPMNTransformation {

	public static void main(String[] args) {
		// Start timing
		long startTime = System.nanoTime();
//		System.out.println(args.length);
		if (args.length == 5) {
			/**
			 * "start_a" "[end_f,end_b]" "[a->b, a->c, a->d, a->e, c->f, d->f, e->f,
			 * start_a->a, b->end_b, f->end_f]" "[[b, c, d, e], [c, d]]" "[[c, e, d]]"
			 */
			String output = args[0];
			String startEventString = args[1];
//			String endEventsString = args[2];
			String dependencyRelationsString = args[2];
			String parallelRelationString = args[3];
			String elementInfoString = args[4];

			List<String> startsEvents = new ArrayList<>();
			startsEvents.add(startEventString);

//			List<String> endEvents = stringToList(endEventsString);
//			LinkedList<String> events = stringToList(dependencyRelationsString);
//			Set<Set<String>> parallelRelations = transformStringToList(parallelRelationString);

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

			DependencyGraph bpmnTransformation = new DependencyGraph();
			for (String dependency : events) {
				// Split the dependency string into source and target
				String[] parts = dependency.split("->");

				// Extract source and target
				String sourceId = parts[0].trim();
				String targetId = parts[1].trim();
				bpmnTransformation.addVertex(sourceId);
				bpmnTransformation.addVertex(targetId);
				bpmnTransformation.addEdge(sourceId, targetId);
			}
			bpmnTransformation.startActivities = startsEvents;
			bpmnTransformation.endActivities = endEvents;
			bpmnTransformation.parallelism = parallelRelations;
			bpmnTransformation.elementInformations = elementsInfo;
			bpmnTransformation.findAndRemoveLoops();

			try {
				BPMNDiscovery bpmnDiscovery = new BPMNDiscovery(bpmnTransformation);
				bpmnDiscovery.DependencyGraphToBPMN();
				bpmnDiscovery.saveMode(output);
			} catch (BPMNModelException e) {
				e.printStackTrace();
			} catch (CloneNotSupportedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			System.out.println(
					"INVALID INPUTS: outputPath (/src/example.bpmn), startEvent, endEvents (List), dependencyRelations(List), parallelRelation (List[List])");
			// correct
//			example1();
			// correct
			// example2();
			// correct
			// example3();
			// correct
			// example4();
			// double loop
//			example5();
//			 example6();
			// three loops
//			example0();
			example8();
//			example0();
		}
		long endTime = System.nanoTime();
		long duration = (endTime - startTime) / 1_000_000; // Convert nanoseconds to milliseco
		System.out.println("Execution time: " + duration + " ms");
	}

	static String path = "C:\\Users\\AliNourEldin\\Desktop\\testt.bpmn";

	private static void example0() {
		String output = path;
		String startEventString = "Client application not submitted";
		String dependencyRelationsString = "[\"Client application not submitted -> Submit application\", \"Submit application -> Receive application\", \"Receive application -> Review application for completeness and accuracy\", \"Review application for completeness and accuracy -> Send application back for correction\", \"Send application back for correction -> Submit application\", \"Review application for completeness and accuracy -> Check application against regulatory requirements\", \"Check application against regulatory requirements -> Reject application\", \"Reject application -> Inform client of rejection\", \"Inform client of rejection -> Application rejected\", \"Check application against regulatory requirements -> Create account\", \"Create account -> Notify client of successful account creation\", \"Notify client of successful account creation -> Account successfully created\", \"Submit application -> Application withdrawn\", \"Receive application -> Application withdrawn\", \"Review application for completeness and accuracy -> Application withdrawn\", \"Send application back for correction -> Application withdrawn\", \"Check application against regulatory requirements -> Application withdrawn\", \"Reject application -> Application withdrawn\", \"Inform client of rejection -> Application withdrawn\", \"Create account -> Application withdrawn\", \"Notify client of successful account creation -> Application withdrawn\"]";
		String parallelRelationString = "[[\"Receive application\", \"Application withdrawn\"], [\"Review application for completeness and accuracy\", \"Application withdrawn\"], [\"Send application back for correction\", \"Application withdrawn\"], [\"Check application against regulatory requirements\", \"Application withdrawn\"], [\"Submit application\", \"Application withdrawn\"], [\"Reject application\", \"Application withdrawn\"], [\"Create account\", \"Application withdrawn\"], [\"Inform client of rejection\", \"Application withdrawn\"], [\"Application rejected\", \"Application withdrawn\"], [\"Notify client of successful account creation\", \"Application withdrawn\"], [\"Account successfully created\", \"Application withdrawn\"]]";
		String elementInfoString = "{\"Client application not submitted\": {\"type\": \"start\", \"participant\": \"Client\"}, \"Account successfully created\": {\"type\": \"end\", \"participant\": \"Customer Service Department\"}, \"Application rejected\": {\"type\": \"end\", \"participant\": \"Compliance Department\"}, \"Application withdrawn\": {\"type\": \"end\", \"participant\": \"Client\"}, \"Submit application\": {\"type\": \"human\", \"participant\": \"Client\"}, \"Receive application\": {\"type\": \"human\", \"participant\": \"Customer Service Department\"}, \"Review application for completeness and accuracy\": {\"type\": \"human\", \"participant\": \"Customer Service Department\"}, \"Send application back for correction\": {\"type\": \"human\", \"participant\": \"Customer Service Department\"}, \"Check application against regulatory requirements\": {\"type\": \"human\", \"participant\": \"Compliance Department\"}, \"Reject application\": {\"type\": \"human\", \"participant\": \"Compliance Department\"}, \"Inform client of rejection\": {\"type\": \"human\", \"participant\": \"Compliance Department\"}, \"Create account\": {\"type\": \"service\", \"participant\": \"Compliance Department\"}, \"Notify client of successful account creation\": {\"type\": \"service\", \"participant\": \"Compliance Department\"}}";

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

		DependencyGraph bpmnTransformation = new DependencyGraph();
		for (String dependency : events) {
			// Split the dependency string into source and target
			String[] parts = dependency.split("->");

			// Extract source and target
			String sourceId = parts[0].trim();
			String targetId = parts[1].trim();
			bpmnTransformation.addVertex(sourceId);
			bpmnTransformation.addVertex(targetId);
			bpmnTransformation.addEdge(sourceId, targetId);
		}
		bpmnTransformation.startActivities = startsEvents;
		bpmnTransformation.endActivities = endEvents;
		bpmnTransformation.parallelism = parallelRelations;
		bpmnTransformation.elementInformations = elementsInfo;
		bpmnTransformation.findAndRemoveLoops();

		try {
			BPMNDiscovery bpmnDiscovery = new BPMNDiscovery(bpmnTransformation);
			System.out.println(bpmnDiscovery.dependencies);
			bpmnDiscovery.DependencyGraphToBPMN();
			bpmnDiscovery.saveMode(output);
		} catch (BPMNModelException e) {
			e.printStackTrace();
		} catch (CloneNotSupportedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	// the ex use to describe the algo in the confluence page
	private static void example1() {
		LinkedList<String> events = new LinkedList();
		events.add("start->a");

		events.add("a->b");
		events.add("a->c");
		events.add("a->d");
		events.add("a->e");

		events.add("b->end");

		events.add("d->f");
		events.add("c->f");

		events.add("e->f");

		events.add("f->end1");

		events.add("a->g");
		events.add("g->f");
//		events.add("f->a");

		String startEvent = "start";

		LinkedList<String> orderedEvents = DepthFirstSearch.DFSToList(events, startEvent);
		System.out.println(orderedEvents);
		List<String> startsEvent = new ArrayList<>();
		startsEvent.add("start");

		List<String> endEvents = new ArrayList<>();
		endEvents.add("end");
		endEvents.add("end1");

		LinkedList<LinkedList<String>> decisionRelations = new LinkedList();
		decisionRelations.add(new LinkedList<String>() {
			{
				add("b");
				add("c");
				add("d");
				add("e");
				add("g");
			}
		});
		decisionRelations.add(new LinkedList<String>() {
			{
				add("c");
				add("d");
			};

		});

		LinkedList<LinkedList<String>> parallelRelations = new LinkedList();
		parallelRelations.add(new LinkedList<String>() {
			{
				add("c");
				add("e");
				add("d");
			}
		});

		Map<String, LinkedList<LinkedList<String>>> relations = new LinkedHashMap<>();
		relations.put(BPMNDiscovery.DECISION, decisionRelations);
		relations.put(BPMNDiscovery.PARALLEL, parallelRelations);
		// Initialize the HashMap
		LinkedList<Pair<List<String>, List<String>>> loops = new LinkedList<>();

		try {
			BPMNDiscovery bpmnDiscovery = new BPMNDiscovery(startsEvent, endEvents, orderedEvents, relations, loops);
			bpmnDiscovery.DependencyGraphToBPMN();
			BPMNElementNode gateway = bpmnDiscovery.model.findElementNodeById("gt-3");
			System.out.println(gateway.getType());

			System.out.println("Finish discovery ...");
			bpmnDiscovery.saveMode(path);
		} catch (BPMNModelException e) {
			e.printStackTrace();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
	}

	// ex1 in tested on in the page confluence
	private static void example2() {
		LinkedList<String> events = new LinkedList();
		events.add("start->a");
		events.add("a->b");
		events.add("a->c");

		events.add("b->d");
		events.add("b->g");

		events.add("c->f");
		events.add("c->e");

		events.add("d->f");
		events.add("e->g");

		events.add("f->h");
		events.add("g->h");
		events.add("h->end");

		String startEvent = "start";

		LinkedList<String> orderedEvents = DepthFirstSearch.DFSToList(events, startEvent);
		System.out.println(orderedEvents);
		List<String> startsEvent = new ArrayList<>();
		startsEvent.add("start");

		List<String> endEvents = new ArrayList<>();
		endEvents.add("end");

		LinkedList<LinkedList<String>> decisionRelations = new LinkedList();
		decisionRelations.add(new LinkedList<String>() {
			{
				add("c");
				add("b");
			}

		});

		LinkedList<LinkedList<String>> parallelRelations = new LinkedList();
		parallelRelations.add(new LinkedList<String>() {
			{
				add("d");
				add("g");
			}

		});
		parallelRelations.add(new LinkedList<String>() {
			{
				add("e");
				add("f");
			}

		});

		Map<String, LinkedList<LinkedList<String>>> relations = new LinkedHashMap<>();
		relations.put(BPMNDiscovery.DECISION, decisionRelations);
		relations.put(BPMNDiscovery.PARALLEL, parallelRelations);

		// Initialize the HashMap
		LinkedList<Pair<List<String>, List<String>>> loops = new LinkedList<>();

		try {
			BPMNDiscovery bpmnDiscovery = new BPMNDiscovery(startsEvent, endEvents, orderedEvents, relations, loops);
			bpmnDiscovery.DependencyGraphToBPMN();
			bpmnDiscovery.saveMode(path);
		} catch (BPMNModelException | CloneNotSupportedException e) {
			e.printStackTrace();
		}
	}

	// ex2 in tested on in the page confluence
	private static void example3() {
		LinkedList<String> events = new LinkedList();
		events.add("start->a");
		events.add("start->b");

		events.add("a->c");
		events.add("a->j");

		events.add("b->d");
		events.add("b->j");

		events.add("c->i");
		events.add("j->i");

		events.add("d->k");
		events.add("i->k");

		events.add("k->end");

		String startEvent = "start";

		LinkedList<String> orderedEvents = DepthFirstSearch.DFSToList(events, startEvent);
		System.out.println(orderedEvents);
		List<String> startsEvent = new ArrayList<>();
		startsEvent.add("start");

		List<String> endEvents = new ArrayList<>();
		endEvents.add("end");

		LinkedList<LinkedList<String>> decisionRelations = new LinkedList();
		decisionRelations.add(new LinkedList<String>() {
			{
				add("c");
				add("b");
			}

		});
		decisionRelations.add(new LinkedList<String>() {
			{
				add("c");
				add("j");
			}

		});
		decisionRelations.add(new LinkedList<String>() {
			{
				add("d");
				add("j");
			}

		});

		LinkedList<LinkedList<String>> parallelRelations = new LinkedList();

		Map<String, LinkedList<LinkedList<String>>> relations = new LinkedHashMap<>();
		relations.put(BPMNDiscovery.DECISION, decisionRelations);
		relations.put(BPMNDiscovery.PARALLEL, parallelRelations);

		// Initialize the HashMap
		LinkedList<Pair<List<String>, List<String>>> loops = new LinkedList<>();

		try {
			BPMNDiscovery bpmnDiscovery = new BPMNDiscovery(startsEvent, endEvents, orderedEvents, relations, loops);
			bpmnDiscovery.DependencyGraphToBPMN();
			bpmnDiscovery.saveMode(path);
		} catch (BPMNModelException e) {
			e.printStackTrace();
		} catch (CloneNotSupportedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// ex3 in tested on in the page confluence
	private static void example4() {
		LinkedList<String> events = new LinkedList();
		events.add("start->a");
		events.add("a->b");
		events.add("a->c");
		events.add("a->d");

		events.add("b->e");
		events.add("b->f");

		events.add("e->h");
		events.add("f->g");

		events.add("g->h");

		events.add("c->g");
		events.add("d->g");

		events.add("h->end");

		String startEvent = "start";

		LinkedList<String> orderedEvents = DepthFirstSearch.DFSToList(events, startEvent);
		System.out.println(orderedEvents);
		List<String> startsEvent = new ArrayList<>();
		startsEvent.add("start");

		List<String> endEvents = new ArrayList<>();
		endEvents.add("end");

		LinkedList<LinkedList<String>> decisionRelations = new LinkedList();
		decisionRelations.add(new LinkedList<String>() {
			{
				add("c");
				add("d");
			}

		});
		decisionRelations.add(new LinkedList<String>() {
			{
				add("e");
				add("f");
			}

		});

		LinkedList<LinkedList<String>> parallelRelations = new LinkedList();
		parallelRelations.add(new LinkedList<String>() {
			{
				add("b");
				add("c");
				add("d");
			}

		});

		Map<String, LinkedList<LinkedList<String>>> relations = new LinkedHashMap<>();
		relations.put(BPMNDiscovery.DECISION, decisionRelations);
		relations.put(BPMNDiscovery.PARALLEL, parallelRelations);

		// Initialize the HashMap
		LinkedList<Pair<List<String>, List<String>>> loops = new LinkedList<>();

		try {
			BPMNDiscovery bpmnDiscovery = new BPMNDiscovery(startsEvent, endEvents, orderedEvents, relations, loops);
			bpmnDiscovery.DependencyGraphToBPMN();
			bpmnDiscovery.saveMode(path);
		} catch (BPMNModelException | CloneNotSupportedException e) {
			e.printStackTrace();
		}
	}

	// double loop
	private static void example5() {
		LinkedList<String> events = new LinkedList();
		events.add("start->a");

		events.add("a->b");
		events.add("a->c");
		events.add("a->d");
		events.add("a->e");

		events.add("b->end");

		events.add("d->f");
		events.add("c->f");

		events.add("e->f");

		events.add("f->end1");

		events.add("a->g");
		events.add("g->f");

		// loop
		events.add("c->a");
		events.add("f->a");

		String startEvent = "start";

		LinkedList<String> orderedEvents = DepthFirstSearch.DFSToList(events, startEvent);
		System.out.println(orderedEvents);
		List<String> startsEvent = new ArrayList<>();
		startsEvent.add("start");

		List<String> endEvents = new ArrayList<>();
		endEvents.add("end");
		endEvents.add("end1");

		LinkedList<LinkedList<String>> decisionRelations = new LinkedList();
		decisionRelations.add(new LinkedList<String>() {
			{
				add("b");
				add("c");
				add("d");
				add("e");
				add("g");
			}
		});
		decisionRelations.add(new LinkedList<String>() {
			{
				add("c");
				add("d");
			};

		});

		LinkedList<LinkedList<String>> parallelRelations = new LinkedList();
		parallelRelations.add(new LinkedList<String>() {
			{
				add("c");
				add("d");
				add("e");
				add("g");
			}
		});

		Map<String, LinkedList<LinkedList<String>>> relations = new LinkedHashMap<>();
		relations.put(BPMNDiscovery.DECISION, decisionRelations);
		relations.put(BPMNDiscovery.PARALLEL, parallelRelations);

//		try {
		BPMNDiscovery bpmnDiscovery = new BPMNDiscovery(startsEvent, endEvents, orderedEvents, relations);
		System.out.println(bpmnDiscovery.loops);
//			bpmnDiscovery.DependencyGraphToBPMN();
//			bpmnDiscovery.saveMode(path);
//		} catch (BPMNModelException | CloneNotSupportedException e) {
//			e.printStackTrace();
//		}
	}

	// exercice nour
	private static void example6() {
		LinkedList<String> events = new LinkedList();
		events.add("start->a");

		events.add("a->b");
		events.add("b->g");
		events.add("g->i");
		events.add("i->end");

		events.add("a->c");
		events.add("c->g");
		events.add("c->h");

		events.add("h->end1");

		events.add("a->d");
		events.add("d->e");
		events.add("e->end2");

		String startEvent = "start";

		LinkedList<String> orderedEvents = DepthFirstSearch.DFSToList(events, startEvent);
		System.out.println(orderedEvents);
		List<String> startsEvent = new ArrayList<>();
		startsEvent.add("start");

		List<String> endEvents = new ArrayList<>();
		endEvents.add("end");
		endEvents.add("end1");
		endEvents.add("end2");

		LinkedList<LinkedList<String>> decisionRelations = new LinkedList();
		decisionRelations.add(new LinkedList<String>() {
			{
				add("b");
				add("c");
			}
		});
		decisionRelations.add(new LinkedList<String>() {
			{
				add("g");
				add("h");
			};

		});

		LinkedList<LinkedList<String>> parallelRelations = new LinkedList();
		parallelRelations.add(new LinkedList<String>() {
			{
				add("b");
				add("c");
				add("d");
			}
		});
//		parallelRelations.add(new LinkedList<String>() {
//			{
//				add("c");
//				
//			}
//		});

		Map<String, LinkedList<LinkedList<String>>> relations = new LinkedHashMap<>();
		relations.put(BPMNDiscovery.DECISION, decisionRelations);
		relations.put(BPMNDiscovery.PARALLEL, parallelRelations);
		// Initialize the HashMap
		LinkedList<Pair<List<String>, List<String>>> loops = new LinkedList<>();

		try {
			BPMNDiscovery bpmnDiscovery = new BPMNDiscovery(startsEvent, endEvents, orderedEvents, relations, loops);
			bpmnDiscovery.DependencyGraphToBPMN();
			bpmnDiscovery.saveMode(path);
		} catch (BPMNModelException e) {
			e.printStackTrace();
		} catch (CloneNotSupportedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// three loops
	private static void example7() {

		List<String> startsEvent = new ArrayList<>();
		startsEvent.add("Client without bank account");

		List<String> transitions = new ArrayList<>();

		transitions.add("Client without bank account -> Submit application");
		transitions.add("Submit application -> Review application");
		transitions.add("Review application -> Notify rejection");
		transitions.add("Notify rejection -> Account creation failed due to incomplete or incorrect information");

		transitions.add("Review application -> Verify application");
		transitions.add("Verify application -> Notify rejection");
		transitions.add("Notify rejection -> Account creation failed due to compliance issues");

		transitions.add("Verify application -> Create bank account");
		transitions.add("Create bank account -> Notify account creation");

		transitions.add("Notify account creation -> Account successfully created");

		Set<Set<String>> parallelRelations = new HashSet();

		Map<String, Map<String, String>> elementsInfo = new HashMap<>();
		Map<String, String> ele1 = new HashMap<>();
		ele1.put("type", "start");
		elementsInfo.put("Client without bank account", ele1);

		Map<String, String> ele2 = new HashMap<>();
		ele2.put("type", "end");
		elementsInfo.put("Account successfully created", ele2);
		
		ele2 = new HashMap<>();
		ele2.put("type", "end");
		elementsInfo.put("Account creation failed due to compliance issues", ele2);
		
		ele2 = new HashMap<>();
		ele2.put("type", "end");
		elementsInfo.put("Account creation failed due to incomplete or incorrect information", ele2);

		DependencyGraph bpmnTransformation = new DependencyGraph();
		for (String dependency : transitions) {
			// Split the dependency string into source and target
			String[] parts = dependency.split("->");

			// Extract source and target
			String sourceId = parts[0].trim();
			String targetId = parts[1].trim();
			bpmnTransformation.addVertex(sourceId);
			bpmnTransformation.addVertex(targetId);
			bpmnTransformation.addEdge(sourceId, targetId);
		}
		List<String> endEvents = new LinkedList();
		for (Map.Entry<String, Map<String, String>> entry : elementsInfo.entrySet()) {
			if (entry.getValue().get("type").contentEquals("end")) {
				endEvents.add(entry.getKey());
			}
		}
		bpmnTransformation.endActivities = endEvents;

		bpmnTransformation.startActivities = startsEvent;
		bpmnTransformation.parallelism = parallelRelations;

		bpmnTransformation.elementInformations = elementsInfo;
//		bpmnTransformation.findAndRemoveLoops();

		try {
			BPMNDiscovery bpmnDiscovery = new BPMNDiscovery(bpmnTransformation);
			System.out.println(bpmnDiscovery.dependenciesGraph.dependencyGraphWithLoop.toString());
//			bpmnDiscovery.dependenciesGraph.vertixLevel();
//			System.out.println(bpmnDiscovery.loops);
//			System.out.println(bpmnDiscovery.dependenciesGraph.toString());
			bpmnDiscovery.DependencyGraphToBPMN();
			bpmnDiscovery.saveMode(path);
		} catch (BPMNModelException e) {
			e.printStackTrace();
		} catch (CloneNotSupportedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// closed loops
	private static void example8() {
		LinkedList<String> list = new LinkedList<>();
		list.add("start->a");
		list.add("a->b");
		list.add("a->c");
		list.add("b->d");
		list.add("d->f");
		list.add("c->e");
		list.add("e->f");
		list.add("f->end");
		
		list.add("f->d");
		list.add("f->e");

		
		
		
		
		String startEvent = "start";

	
		List<String> startsEvent = new ArrayList<>();
		startsEvent.add(startEvent);

	
		
		Set<Set<String>> parallelRelations = new HashSet();
//		parallelRelations.add(new HashSet<>() {
//			{
//				add("c");
//				add("b");
//			}
//		});
		parallelRelations.add(new HashSet<>() {
			{
				add("d");
				add("e");
			}
		});
		Map<String, Map<String, String>> elementsInfo = new HashMap<>();
		Map<String, String> ele1 = new HashMap<>();
		ele1.put("type", "start");
		elementsInfo.put(startEvent, ele1);

		Map<String, String> ele2 = new HashMap<>();
		ele2.put("type", "end");
		elementsInfo.put("end", ele2);
		
		DependencyGraph bpmnTransformation = new DependencyGraph();
		for (String dependency : list) {
			// Split the dependency string into source and target
			String[] parts = dependency.split("->");

			// Extract source and target
			String sourceId = parts[0].trim();
			String targetId = parts[1].trim();
			bpmnTransformation.addVertex(sourceId);
			bpmnTransformation.addVertex(targetId);
			bpmnTransformation.addEdge(sourceId, targetId);
		}
		List<String> endEvents = new LinkedList();
		for (Map.Entry<String, Map<String, String>> entry : elementsInfo.entrySet()) {
			if (entry.getValue().get("type").contentEquals("end")) {
				endEvents.add(entry.getKey());
			}
		}
		bpmnTransformation.endActivities = endEvents;
		bpmnTransformation.startActivities = startsEvent;
		bpmnTransformation.parallelism = parallelRelations;
		bpmnTransformation.elementInformations = elementsInfo;
		bpmnTransformation.findAndRemoveLoops();

		try {
			BPMNDiscovery bpmnDiscovery = new BPMNDiscovery(bpmnTransformation);
			System.out.println(bpmnDiscovery.relations);
//			System.out.println(bpmnDiscovery.dependenciesGraph.dependencyGraphWithLoop.toString());
//			bpmnDiscovery.dependenciesGraph.vertixLevel();
			System.out.println(bpmnDiscovery.loops);
			System.out.println(bpmnDiscovery.dependencies);
			bpmnDiscovery.DependencyGraphToBPMN();
			bpmnDiscovery.saveMode(path);
		} catch (BPMNModelException e) {
			e.printStackTrace();
		} catch (CloneNotSupportedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
