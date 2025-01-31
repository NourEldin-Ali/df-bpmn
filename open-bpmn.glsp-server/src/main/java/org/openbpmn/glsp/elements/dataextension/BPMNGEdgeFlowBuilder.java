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

import org.eclipse.glsp.graph.GLabel;
import org.eclipse.glsp.graph.builder.AbstractGEdgeBuilder;
import org.eclipse.glsp.graph.builder.impl.GEdgePlacementBuilder;
import org.eclipse.glsp.graph.builder.impl.GLabelBuilder;
import org.eclipse.glsp.graph.util.GConstants;
import org.openbpmn.bpmn.BPMNTypes;
import org.openbpmn.bpmn.elements.core.BPMNElementEdge;
import org.openbpmn.glsp.bpmn.BPMNGEdge;
import org.openbpmn.glsp.bpmn.BpmnFactory;
import org.openbpmn.glsp.utils.ModelTypes;

/**
 * BPMN SequenceFlow
 *
 * @author rsoika
 *
 */
public class BPMNGEdgeFlowBuilder extends AbstractGEdgeBuilder<BPMNGEdge, BPMNGEdgeFlowBuilder> {

    private final String name;

    public BPMNGEdgeFlowBuilder(final BPMNElementEdge edge) {
        super(edge.getType());
        this.name = edge.getName();
        this.id = edge.getId();
        if (BPMNTypes.DATA_FLOW.contentEquals(type)) {
            this.addCssClass(BPMNTypes.SEQUENCE_FLOW);
        } else if (BPMNTypes.DATA_REFERENCE.contentEquals(type)) {
            this.addCssClass(BPMNTypes.ASSOCIATION);
        }
//        this.addCssClass(type);
        this.addCssClass("bpmnedge");

        if (edge.getElementNode().getParentNode().getAttributes().getNamedItem("expand").getNodeValue()
                .contentEquals("false")) {
            this.addCssClass("hideElement");
        }
    }

    @Override
    protected void setProperties(final BPMNGEdge edge) {
        super.setProperties(edge);
        edge.setName(name);
        // set the custom BPMN Router Kind
        edge.setRouterKind(GConstants.RouterKind.MANHATTAN);
        // set the label
        GLabel edgeLabel = new GLabelBuilder(ModelTypes.LABEL_HEADING) //
                .edgePlacement(new GEdgePlacementBuilder()//
                        .side(GConstants.EdgeSide.TOP)//
                        .position(0.5d) // center (50%)
                        .offset(3.0d) // 3px offset
                        .rotate(false) //
                        .build())//
                .id(id + "_bpmnlabel") //
                .text(edge.getName()).build();

        edge.getChildren().add(edgeLabel);
    }

    @Override
    protected BPMNGEdge instantiate() {
        return BpmnFactory.eINSTANCE.createBPMNGEdge();
    }

    @Override
    protected BPMNGEdgeFlowBuilder self() {
        return this;
    }

}
