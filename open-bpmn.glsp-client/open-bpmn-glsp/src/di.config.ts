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
import {
    boundsFeature,
    CircularNodeView,
    configureActionHandler,
    configureCommand,
    configureDefaultModelElements,
    configureModelElement,
    configureView,
    ConsoleLogger,
    createDiagramContainer,
    DeleteElementContextMenuItemProvider,
    DiamondNodeView,
    editLabelFeature,
    ForeignObjectView,
    LogLevel,
    moveFeature,
    overrideViewerOptions,
    RevealNamedElementActionProvider,
    RoundedCornerNodeView,
    SCompartment,
    SCompartmentView,
    selectFeature,
    SLabel,
    SLabelView,
    TYPES
} from '@eclipse-glsp/client';
// import { DefaultTypes } from '@eclipse-glsp/protocol';

import 'balloon-css/balloon.min.css';
import { Container, ContainerModule } from 'inversify';
import 'sprotty/css/edit-label.css';
import '../css/diagram.css';
import {
    ContainerHeaderView,
    DataDependencyNodeExtensionNodeView,
    DataDependentNodeExtensionNodeView,
    DataInputNodeExtensionNodeView,
    DataObjectNodeView,
    DataOutputNodeExtensionNodeView,
    IconView,
    LabelNodeView,
    MessageNodeView,

    TextAnnotationNodeView
} from './bpmn-element-views';

import {
    BPMNElementSnapper,
    DrawHelperLinesCommand,
    HelperLineListener,
    HelperLineView,
    RemoveHelperLinesCommand
} from './bpmn-helperlines';

import { BPMNEdgeView } from './bpmn-routing-views';

import {
    BPMNLabelNodeSelectionListener, BPMNMultiNodeSelectionListener
} from './bpmn-select-listeners';

import { BPMNPropertyModule } from '@open-bpmn/open-bpmn-properties';

import {
    TaskNode,
    EventNode,
    GatewayNode,
    Icon,
    PoolNode,
    LaneNode,
    DataObjectNode,
    MessageNode,
    TextAnnotationNode,
    LabelNode,
    MultiLineTextNode,
    BPMNEdge,
    DataObjectExtensionNode,
    DataProcessingExtensionNode
} from '@open-bpmn/open-bpmn-model';

import { MyCustomResponseAction, MyCustomResponseActionHandler } from '@open-bpmn/open-bpmn-properties/lib/bpmn-property-panel';

const bpmnDiagramModule = new ContainerModule((bind, unbind, isBound, rebind) => {
    const context = { bind, unbind, isBound, rebind };

    rebind(TYPES.ILogger).to(ConsoleLogger).inSingletonScope();
    rebind(TYPES.LogLevel).toConstantValue(LogLevel.warn);
    bind(TYPES.ISnapper).to(BPMNElementSnapper);

    bind(TYPES.ICommandPaletteActionProvider).to(RevealNamedElementActionProvider);
    bind(TYPES.IContextMenuItemProvider).to(DeleteElementContextMenuItemProvider);

    // bind new SelectionListener for BPMNLabels and BoundaryEvents
    bind(TYPES.SelectionListener).to(BPMNLabelNodeSelectionListener);
    bind(TYPES.SelectionListener).to(BPMNMultiNodeSelectionListener);

    // bpmn helper lines
    bind(TYPES.MouseListener).to(HelperLineListener);
    configureCommand({ bind, isBound }, DrawHelperLinesCommand);
    configureCommand({ bind, isBound }, RemoveHelperLinesCommand);
    configureView({ bind, isBound }, 'helpline', HelperLineView);
    configureDefaultModelElements(context);

    // collapse
    // configureModelElement(context, 'node:expandable', ExpandableNode, ExpandableNodeView);
    // configureModelElement(context, DefaultTypes.BUTTON_EXPAND, SButton, ExpandButtonView);
    // bind(ExpandHandler).toSelf().inSingletonScope();
    // configureActionHandler(context, CollapseExpandAction.KIND, ExpandHandler);

    configureModelElement(context, 'task', TaskNode, RoundedCornerNodeView, { disable: [moveFeature] });
    configureModelElement(context, 'manualTask', TaskNode, RoundedCornerNodeView);
    configureModelElement(context, 'userTask', TaskNode, RoundedCornerNodeView);
    configureModelElement(context, 'scriptTask', TaskNode, RoundedCornerNodeView);
    configureModelElement(context, 'businessRuleTask', TaskNode, RoundedCornerNodeView);
    configureModelElement(context, 'serviceTask', TaskNode, RoundedCornerNodeView);
    configureModelElement(context, 'sendTask', TaskNode, RoundedCornerNodeView);
    configureModelElement(context, 'receiveTask', TaskNode, RoundedCornerNodeView);

    configureModelElement(context, 'startEvent', EventNode, CircularNodeView);
    configureModelElement(context, 'endEvent', EventNode, CircularNodeView);
    configureModelElement(context, 'intermediateCatchEvent', EventNode, CircularNodeView);
    configureModelElement(context, 'intermediateThrowEvent', EventNode, CircularNodeView);
    configureModelElement(context, 'boundaryEvent', EventNode, CircularNodeView);

    configureModelElement(context, 'exclusiveGateway', GatewayNode, DiamondNodeView);
    configureModelElement(context, 'inclusiveGateway', GatewayNode, DiamondNodeView);
    configureModelElement(context, 'parallelGateway', GatewayNode, DiamondNodeView);
    configureModelElement(context, 'eventBasedGateway', GatewayNode, DiamondNodeView);
    configureModelElement(context, 'complexGateway', GatewayNode, DiamondNodeView);

    configureModelElement(context, 'label:heading', SLabel, SLabelView, { enable: [editLabelFeature] });

    configureModelElement(context, 'comp:comp', SCompartment, SCompartmentView);
    configureModelElement(context, 'icon', Icon, IconView);

    configureModelElement(context, 'comp:header', SCompartment, ContainerHeaderView);

    configureModelElement(context, 'pool', PoolNode, RoundedCornerNodeView, { disable: [moveFeature] });
    configureModelElement(context, 'lane', LaneNode, RoundedCornerNodeView);
    configureModelElement(context, 'dataObject', DataObjectNode, DataObjectNodeView);
    configureModelElement(context, 'message', MessageNode, MessageNodeView);
    configureModelElement(context, 'textAnnotation', TextAnnotationNode, TextAnnotationNodeView);

    configureModelElement(context, 'BPMNLabel', LabelNode, LabelNodeView);

    // textNode of BPMNLabel, TextAnnotation...
    configureModelElement(context, 'bpmn-text-node', MultiLineTextNode, ForeignObjectView, {
        disable: [moveFeature, selectFeature, boundsFeature],
        enable: [editLabelFeature]
    });

    configureModelElement(context, 'container', SCompartment, SCompartmentView);

    // Sequence flows
    configureModelElement(context, 'sequenceFlow', BPMNEdge, BPMNEdgeView);
    configureModelElement(context, 'messageFlow', BPMNEdge, BPMNEdgeView);
    configureModelElement(context, 'association', BPMNEdge, BPMNEdgeView);

    // data object extension
    // input
    configureModelElement(context, 'dataInputObjectLocal', DataObjectExtensionNode, DataInputNodeExtensionNodeView);
    configureModelElement(context, 'dataInputObjectProcess', DataObjectExtensionNode, DataInputNodeExtensionNodeView);
    configureModelElement(context, 'dataInputObjectDataStore', DataObjectExtensionNode, DataInputNodeExtensionNodeView);
    configureModelElement(context, 'dataInputObjectEnvironmentData', DataObjectExtensionNode, DataInputNodeExtensionNodeView);
    configureModelElement(context, 'dataInputObjectEnvironmentDataUser', DataObjectExtensionNode, DataInputNodeExtensionNodeView);
    configureModelElement(context, 'dataInputObjectDependentLocal', DataObjectExtensionNode, DataDependentNodeExtensionNodeView);
    configureModelElement(context, 'dataInputObjectDependentProcess', DataObjectExtensionNode, DataDependentNodeExtensionNodeView);
    configureModelElement(context, 'dataInputObjectDependentDataStore', DataObjectExtensionNode, DataDependentNodeExtensionNodeView);
    configureModelElement(context, 'dataInputObjectDependency', DataObjectExtensionNode, DataDependencyNodeExtensionNodeView);
    // output
    configureModelElement(context, 'dataOutputObjectProcess', DataObjectExtensionNode, DataOutputNodeExtensionNodeView);
    configureModelElement(context, 'dataOutputObjectDataStore', DataObjectExtensionNode, DataOutputNodeExtensionNodeView);
    configureModelElement(context, 'dataOutputObjectEnvironmentData', DataObjectExtensionNode, DataOutputNodeExtensionNodeView);
    configureModelElement(context, 'dataOutputObjectEnvironmentDataUser', DataObjectExtensionNode, DataOutputNodeExtensionNodeView);
    // attribute
    configureModelElement(context, 'attribute', DataObjectExtensionNode, RoundedCornerNodeView);
    // data processing
    configureModelElement(context, 'dataProcessing', DataProcessingExtensionNode, DiamondNodeView);
    // data flow
    configureModelElement(context, 'dataFlow', BPMNEdge, BPMNEdgeView);
    configureModelElement(context, 'dataReference', BPMNEdge, BPMNEdgeView);

    // add actions
    configureActionHandler(context, MyCustomResponseAction.KIND, MyCustomResponseActionHandler);

});

/*
 * Create the createClientContainer with the diagramModule and the BPMN bpmnPropertyModule...
 */
export default function createBPMNDiagramContainer(widgetId: string): Container {
    // Note: the widgetId is generated by the GLSP core and is something like 'bpmn-diagram_0'
    const container = createDiagramContainer(bpmnDiagramModule, BPMNPropertyModule);
    overrideViewerOptions(container, {
        baseDiv: widgetId,
        hiddenDiv: widgetId + '_hidden'
    });
    return container;
}
