package org.openbpmn.bpmn.discovery.compare;
import org.camunda.bpm.model.bpmn.instance.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class BPMNComparator {

    public static boolean compareElements(List<FlowElement> elements1, List<FlowElement> elements2) {
        Map<String, Integer> countMap1 = createCountMap(elements1);
        Map<String, Integer> countMap2 = createCountMap(elements2);

        return countMap1.equals(countMap2);
    }

    private static Map<String, Integer> createCountMap(List<FlowElement> elements) {
        Map<String, Integer> countMap = new HashMap<>();

        for (FlowElement element : elements) {
            String key = "";
            if (element instanceof Task) {
                key = "Task:" + element.getName();
            } else if (element instanceof Gateway) {
                key = "Gateway:" + element.getElementType().getTypeName();
            } else if (element instanceof StartEvent) {
                key = "StartEvent:" + element.getElementType().getTypeName();
            } else if (element instanceof EndEvent) {
                key = "EndEvent:" + element.getElementType().getTypeName();
            } else if (element instanceof IntermediateThrowEvent) {
                key = "IntermediateEvent:" + element.getElementType().getTypeName();
            } else if (element instanceof SubProcess) {
                key = "SubProcess:" + element.getName();
            }

            countMap.put(key, countMap.getOrDefault(key, 0) + 1);
        }

        return countMap;
    }

    public static boolean compareFlows(List<SequenceFlow> flows1, List<SequenceFlow> flows2) {
        if (flows1.size() != flows2.size()) {
            return false;
        }

        for (SequenceFlow flow1 : flows1) {
            boolean matchFound = false;
            for (SequenceFlow flow2 : flows2) {
                if (flow1.getSource().getElementType().getTypeName().equals(flow2.getSource().getElementType().getTypeName()) &&
                        flow1.getTarget().getElementType().getTypeName().equals(flow2.getTarget().getElementType().getTypeName())) {
                    matchFound = true;
                    break;
                }
            }
            if (!matchFound) {
                return false;
            }
        }
        return true;
    }
}

