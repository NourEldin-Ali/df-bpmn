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

import java.util.logging.Logger;

import org.eclipse.glsp.graph.builder.AbstractGNodeBuilder;
import org.eclipse.glsp.graph.builder.impl.GArguments;
import org.eclipse.glsp.graph.builder.impl.GLayoutOptions;
import org.eclipse.glsp.graph.util.GConstants;
import org.eclipse.glsp.graph.util.GraphUtil;
import org.openbpmn.bpmn.elements.DataObjectAttributeExtension;
import org.openbpmn.bpmn.elements.core.BPMNBounds;
import org.openbpmn.bpmn.exceptions.BPMNMissingElementException;
import org.openbpmn.glsp.bpmn.BpmnFactory;
import org.openbpmn.glsp.bpmn.DataAttributeExtensionGNode;
import org.openbpmn.glsp.bpmn.IconGCompartment;
import org.openbpmn.glsp.elements.IconGCompartmentBuilder;
import org.openbpmn.glsp.model.BPMNGModelFactory;
import org.openbpmn.glsp.utils.BPMNGraphUtil;

/**
 * BPMN 2.0 Task Element.
 * <p>
 * The method builds a GNode from a BPMNTask element. The builder is called from
 * the method createGModelFromProcess of the BPMNGModelFactory.
 *
 * @author Ali Nour Eldin
 *
 */
public class DataAttributeExtensionGNodeBuilder
        extends AbstractGNodeBuilder<DataAttributeExtensionGNode, DataAttributeExtensionGNodeBuilder> {

    private static Logger logger = Logger.getLogger(BPMNGModelFactory.class.getName());
    private static final String V_GRAB = "vGrab";
    private static final String H_GRAB = "hGrab";
    private final String name;

    public DataAttributeExtensionGNodeBuilder(final DataObjectAttributeExtension data) {
        super(data.getType());
        this.name = data.getName();
        this.id = data.getId();

        try {
            BPMNBounds bpmnBounds = data.getBounds();
            this.size = GraphUtil.dimension(bpmnBounds.getDimension().getWidth(),
                    bpmnBounds.getDimension().getHeight());
//            System.out.println(data.getBounds().getPosition());
        } catch (BPMNMissingElementException e) {
            // should not happen
            logger.severe("BPMN-DataInputExtension does not support a BPMNBounds object!");
        }
        // set Layout options
        this.addCssClass(type);
        this.addCssClass("dataAttributeExtension");

        this.addArguments(GArguments.cornerRadius(0));

        if (data.getElementNode().getParentNode().getParentNode().getAttributes().getNamedItem("expand").getNodeValue()
                .contentEquals("false")) {
            this.addCssClass("hideElement");
        }
    }

    @Override
    protected DataAttributeExtensionGNode instantiate() {
        return BpmnFactory.eINSTANCE.createDataAttributeExtensionGNode();
    }

    @Override
    protected DataAttributeExtensionGNodeBuilder self() {
        return this;
    }

    @Override
    public void setProperties(final DataAttributeExtensionGNode node) {
        super.setProperties(node);
        node.setName(name);

        node.setLayout(GConstants.Layout.VBOX);
//        node.getLayoutOptions().put(GLayoutOptions.KEY_H_ALIGN, GConstants.HAlign.CENTER);
//        node.getLayoutOptions().put(GLayoutOptions.KEY_V_ALIGN, GConstants.VAlign.CENTER);
        // Set min width/height
        node.getLayoutOptions().put(GLayoutOptions.KEY_MIN_WIDTH, DataObjectAttributeExtension.DEFAULT_WIDTH);
        node.getLayoutOptions().put(GLayoutOptions.KEY_MIN_HEIGHT, DataObjectAttributeExtension.DEFAULT_HEIGHT);

        node.getLayoutOptions().put(H_GRAB, true);
        node.getLayoutOptions().put(V_GRAB, true);

//        node.getLayoutOptions().put(GLayoutOptions.KEY_PREF_WIDTH, size.getWidth());
//        node.getLayoutOptions().put(GLayoutOptions.KEY_PREF_HEIGHT, size.getHeight());

        node.getLayoutOptions().put(GLayoutOptions.KEY_V_GAP, 1);

        IconGCompartment taskIcon = new IconGCompartmentBuilder(). //
                id(node.getId() + "_icon"). //
                layoutOptions(new GLayoutOptions().hAlign(GConstants.HAlign.LEFT)). //
                build();

        node.getChildren().add(taskIcon);
        // node.getChildren().add(BPMNGraphUtil.createCompartmentHeader(node));

        node.getChildren().add(BPMNGraphUtil.createMultiLineTextNode(id + "_name", name));

    }

}
