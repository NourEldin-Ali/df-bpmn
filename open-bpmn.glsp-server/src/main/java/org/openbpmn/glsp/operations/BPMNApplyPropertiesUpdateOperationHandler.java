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
package org.openbpmn.glsp.operations;

import java.io.StringReader;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.eclipse.glsp.graph.GModelElement;
import org.eclipse.glsp.server.actions.ActionDispatcher;
import org.eclipse.glsp.server.operations.AbstractOperationHandler;
import org.openbpmn.bpmn.elements.BPMNElementNode;
import org.openbpmn.extension.BPMNExtension;
import org.openbpmn.glsp.bpmn.BPMNGNode;
import org.openbpmn.glsp.model.BPMNGModelState;

import com.google.inject.Inject;

/**
 * The BPMNApplyPropertiesUpdateOperationHandler is responsible for processing
 * the {@link BPMNApplyPropertiesUpdateOperation} send by the client for updates
 * of the element properties
 *
 */
public class BPMNApplyPropertiesUpdateOperationHandler
        extends AbstractOperationHandler<BPMNApplyPropertiesUpdateOperation> {
    private static Logger logger = Logger.getLogger(BPMNApplyPropertiesUpdateOperationHandler.class.getName());

    @Inject
    protected ActionDispatcher actionDispatcher;

    @Inject
    protected BPMNGModelState modelState;

    @Inject
    protected Set<BPMNExtension> extensions;

    /**
     *
     */
    @Override
    protected void executeOperation(final BPMNApplyPropertiesUpdateOperation operation) {
        long l = System.currentTimeMillis();
        String jsonData = operation.getJsonData();

        // validate GModel id
        String elementID = operation.getId();
        GModelElement gModelElement = null;
        BPMNElementNode bpmnElement = null;
        if (modelState.getRoot().getId().equals(elementID)) {
            // bpmnElement = modelState.getBpmnModel().openDefaultProcess();
            gModelElement = modelState.getRoot();
        }
        Optional<BPMNGNode> _baseElement = modelState.getIndex().findElementByClass(elementID, BPMNGNode.class);
        if (!_baseElement.isEmpty()) {
            gModelElement = _baseElement.get();
            bpmnElement = modelState.getBpmnModel().findBPMNNodeById(elementID);
            // throw new RuntimeException("Cannot find BaseElement with id '" +
            // operation.getId() + "'");
        }

        // validate BPMN element

        if (bpmnElement == null) {
            throw new IllegalArgumentException(
                    "BPMN Element with id " + operation.getId() + " is not defined in current model!");
        }
        // parse json....
        JsonObject json = null;
        try (JsonReader reader = Json.createReader(new StringReader(jsonData))) {
            json = reader.readObject();
        } catch (JsonException e) {
            throw new RuntimeException("Cannot read json data : " + e.getMessage());
        }

        // Now call the extensions to update the property data according to the BPMN
        // element. The updatePropertiesData can also update the given JSON object!
        if (extensions != null) {
            for (BPMNExtension extension : extensions) {
                // validate if the extension can handle this BPMN element
                if (extension.handlesBPMNElement(bpmnElement)) {
                    extension.updatePropertiesData(json, bpmnElement, gModelElement);

//                    if (BPMNModel.isEvent(bpmnElement) || BPMNModel.isGateway(bpmnElement)) {
//                        extension.updatePropertiesData(json, bpmnElement, element.get());
//                    } else {
//                        extension.updatePropertiesData(json, bpmnElement, element.get());
//                    }
                }
            }
        }

        // finally we need to update the JSONFormsData property of the selected element
        gModelElement.getArgs().put("JSONFormsData", json.toString());
        logger.info("....execute Update " + operation.getId() + " in " + (System.currentTimeMillis() - l) + "ms");

    }

}
