/********************************************************************************
 * Copyright (c) 2022 Imixs Software Solutions GmbH and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the Eclipse
 * Public License v. 2.0 are satisfied: GNU General Public License, version 2
 * with the GNU Classpath Exception which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 ********************************************************************************/
package org.openbpmn.glsp.elements.dataextension;

import java.util.Optional;
import java.util.logging.Logger;

import org.eclipse.glsp.server.actions.ActionDispatcher;
import org.eclipse.glsp.server.operations.CreateEdgeOperation;
import org.openbpmn.bpmn.BPMNTypes;
import org.openbpmn.bpmn.elements.Activity;
import org.openbpmn.bpmn.elements.BPMNProcess;
import org.openbpmn.bpmn.elements.core.BPMNElement;
import org.openbpmn.bpmn.exceptions.BPMNModelException;
import org.openbpmn.glsp.action.MyCustomRequestAction;
import org.openbpmn.glsp.bpmn.BPMNGNode;
import org.openbpmn.glsp.elements.CreateBPMNEdgeOperationHandler;
import org.openbpmn.glsp.model.BPMNGModelState;

import com.google.inject.Inject;

public class BPMNGEdgeFlowCreateHandler extends CreateBPMNEdgeOperationHandler {

	protected final String label;
	private static Logger logger = Logger.getLogger(BPMNGEdgeFlowCreateHandler.class.getName());

	@Inject
	protected BPMNGModelState modelState;

	@Inject
	protected ActionDispatcher actionDispatcher;

	/**
	 * Default constructor
	 * <p>
	 * We use this constructor to overwrite the handledElementTypeIds
	 */
	public BPMNGEdgeFlowCreateHandler() {
		super(BPMNTypes.BPMN_EXTENSION_EDGES);
		this.label = "Flow extesnsion";
	}

	/**
	 * Adds a new BPMNEdge to the diagram. Depending on the type a SequenceFlow,
	 * MessageFlow or Association is crated.
	 */
	@Override
	protected void executeOperation(final CreateEdgeOperation operation) {
		if (operation.getSourceElementId() == null || operation.getTargetElementId() == null) {
			throw new IllegalArgumentException("Incomplete create connection action");
		}

		String edgeType = operation.getElementTypeId();
		try {
			Optional<BPMNGNode> element = null;
			String targetId = operation.getTargetElementId();

			// find GNode
			element = modelState.getIndex().findElementByClass(targetId, BPMNGNode.class);
			if (element.isPresent()) {
				targetId = element.get().getId();
			}

			String sourceId = operation.getSourceElementId();

			// find GNode
			element = modelState.getIndex().findElementByClass(sourceId, BPMNGNode.class);
			if (element.isPresent()) {
				sourceId = element.get().getId();
			}

			// Depending on the edgeType we use here different method to create the BPMN
			// edge

			// Verify that both Elements are members of the same process...
//                System.out.println(modelState.getBpmnModel().findElementExtensionNodeById(sourceId).getProcessId());
			String sourceProcessId = modelState.getBpmnModel().findElementExtensionNodeById(sourceId).getProcessId();

			String targetProcessId = modelState.getBpmnModel().findElementExtensionNodeById(targetId).getProcessId();

			if (sourceProcessId == null || !sourceProcessId.contentEquals(targetProcessId)) {
				throw new IllegalArgumentException(
						"Target and Source elements should not members of the same process!");
			}

			String parentSourceId = modelState.getBpmnModel().findElementDataExtensionNodeById(sourceId)
					.getElementNode().getParentNode().getAttributes().getNamedItem("id").getTextContent();
			String parentTargetId = modelState.getBpmnModel().findElementDataExtensionNodeById(targetId)
					.getElementNode().getParentNode().getAttributes().getNamedItem("id").getTextContent();

			// check if the edge from child to parent
			if (targetId.contentEquals(parentSourceId) || sourceId.contentEquals(parentTargetId)) {
				throw new IllegalArgumentException(
						"Target and Source elements should not members of the same data object!");
			}

			// check if the target is an input
			// or an attribute of input!
			BPMNElement targetElement = modelState.getBpmnModel().findElementDataExtensionNodeById(targetId);
			String targetElementTag = targetElement.getElementNode().getLocalName();
			if (targetElement.getElementNode().getLocalName().contentEquals(BPMNTypes.DATA_OBJECT_ATTRIBUTE)) {
				targetElementTag = targetElement.getElementNode().getParentNode().getLocalName();
			}

			boolean isInput = (targetElementTag.contentEquals(BPMNTypes.DATA_INPUT_OBJECT_LOCAL)
					|| targetElementTag.contentEquals(BPMNTypes.DATA_INPUT_OBJECT_DATA_STORE)
//					|| targetElementTag.contentEquals(BPMNTypes.DATA_INPUT_OBJECT_DEPENDENCY)
					|| targetElementTag.contentEquals(BPMNTypes.DATA_INPUT_OBJECT_DEPENDENT_DATA_STORE)
					|| targetElementTag.contentEquals(BPMNTypes.DATA_INPUT_OBJECT_DEPENDENT_LOCAL)
					|| targetElementTag.contentEquals(BPMNTypes.DATA_INPUT_OBJECT_DEPENDENT_PROCESS)
					|| targetElementTag.contentEquals(BPMNTypes.DATA_INPUT_OBJECT_ENVIRONMENT_DATA)
					|| targetElementTag.contentEquals(BPMNTypes.DATA_INPUT_OBJECT_ENVIRONMENT_DATA_USER)
					|| targetElementTag.contentEquals(BPMNTypes.DATA_INPUT_OBJECT_LOCAL)
					|| targetElementTag.contentEquals(BPMNTypes.DATA_INPUT_OBJECT_PROCESS));
			if (isInput) {
				throw new IllegalArgumentException("Target elements should not be a data input object!");
			}

			// check if the target is an dependency data => the source should be dependent
			// data
			// or the attributes of the input!
			BPMNElement sourceElement = modelState.getBpmnModel().findElementDataExtensionNodeById(sourceId);
//			System.out.println(sourceElement.getElementNode().getLocalName());
			String sourceElementTag = sourceElement.getElementNode().getLocalName();
			if (sourceElement.getElementNode().getLocalName().contentEquals(BPMNTypes.DATA_OBJECT_ATTRIBUTE)) {
				sourceElementTag = sourceElement.getElementNode().getParentNode().getLocalName();
			}

			boolean isErrorInput = (targetElementTag.contentEquals(BPMNTypes.DATA_INPUT_OBJECT_DEPENDENCY)
					&& !(sourceElementTag.contentEquals(BPMNTypes.DATA_INPUT_OBJECT_DEPENDENT_DATA_STORE)
							|| sourceElementTag.contentEquals(BPMNTypes.DATA_INPUT_OBJECT_DEPENDENT_LOCAL)
							|| sourceElementTag.contentEquals(BPMNTypes.DATA_INPUT_OBJECT_DEPENDENT_PROCESS))
					|| ((sourceElementTag.contentEquals(BPMNTypes.DATA_INPUT_OBJECT_DEPENDENT_DATA_STORE)
							|| sourceElementTag.contentEquals(BPMNTypes.DATA_INPUT_OBJECT_DEPENDENT_LOCAL)
							|| sourceElementTag.contentEquals(BPMNTypes.DATA_INPUT_OBJECT_DEPENDENT_PROCESS)

					) && !targetElementTag.contentEquals(BPMNTypes.DATA_INPUT_OBJECT_DEPENDENCY)));
			if (isErrorInput) {
				throw new IllegalArgumentException("Target elements should not be a data depentent object");
			}

			// check if working on with data processing

			// check source = data processing , and the target should be output
			isErrorInput = sourceElementTag.contentEquals(BPMNTypes.DATA_PROCESSING)
					&& !(targetElementTag.contentEquals(BPMNTypes.DATA_OUTPUT_OBJECT_DATA_STORE)
							|| targetElementTag.contentEquals(BPMNTypes.DATA_OUTPUT_OBJECT_ENVIRONMENT_DATA)
							|| targetElementTag.contentEquals(BPMNTypes.DATA_OUTPUT_OBJECT_ENVIRONMENT_DATA_USER)
							|| targetElementTag.contentEquals(BPMNTypes.DATA_OUTPUT_OBJECT_PROCESS));

			if (isErrorInput) {
				throw new IllegalArgumentException("Target elements should be a data output object");
			}

			// check target = data processing , and the source should be input
			isErrorInput = targetElementTag.contentEquals(BPMNTypes.DATA_PROCESSING)
					&& !(sourceElementTag.contentEquals(BPMNTypes.DATA_INPUT_OBJECT_DATA_STORE)
							|| sourceElementTag.contentEquals(BPMNTypes.DATA_INPUT_OBJECT_DEPENDENCY)
							|| sourceElementTag.contentEquals(BPMNTypes.DATA_INPUT_OBJECT_ENVIRONMENT_DATA)
							|| sourceElementTag.contentEquals(BPMNTypes.DATA_INPUT_OBJECT_ENVIRONMENT_DATA_USER)
							|| sourceElementTag.contentEquals(BPMNTypes.DATA_INPUT_OBJECT_LOCAL)
							|| sourceElementTag.contentEquals(BPMNTypes.DATA_INPUT_OBJECT_PROCESS));

			if (isErrorInput) {
				throw new IllegalArgumentException("Target elements should be a data output object");
			}

			// open the process and create the sequence flow...
			BPMNProcess bpmnProcess = modelState.getBpmnModel().openProcess(targetProcessId);
			Activity act = (Activity) bpmnProcess
					.findElementById(modelState.getBpmnModel().findElementExtensionNodeById(sourceId).getId());
			if (BPMNTypes.DATA_FLOW.equals(edgeType)) {
				act.addDataFlow(sourceId, targetId);
//                bpmnProcess.addSequenceFlow(BPMNModel.generateShortID("SequenceFlow"), sourceId, targetId);

			} else if (BPMNTypes.DATA_REFERENCE.equals(edgeType)) {
				act.addDataReference(sourceId, targetId);
			}

			modelState.reset();
		} catch (BPMNModelException e) {
			logger.severe(e.getMessage());
		}

	}

	@Override
	public String getLabel() {
		return label;
	}
}
