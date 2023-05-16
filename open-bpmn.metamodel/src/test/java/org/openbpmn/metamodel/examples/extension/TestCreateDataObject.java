package org.openbpmn.metamodel.examples.extension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.logging.Logger;

import javax.xml.crypto.Data;

import org.junit.jupiter.api.Test;
import org.openbpmn.bpmn.BPMNModel;
import org.openbpmn.bpmn.BPMNTypes;
import org.openbpmn.bpmn.elements.Activity;
import org.openbpmn.bpmn.elements.BPMNProcess;
import org.openbpmn.bpmn.elements.DataInputObjectExtension;
import org.openbpmn.bpmn.elements.DataObjectAttributeExtension;
import org.openbpmn.bpmn.elements.DataOutputObjectExtension;
import org.openbpmn.bpmn.elements.DataProcessingExtension;
import org.openbpmn.bpmn.exceptions.BPMNModelException;
import org.openbpmn.bpmn.util.BPMNModelFactory;

/**
 * This test class tests the creation of specific BPMN element types
 * 
 * @author Ali Nour Eldin
 *
 */
public class TestCreateDataObject {

    private static Logger logger = Logger.getLogger(TestCreateDataObject.class.getName());

   
    /**
     * This test creates a bpmn file with a process definition
     */
    @Test
    public void testCreateDataObject() {
    	String out = "src/test/resources/data-add.bpmn";

        logger.info("...create model with data");

        String exporter = "demo";
        String version = "1.0.0";
        String targetNameSpace = "http://org.openbpmn";
        BPMNModel model = BPMNModelFactory.createInstance(exporter, version, targetNameSpace);

        try {
            BPMNProcess process = model.openDefaultProces();
            Activity activity =  process.addTask("task_1", "Task-1", BPMNTypes.TASK);
            
            //add input with attribute
            DataInputObjectExtension  dataObjectInput =  activity.addDataInputObject(BPMNTypes.DATA_INPUT_OBJECT_PROCESS,"inputTest","Sting",true,null);
//            dataObjectInput.addAttributeObject(BPMNTypes.DATA_OBJECT_ATTRIBUTE, "attInputTest","string");

            //add output with attribute
            DataOutputObjectExtension  dataObjectOutput =  activity.addDataOutputObject(BPMNTypes.DATA_OUTPUT_OBJECT_ENVIRONMENT_DATA,"outTest","Sting",true,"state");
            DataObjectAttributeExtension dataObjectAttributeExtension  = dataObjectOutput.addAttributeObject("attOutputTest","string");
            
            DataProcessingExtension dataProc =  activity.addDataProcessing("Data operation");
            
            //add dataFlow
            activity.addDataFlow(dataObjectInput.getId(), dataProc.getId());
            activity.addDataFlow(dataProc.getId(), dataObjectAttributeExtension.getId());
            
        } catch (BPMNModelException e) {
            e.printStackTrace();
            fail();
        }
        assertNotNull(model);
        assertEquals(1, model.getProcesses().size());

        model.save(out);
        logger.info("...model created sucessful: " + out);
    }

}
