package org.openbpmn.metamodel.examples.extension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.logging.Logger;

import org.junit.jupiter.api.Test;
import org.openbpmn.bpmn.BPMNModel;
import org.openbpmn.bpmn.BPMNNS;
import org.openbpmn.bpmn.BPMNTypes;
import org.openbpmn.bpmn.elements.Activity;
import org.openbpmn.bpmn.elements.BPMNProcess;
import org.openbpmn.bpmn.elements.DataObjectAttributeExtension;
import org.openbpmn.bpmn.exceptions.BPMNModelException;
import org.openbpmn.bpmn.util.BPMNModelFactory;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This test class reads a BPMN Model instance and prints the node elements
 * 
 * @author rsoika
 *
 */
public class TestReadRemoveData {

    private static Logger logger = Logger.getLogger(TestReadRemoveData.class.getName());

    /**
     * This test parses a bpmn file
     * @throws BPMNModelException 
     */
    @Test
    public void testReadRemoveData() throws BPMNModelException {

        logger.info("...read model");

        BPMNModel model = BPMNModelFactory.read("/data-read-update.bpmn");
//        System.out.println("Root Element :" + model.getDoc().getDocumentElement().getNodeName());
//        System.out.println("------");
//        if (model.getDoc().hasChildNodes()) {
//            printNote(model.getDoc().getChildNodes());
//        }

        // next validate the BPMN Default Namespaces
        BPMNProcess bpmnModel =  model.openDefaultProcess();
        Activity act = (Activity) bpmnModel.findElementById("task_1");
        
//        act.deleteElementById("DataFlowExtension_vtdGZw");
//        act.deleteElementById("DataFlowExtension_iHwIRQ");
//        act.deleteElementById("DataProcessingExtension_ZBdwoA");
        
        assertEquals("http://www.omg.org/spec/BPMN/20100524/MODEL", model.getUri(BPMNNS.BPMN2));
        logger.info("...model read sucessful");

        String out = "src/test/resources/data-read-remove.bpmn";
        model.save(out);
        logger.info("...model save sucessful");
    }
    

    private static void printNote(NodeList nodeList) {

        for (int count = 0; count < nodeList.getLength(); count++) {

            Node tempNode = nodeList.item(count);

            // make sure it's element node.
            if (tempNode.getNodeType() == Node.ELEMENT_NODE) {

                // get node name and value
                System.out.println("\nNode Name =" + tempNode.getNodeName() + " [OPEN]");
                System.out.println("Node Value =" + tempNode.getTextContent());

                if (tempNode.hasAttributes()) {

                    // get attributes names and values
                    NamedNodeMap nodeMap = tempNode.getAttributes();
                    for (int i = 0; i < nodeMap.getLength(); i++) {
                        Node node = nodeMap.item(i);
                        System.out.println("attr name : " + node.getNodeName());
                        System.out.println("attr value : " + node.getNodeValue());
                    }

                }

                if (tempNode.hasChildNodes()) {
                    // loop again if has child nodes
                    printNote(tempNode.getChildNodes());
                }

                System.out.println("Node Name =" + tempNode.getNodeName() + " [CLOSE]");

            }

        }

    }
}
