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
import org.openbpmn.bpmn.elements.BPMNProcess;
import org.openbpmn.bpmn.elements.DataInputObjectExtension;
import org.openbpmn.bpmn.elements.DataObjectAttributeExtension;
import org.openbpmn.bpmn.elements.DataOutputObjectExtension;
import org.openbpmn.bpmn.elements.core.BPMNElementNode;
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
public class BPMNCreateDataAttributeExtensionHandler extends CreateBPMNNodeOperationHandler {

    private static Logger logger = LogManager.getLogger(BPMNCreateDataAttributeExtensionHandler.class);

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
    public BPMNCreateDataAttributeExtensionHandler() {
        super(BPMNTypes.DATA_OBJECT_ATTRIBUTE);
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
                BPMNElementNode dataObject = bpmnProcess
                        .findDataObjectByPoint(BPMNGraphUtil.createBPMNPoint(dropPoint));
                Optional<GPoint> point = operation.getLocation();
                if (dataObject instanceof DataInputObjectExtension) {
                    DataInputObjectExtension data = (DataInputObjectExtension) dataObject;
                    DataObjectAttributeExtension attribute = data.addAttributeObject(getLabel(), "any");
                    if (point.isPresent()) {

                        double elementX = data.getBounds().getPosition().getX() + 25;
                        double elementY = data.getBounds().getPosition().getY()
                                + data.getBounds().getDimension().getHeight()
                                + DataObjectAttributeExtension.DEFAULT_HEIGHT * (data.getDataAttributes().size() - 1);
                        attribute.getBounds().setPosition(elementX, elementY);
                        attribute.getBounds().setDimension(DataObjectAttributeExtension.DEFAULT_WIDTH,
                                DataObjectAttributeExtension.DEFAULT_HEIGHT);

                        logger.debug("new BPMN Data attribute Position = " + elementX + "," + elementY);
                    }
                    modelState.reset();
                    actionDispatcher.dispatchAfterNextUpdate(new SelectAction(),
                            new SelectAction(List.of(attribute.getId())));

                } else if (dataObject instanceof DataOutputObjectExtension) {
                    DataOutputObjectExtension data = (DataOutputObjectExtension) dataObject;
                    DataObjectAttributeExtension attribute = data.addAttributeObject(getLabel(), "any");
                    if (point.isPresent()) {
//                        double elementX = point.get().getX();
//                        double elementY = point.get().getY();
//                        // compute relative center position...
//                        elementX = elementX - (DataObjectAttributeExtension.DEFAULT_WIDTH / 2);
//                        elementY = elementY - (DataObjectAttributeExtension.DEFAULT_HEIGHT / 2);
                        double elementX = data.getBounds().getPosition().getX() + 25;
                        double elementY = data.getBounds().getPosition().getY()
                                + data.getBounds().getDimension().getHeight()
                                + DataObjectAttributeExtension.DEFAULT_HEIGHT * (data.getDataAttributes().size() - 1);
                        attribute.getBounds().setPosition(elementX, elementY);
                        attribute.getBounds().setDimension(DataObjectAttributeExtension.DEFAULT_WIDTH,
                                DataObjectAttributeExtension.DEFAULT_HEIGHT);

                        logger.debug("new BPMN Data attribute Position = " + elementX + "," + elementY);
                    }
                    modelState.reset();
                    actionDispatcher.dispatchAfterNextUpdate(new SelectAction(),
                            new SelectAction(List.of(attribute.getId())));

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
        int nodeCounter = GModelUtil.generateId(BpmnPackage.Literals.DATA_ATTRIBUTE_EXTENSION_GNODE, elementTypeId,
                modelState);
        return "attribute-" + (nodeCounter + 1);
    }

}
