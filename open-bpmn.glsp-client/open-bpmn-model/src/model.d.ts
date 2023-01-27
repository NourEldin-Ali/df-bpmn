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
import { Args, Bounds, CircularNode, DiamondNode, Dimension, EditableLabel, ForeignObjectElement, LayoutContainer, Nameable, RectangularNode, SArgumentable, SChildElement, SEdge, SModelElement, SShapeElement, WithEditableLabel } from '@eclipse-glsp/client';
export interface BPMNFlowElement {
}
export declare class LabelNode extends RectangularNode {
    static readonly DEFAULT_FEATURES: symbol[];
}
export declare class TaskNode extends RectangularNode implements BPMNFlowElement {
    static readonly DEFAULT_FEATURES: symbol[];
}
export declare class EventNode extends CircularNode implements BPMNFlowElement {
    static readonly DEFAULT_FEATURES: symbol[];
    kind?: string;
    documentation: string;
}
export declare class GatewayNode extends DiamondNode implements BPMNFlowElement {
    static readonly DEFAULT_FEATURES: symbol[];
    kind?: string;
    documentation: string;
}
export declare class DataObjectNode extends RectangularNode implements BPMNFlowElement {
    static readonly DEFAULT_FEATURES: symbol[];
    documentation: string;
}
export declare class MessageNode extends RectangularNode implements BPMNFlowElement {
    static readonly DEFAULT_FEATURES: symbol[];
    documentation: string;
}
export declare class TextAnnotationNode extends RectangularNode implements BPMNFlowElement {
    static readonly DEFAULT_FEATURES: symbol[];
    documentation: string;
}
/**
 * @author Ali Nour Eldin
 */
export declare class DataObjectExtensionNode extends RectangularNode implements BPMNFlowElement {
    static readonly DEFAULT_FEATURES: symbol[];
}
export declare class DataProcessingExtensionNode extends DiamondNode implements BPMNFlowElement {
    static readonly DEFAULT_FEATURES: symbol[];
    kind?: string;
    documentation: string;
}
export declare class MultiLineTextNode extends ForeignObjectElement implements SArgumentable, EditableLabel {
    readonly isMultiLine = true;
    readonly args: Args;
    text: string;
    set bounds(bounds: Bounds);
    get bounds(): Bounds;
    get code(): string;
    namespace: string;
    get editControlDimension(): Dimension;
}
export declare function isTaskNode(element: SModelElement): element is TaskNode;
export declare function isPoolNode(element: SModelElement): element is PoolNode;
export declare function isContainerNode(element: SModelElement): element is LaneNode | PoolNode;
export declare function isLaneNode(element: SModelElement): element is LaneNode;
export declare function isEventNode(element: SModelElement): element is EventNode;
export declare function isGatewayNode(element: SModelElement): element is GatewayNode;
/**
 * data extension

 * @param element
 * @returns
 */
export declare function isDataObjectExtensionNode(element: SModelElement): element is DataObjectExtensionNode;
export declare function isDataProcessingExtensionNode(element: SModelElement): element is DataProcessingExtensionNode;
export declare function isBPMNLabelNode(element: SModelElement): element is SModelElement;
export declare function isBPMNNode(element: SModelElement): element is TaskNode | EventNode | GatewayNode;
export declare function isBoundaryEvent(element: SModelElement): element is EventNode;
export declare class BPMNEdge extends SEdge {
    kind?: string;
    documentation: string;
}
export declare class Icon extends SShapeElement implements LayoutContainer {
    static readonly DEFAULT_FEATURES: symbol[];
    layout: string;
    layoutOptions?: {
        [key: string]: string | number | boolean;
    };
    size: {
        width: number;
        height: number;
    };
}
export declare class PoolNode extends RectangularNode implements Nameable, WithEditableLabel {
    static readonly DEFAULT_FEATURES: symbol[];
    name: string;
    get editableLabel(): (SChildElement & EditableLabel) | undefined;
}
export declare class LaneNode extends RectangularNode implements Nameable, WithEditableLabel {
    static readonly DEFAULT_FEATURES: symbol[];
    name: string;
    get editableLabel(): (SChildElement & EditableLabel) | undefined;
}
//# sourceMappingURL=model.d.ts.map