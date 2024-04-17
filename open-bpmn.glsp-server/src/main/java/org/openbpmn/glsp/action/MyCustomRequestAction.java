package org.openbpmn.glsp.action;

import org.eclipse.glsp.server.actions.RequestAction;

public class MyCustomRequestAction extends RequestAction<MyCustomResponseAction> {
	public static final String KIND = "myCustomResponse";
	private String responceCode;
	private String additionalInformation;

	public MyCustomRequestAction() {
		super(KIND);
	}

	public String getResponceCode() {
		return responceCode;
	}

	public void setResponceCode(final String responceCode) {
		this.responceCode = responceCode;
	}

	public String getAdditionalInformation() {
		return additionalInformation;
	}

	public void setAdditionalInformation(final String additionalInformation) {
		this.additionalInformation = additionalInformation;
	}
}
