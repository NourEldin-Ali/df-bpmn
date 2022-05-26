/********************************************************************************
 *  Open-BPMN
 *  
 *  Copyright (C) 2022 Imixs Software Solutions GmbH,  
 *  http://www.imixs.com
 *  
 *  This program is free software; you can redistribute it and/or 
 *  modify it under the terms of the GNU General Public License 
 *  as published by the Free Software Foundation; either version 3 
 *  of the License, or (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful, 
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of 
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 *  General Public License for more details.
 *  
 *  You can receive a copy of the GNU General Public
 *  License at http://www.gnu.org/licenses/gpl.html
 *  
 *  Project: 
 *      https://github.com/imixs/open-bpmn
 *  
 *  Contributors:  
 *      Imixs Software Solutions GmbH - Project Management
 *      Ralph Soika - Software Developer
 ********************************************************************************/
import {
	angleOfPoint,
	Point,
	PolylineEdgeViewWithGapsOnIntersections,
	RenderingContext,
	SEdge,
	IViewArgs,
	isIntersectingRoutedPoint,
	toDegrees
} from '@eclipse-glsp/client';
import { injectable } from 'inversify';
import { VNode } from 'snabbdom';
import { svg } from 'sprotty';

/****************************************************************************
 * This module provides BPMN Routings views for sequence flows
 *
 * Layout for the bpmn sequence flow. The View extends the `PolylineEdgeView` that renders gaps on intersections,
 * and the `JumpingPolylineEdgeView` that renders jumps over intersections.
 * In addition the view render rounded corners for a manhattan routing and an arrow on the edge end point
 ****************************************************************************/

// eslint-disable-next-line @typescript-eslint/no-unused-vars
const JSX = { createElement: svg };

@injectable()
export class BPMNSequenceFlowView extends PolylineEdgeViewWithGapsOnIntersections {
	protected renderAdditionals(edge: SEdge, segments: Point[], context: RenderingContext): VNode[] {
		const additionals = super.renderAdditionals(edge, segments, context);
		const p1 = segments[segments.length - 2];
		const p2 = segments[segments.length - 1];

		const arrow: any = (
			<path
				class-sprotty-edge={true}
				class-arrow={true}
				d='M 1,0 L 14,-4 L 14,4 Z'
				transform={`rotate(${toDegrees(angleOfPoint({ x: p1.x - p2.x, y: p1.y - p2.y }))} ${p2.x} ${p2.y}) translate(${p2.x} ${p2.y
					})`}
			/>
		);
		additionals.push(arrow);
		return additionals;
	}

	/*
	 * The goal of this method is to render rounded corners. Therefore we compute always the next segment to decide
	 * the corner angle.
	 */
	protected renderLine(edge: SEdge, segments: Point[], context: RenderingContext, args?: IViewArgs): VNode {
		let path = '';
		// let radius = 10;
		let radius = 10;
		for (let i = 0; i < segments.length; i++) {
			const p = segments[i];
			// start point?
			if (i === 0) {
				path = `M ${p.x},${p.y}`;
			}
			if (isIntersectingRoutedPoint(p)) {
				path += this.intersectionPath(edge, segments, p, args);
			}
			// line...
			if (i > 0) {
				// compute the direction of the next line...
				if (i < segments.length - 1) {
					const plast = segments[i - 1];
					const pnext = segments[i + 1];
					// draw lines ending with rounded corners...
					// right-down  ↴
					radius=this.computeMaxRadius(p,plast,pnext);
					if (plast.x < p.x && p.y < pnext.y) {
						path += ` L ${p.x - radius},${p.y}  Q ${p.x},${p.y} ${p.x},${p.y + radius}`;
						// down-right  ↳
					} else if (plast.y < p.y && p.x < pnext.x) {
						path += ` L ${p.x},${p.y - radius}  Q ${p.x},${p.y} ${p.x + radius},${p.y}`;
						// right-up  _↑
					} else if (plast.x < p.x && p.y > pnext.y) {
						path += ` L ${p.x - radius},${p.y}  Q ${p.x},${p.y} ${p.x},${p.y - radius}`;
						// up-right  ↱
					} else if (plast.y > p.y && p.x < pnext.x) {
						path += ` L ${p.x},${p.y + radius}  Q ${p.x},${p.y} ${p.x + radius},${p.y}`;
						// down-left  ↲
					} else if (plast.y < p.y && p.x > pnext.x) {
						path += ` L ${p.x},${p.y - radius}  Q ${p.x},${p.y} ${p.x - radius},${p.y}`;
						// left-down  ↓-
					} else if (plast.x > p.x && p.y < pnext.y) {
						path += ` L ${p.x + radius},${p.y}  Q ${p.x},${p.y} ${p.x},${p.y + radius}`;
						// up-left  ↰
					} else if (plast.y > p.y && p.x > pnext.x) {
						path += ` L ${p.x},${p.y + radius}  Q ${p.x},${p.y} ${p.x - radius},${p.y}`;
						// left-up ↑_
					} else if (plast.x > p.x && p.y > pnext.y) {
						path += ` L ${p.x + radius},${p.y}  Q ${p.x},${p.y} ${p.x},${p.y - radius}`;
					} else {
						// default
						path += ` L ${p.x},${p.y}`;
					}
					

				} else {
					// default behaviour
					path += ` L ${p.x},${p.y}`;
				}
			}
		}

		const vnode: any = (
			<path d={path} />
		);
		return vnode;
		// return <path d={path} />;
	}
	
	/**
	 * Helper method to compute the maximum possible radius. 
	 * If two poins are very close the radius need to be reduced 
	 */
	protected computeMaxRadius(pCurrent: Point, pLast: Point, pNext: Point): number {
		let radius = 10;
		const dRef=0.5
		// verfiy last point
		let xDif=Math.abs(pCurrent.x-pLast.x);
		let yDif=Math.abs(pCurrent.y-pLast.y);
		if ( xDif>0 && xDif<10) {
			radius=xDif*dRef;
			return radius;
		}
		if (yDif>0 && yDif <10) {
			radius=yDif*dRef;
			return radius;
		}
		// verify next point
		xDif=Math.abs(pCurrent.x-pNext.x);
		yDif=Math.abs(pCurrent.y-pNext.y);
		if ( xDif>0 && xDif<10) {
			radius=xDif*dRef;
			return radius;
		}
		if (yDif>0 && yDif <10) {
			radius=yDif*dRef;
			return radius;
		}
		// default
		return radius;
	}
	
}
