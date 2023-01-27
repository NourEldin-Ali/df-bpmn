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
import org.openbpmn.bpmn.elements.DataProcessingExtension;
import org.openbpmn.bpmn.elements.Gateway;
import org.openbpmn.bpmn.elements.core.BPMNLabel;
import org.openbpmn.bpmn.exceptions.BPMNModelException;
import org.openbpmn.glsp.bpmn.BpmnPackage;
import org.openbpmn.glsp.elements.CreateBPMNNodeOperationHandler;
import org.openbpmn.glsp.model.BPMNGModelState;
import org.openbpmn.glsp.utils.BPMNGraphUtil;

import com.google.inject.Inject;

/**
 * OperationHandler to create a new gateway.
 *
 * @author rsoika
 *
 */
public class BPMNCreateDataProcessingHandler extends CreateBPMNNodeOperationHandler { // CreateNodeOperationHandler

    private static Logger logger = LogManager.getLogger(BPMNCreateDataProcessingHandler.class);

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
    public BPMNCreateDataProcessingHandler() {
        super(BPMNTypes.DATA_PROCESSING);
    }

    @Override
    public void executeOperation(final CreateNodeOperation operation) {
        elementTypeId = operation.getElementTypeId();

        try {
            BPMNProcess bpmnProcess = findProcessByCreateNodeOperation(operation);

            if (bpmnProcess != null) {
                GPoint dropPoint = operation.getLocation().orElse(null);
                Activity task = bpmnProcess.findActivityByPoint(BPMNGraphUtil.createBPMNPoint(dropPoint));
                DataProcessingExtension dataProcessing = task.addDataProcessing(getLabel());
                Optional<GPoint> point = operation.getLocation();
                if (point.isPresent()) {

                    double elementX = point.get().getX();
                    double elementY = point.get().getY();

                    // compute relative center position...
                    elementX = elementX - (DataProcessingExtension.DEFAULT_WIDTH / 2);
                    elementY = elementY - (DataProcessingExtension.DEFAULT_HEIGHT / 2);

                    dataProcessing.getBounds().setPosition(elementX, elementY);
                    dataProcessing.getBounds().setDimension(Gateway.DEFAULT_WIDTH, Gateway.DEFAULT_HEIGHT);
                    // set label bounds
                    double labelX = elementX + (DataProcessingExtension.DEFAULT_WIDTH / 2)
                            - (BPMNLabel.DEFAULT_WIDTH / 2);
                    double labelY = elementY + DataProcessingExtension.DEFAULT_HEIGHT + Gateway.LABEL_OFFSET;
                    logger.debug("new BPMNLabel Position = " + labelX + "," + labelY);
                    dataProcessing.getLabel().updateLocation(labelX, labelY);
                    dataProcessing.getLabel().updateDimension(BPMNLabel.DEFAULT_WIDTH, BPMNLabel.DEFAULT_HEIGHT);
                    modelState.reset();
                    actionDispatcher.dispatchAfterNextUpdate(new SelectAction(),
                            new SelectAction(List.of(dataProcessing.getId())));
                }
            }
        } catch (BPMNModelException e) {
            e.printStackTrace();
        }

    }

    @Override
    public String getLabel() {
        int nodeCounter = GModelUtil.generateId(BpmnPackage.Literals.DATA_PROCESSING_EXTENSTION_GNODE, elementTypeId,
                modelState);
        nodeCounter++; // start with 1
        return "dataProcessing-" + nodeCounter;
    }

}
