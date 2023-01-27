"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.LaneNode = exports.PoolNode = exports.Icon = exports.BPMNEdge = exports.isBoundaryEvent = exports.isBPMNNode = exports.isBPMNLabelNode = exports.isDataProcessingExtensionNode = exports.isDataObjectExtensionNode = exports.isGatewayNode = exports.isEventNode = exports.isLaneNode = exports.isContainerNode = exports.isPoolNode = exports.isTaskNode = exports.MultiLineTextNode = exports.DataProcessingExtensionNode = exports.DataObjectExtensionNode = exports.TextAnnotationNode = exports.MessageNode = exports.DataObjectNode = exports.GatewayNode = exports.EventNode = exports.TaskNode = exports.LabelNode = void 0;
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
const client_1 = require("@eclipse-glsp/client");
class LabelNode extends client_1.RectangularNode {
}
exports.LabelNode = LabelNode;
LabelNode.DEFAULT_FEATURES = [client_1.selectFeature, client_1.moveFeature, client_1.layoutContainerFeature, client_1.hoverFeedbackFeature];
class TaskNode extends client_1.RectangularNode {
}
exports.TaskNode = TaskNode;
TaskNode.DEFAULT_FEATURES = [
    client_1.connectableFeature,
    client_1.deletableFeature,
    client_1.selectFeature,
    client_1.boundsFeature,
    client_1.moveFeature,
    client_1.layoutContainerFeature,
    client_1.fadeFeature,
    client_1.hoverFeedbackFeature
];
/* CircularNode RectangularNode */
class EventNode extends client_1.CircularNode {
}
exports.EventNode = EventNode;
EventNode.DEFAULT_FEATURES = [
    client_1.connectableFeature,
    client_1.deletableFeature,
    client_1.selectFeature,
    client_1.boundsFeature,
    client_1.moveFeature,
    client_1.layoutContainerFeature,
    client_1.fadeFeature,
    client_1.hoverFeedbackFeature,
    client_1.popupFeature,
    client_1.nameFeature
];
// DiamondNode  //  Nameable, WithEditableLabel,
class GatewayNode extends client_1.DiamondNode {
}
exports.GatewayNode = GatewayNode;
GatewayNode.DEFAULT_FEATURES = [
    client_1.connectableFeature,
    client_1.deletableFeature,
    client_1.selectFeature,
    client_1.boundsFeature,
    client_1.moveFeature,
    client_1.layoutContainerFeature,
    client_1.fadeFeature,
    client_1.hoverFeedbackFeature,
    client_1.popupFeature,
    client_1.nameFeature
];
class DataObjectNode extends client_1.RectangularNode {
}
exports.DataObjectNode = DataObjectNode;
DataObjectNode.DEFAULT_FEATURES = [
    client_1.connectableFeature,
    client_1.deletableFeature,
    client_1.selectFeature,
    client_1.boundsFeature,
    client_1.moveFeature,
    client_1.layoutContainerFeature,
    client_1.fadeFeature,
    client_1.hoverFeedbackFeature,
    client_1.popupFeature,
    client_1.nameFeature
];
class MessageNode extends client_1.RectangularNode {
}
exports.MessageNode = MessageNode;
MessageNode.DEFAULT_FEATURES = [
    client_1.connectableFeature,
    client_1.deletableFeature,
    client_1.selectFeature,
    client_1.boundsFeature,
    client_1.moveFeature,
    client_1.layoutContainerFeature,
    client_1.fadeFeature,
    client_1.hoverFeedbackFeature,
    client_1.popupFeature
];
class TextAnnotationNode extends client_1.RectangularNode {
}
exports.TextAnnotationNode = TextAnnotationNode;
TextAnnotationNode.DEFAULT_FEATURES = [
    client_1.connectableFeature,
    client_1.deletableFeature,
    client_1.selectFeature,
    client_1.boundsFeature,
    client_1.moveFeature,
    client_1.layoutContainerFeature,
    client_1.fadeFeature,
    client_1.hoverFeedbackFeature,
    client_1.popupFeature
];
/**
 * @author Ali Nour Eldin
 */
class DataObjectExtensionNode extends client_1.RectangularNode {
}
exports.DataObjectExtensionNode = DataObjectExtensionNode;
DataObjectExtensionNode.DEFAULT_FEATURES = [
    client_1.connectableFeature,
    client_1.deletableFeature,
    client_1.selectFeature,
    client_1.boundsFeature,
    client_1.moveFeature,
    client_1.layoutContainerFeature,
    client_1.fadeFeature,
    client_1.hoverFeedbackFeature
];
class DataProcessingExtensionNode extends client_1.DiamondNode {
}
exports.DataProcessingExtensionNode = DataProcessingExtensionNode;
DataProcessingExtensionNode.DEFAULT_FEATURES = [
    client_1.connectableFeature,
    client_1.deletableFeature,
    client_1.selectFeature,
    client_1.boundsFeature,
    client_1.moveFeature,
    client_1.layoutContainerFeature,
    client_1.fadeFeature,
    client_1.hoverFeedbackFeature,
    client_1.popupFeature,
    client_1.nameFeature
];
/*
 * This class provides a new Node displaying a multiline textblock.
 * The node also allows editing the text.
 * We are using this model object for BPMN TextAnnotations.
 *
 * See: https://www.eclipse.org/glsp/documentation/rendering/#default-views
 */
class MultiLineTextNode extends client_1.ForeignObjectElement {
    constructor() {
        super(...arguments);
        this.isMultiLine = true;
        this.text = '';
        this.namespace = 'http://www.w3.org/1999/xhtml';
    }
    set bounds(bounds) {
        /* ignore set bounds, always use the parent's bounds */
    }
    get bounds() {
        if ((0, client_1.isBoundsAware)(this.parent)) {
            return {
                x: this.position.x,
                y: this.position.y,
                width: this.parent.bounds.width,
                height: this.parent.bounds.height
            };
        }
        return client_1.Bounds.EMPTY;
    }
    // @ts-expect-error Arguments are set in the element
    get code() {
        if (this.text === '') {
            const textArg = this.args['text'];
            if (typeof textArg === 'string') {
                this.text = textArg;
            }
        }
        return `<pre>${this.text}</pre>`;
    }
    get editControlDimension() {
        return {
            width: this.bounds.width - 4,
            height: this.bounds.height - 4
        };
    }
}
exports.MultiLineTextNode = MultiLineTextNode;
/*
 * Helper Methods to determind if a ModelElement is of a specific type
 * The methods return the corresponding node
 */
function isTaskNode(element) {
    return element instanceof TaskNode || false;
}
exports.isTaskNode = isTaskNode;
function isPoolNode(element) {
    return element instanceof PoolNode || false;
}
exports.isPoolNode = isPoolNode;
function isContainerNode(element) {
    return element instanceof LaneNode || element instanceof PoolNode || false;
}
exports.isContainerNode = isContainerNode;
function isLaneNode(element) {
    return element instanceof LaneNode || false;
}
exports.isLaneNode = isLaneNode;
function isEventNode(element) {
    return element instanceof EventNode || false;
}
exports.isEventNode = isEventNode;
function isGatewayNode(element) {
    return element instanceof GatewayNode || false;
}
exports.isGatewayNode = isGatewayNode;
/**
 * data extension

 * @param element
 * @returns
 */
function isDataObjectExtensionNode(element) {
    return element instanceof DataObjectExtensionNode || false;
}
exports.isDataObjectExtensionNode = isDataObjectExtensionNode;
function isDataProcessingExtensionNode(element) {
    return element instanceof DataProcessingExtensionNode || false;
}
exports.isDataProcessingExtensionNode = isDataProcessingExtensionNode;
/*
 * Indicates that the ModelElement has a independed BPNNLabel
 */
function isBPMNLabelNode(element) {
    return (element instanceof EventNode ||
        element instanceof GatewayNode ||
        element instanceof DataObjectNode ||
        element instanceof MessageNode ||
        element instanceof DataProcessingExtensionNode ||
        false);
}
exports.isBPMNLabelNode = isBPMNLabelNode;
/*
 * This method returns the BPMN Node Element of a given SModelElement.
 * The method detects if the given ModelElement is for example a SPort
 * or label:heading. In this case the method returns the parent element
 * which is a Task, Event or Gateway node
 */
function isBPMNNode(element) {
    return (element instanceof TaskNode ||
        element instanceof EventNode ||
        element instanceof GatewayNode ||
        element instanceof DataObjectNode ||
        element instanceof MessageNode ||
        element instanceof PoolNode ||
        element instanceof DataObjectExtensionNode ||
        element instanceof DataProcessingExtensionNode);
}
exports.isBPMNNode = isBPMNNode;
/*
 * Returns ture if the BPMN Node Elmeent is a BoundaryEvent
 */
function isBoundaryEvent(element) {
    return element instanceof EventNode && element.type === 'boundaryEvent';
}
exports.isBoundaryEvent = isBoundaryEvent;
class BPMNEdge extends client_1.SEdge {
}
exports.BPMNEdge = BPMNEdge;
class Icon extends client_1.SShapeElement {
    constructor() {
        super(...arguments);
        this.size = {
            width: 16,
            height: 16
        };
    }
}
exports.Icon = Icon;
Icon.DEFAULT_FEATURES = [client_1.boundsFeature, client_1.layoutContainerFeature, client_1.layoutableChildFeature, client_1.fadeFeature];
class PoolNode extends client_1.RectangularNode {
    constructor() {
        super(...arguments);
        this.name = '';
    }
    get editableLabel() {
        const label = this.children.find(element => element.type === 'label:heading');
        if (label && (0, client_1.isEditableLabel)(label)) {
            return label;
        }
        return undefined;
    }
}
exports.PoolNode = PoolNode;
PoolNode.DEFAULT_FEATURES = [
    client_1.deletableFeature,
    client_1.selectFeature,
    client_1.boundsFeature,
    client_1.layoutContainerFeature,
    client_1.fadeFeature,
    client_1.hoverFeedbackFeature,
    client_1.popupFeature,
    client_1.nameFeature,
    client_1.withEditLabelFeature
];
class LaneNode extends client_1.RectangularNode {
    constructor() {
        super(...arguments);
        this.name = '';
    }
    get editableLabel() {
        const label = this.children.find(element => element.type === 'label:heading');
        if (label && (0, client_1.isEditableLabel)(label)) {
            return label;
        }
        return undefined;
    }
}
exports.LaneNode = LaneNode;
LaneNode.DEFAULT_FEATURES = [
    client_1.deletableFeature,
    client_1.selectFeature,
    client_1.boundsFeature,
    client_1.moveFeature,
    client_1.layoutContainerFeature,
    client_1.fadeFeature,
    client_1.hoverFeedbackFeature,
    client_1.popupFeature,
    client_1.nameFeature,
    client_1.withEditLabelFeature
];
//# sourceMappingURL=model.js.map