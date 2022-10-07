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
package org.openbpmn.glsp.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import org.eclipse.emf.common.util.EList;
import org.eclipse.glsp.graph.GCompartment;
import org.eclipse.glsp.graph.GLabel;
import org.eclipse.glsp.graph.GModelElement;
import org.eclipse.glsp.graph.builder.impl.GCompartmentBuilder;
import org.eclipse.glsp.graph.builder.impl.GLabelBuilder;
import org.eclipse.glsp.graph.util.GConstants;
import org.openbpmn.glsp.bpmn.BaseElementGNode;
import org.openbpmn.glsp.bpmn.IconGNode;
import org.openbpmn.glsp.elements.IconGNodeBuilder;
import org.openbpmn.model.BPMNGModelState;

/**
 * The BPMNBuilderHelper provides helper methods to create GNode Elements
 *
 * @author rsoika
 *
 */
public class BPMNBuilderHelper {
    @SuppressWarnings("unused")
    private static Logger logger = Logger.getLogger(BPMNBuilderHelper.class.getName());

    public static IconGNode createCompartmentIcon(final BaseElementGNode node) {
        return new IconGNodeBuilder(). //
                id(node.getId() + "_icon"). //
                build();
    }

    /**
     * Creates a GLabel for the name of a BaseElement
     *
     * @param node
     * @return GPort
     */
    public static GLabel createCompartmentHeader(final BaseElementGNode node) {
        return new GLabelBuilder(ModelTypes.LABEL_HEADING) //
                .id(node.getId() + "_header") //
                .text(node.getName()) //
                .build();
    }

    /**
     * Creates the Header for a BPMNPool or BPMNLane
     *
     * @param node
     * @return GCompartment
     */
    public static GCompartment createBPMNContainerHeader(final BaseElementGNode node) {
        Map<String, Object> layoutOptions = new HashMap<>();

        GLabel glabel = new GLabelBuilder(ModelTypes.LABEL_HEADING) //
                .id(node.getId() + "_header_label") //
                .text(node.getName()) //
                .build();

//        GLabel glabel = createCompartmentHeader(node);

        return new GCompartmentBuilder(ModelTypes.COMP_HEADER) //
                .id(node.getId() + "_header") //
                .layout(GConstants.Layout.HBOX) //
                .layoutOptions(layoutOptions) //
                .add(glabel) //
                .addCssClass("label") //
                .build();
    }

    /**
     * This method tests if the given element has a Child of type GLabel. This is
     * the case for Task Elements. In this case the method returns the GLabel.
     * Otherwise the method returns null.
     *
     * @return GLabel of an element or null if no GLabel was found
     */
    public static GLabel findCompartmentHeader(final BaseElementGNode element) {

        EList<GModelElement> childs = element.getChildren();
        for (GModelElement child : childs) {
            if (child instanceof GLabel) {
                // return Optional.of(child);
                return (GLabel) child;
            }
        }
        // we did not found a GLabel
        return null;
    }

    public static GLabel findPoolLabel(final BPMNGModelState modelState, final String poolID) {

        Optional<GModelElement> elementHeader = modelState.getIndex().get(poolID + "_header");
        if (elementHeader.isPresent()) {

            GModelElement headerElement = elementHeader.get();
            EList<GModelElement> childs = headerElement.getChildren();
            for (GModelElement child : childs) {
                if (child instanceof GLabel) {
                    // return Optional.of(child);
                    return (GLabel) child;
                }
            }

        }
        // we did not found a GLabel
        return null;
    }

}
