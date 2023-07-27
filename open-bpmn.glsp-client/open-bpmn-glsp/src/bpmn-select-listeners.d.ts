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
import { ActionDispatcher, RequestAction, ResponseAction, SetContextActions, SModelRoot } from '@eclipse-glsp/client';
import { SelectionListener } from '@eclipse-glsp/client/lib/features/select/selection-service';
/****************************************************************************
 * This module provides BPMN select listeners for custom behavior.
 *
 ****************************************************************************/
/**
 * This selectionListener selects additional associated BoundaryEvents and BPMNLabels.
 * This allows to move both independent Nodes (TaskNode and BoundaryEvent, GNode and GLabel)
 */
export declare class BPMNLabelNodeSelectionListener implements SelectionListener {
    protected actionDispatcher: ActionDispatcher;
    selectionChanged(root: Readonly<SModelRoot>, selectedElements: string[]): void;
}
/**
 * This selectionListener reacts on multiple selection. In case the selection list contains
 * a Pool or a Lane, these elements are removed from the selection. This is to support
 * multi-node selections within a Pool with the Marquee Tool.
 * As a consequence it is not possible to select multiple Pools
 */
export declare class BPMNMultiNodeSelectionListener implements SelectionListener {
    protected actionDispatcher: ActionDispatcher;
    selectionChanged(root: Readonly<SModelRoot>, selectedElements: string[]): void;
}
export interface MyCustomResponseAction extends ResponseAction {
    kind: typeof MyCustomResponseAction.KIND;
}
export declare namespace MyCustomResponseAction {
    const KIND = "myCustomResponse";
    function is(object: any): object is SetContextActions;
    function create(options?: {
        responseId?: string;
    }): SetContextActions;
}
export interface MyCustomAction extends RequestAction<MyCustomResponseAction> {
    kind: typeof MyCustomAction.KIND;
    additionalInformation: string;
}
export declare namespace MyCustomAction {
    const KIND = "myCustomKind";
    function is(object: any): object is MyCustomAction;
    function create(options: {
        additionalInformation?: string;
        requestId?: string;
    }): MyCustomAction;
}
//# sourceMappingURL=bpmn-select-listeners.d.ts.map