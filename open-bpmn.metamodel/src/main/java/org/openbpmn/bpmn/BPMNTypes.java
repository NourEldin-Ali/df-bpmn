package org.openbpmn.bpmn;

import java.util.Arrays;
import java.util.List;

import org.openbpmn.bpmn.elements.SequenceFlow;
import org.openbpmn.bpmn.elements.core.BPMNElement;
import org.openbpmn.bpmn.elements.core.BPMNElementNode;

/**
 * The BPMNTypes provides constants defining the visual elements contained in a
 * BPMNDiagram.
 * 
 * @author rsoika
 *
 */
public class BPMNTypes {

    // Process Types
    public static final String PROCESS_TYPE_PUBLIC = "Public";
    public static final String PROCESS_TYPE_PRIVATE = "Private";
    public static final String PROCESS_TYPE_NONE = "None";

    // Tasks
    public static final String TASK = "task";
    public static final String USER_TASK = "userTask";
    public static final String SCRIPT_TASK = "scriptTask";
    public static final String SERVICE_TASK = "serviceTask";
    public static final String SEND_TASK = "sendTask";
    public static final String MANUAL_TASK = "manualTask";
    public static final String BUSINESSRULE_TASK = "businessRuleTask";
    public static final String RECEIVE_TASK = "receiveTask";

    // Events
    public static final String EVENT = "event";
    public static final String START_EVENT = "startEvent";
    public static final String END_EVENT = "endEvent";
    public static final String CATCH_EVENT = "intermediateCatchEvent";
    public static final String THROW_EVENT = "intermediateThrowEvent";
    public static final String BOUNDARY_EVENT = "boundaryEvent";

    // Event Definitions
    public static final String EVENT_DEFINITION_CONDITIONAL = "conditionalEventDefinition";
    public static final String EVENT_DEFINITION_COMPENSATION = "compensationEventDefinition";
    public static final String EVENT_DEFINITION_TIMER = "timerEventDefinition";
    public static final String EVENT_DEFINITION_SIGNAL = "signalEventDefinition";
    public static final String EVENT_DEFINITION_ESCALATION = "escalationEventDefinition";
    public static final String EVENT_DEFINITION_MESSAGE = "messageEventDefinition";
    public static final String EVENT_DEFINITION_LINK = "linkEventDefinition";
    public static final String EVENT_DEFINITION_ERROR = "errorEventDefinition";
    public static final String EVENT_DEFINITION_TERMINATE = "terminateEventDefinition";
    public static final String EVENT_DEFINITION_CANCEL = "cancelEventDefinition";

    // Multiple Event Definitions
    public static final String MULTIPLE_EVENT_DEFINITIONS = "multipleEventDefinition";

    // Gateways
    public static final String GATEWAY = "gateway";
    public static final String EXCLUSIVE_GATEWAY = "exclusiveGateway";
    public static final String INCLUSIVE_GATEWAY = "inclusiveGateway";
    public static final String EVENTBASED_GATEWAY = "eventBasedGateway";
    public static final String PARALLEL_GATEWAY = "parallelGateway";
    public static final String COMPLEX_GATEWAY = "complexGateway";

    // Others
    public static final String DATAOBJECT = "dataObject";
    public static final String TEXTANNOTATION = "textAnnotation";
    public static final String POOL = "pool";
    public static final String LANE = "lane";
    public static final String MESSAGE = "message";
    public static final String SIGNAL = "signal";
    public static final String SEQUENCE_FLOW = "sequenceFlow";
    public static final String MESSAGE_FLOW = "messageFlow";
    public static final String ASSOCIATION = "association";
    public static final String BPMNLABEL = "BPMNLabel";
    public static final String EXTENSION = "extension";
    public static final String PARTICIPANT = "participant";
    public static final String LANESET = "laneSet";

    // Type Collections
    public final static List<String> BPMN_TASKS = Arrays.asList(new String[] { //
            BPMNTypes.TASK, //
            BPMNTypes.MANUAL_TASK, //
            BPMNTypes.USER_TASK, //
            BPMNTypes.SCRIPT_TASK, //
            BPMNTypes.BUSINESSRULE_TASK, //
            BPMNTypes.SERVICE_TASK, //
            BPMNTypes.SEND_TASK, //
            BPMNTypes.RECEIVE_TASK //
    });

    public final static List<String> BPMN_ACTIVITIES = Arrays.asList(new String[] { //
            BPMNTypes.TASK, //
            BPMNTypes.MANUAL_TASK, //
            BPMNTypes.USER_TASK, //
            BPMNTypes.SCRIPT_TASK, //
            BPMNTypes.BUSINESSRULE_TASK, //
            BPMNTypes.SERVICE_TASK, //
            BPMNTypes.SEND_TASK, //
            BPMNTypes.RECEIVE_TASK, //
            "subProcess", "adHocSubProcess", "transaction", "callActivity" });

    public final static List<String> BPMN_EVENTS = Arrays.asList(new String[] { //
            BPMNTypes.EVENT, //
            BPMNTypes.START_EVENT, //
            BPMNTypes.END_EVENT, //
            BPMNTypes.CATCH_EVENT, //
            BPMNTypes.THROW_EVENT, //
            BPMNTypes.BOUNDARY_EVENT //
    });






	public final static List<String> BPMN_FLOWELEMENTS = Arrays.asList(//
			BPMNTypes.TASK, //
			BPMNTypes.MANUAL_TASK, //
			BPMNTypes.USER_TASK, //
			BPMNTypes.SCRIPT_TASK, //
			BPMNTypes.BUSINESSRULE_TASK, //
			BPMNTypes.SERVICE_TASK, //
			BPMNTypes.SEND_TASK, //
			BPMNTypes.RECEIVE_TASK, //

            BPMNTypes.DATAOBJECT, //
            BPMNTypes.TEXTANNOTATION, //
            BPMNTypes.MESSAGE, //

			BPMNTypes.START_EVENT, //
			BPMNTypes.END_EVENT, //
			BPMNTypes.CATCH_EVENT, //
			BPMNTypes.THROW_EVENT, //
			BPMNTypes.BOUNDARY_EVENT, //

			BPMNTypes.SEQUENCE_FLOW);

	public final static List<String> BPMN_NODE_ELEMENTS = Arrays.asList(//
			BPMNTypes.TASK, //
			BPMNTypes.MANUAL_TASK, //
			BPMNTypes.USER_TASK, //
			BPMNTypes.SCRIPT_TASK, //
			BPMNTypes.BUSINESSRULE_TASK, //
			BPMNTypes.SERVICE_TASK, //
			BPMNTypes.SEND_TASK, //
			BPMNTypes.RECEIVE_TASK, //

			BPMNTypes.EXCLUSIVE_GATEWAY, //
			BPMNTypes.PARALLEL_GATEWAY, //
			BPMNTypes.EVENTBASED_GATEWAY, //
			BPMNTypes.COMPLEX_GATEWAY, //
			BPMNTypes.INCLUSIVE_GATEWAY, //

			BPMNTypes.START_EVENT, //
			BPMNTypes.END_EVENT, //
			BPMNTypes.CATCH_EVENT, //
			BPMNTypes.THROW_EVENT, //
			BPMNTypes.BOUNDARY_EVENT, //

			BPMNTypes.DATAOBJECT, //
			BPMNTypes.TEXTANNOTATION, //

			BPMNTypes.POOL);


	public static List<String> BPMN_EDGES = Arrays.asList(new String[] { //
			BPMNTypes.SEQUENCE_FLOW, //
			BPMNTypes.MESSAGE_FLOW, //
			BPMNTypes.ASSOCIATION

	});

	/**-------------------
	 *  Add the data definition needed to DA-BPMN
	 * @author Ali Nour Eldin
	 */
	public static final String DATA_OBJECT_EXTENSION = "DataObjectExtension";
	// Data input object types
	public static final String DATA_INPUT_OBJECT_LOCAL = "dataInputObjectLocal";
	public static final String DATA_INPUT_OBJECT_PROCESS = "dataInputObjectProcess";
	public static final String DATA_INPUT_OBJECT_DATA_STORE = "dataInputObjectDataStore";
	public static final String DATA_INPUT_OBJECT_ENVIRONMENT_DATA = "dataInputObjectEnvironmentData";
	public static final String DATA_INPUT_OBJECT_ENVIRONMENT_DATA_USER = "dataInputObjectEnvironmentDataUser";

	public static final String DATA_INPUT_OBJECT_DEPENDENT_LOCAL = "dataInputObjectDependentLocal";
	public static final String DATA_INPUT_OBJECT_DEPENDENT_PROCESS = "dataInputObjectDependentProcess";
	public static final String DATA_INPUT_OBJECT_DEPENDENT_DATA_STORE = "dataInputObjectDependentDataStore";
	public static final String DATA_INPUT_OBJECT_DEPENDENCY = "dataInputObjectDependency";

	// Data output object types
	public static final String DATA_OUTPUT_OBJECT_PROCESS = "dataOutputObjectProcess";
	public static final String DATA_OUTPUT_OBJECT_DATA_STORE = "dataOutputObjectDataStore";
	public static final String DATA_OUTPUT_OBJECT_ENVIRONMENT_DATA = "dataOutputObjectEnvironmentData";
	public static final String DATA_OUTPUT_OBJECT_ENVIRONMENT_DATA_USER = "dataOutputObjectEnvironmentDataUser";

	// Data processing
	public static final String DATA_PROCESSING = "dataProcessing";

	// Others
	public static final String DATA_OBJECT_ATTRIBUTE = "attribute";
	public static final String DATA_FLOW = "dataFlow";
	public static final String DATA_REFERENCE = "dataReference";

	// tagForId
	public static final String DATA_FLOW_Extension = "DataFlowExtension";
	public static final String DATA_OBJECT_ATTRIBUTE_Extension = "DataObjectAttributeExtension";
	public static final String DATA_OUTPUT_OBJECT_Extension = "DataOutputObjectExtension";
	public static final String DATA_INPUT_OBJECT_Extension = "DataInputObjectExtension";
	public static final String DATA_PROCESSING_EXTENSION = "DataProcessingExtension";
	public static final String DATA_REFERENCE_EXTENSION = "DataReferenceExtension";

	// Type Collections
	public static List<String> BPMN_DATA_INPUT_EXTENSION = Arrays.asList(new String[] { //
			BPMNTypes.DATA_INPUT_OBJECT_LOCAL, //
			BPMNTypes.DATA_INPUT_OBJECT_PROCESS, //
			BPMNTypes.DATA_INPUT_OBJECT_DATA_STORE, //
			BPMNTypes.DATA_INPUT_OBJECT_ENVIRONMENT_DATA, //
			BPMNTypes.DATA_INPUT_OBJECT_ENVIRONMENT_DATA_USER, //
			BPMNTypes.DATA_INPUT_OBJECT_DEPENDENT_LOCAL, //
			BPMNTypes.DATA_INPUT_OBJECT_DEPENDENT_PROCESS, //
			BPMNTypes.DATA_INPUT_OBJECT_DEPENDENT_DATA_STORE, //
			BPMNTypes.DATA_INPUT_OBJECT_DEPENDENCY, });

	public static List<String> BPMN_DATA_OUTPUT_EXTENSION = Arrays.asList(new String[] { //
			BPMNTypes.DATA_OUTPUT_OBJECT_PROCESS, //
			BPMNTypes.DATA_OUTPUT_OBJECT_DATA_STORE, //
			BPMNTypes.DATA_OUTPUT_OBJECT_ENVIRONMENT_DATA, //
			BPMNTypes.DATA_OUTPUT_OBJECT_ENVIRONMENT_DATA_USER, //

	});
	public final static List<String> BPMN_DATA_OBJECTS_EXTENSION = Arrays.asList(new String[] { ////
			BPMNTypes.DATA_INPUT_OBJECT_LOCAL, //
			BPMNTypes.DATA_INPUT_OBJECT_PROCESS, //
			BPMNTypes.DATA_INPUT_OBJECT_DATA_STORE, //
			BPMNTypes.DATA_INPUT_OBJECT_ENVIRONMENT_DATA, //
			BPMNTypes.DATA_INPUT_OBJECT_ENVIRONMENT_DATA_USER, //
			BPMNTypes.DATA_INPUT_OBJECT_DEPENDENT_LOCAL, //
			BPMNTypes.DATA_INPUT_OBJECT_DEPENDENT_PROCESS, //
			BPMNTypes.DATA_INPUT_OBJECT_DEPENDENT_DATA_STORE, //
			BPMNTypes.DATA_INPUT_OBJECT_DEPENDENCY, //

			BPMNTypes.DATA_OUTPUT_OBJECT_PROCESS, //
			BPMNTypes.DATA_OUTPUT_OBJECT_DATA_STORE, //
			BPMNTypes.DATA_OUTPUT_OBJECT_ENVIRONMENT_DATA, //
			BPMNTypes.DATA_OUTPUT_OBJECT_ENVIRONMENT_DATA_USER, //

	});
	public final static List<String> BPMN_NODE_ELEMENTS_EXTENSION = Arrays.asList(new String[] { ////
			BPMNTypes.DATA_OBJECT_EXTENSION, //
			BPMNTypes.DATA_INPUT_OBJECT_PROCESS, //
			BPMNTypes.DATA_INPUT_OBJECT_DATA_STORE, //
			BPMNTypes.DATA_INPUT_OBJECT_LOCAL, //
			BPMNTypes.DATA_INPUT_OBJECT_ENVIRONMENT_DATA, //
			BPMNTypes.DATA_INPUT_OBJECT_ENVIRONMENT_DATA_USER, //
			BPMNTypes.DATA_INPUT_OBJECT_DEPENDENT_LOCAL, //
			BPMNTypes.DATA_INPUT_OBJECT_DEPENDENT_PROCESS, //
			BPMNTypes.DATA_INPUT_OBJECT_DEPENDENT_DATA_STORE, //
			BPMNTypes.DATA_INPUT_OBJECT_DEPENDENCY, //

			BPMNTypes.DATA_OUTPUT_OBJECT_PROCESS, //
			BPMNTypes.DATA_OUTPUT_OBJECT_DATA_STORE, //
			BPMNTypes.DATA_OUTPUT_OBJECT_ENVIRONMENT_DATA, //
			BPMNTypes.DATA_OUTPUT_OBJECT_ENVIRONMENT_DATA_USER, //

			BPMNTypes.DATA_OBJECT_ATTRIBUTE, //

	});
	public static List<String> BPMN_EXTENSION_EDGES = Arrays.asList(new String[] { //
			BPMNTypes.DATA_FLOW, //
			BPMNTypes.DATA_REFERENCE });

    public final static List<String> BPMN_EDGE_ELEMENTS = Arrays.asList(new String[] { //
            BPMNTypes.SEQUENCE_FLOW, //
            BPMNTypes.MESSAGE_FLOW, //
            BPMNTypes.ASSOCIATION //
    });

    public final static List<String> BPMN_EVENT_DEFINITIONS = Arrays.asList(new String[] { //
            BPMNTypes.EVENT_DEFINITION_CONDITIONAL, //
            BPMNTypes.EVENT_DEFINITION_TIMER, //
            BPMNTypes.EVENT_DEFINITION_SIGNAL, //
            BPMNTypes.EVENT_DEFINITION_MESSAGE, //
            BPMNTypes.EVENT_DEFINITION_LINK, //
            BPMNTypes.EVENT_DEFINITION_ERROR, //
            BPMNTypes.EVENT_DEFINITION_TERMINATE, //
            BPMNTypes.EVENT_DEFINITION_COMPENSATION });

    public final static List<String> BPMN_GATEWAYS = Arrays.asList(new String[] { //
            BPMNTypes.GATEWAY, //
            BPMNTypes.EXCLUSIVE_GATEWAY, //
            BPMNTypes.INCLUSIVE_GATEWAY, //
            BPMNTypes.PARALLEL_GATEWAY, //
            BPMNTypes.EVENTBASED_GATEWAY, //
            BPMNTypes.COMPLEX_GATEWAY //
    });
    public final static List<String> BPMN_FLOWELEMENT_NODES = Arrays.asList(//
            BPMNTypes.TASK, //
            BPMNTypes.MANUAL_TASK, //
            BPMNTypes.USER_TASK, //
            BPMNTypes.SCRIPT_TASK, //
            BPMNTypes.BUSINESSRULE_TASK, //
            BPMNTypes.SERVICE_TASK, //
            BPMNTypes.SEND_TASK, //
            BPMNTypes.RECEIVE_TASK, //

            BPMNTypes.EXCLUSIVE_GATEWAY, //
            BPMNTypes.PARALLEL_GATEWAY, //
            BPMNTypes.EVENTBASED_GATEWAY, //
            BPMNTypes.COMPLEX_GATEWAY, //
            BPMNTypes.INCLUSIVE_GATEWAY, //

            BPMNTypes.START_EVENT, //
            BPMNTypes.END_EVENT, //
            BPMNTypes.CATCH_EVENT, //
            BPMNTypes.THROW_EVENT, //
            BPMNTypes.BOUNDARY_EVENT //
    );

    /**
     * Returns true if the given element is a FlowElement, which are all
     * BPMNElementNodes from the type Event, Gateway or Activity.
     * SequenceFlows are not included as this is a Edge type!
     */
    public static boolean isFlowElementNode(BPMNElement element) {
        if (element instanceof BPMNElementNode) {
            return BPMN_FLOWELEMENT_NODES.contains(((BPMNElementNode) element).getType());
        }
        return false;
    }

    /**
     * Returns true if the given element is a SequenceFlow
     */
    public static boolean isSequenceFlow(BPMNElement element) {
        if (element instanceof SequenceFlow) {
            return true;
        }
        return false;
    }
}
