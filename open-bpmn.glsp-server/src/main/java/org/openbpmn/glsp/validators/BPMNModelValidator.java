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
package org.openbpmn.glsp.validators;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.eclipse.glsp.graph.GEdge;
import org.eclipse.glsp.graph.GModelElement;
import org.eclipse.glsp.graph.GNode;
import org.eclipse.glsp.server.features.validation.Marker;
import org.eclipse.glsp.server.features.validation.MarkerKind;
import org.eclipse.glsp.server.features.validation.ModelValidator;
import org.openbpmn.bpmn.BPMNNS;
import org.openbpmn.bpmn.BPMNTypes;
import org.openbpmn.bpmn.elements.DataFlowExtension;
import org.openbpmn.bpmn.elements.DataInputObjectExtension;
import org.openbpmn.bpmn.elements.DataObjectAttributeExtension;
import org.openbpmn.bpmn.elements.DataOutputObjectExtension;
import org.openbpmn.bpmn.elements.DataProcessingExtension;
import org.openbpmn.bpmn.elements.core.BPMNElement;
import org.openbpmn.glsp.bpmn.DataAttributeExtensionGNode;
import org.openbpmn.glsp.bpmn.DataObjectExtensionGNode;
import org.openbpmn.glsp.bpmn.DataProcessingExtenstionGNode;
import org.openbpmn.glsp.elements.dataextension.DataInputExtensionGNodeBuilder;
import org.openbpmn.glsp.model.BPMNGModelState;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.inject.Inject;

/**
 * The BPMNModelValidator is used to validate the complete model. It is
 * triggered when the client starts 'Validate model' action.
 * <p>
 * The Result of a validaten can be verfied in the 'Problems View' from Theia.
 *
 * <p>
 * Currently we do not validate any model logic here.
 *
 * @author rsoika
 * @see: https://www.eclipse.org/glsp/documentation/validation/
 */
public class BPMNModelValidator implements ModelValidator {
    private static Logger logger = Logger.getLogger(BPMNModelValidator.class.getName());

    @Inject
    protected BPMNGModelState modelState;

    @Override
    public List<Marker> validate(final GModelElement... elements) {
        logger.fine("...starting validating model...");
        List<Marker> markers = new ArrayList<>();

        // DF-BPMN validator
        markers.addAll(DFBPMNValidator(elements));
//		for (GModelElement element : elements) {
//			if (element instanceof GNode) {
//				if (element.getId().contentEquals("task_tKuyrg")) {
//					markers.add(validateGNode((GNode) element));
//				}
//			}
//			if (element instanceof GEdge) {
//				if (element.getId().contentEquals("DataFlowExtension_5qTM0A")) {
//					markers.add(validateGEdge((GEdge) element));
//				}
//			}
//			element.getChildren().forEach(child -> markers.addAll(validate(child)));
//		}
        return markers;
    }

    public List<Marker> DFBPMNValidator(final GModelElement... elements) {
        List<Marker> markers = new ArrayList<>();
        for (GModelElement element : elements) {
            if (element instanceof GNode) {
                if (element instanceof DataObjectExtensionGNode || element instanceof DataAttributeExtensionGNode
                        || element instanceof DataProcessingExtenstionGNode) {
                    Marker validatorMarker = validateDataElements((GNode) element);
                    if (validatorMarker != null)
                        markers.add(validatorMarker);
                }
            } else if (element instanceof GEdge) {
                if (element.getType().contentEquals(BPMNTypes.DATA_FLOW)) {
                    Marker validatorMarker = validateDataFlow((GEdge) element);
                    if (validatorMarker != null) {
                        markers.add(validatorMarker);
                    }

                }
            }
            element.getChildren().forEach(child -> markers.addAll(DFBPMNValidator(child)));
        }
        return markers;
    }

    // create a dummy marker
    protected Marker validateDataElements(final GNode element) {

//		System.out.println(element.getType());
//		System.out.println(element.getArgs().get("JSONFormsData"));
        Map<String, Object> data = new Gson().fromJson((String) element.getArgs().get("JSONFormsData"),
                new TypeToken<HashMap<String, Object>>() {
                }.getType());

        // check if output state is deleted
        if (element.getType().contentEquals(BPMNTypes.DATA_OUTPUT_OBJECT_DATA_STORE)) {
            if (data.get("state").toString().contentEquals("delete")) {
//				System.out.println(element.getChildren().size());
                if (element.getChildren().size() > 3) {
                    return new Marker("Node",
                            "The data store output with 'delete' state should not have any attributes", element.getId(),
                            MarkerKind.ERROR);
                }
            }

            DataOutputObjectExtension bpmnElement = (DataOutputObjectExtension) modelState.getBpmnModel()
                    .findElementDataExtensionNodeById(element.getId());
            if (bpmnElement != null) {
                if (!bpmnElement.getAttribute("state").contentEquals("read")) {
                    Map<String, Object> values = modelState.getBpmnModel().openDefaultProces().getDataStoresExtensions()
                            .get(bpmnElement.getName());
//					System.out.println(values.get("readonly"));
                    if (Boolean.parseBoolean((String) values.get("readonly")) == true) {
                        return new Marker("Node",
                                "The data store output is read only data object, should not used as output data for insert/update/delete",
                                element.getId(), MarkerKind.ERROR);
                    }
                }


            }
        }

        // check attribute
        if (element.getType().contentEquals(BPMNTypes.DATA_OBJECT_ATTRIBUTE)) {
            // check the attribute of the attribute if data store output with state "delete"
            GNode parentElement = (GNode) element.getParent();
            if (parentElement.getType().contentEquals(BPMNTypes.DATA_OUTPUT_OBJECT_DATA_STORE)) {
//				Map<String, Map<String, Object>> dataStoreVariables = openProcess.getDataStoresExtensions();

                Map<String, Object> parentData = new Gson().fromJson(
                        (String) parentElement.getArgs().get("JSONFormsData"),
                        new TypeToken<HashMap<String, Object>>() {
                        }.getType());
                if (parentData.get("state").toString().contentEquals("delete")) {
                    if (parentElement.getChildren().size() > 3) {
                        return new Marker("Node",
                                "The data store output with 'delete' state should not have any attributes",
                                element.getId(), MarkerKind.ERROR);
                    }
                }
            }
            DataObjectAttributeExtension bpmnElement = (DataObjectAttributeExtension) modelState.getBpmnModel()
                    .findElementDataExtensionNodeById(element.getId());

            boolean isConnectedToInput = bpmnElement.hasChildNode(BPMNNS.BPMN2, "incoming");
            boolean isConnectedToOutput = bpmnElement.hasChildNode(BPMNNS.BPMN2, "outgoing");
            if (!isConnectedToInput && !isConnectedToOutput) {
                return new Marker("Node", "Data attribute should be connect to other data object", element.getId(),
                        MarkerKind.ERROR);
            }

        }

        // check if data item is data processing
        if (element.getType().contentEquals(BPMNTypes.DATA_PROCESSING)) {
            DataProcessingExtension bpmnElement = (DataProcessingExtension) modelState.getBpmnModel()
                    .findElementDataExtensionNodeById(element.getId());
            boolean isConnectedToInput = bpmnElement.hasChildNode(BPMNNS.BPMN2, "incoming");
            boolean isConnectedToOutput = bpmnElement.hasChildNode(BPMNNS.BPMN2, "outgoing");

            if (!isConnectedToInput && !isConnectedToOutput) {
                return new Marker("Node", "Data processing operater should be connect to input and output data objects",
                        element.getId(), MarkerKind.ERROR);
            } else if (!isConnectedToInput) {
                return new Marker("Node", "Data processing operater should be connect to input data object",
                        element.getId(), MarkerKind.ERROR);
            } else if (!isConnectedToOutput) {
                return new Marker("Node", "Data processing operater should be connect to output data object",
                        element.getId(), MarkerKind.ERROR);
            }
            // validate if the user add gherkin
            boolean isGherkinAdded = bpmnElement.hasAttribute("gherkin")
                    && !bpmnElement.getAttribute("gherkin").isEmpty();
            if (!isGherkinAdded) {
                return new Marker("Node", "Data processing operater should be contain the gherkin to be executed",
                        element.getId(), MarkerKind.ERROR);
            }

        } else
            // check the data object types
            if (data.get("type").toString().toLowerCase().contentEquals("none")) {
                return new Marker("Node", "You should select a data type", element.getId(), MarkerKind.ERROR);
            }

        try {

            // check if the data object if connected by a data flow
            DataInputObjectExtension bpmnInputElement = (DataInputObjectExtension) modelState.getBpmnModel()
                    .findElementDataExtensionNodeById(element.getId());
            if (bpmnInputElement != null) {
                boolean hasAttribute = bpmnInputElement.hasChildNode(BPMNNS.BPMN2, "attribute");
                boolean isConnectedToInput = bpmnInputElement.hasChildNode(BPMNNS.BPMN2, "incoming");
                boolean isConnectedToOutput = bpmnInputElement.hasChildNode(BPMNNS.BPMN2, "outgoing");
                if ((!isConnectedToInput && !isConnectedToOutput) && !hasAttribute) {
                    return new Marker("Node", "Data input should be connect to other data object", element.getId(),
                            MarkerKind.ERROR);
                }
            }
        } catch (Exception e) {
        }

        try {
            DataOutputObjectExtension bpmnOutputElement = (DataOutputObjectExtension) modelState.getBpmnModel()
                    .findElementDataExtensionNodeById(element.getId());

            if (bpmnOutputElement != null) {
                boolean hasAttribute = bpmnOutputElement.hasChildNode(BPMNNS.BPMN2, "attribute");
                boolean isConnectedToInput = bpmnOutputElement.hasChildNode(BPMNNS.BPMN2, "incoming");
                boolean isConnectedToOutput = bpmnOutputElement.hasChildNode(BPMNNS.BPMN2, "outgoing");
                System.out.println(bpmnOutputElement.getName());
                System.out.println(hasAttribute);
                System.out.println(isConnectedToInput);
                System.out.println(isConnectedToOutput);
                System.out.println("***************************");
                if ((!isConnectedToInput && !isConnectedToOutput) && !hasAttribute && !bpmnOutputElement.getAttribute("state").contentEquals("delete")) {
                    return new Marker("Node", "Data attribute should be connect to other data object", element.getId(),
                            MarkerKind.ERROR);
                }
            }
        } catch (Exception e) {
        }
        return null;
    }

    // create a dummy marker
    protected Marker validateDataFlow(final GEdge element) {
//		System.out.println(element.getTarget().getType());
//		System.out.println(element.getSourceId());
//		System.out.println(element.getTargetId());

        if (element.getTarget().getType().contentEquals(BPMNTypes.DATA_PROCESSING)
                || element.getSource().getType().contentEquals(BPMNTypes.DATA_PROCESSING)) {
            return null;
        }

        BPMNElement source = modelState.getBpmnModel().findElementDataExtensionNodeById(element.getSourceId());
        BPMNElement target = modelState.getBpmnModel().findElementDataExtensionNodeById(element.getTargetId());

        if (source.hasAttribute("type") && target.hasAttribute("type")) {
            if (!source.getAttribute("type").contentEquals(target.getAttribute("type"))) {
                return new Marker("Edge", "The source and hte target should have the same type", element.getId(),
                        MarkerKind.ERROR);
            }
        }
        return null;
    }

    // create a dummy marker
//	protected Marker validateGNode(final GNode element) {
//		return new Marker("Node", "This graphical element is a node", element.getId(), MarkerKind.ERROR);
//
//	}
//
//	protected Marker validateGEdge(final GEdge element) {
//		return new Marker("Edge", "This graphical element is a edge", element.getId(), MarkerKind.WARNING);
//
//	}

}