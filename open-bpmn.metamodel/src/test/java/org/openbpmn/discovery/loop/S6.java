package org.openbpmn.discovery.loop;

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

public class S6 {
	private static Logger logger = Logger.getLogger(S6.class.getName());

	/**
	 * This test creates an empty BPMN model instance
	 * 
	 * @throws BPMNModelException
	 * @throws CloneNotSupportedException 
	 */
	@Test
	public void testInputProcess() throws BPMNModelException, CloneNotSupportedException {
		String path = "src/test/resources/discovery/process_1-empty-1.bpmn";
		LinkedList<String> list = new LinkedList<>();
		List<String> startsEvent = new ArrayList<>();
		Set<Set<String>> parallelRelations = new HashSet();
		DependencyGraph bpmnTransformation = new DependencyGraph();
		Map<String, Map<String, String>> elementsInfo = new HashMap<>();
		List<String> endEvents = new LinkedList();

		// start event
		String startEvent = "start";
		startsEvent.add(startEvent);

		// dependencies
		list.add("start->a");
		list.add("a->b");
		list.add("a->c");
		list.add("b->d");
		list.add("b->f");
		list.add("c->d");
		list.add("c->f");
		list.add("f->end");
		list.add("d->end");

		// parallelism
		parallelRelations.add(new HashSet<>() {
			{
				add("d");
				add("f");
			}
		});

		// elements info
		// start/end/human/service
		Map<String, String> ele1 = new HashMap<>();
		ele1.put("type", "start");
		elementsInfo.put(startEvent, ele1);

		Map<String, String> ele2 = new HashMap<>();
		ele2.put("type", "end");
		elementsInfo.put("end", ele2);

		// extract dependencies
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
		// find loops
		bpmnTransformation.findAndRemoveLoops();

		BPMNDiscovery bpmnDiscovery = new BPMNDiscovery(bpmnTransformation);
		bpmnDiscovery.DependencyGraphToBPMN();
		bpmnDiscovery.saveMode(path);

	}
}