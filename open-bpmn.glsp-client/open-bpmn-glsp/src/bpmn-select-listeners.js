"use strict";
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.MyCustomAction = exports.MyCustomResponseAction = exports.BPMNMultiNodeSelectionListener = exports.BPMNLabelNodeSelectionListener = void 0;
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
const open_bpmn_model_1 = require("@open-bpmn/open-bpmn-model");
const inversify_1 = require("inversify");
/****************************************************************************
 * This module provides BPMN select listeners for custom behavior.
 *
 ****************************************************************************/
/**
 * This selectionListener selects additional associated BoundaryEvents and BPMNLabels.
 * This allows to move both independent Nodes (TaskNode and BoundaryEvent, GNode and GLabel)
 */
let BPMNLabelNodeSelectionListener = class BPMNLabelNodeSelectionListener {
    selectionChanged(root, selectedElements) {
        const additionalSelection = [''];
        // We are interested in Tasks with BoundaryEvents ...
        const selectedTaskNodes = (0, client_1.getElements)(root.index, selectedElements, open_bpmn_model_1.isTaskNode);
        // - first get a list of all selected TaskIDs
        const taskIds = selectedTaskNodes.map(task => task.id);
        // - next iterate over all BoundaryEvents
        const boundaryEvents = (0, client_1.filter)(root.index, open_bpmn_model_1.isBoundaryEvent);
        if (selectedTaskNodes.length > 0) {
            // do we have a boundaryEvent that matches this taskID?
            boundaryEvents.forEach(b => {
                if ((0, client_1.hasArguments)(b)) {
                    const taskRef = b.args.attachedToRef + '';
                    if (taskIds.includes(taskRef)) {
                        additionalSelection.push(b.id);
                    }
                }
            });
        }
        // ... and we are interested in BPMNLabelNodes
        const eventNodes = (0, client_1.getElements)(root.index, selectedElements, open_bpmn_model_1.isBPMNLabelNode);
        if (eventNodes.length > 0) {
            // find the associated BPMNLabels
            const eventLabelIds = eventNodes.map(node => node.id + '_bpmnlabel');
            eventLabelIds.forEach(l => {
                additionalSelection.push(l);
            });
        }
        // finally dispatch the additional elementIDs...
        this.actionDispatcher.dispatch(client_1.SelectAction.create({ selectedElementsIDs: additionalSelection }));
    }
};
__decorate([
    (0, inversify_1.inject)(client_1.TYPES.IActionDispatcher),
    __metadata("design:type", client_1.ActionDispatcher)
], BPMNLabelNodeSelectionListener.prototype, "actionDispatcher", void 0);
BPMNLabelNodeSelectionListener = __decorate([
    (0, inversify_1.injectable)()
], BPMNLabelNodeSelectionListener);
exports.BPMNLabelNodeSelectionListener = BPMNLabelNodeSelectionListener;
/**
 * This selectionListener reacts on multiple selection. In case the selection list contains
 * a Pool or a Lane, these elements are removed from the selection. This is to support
 * multi-node selections within a Pool with the Marquee Tool.
 * As a consequence it is not possible to select multiple Pools
 */
let BPMNMultiNodeSelectionListener = class BPMNMultiNodeSelectionListener {
    selectionChanged(root, selectedElements) {
        // react only if more than one element is selected
        if (selectedElements.length < 2) {
            return;
        }
        const containerIDs = [];
        // We are interested in Pools and Lanes ...
        const selectedPools = (0, client_1.getElements)(root.index, selectedElements, open_bpmn_model_1.isPoolNode);
        const selectedLanes = (0, client_1.getElements)(root.index, selectedElements, open_bpmn_model_1.isLaneNode);
        // collect pools...
        selectedPools.forEach(elementNode => {
            containerIDs.push(elementNode.id);
        });
        // collect lanes...
        selectedLanes.forEach(elementNode => {
            containerIDs.push(elementNode.id);
        });
        // if the size of the selected containers is equals the size of selectionElements
        // this means we only have containers selected and can skip this method
        if (selectedElements.length === containerIDs.length) {
            return;
        }
        // filter current selection...
        selectedElements = selectedElements.filter(function (element) {
            return !containerIDs.includes(element);
        });
        // finally dispatch the updated selected and unselected IDs...
        this.actionDispatcher.dispatch(client_1.SelectAction.create({ selectedElementsIDs: selectedElements, deselectedElementsIDs: containerIDs }));
    }
};
__decorate([
    (0, inversify_1.inject)(client_1.TYPES.IActionDispatcher),
    __metadata("design:type", client_1.ActionDispatcher)
], BPMNMultiNodeSelectionListener.prototype, "actionDispatcher", void 0);
BPMNMultiNodeSelectionListener = __decorate([
    (0, inversify_1.injectable)()
], BPMNMultiNodeSelectionListener);
exports.BPMNMultiNodeSelectionListener = BPMNMultiNodeSelectionListener;
var MyCustomResponseAction;
(function (MyCustomResponseAction) {
    MyCustomResponseAction.KIND = 'myCustomResponse';
    function is(object) {
        return client_1.Action.hasKind(object, MyCustomResponseAction.KIND);
    }
    MyCustomResponseAction.is = is;
    function create(options = {}) {
        return Object.assign({ kind: MyCustomResponseAction.KIND, responseId: '' }, options);
    }
    MyCustomResponseAction.create = create;
})(MyCustomResponseAction = exports.MyCustomResponseAction || (exports.MyCustomResponseAction = {}));
var MyCustomAction;
(function (MyCustomAction) {
    MyCustomAction.KIND = 'myCustomKind';
    function is(object) {
        return (client_1.RequestAction.hasKind(object, MyCustomAction.KIND) && (0, client_1.hasStringProp)(object, 'additionalInformation'));
    }
    MyCustomAction.is = is;
    function create(options) {
        return Object.assign({ kind: MyCustomAction.KIND, requestId: '' }, options);
    }
    MyCustomAction.create = create;
})(MyCustomAction = exports.MyCustomAction || (exports.MyCustomAction = {}));
//# sourceMappingURL=bpmn-select-listeners.js.map