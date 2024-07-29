package org.openbpmn.discovery.loop;

import java.util.*;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import org.openbpmn.bpmn.discovery.BPMNDiscovery;
import org.openbpmn.bpmn.discovery.model.DecisionMerger;
import org.openbpmn.bpmn.discovery.model.DependencyGraph;
import org.openbpmn.bpmn.discovery.model.LoopMerger;
import org.openbpmn.bpmn.discovery.model.ParallelismMerger;
import org.openbpmn.bpmn.exceptions.BPMNModelException;

public class AllLoops {
	private static Logger logger = Logger.getLogger(AllLoops.class.getName());

	/**
	 * This test creates an empty BPMN model instance
	 * 
	 * @throws BPMNModelException
	 * @throws CloneNotSupportedException 
	 */
	@Test
	public void testInputProcess() throws BPMNModelException, CloneNotSupportedException {
		String path = "src/test/resources/discovery/loop/test1.bpmn";
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
		list.add("a->a");
		list.add("b->end");
		
		list.add("a->c");
		list.add("c->z");
		list.add("z->c");
		list.add("a->d");
		
		list.add("c->e");
		list.add("c->f");
		list.add("e->e");
		list.add("f->f");
		list.add("f->e");
		list.add("e->f");
//		list.add("f->c");
		list.add("e->h");
		list.add("f->h");
		
		
		list.add("h->k");
		list.add("h->a");
		list.add("k->l");
		list.add("l->end_1");
		
		list.add("d->i");
		list.add("d->j");
		list.add("i->k");
		list.add("j->k");

		// parallelism
		parallelRelations.add(new HashSet<>() {
			{
				add("e");
				add("f");
			}
		});
		parallelRelations.add(new HashSet<>() {
			{
				add("i");
				add("j");
			}
		});
		parallelRelations.add(new HashSet<>() {
			{
				add("c");
				add("d");
			}
		});
	
		// elements info
		// start/end/human/service

		elementsInfo.put(startEvent, new HashMap<String, String>() {{ put("type", "start"); }});
		elementsInfo.put("end", new HashMap<String, String>() {{ put("type", "end"); }});
		elementsInfo.put("end_1", new HashMap<String, String>() {{ put("type", "end"); }});



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

		System.out.println(relations);



		BPMNDiscovery bpmnDiscovery = new BPMNDiscovery(bpmnTransformation);
		bpmnDiscovery.DependencyGraphToBPMN();
		bpmnDiscovery.saveMode(path);
		
	}
}