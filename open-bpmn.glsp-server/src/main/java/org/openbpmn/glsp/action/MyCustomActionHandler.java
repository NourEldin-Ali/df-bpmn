package org.openbpmn.glsp.action;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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

public class MyCustomActionHandler extends AbstractActionHandler<MyCustomResponseAction> {
	@Inject
	protected BPMNGModelState modelState;

	@Override
	protected List<Action> executeAction(final MyCustomResponseAction actualAction) {
		// implement your custom logic to handle the action
		System.out.println("---------------------------");

		if (actualAction.getAdditionalInformation().toLowerCase().contains("tobonita")) {
			System.out.println("to Bonita Start");
			DFBPMNToProc dfbpmnToProc = new DFBPMNToProc(modelState.getBpmnModel(),
					modelState.getBpmnModel().openDefaultProces().getAttribute("exportName"),
					modelState.getBpmnModel().openDefaultProces().getAttribute("bonitaProjectPath"));
			dfbpmnToProc.createDiagrame();
			System.out.println("to Bonita Done");
		} else if (actualAction.getAdditionalInformation().toLowerCase().contains("gherkin")) {
			System.out.println("Gherkin Start");
			Activity bpmnElement = (Activity) modelState.getBpmnModel()
					.findElementExtensionNodeById(actualAction.getElementId());
			DataProcessingExtension dataProcessing = (DataProcessingExtension) bpmnElement
					.findElementById(actualAction.getElementId());
			List<String> input = dataProcessing.activity.getDataProcessingIncoming(dataProcessing.getId()).stream()
					.map((element) -> element.getName()).collect(Collectors.toList());

			List<String> output = dataProcessing.activity.getDataProcessingOutgoing(dataProcessing.getId()).stream()
					.map((element) -> element.getName()).collect(Collectors.toList());

			try {
				String results = sendPost(input.toString(), dataProcessing.getDocumentation(), output.get(0));
				dataProcessing.setAttribute("gherkin", results);
//				System.out.println("------------RESULTS-----------------------");
//				System.out.println(results);
//				System.out.println("-----------------------------------");

				Map<String, String> options = modelState.getClientOptions();
				String uri = MapUtil.getValue(options, "sourceUri").orElse(null);
				if (uri == null || uri.isEmpty()) {
					uri = options.get("uri");
				}
				BPMNModel model = modelState.getBpmnModel();
				File f = new File(uri);
				java.net.URI targetURI = f.toURI();
				model.save(targetURI);

				modelState.reset();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("Gherkin Done");
		}
		System.out.println("---------------------------");
		return listOf(new MyCustomRequestAction());
	}

	// HTTP POST request
	private String sendPost(String inputsValue, String descriptionValue, String outputValue) throws Exception {

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
}
