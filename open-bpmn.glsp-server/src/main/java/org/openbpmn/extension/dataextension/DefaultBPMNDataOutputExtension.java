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
package org.openbpmn.extension.dataextension;

import java.util.Set;

import javax.json.JsonObject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.glsp.graph.GModelElement;
import org.eclipse.glsp.graph.GNode;
import org.openbpmn.bpmn.BPMNTypes;
import org.openbpmn.bpmn.elements.DataOutputObjectExtension;
import org.openbpmn.bpmn.elements.core.BPMNElement;
import org.openbpmn.extension.AbstractBPMNElementExtension;
import org.openbpmn.glsp.bpmn.BPMNGNode;
import org.openbpmn.glsp.jsonforms.DataBuilder;
import org.openbpmn.glsp.jsonforms.SchemaBuilder;
import org.openbpmn.glsp.jsonforms.UISchemaBuilder;
import org.openbpmn.glsp.jsonforms.UISchemaBuilder.Layout;
import org.openbpmn.glsp.utils.BPMNGraphUtil;

/**
 * This is the Default BPMNEvent extension providing the JSONForms shemata.
 *
 * @author Ali Nour Eldin
 *
 */
public class DefaultBPMNDataOutputExtension extends AbstractBPMNElementExtension {

    private static Logger logger = LogManager.getLogger(DefaultBPMNDataOutputExtension.class);

    public DefaultBPMNDataOutputExtension() {
        super();
    }

    @Override
    public boolean handlesElementTypeId(final String elementTypeId) {
        return BPMNTypes.BPMN_DATA_OUTPUT_EXTENSION.contains(elementTypeId);
    }

    /**
     * This Extension is for BPMNActivities only
     */
    @Override
    public boolean handlesBPMNElement(final BPMNElement bpmnElement) {
        return (bpmnElement instanceof DataOutputObjectExtension);
    }

    /**
     * This Helper Method generates a JSON Object with the BPMNElement properties.
     * <p>
     * This json object is used on the GLSP Client to generate the EMF JsonForms
     */
    @Override
    public void buildPropertiesForm(final BPMNElement bpmnElement, final DataBuilder dataBuilder,
            final SchemaBuilder schemaBuilder, final UISchemaBuilder uiSchemaBuilder) {

        dataBuilder //
                .addData("name", bpmnElement.getName()) //
                .addData("type", bpmnElement.getAttribute("type")) //
                .addData("state", bpmnElement.getAttribute("state")) //
                .addData("isMultiple", bpmnElement.getAttribute("isMultiple").contentEquals("true") ? true : false);

        schemaBuilder //
                .addProperty("name", "string", null) //
                .addProperty("type", "string", null) //
                .addProperty("state", "string", null) //
                .addProperty("isMultiple", "boolean", null) //

        ;

//Map<String, String> multilineOption = new HashMap<>();
//multilineOption.put("multi", "true");
        uiSchemaBuilder. //
                addCategory("General") //
                .addLayout(Layout.HORIZONTAL) //
                .addElements("name") //
                .addElements("type") //
                .addElements("state") //
                .addElements("isMultiple") //
//        .addLayout(Layout.VERTICAL) //

        ;

    }

    @Override
    public void updatePropertiesData(final JsonObject json, String category,final BPMNElement bpmnElement,
            final GModelElement gNodeElement) {

        // default update of name and documentation

        Set<String> features = json.keySet();
        for (String feature : features) {
            if ("name".equals(feature)) {
                String text = json.getString(feature);
                if (text.contains(":")) {
                    bpmnElement.setName(text.split(":")[0]);
                    bpmnElement.setAttribute("type", text.split(":")[1]);
                    // update the bpmn-text-node of the GNodeElement
                    GNode gnode = BPMNGraphUtil.findMultiLineTextNode((BPMNGNode) gNodeElement);
                    if (gnode != null) {
                        gnode.getArgs().put("text", text.split(":")[0]);
                    }

                } else {
                    bpmnElement.setName(text);
                    // update the bpmn-text-node of the GNodeElement
                    GNode gnode = BPMNGraphUtil.findMultiLineTextNode((BPMNGNode) gNodeElement);
                    if (gnode != null) {
                        gnode.getArgs().put("text", text);
                    }

                }
                continue;
            }

            if ("isMultiple".equals(feature)) {
                bpmnElement.setAttribute(feature, json.getBoolean(feature) == true ? "true" : "false");
                continue;
            }
            bpmnElement.setAttribute(feature, json.getString(feature));

        }

    }

}
