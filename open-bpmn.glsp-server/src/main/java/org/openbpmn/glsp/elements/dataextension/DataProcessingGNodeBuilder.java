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

import org.eclipse.glsp.graph.builder.AbstractGNodeBuilder;
import org.eclipse.glsp.graph.util.GConstants;
import org.eclipse.glsp.graph.util.GraphUtil;
import org.openbpmn.bpmn.elements.DataProcessingExtension;
import org.openbpmn.glsp.bpmn.BpmnFactory;
import org.openbpmn.glsp.bpmn.DataProcessingExtenstionGNode;
import org.openbpmn.glsp.utils.BPMNGraphUtil;

/**
 * BPMN 2.0 Gateway Element.
 * <p>
 * The method builds a GNode from a BPMNGateway element. The builder is called
 * from the method createGModelFromProcess of the BPMNGModelFactory.
 *
 * @author rsoika
 *
 */
public class DataProcessingGNodeBuilder
        extends AbstractGNodeBuilder<DataProcessingExtenstionGNode, DataProcessingGNodeBuilder> {

    private final String name;

    public DataProcessingGNodeBuilder(final DataProcessingExtension dataProcessing) {
        super(dataProcessing.getType());
        this.name = dataProcessing.getName();
        this.id = dataProcessing.getId();

        // set Layout options
        this.addCssClass(type);
        this.addCssClass("dataProcessingExtension");
        if (dataProcessing.getElementNode().getParentNode().getAttributes().getNamedItem("expand").getNodeValue()
                .contentEquals("false")) {
            this.addCssClass("hideElement");
        }
    }

    @Override
    protected DataProcessingExtenstionGNode instantiate() {
        return BpmnFactory.eINSTANCE.createDataProcessingExtenstionGNode();
    }

    @Override
    protected DataProcessingGNodeBuilder self() {
        return this;
    }

    @Override
    public void setProperties(final DataProcessingExtenstionGNode node) {
        super.setProperties(node);
        node.setName(name);
        node.setLayout(GConstants.Layout.FREEFORM);
        size = GraphUtil.dimension(DataProcessingExtension.DEFAULT_WIDTH, DataProcessingExtension.DEFAULT_HEIGHT);
        node.setSize(size);

        node.getLayoutOptions().put("minWidth", DataProcessingExtension.DEFAULT_WIDTH);
        node.getLayoutOptions().put("minHeight", DataProcessingExtension.DEFAULT_HEIGHT);

        node.getChildren().add(BPMNGraphUtil.createCompartmentIcon(node));

    }

}
