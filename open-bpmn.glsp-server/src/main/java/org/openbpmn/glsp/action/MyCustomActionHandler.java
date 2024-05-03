package org.openbpmn.glsp.action;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.zip.DeflaterOutputStream;

import org.eclipse.glsp.server.actions.AbstractActionHandler;
import org.eclipse.glsp.server.actions.Action;
import org.eclipse.glsp.server.utils.MapUtil;
import org.openbpm.bpmn.converter.DFBPMNToProc;
import org.openbpmn.bpmn.BPMNModel;
import org.openbpmn.bpmn.elements.Activity;
import org.openbpmn.bpmn.elements.DataProcessingExtension;
import org.openbpmn.bpmn.elements.core.BPMNElement;
import org.openbpmn.glsp.model.BPMNGModelState;
import org.openbpmn.glsp.model.BPMNSourceModelStorage;

import com.google.inject.Inject;

import it.unibz.deltabpmn.bpmn.DfbpmnModelReader;
import it.unibz.deltabpmn.verification.mcmt.translation.DABProcessTranslator;

public class MyCustomActionHandler extends AbstractActionHandler<MyCustomResponseAction> {
	@Inject
	protected BPMNGModelState modelState;

	@Override
	protected List<Action> executeAction(final MyCustomResponseAction actualAction) {
		// implement your custom logic to handle the action
		System.out.println("---------------------------");
		MyCustomRequestAction request = new MyCustomRequestAction();
		if (actualAction.getAdditionalInformation().toLowerCase().contains("tobonita")) {
			System.out.println("DF-BPMN to Bonita has been started");
			DFBPMNToProc dfbpmnToProc = new DFBPMNToProc(modelState.getBpmnModel(),
					modelState.getBpmnModel().openDefaultProces().getAttribute("exportName"),
					modelState.getBpmnModel().openDefaultProces().getAttribute("bonitaProjectPath"));
			if (dfbpmnToProc.createDiagrame() != null) {
				request.setResponceCode("200");
			} else {
				request.setResponceCode("400");
			}
			System.out.println("DF-BPMN to Bonita done");
		} else if (actualAction.getAdditionalInformation().toLowerCase().contains("tomcmt")) {
			System.out.println("DF-BPMN to MCMT has been started");
			System.out.println(modelState.getPath());
			String exportPath = modelState.getBpmnModel().openDefaultProces().getAttribute("mcmtpath");
			
			try {
				DfbpmnModelReader modelReader = new DfbpmnModelReader(modelState.getPath(),exportPath.isEmpty()?null:exportPath);

				for (DABProcessTranslator processTranslator : modelReader.getProcessTranslators())
					processTranslator.generateMCMTTranslation();
				request.setResponceCode("204");
			} catch (Exception e) {
				request.setResponceCode("404");
				e.printStackTrace();
			}
			System.out.println("DF-BPMN to MCMT done");
		} else if (actualAction.getAdditionalInformation().toLowerCase().contains("gherkin")) {
			System.out.println("Gherkin Generation has been start");
			Activity bpmnElement = (Activity) modelState.getBpmnModel()
					.findElementExtensionNodeById(actualAction.getElementId());
			DataProcessingExtension dataProcessing = (DataProcessingExtension) bpmnElement
					.findElementById(actualAction.getElementId());
			if (dataProcessing.getDocumentation().isEmpty()) {
				request.setResponceCode("401");
			} else {

				List<String> input = dataProcessing.activity.getDataProcessingIncoming(dataProcessing.getId()).stream()
						.map((element) -> element.getName()).collect(Collectors.toList());

				List<String> output = dataProcessing.activity.getDataProcessingOutgoing(dataProcessing.getId()).stream()
						.map((element) -> element.getName()).collect(Collectors.toList());

				try {
					String results = generateGherkinAPI(input.toString(), dataProcessing.getDocumentation(),
							output.get(0));
					dataProcessing.setAttribute("gherkin", results);
					modelState.forceSave();
					modelState.reset();
					request.setResponceCode("200");
				} catch (Exception e) {
					request.setResponceCode("400");
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			System.out.println("Gherkin Generation done");
		} else if (actualAction.getAdditionalInformation().toLowerCase().contains("groovy")) {
			System.out.println("Code Generation has been started");
			Activity bpmnElement = (Activity) modelState.getBpmnModel()
					.findElementExtensionNodeById(actualAction.getElementId());
			DataProcessingExtension dataProcessing = (DataProcessingExtension) bpmnElement
					.findElementById(actualAction.getElementId());
			if (dataProcessing.getAttribute("gherkin").isEmpty()) {
				request.setResponceCode("402");
			} else {
				List<String> input = dataProcessing.activity.getDataProcessingIncoming(dataProcessing.getId()).stream()
						.map((element) -> element.getName()).collect(Collectors.toList());

				List<String> output = dataProcessing.activity.getDataProcessingOutgoing(dataProcessing.getId()).stream()
						.map((element) -> element.getName()).collect(Collectors.toList());

				try {
					String results = generateGroovyAPI(input.toString(), dataProcessing.getAttribute("gherkin"),
							output.get(0));
					dataProcessing.setAttribute("groovy", results);
					modelState.forceSave();
					modelState.reset();
					request.setResponceCode("200");
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					request.setResponceCode("400");
				}
			}
			System.out.println("Code Generation done");
		} else if (actualAction.getAdditionalInformation().toLowerCase().contains("unittest")) {
			System.out.println("Unittest has been started");
			Activity bpmnElement = (Activity) modelState.getBpmnModel()
					.findElementExtensionNodeById(actualAction.getElementId());
			DataProcessingExtension dataProcessing = (DataProcessingExtension) bpmnElement
					.findElementById(actualAction.getElementId());
			if (dataProcessing.getAttribute("groovy").isEmpty()) {
				request.setResponceCode("403");
			} else {
				try {
					String results = generateUnitTestAPI(dataProcessing.getAttribute("groovy"));
					String encodedScript = Base64.getUrlEncoder().withoutPadding().encodeToString(results.getBytes());
					request.setAdditionalInformation("https://gwc-experiment.appspot.com/?code=" + encodedScript);
					request.setResponceCode("203");
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					request.setResponceCode("400");
				}
			}
			System.out.println("Unit-test has been done");
		}
		System.out.println("---------------------------");

		return listOf(request);
	}

	/**
	 * This function is used connect to the flask server to generate gherkin syntax
	 * 
	 * @param inputsValue
	 * @param descriptionValue
	 * @param outputValue
	 * @return
	 * @throws Exception
	 */
	private String generateGherkinAPI(String inputsValue, String descriptionValue, String outputValue)
			throws Exception {

		String url = "http://localhost:3001/gherkin";
		URL obj = new URL(url);
		HttpURLConnection connection = (HttpURLConnection) obj.openConnection();

		// add request header
		connection.setRequestMethod("POST");

		// Set Do Output to true if you want to use URLConnection for output.
		connection.setDoOutput(true);

		String urlParameters = "inputs=" + inputsValue + "&description=" + descriptionValue + "&output=" + outputValue;
		byte[] postData = urlParameters.getBytes(StandardCharsets.UTF_8);

		try (OutputStream wr = connection.getOutputStream()) {
			wr.write(postData);
		}

		if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
			// Success

			BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine + "\n");
			}
			in.close();

			return response.toString();
		} else {
			// Error handling code goes here
//            System.out.println("Generate failed");
		}

		return "CONNECTION FAILED";

	}

	/**
	 * This function is used connect to the flask server to generate gherkin syntax
	 * 
	 * @param inputsValue
	 * @param descriptionValue
	 * @param outputValue
	 * @return
	 * @throws Exception
	 */
	private String generateGroovyAPI(String inputsValue, String descriptionValue, String outputValue) throws Exception {

		String url = "http://localhost:3001/groovy";
		URL obj = new URL(url);
		HttpURLConnection connection = (HttpURLConnection) obj.openConnection();

		// add request header
		connection.setRequestMethod("POST");

		// Set Do Output to true if you want to use URLConnection for output.
		connection.setDoOutput(true);

		String urlParameters = "inputs=" + inputsValue + "&description=" + descriptionValue + "&output=" + outputValue;
		byte[] postData = urlParameters.getBytes(StandardCharsets.UTF_8);

		try (OutputStream wr = connection.getOutputStream()) {
			wr.write(postData);
		}

		if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
			// Success

			BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine + "\n");
			}
			in.close();

			return response.toString();
		} else {
			// Error handling code goes here
//            System.out.println("Generate failed");
		}

		return "CONNECTION FAILED";

	}

	private String generateUnitTestAPI(String script) throws Exception {

		String url = "http://localhost:3001/unittest";
		URL obj = new URL(url);
		HttpURLConnection connection = (HttpURLConnection) obj.openConnection();

		// add request header
		connection.setRequestMethod("POST");

		// Set Do Output to true if you want to use URLConnection for output.
		connection.setDoOutput(true);

		String urlParameters = "script=" + script;
		byte[] postData = urlParameters.getBytes(StandardCharsets.UTF_8);

		try (OutputStream wr = connection.getOutputStream()) {
			wr.write(postData);
		}

		if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
			// Success

			BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine + "\n");
			}
			in.close();

			return response.toString();
		} else {
			// Error handling code goes here
//            System.out.println("Generate failed");
		}

		return "CONNECTION FAILED";

	}
}
