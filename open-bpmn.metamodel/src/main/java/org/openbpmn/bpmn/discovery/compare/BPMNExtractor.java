package org.openbpmn.bpmn.discovery.compare;

import org.camunda.bpm.model.bpmn.instance.*;
import java.util.*;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;


public class BPMNExtractor {
    public static List<FlowElement> extractElements(BpmnModelInstance modelInstance) {
        Collection<FlowElement> flowElements = modelInstance.getModelElementsByType(FlowElement.class);
        return new ArrayList<>(flowElements);
    }

    public static List<SequenceFlow> extractFlows(BpmnModelInstance modelInstance) {
        return new ArrayList<>(modelInstance.getModelElementsByType(SequenceFlow.class));
    }
}
