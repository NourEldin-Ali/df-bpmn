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
package org.openbpmn.extension.dataextension;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonObject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.glsp.graph.GModelElement;
import org.eclipse.glsp.graph.GNode;
import org.openbpm.bpmn.converter.DFBPMNToProc;
import org.openbpmn.bpmn.BPMNTypes;
import org.openbpmn.bpmn.elements.Activity;
import org.openbpmn.bpmn.elements.DataProcessingExtension;
import org.openbpmn.bpmn.elements.core.BPMNElement;
import org.openbpmn.extension.AbstractBPMNElementExtension;
import org.openbpmn.glsp.bpmn.BPMNGNode;
import org.openbpmn.glsp.jsonforms.DataBuilder;
import org.openbpmn.glsp.jsonforms.SchemaBuilder;
import org.openbpmn.glsp.jsonforms.UISchemaBuilder;
import org.openbpmn.glsp.jsonforms.UISchemaBuilder.Layout;
import org.openbpmn.glsp.utils.BPMNGraphUtil;

/**
 * This is the Default BPMNEvent extension providing the JSONForms shemata.
 *
 * @author Ali Nour Eldin
 *
 */
public class DefaultBPMNDataProcessingExtension extends AbstractBPMNElementExtension {

	private static Logger logger = LogManager.getLogger(DefaultBPMNDataProcessingExtension.class);

	public DefaultBPMNDataProcessingExtension() {
		super();
	}

	@Override
	public boolean handlesElementTypeId(final String elementTypeId) {
		return BPMNTypes.DATA_PROCESSING.contains(elementTypeId);
	}

	/**
	 * This Extension is for BPMNActivities only
	 */
	@Override
	public boolean handlesBPMNElement(final BPMNElement bpmnElement) {
		return (bpmnElement instanceof DataProcessingExtension);
	}

	/**
	 * This Helper Method generates a JSON Object with the BPMNElement properties.
	 * <p>
	 * This json object is used on the GLSP Client to generate the EMF JsonForms
	 */
	@Override
	public void buildPropertiesForm(final BPMNElement bpmnElement, final DataBuilder dataBuilder,
			final SchemaBuilder schemaBuilder, final UISchemaBuilder uiSchemaBuilder) {

		dataBuilder //
				.addData("name", bpmnElement.getName()) //
				.addData("documentation", bpmnElement.getDocumentation()) //
				.addData("gherkin", bpmnElement.getAttribute("gherkin")) //
				.addData("exporter", ""); //
		

		schemaBuilder //
				.addProperty("name", "string", null) //
				.addProperty("documentation", "string", "Add data processing description")
				.addProperty("gherkin", "string", "update data processing behavior description")
				.addProperty("exporter", "string", null);

		Map<String, String> multilineOption = new HashMap<>();
		multilineOption.put("multi", "true");
		uiSchemaBuilder. //
				addCategory("General") //
				.addLayout(Layout.HORIZONTAL) //
				.addElements("name") //
				.addLayout(Layout.VERTICAL) //
				.addElement("documentation", "Documentation", multilineOption) //
				.addCategory("Behavior") //
				.addLayout(Layout.VERTICAL) //
				.addElement("gherkin", "Documentation", multilineOption) //
				.addElements("generate") //
		;

	}

	@Override
	public void updatePropertiesData(final JsonObject json, String category, final BPMNElement bpmnElement,
			final GModelElement gNodeElement) {

		// default update of name and documentation

		Set<String> features = json.keySet();
		System.out.println(features);
		for (String feature : features) {
			if ("name".equals(feature)) {
				String text = json.getString(feature);
				bpmnElement.setName(text);
				// update the bpmn-text-node of the GNodeElement
				GNode gnode = BPMNGraphUtil.findMultiLineTextNode((BPMNGNode) gNodeElement);
				if (gnode != null) {
					gnode.getArgs().put("text", text);
				}
				continue;
			}
			if ("documentation".equals(feature)) {
				bpmnElement.setDocumentation(json.getString(feature));
				continue;
			}
			if ("gherkin".equals(feature)) {
				
				bpmnElement.setAttribute("gherkin",json.getString(feature));
				continue;
			}
		}
		if(!features.contains("gherkin")) {
			bpmnElement.removeAttribute("gherkin");
		}
		if(!features.contains("documentation")) {
			bpmnElement.setDocumentation("");
		}
		modelState.reset();
//		if ("generate".equals(category.toLowerCase())) {
//			System.out.println("Generate behavior");
//			DataProcessingExtension dataProcessing = (DataProcessingExtension) bpmnElement;
//			List<String> input = dataProcessing.activity.getDataProcessingIncoming(bpmnElement.getId()).stream()
//					.map((element) -> element.getName()).collect(Collectors.toList());
//
//			modelState.reset();
//			List<String> output = dataProcessing.activity.getDataProcessingOutgoing(bpmnElement.getId()).stream()
//					.map((element) -> element.getName()).collect(Collectors.toList());
//
//			
//			try {
//				String results =sendPost(input.toString(), dataProcessing.getDocumentation(), output.get(0));
//				bpmnElement.setAttribute("gherkin",results);
//			} catch (Exception e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			
//
//			// activity.getDataProcessingIncoming
//		}

	}


}
