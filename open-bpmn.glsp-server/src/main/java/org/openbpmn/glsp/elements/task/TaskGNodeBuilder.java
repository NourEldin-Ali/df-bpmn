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
package org.openbpmn.glsp.elements.task;

import java.util.logging.Logger;

import org.eclipse.glsp.graph.builder.AbstractGNodeBuilder;
import org.eclipse.glsp.graph.builder.impl.GArguments;
import org.eclipse.glsp.graph.builder.impl.GLayoutOptions;
import org.eclipse.glsp.graph.util.GConstants;
import org.eclipse.glsp.graph.util.GraphUtil;
import org.openbpmn.bpmn.elements.Activity;
import org.openbpmn.bpmn.elements.core.BPMNBounds;
import org.openbpmn.bpmn.exceptions.BPMNMissingElementException;
import org.openbpmn.glsp.bpmn.BpmnFactory;
import org.openbpmn.glsp.bpmn.IconGCompartment;
import org.openbpmn.glsp.bpmn.TaskGNode;
import org.openbpmn.glsp.elements.IconGCompartmentBuilder;
import org.openbpmn.glsp.model.BPMNGModelFactory;
import org.openbpmn.glsp.utils.BPMNGraphUtil;

/**
 * BPMN 2.0 Task Element.
 * <p>
 * The method builds a GNode from a BPMNTask element. The builder is called from
 * the method createGModelFromProcess of the BPMNGModelFactory.
 *
 * @author rsoika
 *
 */
public class TaskGNodeBuilder extends AbstractGNodeBuilder<TaskGNode, TaskGNodeBuilder> {

    private static Logger logger = Logger.getLogger(BPMNGModelFactory.class.getName());
    private static final String V_GRAB = "vGrab";
    private static final String H_GRAB = "hGrab";
    private final String name;

    public TaskGNodeBuilder(final Activity activity) {
        super(activity.getType());
        this.name = activity.getName();
        this.id = activity.getId();

        try {

            BPMNBounds bpmnBounds = activity.getBounds();
            if (activity.getAttribute("expand").contentEquals("true")) {
                this.size = GraphUtil.dimension(bpmnBounds.getDimension().getWidth(),
                        bpmnBounds.getDimension().getHeight());
            } else {
                this.size = GraphUtil.dimension(Activity.DEFAULT_WIDTH, Activity.DEFAULT_HEIGHT);
            }

        } catch (BPMNMissingElementException e) {
            // should not happen
            logger.severe("BPMNActivity does not support a BPMNBounds object!");
        }
        // set Layout options
        this.addCssClass(type);
        this.addCssClass("task");

        this.addArguments(GArguments.cornerRadius(5));
    }

    @Override
    protected TaskGNode instantiate() {
        return BpmnFactory.eINSTANCE.createTaskGNode();
    }

    @Override
    protected TaskGNodeBuilder self() {
        return this;
    }

    @Override
    public void setProperties(final TaskGNode node) {
        super.setProperties(node);
        node.setName(name);

        node.setLayout(GConstants.Layout.VBOX);
        // Set min width/height
        node.getLayoutOptions().put(GLayoutOptions.KEY_MIN_WIDTH, Activity.DEFAULT_WIDTH);
        node.getLayoutOptions().put(GLayoutOptions.KEY_MIN_HEIGHT, Activity.DEFAULT_HEIGHT);
        node.getLayoutOptions().put(H_GRAB, true);
        node.getLayoutOptions().put(V_GRAB, true);
        node.getLayoutOptions().put(GLayoutOptions.KEY_PREF_WIDTH, size.getWidth());
        node.getLayoutOptions().put(GLayoutOptions.KEY_PREF_HEIGHT, size.getHeight());
        node.getLayoutOptions().put(GLayoutOptions.KEY_V_GAP, 1);

        IconGCompartment taskIcon = new IconGCompartmentBuilder(). //
                id(node.getId() + "_icon"). //
                layoutOptions(new GLayoutOptions().hAlign(GConstants.HAlign.LEFT)). //
                build();

        node.getChildren().add(taskIcon);
        node.getChildren().add(BPMNGraphUtil.createMultiLineTextNode(id + "_name", name));

//        GModelElement b = new GNodeBuilder().type("node:expandable").addCssClass("node-expandable")
////                .position(point.orElse(GraphUtil.point(0, 0)))
//                .layout(GConstants.Layout.HBOX).layoutOptions(new GLayoutOptions().hGap(15))
//                .add(new GLabelBuilder().text("Expand").build()).add(new GButtonBuilder()
//                        .type(DefaultTypes.EXPAND_BUTTON).addCssClass("button-expand").enabled(true).build())
//                .build();
//
//        node.getChildren().add(b);

    }
//    @Override
//    public void setProperties(final TaskGNode node) {
//        super.setProperties(node);
//        node.setName(name);
//
//        node.setLayout(GConstants.Layout.VBOX);
//        // Set min width/height for the Pool element
//        node.getLayoutOptions().put(GLayoutOptions.KEY_MIN_WIDTH, Participant.MIN_WIDTH);
//        node.getLayoutOptions().put(GLayoutOptions.KEY_MIN_HEIGHT, Participant.MIN_HEIGHT);
////        node.getLayoutOptions().put(GLayoutOptions.KEY_PREF_WIDTH, size.getWidth());
////        node.getLayoutOptions().put(GLayoutOptions.KEY_PREF_HEIGHT, size.getHeight());
//
//        node.getChildren().add(BPMNGraphUtil.createBPMNContainerHeader(node));
//        node.getChildren().add(createContainerCompartment(node));
//
//    }
//
//    /**
//     * Creates the Container compartment used for the process BPMNFlowElements
//     *
//     * @param node
//     * @return
//     */
//    private GCompartment createContainerCompartment(final TaskGNode node) {
//        // DefaultTypes.NODE ModelTypes.CONTAINER
//        return new GCompartmentBuilder(ModelTypes.CONTAINER) //
//                .id(node.getId() + "_container") //
//                .layout(GConstants.Layout.FREEFORM) //
//                .build();
//    }

}
