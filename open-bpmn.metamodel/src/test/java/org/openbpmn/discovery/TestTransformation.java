package org.openbpmn.discovery;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;
import org.openbpm.bpmn.converter.DFBPMNToProc;
import org.openbpmn.bpmn.BPMNModel;
import org.openbpmn.bpmn.BPMNTypes;
import org.openbpmn.bpmn.discovery.RelationConverter;
import org.openbpmn.bpmn.elements.Activity;
import org.openbpmn.bpmn.elements.BPMNProcess;
import org.openbpmn.bpmn.elements.SequenceFlow;
import org.openbpmn.bpmn.exceptions.BPMNModelException;
import org.openbpmn.bpmn.util.BPMNModelFactory;

public class TestTransformation {
	private static Logger logger = Logger.getLogger(TestTransformation.class.getName());

	/**
	 * This test creates an empty BPMN model instance
	 * 
	 * @throws BPMNModelException
	 */
	@Test
	public void testInputProcess() throws BPMNModelException {

		 logger.info("...creating new empty model");
		BPMNModel model1 = BPMNModelFactory.createInstance("demo", "1.0.0", "http://org.openbpmn");
		BPMNProcess process = model1.openDefaultProces();
		
		
		Activity activity1 = process.addTask("t", "tas", BPMNTypes.TASK);
		Activity activity2 = process.addTask("t2", "tas2", BPMNTypes.TASK);
		process.addSequenceFlow("s1", activity1.getId(), activity2.getId());

		model1.save("C:\\Users\\AliNourEldin\\Desktop\\bpmn-layout\\bpmn-auto-layout\\test\\fixtures\\test.bpmn");
		logger.info("...model creation sucessful");

		List<String> events = new ArrayList<>();
		events.add("a->b");
		events.add("a->c");
		events.add("d->e");
		events.add("b->k");
		events.add("c->e");
		events.add("a->d");

		String startEvent = "a";



	}
}