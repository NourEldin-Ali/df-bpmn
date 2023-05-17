package org.openbpmn.convert;

import java.util.logging.Logger;

import org.junit.jupiter.api.Test;
import org.openbpm.bpmn.converter.DFBPMNToProc;
import org.openbpmn.bpmn.BPMNModel;
import org.openbpmn.bpmn.BPMNTypes;
import org.openbpmn.bpmn.elements.Activity;
import org.openbpmn.bpmn.elements.BPMNProcess;
import org.openbpmn.bpmn.exceptions.BPMNModelException;
import org.openbpmn.bpmn.util.BPMNModelFactory;
import org.w3c.dom.Element;

public class TestCreateProc {
	private static Logger logger = Logger.getLogger(TestCreateProc.class.getName());

	/**
	 * This test creates an empty BPMN model instance
	 * 
	 * @throws BPMNModelException
	 */
	@Test
	public void testCreateEmptyModel() throws BPMNModelException {

		logger.info("...creating new empty proc");
		BPMNModel model = BPMNModelFactory.read("/df-bpmn.bpmn");
		
//		System.out.println(model);
//		for(BPMNProcess bpmnProcess:model.getProcesses()) {
//			System.out.println( model.openProcess(bpmnProcess.getId()).getActivities().size());
//		}
//		
//		logger.info( model.openDefaultProcess().getId());
//		for (Activity activity : model.openDefaultProcess().getActivities()) {
//			System.out.println(activity.getBpmnShape().getFirstChild().getNextSibling().getAttributes().getNamedItem("x").getNodeValue());
//		}
//		Element e=(Element) model.findBPMNPlaneElement("BPMNShape",..getId());

		DFBPMNToProc dfbpmnToProc = new DFBPMNToProc(model,"DFBPMN","C:\\BonitaStudioCommunity-2022.2-u0\\workspace\\procurement-example1");
		dfbpmnToProc.createDiagrame();
		logger.info("...proc creation sucessful");
	}
}
