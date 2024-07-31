package org.openbpmn.discovery.loop;

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

public class S1 {
	private static Logger logger = Logger.getLogger(S1.class.getName());

	/**
	 * This test creates an empty BPMN model instance
	 * 
	 * @throws BPMNModelException
	 * @throws CloneNotSupportedException 
	 */
	@Test
	public void testInputProcess() throws BPMNModelException, CloneNotSupportedException {
		logger.info("s1.bpmn done: Start generating model");
		String path = "src/test/resources/discovery/loop/s1_results.bpmn";
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
		list.add("a->d");
		list.add("a->e");
		list.add("a->f");

		list.add("b->c");


		list.add("c->g");
		list.add("d->g");
		list.add("e->g");
		list.add("f->g");

		list.add("c->b");
		list.add("d->b");
		list.add("e->b");
		list.add("f->b");

		list.add("c->d");
		list.add("d->d");
		list.add("e->d");
		list.add("f->d");

		list.add("c->e");
		list.add("d->e");
		list.add("e->e");
		list.add("f->e");

		list.add("c->f");
		list.add("d->f");
		list.add("e->f");
		list.add("f->f");

		list.add("g->end");


		// parallelism
		parallelRelations.add(new HashSet<>() {
			{
				add("e");
				add("f");
			}
		});

		// elements info
		// start/end/human/service

		elementsInfo.put(startEvent, new HashMap<String, String>() {{ put("type", "start"); }});
		elementsInfo.put("end", new HashMap<String, String>() {{ put("type", "end"); }});



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
		bpmnTransformation.findParallelism();
		bpmnTransformation.findInclusive();
		bpmnTransformation.findExculisve();



		System.out.println(bpmnTransformation.loops);
		System.out.println(bpmnTransformation.getLoops());

		LoopMerger loopMerger = new LoopMerger(bpmnTransformation.loops, bpmnTransformation.dependencyGraph);
		System.out.println(loopMerger.getMergedLoop());


		//get exclusive
		DecisionMerger decisionMerger = new DecisionMerger(bpmnTransformation.exlusive, bpmnTransformation.dependencyGraph);
		LinkedList<LinkedList<String>> decisionRelations = decisionMerger.getDecisions();

		//get parallelism
		ParallelismMerger parallelismMerger = new ParallelismMerger(bpmnTransformation.parallelism,
				bpmnTransformation.dependencyGraph);
		LinkedList<LinkedList<String>> parallelMergeRelations = parallelismMerger.getParallelims();

		//get inclusive
		ParallelismMerger inclusiveMerger = new ParallelismMerger(bpmnTransformation.inclusive,
				bpmnTransformation.dependencyGraph);
		LinkedList<LinkedList<String>> inclusiveRelations = inclusiveMerger.getParallelims();


		Map<String, LinkedList<LinkedList<String>>> relations = new LinkedHashMap<>();
		relations.put(BPMNDiscovery.DECISION, decisionRelations);
		relations.put(BPMNDiscovery.PARALLEL, parallelMergeRelations);
		relations.put(BPMNDiscovery.INCLUSIVE, inclusiveRelations);

//		System.out.println(relations);



//		BPMNDiscovery bpmnDiscovery = new BPMNDiscovery(bpmnTransformation);
//		bpmnDiscovery.DependencyGraphToBPMN();
//		bpmnDiscovery.saveMode(path);




		//compaire the two models
		boolean results = BPMNComparatorExecutor.execute(path, "src/main/resources/discovery/loop/s1.bpmn");
		if(!results){
			logger.warning("s1.bpmn: The two models are not equivalent");
		}else{
			logger.info("s1.bpmn done: The two models are equivalent");
		}

		
	}
}