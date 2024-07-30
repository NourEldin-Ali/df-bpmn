package org.openbpmn.bpmn.discovery.compare;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.FlowElement;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;

import java.util.List;

public class BPMNComparatorExecutor {
    public static void main(String[] args) {
        BPMNComparatorExecutor.execute(
                "C:\\Users\\AliNourEldin\\Desktop\\da-bpmn\\open-bpmn\\open-bpmn.metamodel\\src\\test\\resources\\discovery\\loop\\test.bpmn",
                "C:\\Users\\AliNourEldin\\Desktop\\da-bpmn\\open-bpmn\\open-bpmn.metamodel\\src\\test\\resources\\discovery\\loop\\test1.bpmn"
        );
    }
    public static boolean execute(String path1, String path2) {
        BpmnModelInstance model1 = BPMNLoader.loadBPMN(path1);
        BpmnModelInstance model2 = BPMNLoader.loadBPMN(path2);

        List<FlowElement> elements1 = BPMNExtractor.extractElements(model1);
        List<FlowElement> elements2 = BPMNExtractor.extractElements(model2);

        List<SequenceFlow> flows1 = BPMNExtractor.extractFlows(model1);
        List<SequenceFlow> flows2 = BPMNExtractor.extractFlows(model2);

        boolean areElementsEqual = BPMNComparator.compareElements(elements1, elements2);
        boolean areFlowsEqual = BPMNComparator.compareFlows(flows1, flows2);

        if (areElementsEqual && areFlowsEqual) {
            System.out.println("The BPMN models are equivalent.");
            return true;
        } else {
            System.out.println("The BPMN models are not equivalent.");
            return false;
        }
    }
}

