package org.openbpmn.discovery.noloop;

import org.junit.jupiter.api.Test;
import org.openbpmn.bpmn.discovery.BPMNDiscovery;
import org.openbpmn.bpmn.discovery.compare.BPMNComparatorExecutor;
import org.openbpmn.bpmn.discovery.model.DecisionMerger;
import org.openbpmn.bpmn.discovery.model.DependencyGraph;
import org.openbpmn.bpmn.discovery.model.LoopMerger;
import org.openbpmn.bpmn.discovery.model.ParallelismMerger;
import org.openbpmn.bpmn.exceptions.BPMNModelException;

import java.util.*;
import java.util.logging.Logger;

// https://bonitasoft.atlassian.net/wiki/spaces/TRI/pages/23300964405/BPMN+without+loops#M0
public class M7 {
	private static Logger logger = Logger.getLogger(M7.class.getName());

	/**
	 * This test creates an empty BPMN model instance
	 * 
	 * @throws BPMNModelException
	 * @throws CloneNotSupportedException 
	 */
	@Test
	public void testInputProcess() throws BPMNModelException, CloneNotSupportedException {
		logger.info("M7.bpmn done: Start generating model");
		String path = "src/test/resources/discovery/noloop/m7_results.bpmn";
		LinkedList<String> list = new LinkedList<>();
		List<String> startsEvent = new ArrayList<>();
		Set<Set<String>> parallelRelations = new HashSet<>();
		DependencyGraph bpmnTransformation = new DependencyGraph();
		Map<String, Map<String, String>> elementsInfo = new HashMap<>();
		List<String> endEvents = new LinkedList<>();
		Map<String,String> elementsName = new HashMap<>();


		// start event
		String startEvent = "start";
		startsEvent.add(startEvent);

		// dependencies
		list.add("start->a");
		
		list.add("a->b");
		list.add("a->e");

		list.add("b->c");
		list.add("b->f");

		list.add("e->f");

		list.add("c->d");
		list.add("c->h");

		list.add("f->g");
		list.add("f->h");

		list.add("d->end");
		list.add("h->end");
		list.add("g->end");

	
		// elements info
		// start/end/human/service

		elementsInfo.put(startEvent, new HashMap<String, String>() {{ put("type", "start"); }});
		elementsInfo.put("end", new HashMap<String, String>() {{ put("type", "end"); }});

		//parallelism
		parallelRelations.add(new HashSet<>() {
			{
				add("b");
				add("e");
			}
		});
		parallelRelations.add(new HashSet<>() {
			{
				add("c");
				add("f");
			}
		});
		parallelRelations.add(new HashSet<>() {
			{
				add("d");
				add("h");
			}
		});
		parallelRelations.add(new HashSet<>() {
			{
				add("d");
				add("g");
			}
		});
		parallelRelations.add(new HashSet<>() {
			{
				add("h");
				add("g");
			}
		});

		// extract dependencies
		for (String dependency : list) {
			// Split the dependency string into source and target
			String[] parts = dependency.split("->");

			// Extract source and target
			String sourceId = parts[0].trim();
			String targetId = parts[1].trim();
			elementsName.put(sourceId, parts[0]);
			elementsName.put(targetId, parts[1]);
			bpmnTransformation.addVertex(sourceId);
			bpmnTransformation.addVertex(targetId);
			bpmnTransformation.addEdge(sourceId, targetId);
			
		}

		// extract end events
		for (Map.Entry<String, Map<String, String>> entry : elementsInfo.entrySet()) {
			if (entry.getValue().get("type").contentEquals("end")) {
				endEvents.add(entry.getKey());
			}
		}
		bpmnTransformation.endActivities = endEvents;
		bpmnTransformation.startActivities = startsEvent;
		bpmnTransformation.parallelism = parallelRelations;
		bpmnTransformation.elementInformations = elementsInfo;
		bpmnTransformation.elementsName = elementsName;
		// find loops
		bpmnTransformation.findAndRemoveLoops();
		bpmnTransformation.removeParallelism();
		bpmnTransformation.findInclusive();
		bpmnTransformation.findExculisve();



		System.out.println(bpmnTransformation.loops);
		System.out.println(bpmnTransformation.getLoops());

		LoopMerger loopMerger = new LoopMerger(bpmnTransformation.loops, bpmnTransformation.dependencyGraph);
		System.out.println(loopMerger.getMergedLoop());


		//get exclusive
		DecisionMerger decisionMerger = new DecisionMerger(bpmnTransformation.exlusive, bpmnTransformation.dependencyGraph);
		LinkedList<LinkedList<String>> decisionRelations = decisionMerger.getDecisions();
			System.out.println(decisionRelations);
		//get parallelism
		ParallelismMerger parallelismMerger = new ParallelismMerger(bpmnTransformation.parallelism,
				bpmnTransformation.dependencyGraph);
		LinkedList<LinkedList<String>> parallelMergeRelations = parallelismMerger.getParallelims();
		System.out.println("parallelMergeRelations");
		System.out.println(parallelMergeRelations);
		//get inclusive
		ParallelismMerger inclusiveMerger = new ParallelismMerger(bpmnTransformation.inclusive,
				bpmnTransformation.dependencyGraph);
		LinkedList<LinkedList<String>> inclusiveRelations = inclusiveMerger.getParallelims();


		Map<String, LinkedList<LinkedList<String>>> relations = new LinkedHashMap<>();
		relations.put(BPMNDiscovery.DECISION, decisionRelations);
		relations.put(BPMNDiscovery.PARALLEL, parallelMergeRelations);
		relations.put(BPMNDiscovery.INCLUSIVE, inclusiveRelations);

//		System.out.println(relations);



		BPMNDiscovery bpmnDiscovery = new BPMNDiscovery(bpmnTransformation);
		bpmnDiscovery.DependencyGraphToBPMN();
		bpmnDiscovery.saveMode(path);




		//compaire the two models
		boolean results = BPMNComparatorExecutor.execute(path, "src/main/resources/discovery/noloop/M7.bpmn");
		if(!results){
			logger.warning("M7.bpmn: The two models are not equivalent");
		}else{
			logger.info("M7.bpmn done: The two models are equivalent");
		}

		
	}
}