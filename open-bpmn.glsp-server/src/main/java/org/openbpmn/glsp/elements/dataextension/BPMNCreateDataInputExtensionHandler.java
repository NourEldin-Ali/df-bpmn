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

import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.glsp.graph.GPoint;
import org.eclipse.glsp.server.actions.ActionDispatcher;
import org.eclipse.glsp.server.actions.SelectAction;
import org.eclipse.glsp.server.operations.CreateNodeOperation;
import org.eclipse.glsp.server.utils.GModelUtil;
import org.openbpmn.bpmn.BPMNTypes;
import org.openbpmn.bpmn.elements.Activity;
import org.openbpmn.bpmn.elements.BPMNProcess;
import org.openbpmn.bpmn.elements.DataInputObjectExtension;
import org.openbpmn.bpmn.elements.DataOutputObjectExtension;
import org.openbpmn.bpmn.exceptions.BPMNModelException;
import org.openbpmn.glsp.bpmn.BpmnPackage;
import org.openbpmn.glsp.elements.CreateBPMNNodeOperationHandler;
import org.openbpmn.glsp.model.BPMNGModelState;
import org.openbpmn.glsp.utils.BPMNGraphUtil;

import com.google.inject.Inject;

/**
 * The BPMNCreateTaskHandler is a GLSP CreateNodeOperation bound to the
 * DiagramModule and called when ever a BPMNTask is newly created within the
 * model.
 *
 * @author rsoika
 *
 */
public class BPMNCreateDataInputExtensionHandler extends CreateBPMNNodeOperationHandler {

    private static Logger logger = LogManager.getLogger(BPMNCreateDataInputExtensionHandler.class);

    @Inject
    protected BPMNGModelState modelState;

    @Inject
    protected ActionDispatcher actionDispatcher;

    private String elementTypeId;

    /**
     * Default constructor
     * <p>
     * We use this constructor to overwrite the handledElementTypeIds
     */
    public BPMNCreateDataInputExtensionHandler() {
        super(BPMNTypes.BPMN_DATA_INPUT_EXTENSION);
    }

    @Override
    protected void executeOperation(final CreateNodeOperation operation) {
        elementTypeId = operation.getElementTypeId();

        try {
            // find the process - either the default process for Root container or the
            // corresponding participant process
            BPMNProcess bpmnProcess = findProcessByCreateNodeOperation(operation);

            if (bpmnProcess != null) {
                GPoint dropPoint = operation.getLocation().orElse(null);
                Activity task = bpmnProcess.findActivityByPoint(BPMNGraphUtil.createBPMNPoint(dropPoint));
                Optional<GPoint> point = operation.getLocation();
                if (BPMNTypes.BPMN_DATA_INPUT_EXTENSION.contains(elementTypeId)) {
                    DataInputObjectExtension data = task.addDataInputObject(elementTypeId, getLabel(), "any", false,
                            getLabel());
                    if (point.isPresent()) {
                        double elementX = point.get().getX();
                        double elementY = point.get().getY();
                        // compute relative center position...
                        elementX = elementX - (DataInputObjectExtension.DEFAULT_WIDTH / 2);
                        elementY = elementY - (DataInputObjectExtension.DEFAULT_HEIGHT / 2);

                        data.getBounds().setPosition(elementX, elementY);
                        data.getBounds().setDimension(DataInputObjectExtension.DEFAULT_WIDTH,
                                DataInputObjectExtension.DEFAULT_HEIGHT);

                        logger.debug("new BPMN Data Position = " + elementX + "," + elementY);
                    }
                    modelState.reset();
                    actionDispatcher.dispatchAfterNextUpdate(new SelectAction(),
                            new SelectAction(List.of(data.getId())));
                } else if (BPMNTypes.BPMN_DATA_OUTPUT_EXTENSION.contains(elementTypeId)) {
                    DataOutputObjectExtension data = task.addDataOutputObject(elementTypeId, getLabel(), "any", false,
                            "init");
                    if (point.isPresent()) {
                        double elementX = point.get().getX();
                        double elementY = point.get().getY();
                        // compute relative center position...
                        elementX = elementX - (DataOutputObjectExtension.DEFAULT_WIDTH / 2);
                        elementY = elementY - (DataOutputObjectExtension.DEFAULT_HEIGHT / 2);

                        data.getBounds().setPosition(elementX, elementY);
                        data.getBounds().setDimension(DataOutputObjectExtension.DEFAULT_WIDTH,
                                DataOutputObjectExtension.DEFAULT_HEIGHT);

                        logger.debug("new BPMNActivity Position = " + elementX + "," + elementY);
                    }
                    modelState.reset();
                    actionDispatcher.dispatchAfterNextUpdate(new SelectAction(),
                            new SelectAction(List.of(data.getId())));
                } else {
                    // not supported element
                }

            } else {
                // should not happen
                logger.fatal("Unable to find a vaild BPMNElement to place the new node: " + elementTypeId);
            }
        } catch (BPMNModelException e) {
            e.printStackTrace();
        }

    }

    @Override
    public String getLabel() {
        int nodeCounter = GModelUtil.generateId(BpmnPackage.Literals.DATA_OBJECT_EXTENSION_GNODE, elementTypeId,
                modelState);
        return "DataInput-" + (nodeCounter + 1);
    }

}
