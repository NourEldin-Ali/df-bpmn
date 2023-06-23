package org.openbpmn.convert;

import java.util.logging.Logger;

import org.junit.jupiter.api.Test;
import org.openbpm.bpmn.converter.DFBPMNToProc;
import org.openbpmn.bpmn.BPMNModel;
import org.openbpmn.bpmn.exceptions.BPMNModelException;
import org.openbpmn.bpmn.util.BPMNModelFactory;

public class TestConvertingProcessVariable {
	private static Logger logger = Logger.getLogger(TestConvertingProcessVariable.class.getName());

	/**
	 * This test creates an empty BPMN model instance
	 * 
	 * @throws BPMNModelException
	 */
	@Test
	public void testInputProcess() throws BPMNModelException {
		logger.info("...creating new empty proc");
		BPMNModel model = BPMNModelFactory.read("/data/dataObject.bpmn");
//		System.out.println(model.openProcess("process_5Pr7ZQ").getActivities().stream().anyMatch(activity-> activity.getDataInputObjects().stream().anyMatch(data -> data.getElementNode().getLocalName().equals(BPMNTypes.DATA_INPUT_OBJECT_ENVIRONMENT_DATA_USER))));
//		model.openProcess("process_5Pr7ZQ").getActivities().stream().forEach(activity -> {
//			activity.getDataInputObjects().stream().forEach(data ->{
//System.out.println(data.getName());
//System.out.println(data.getElementNode().getLocalName());
//			});
//		});
//		System.out.println(model.openProcess("process_UCR5cw").getGateways().size());
		DFBPMNToProc dfbpmnToProc = new DFBPMNToProc(model,model.openDefaultProces().getAttribute("exportName"),model.openDefaultProces().getAttribute("bonitaProjectPath"));
		dfbpmnToProc.createDiagrame();
		logger.info("...proc creation sucessful");

	}
}
