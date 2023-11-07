package org.openbpmn.glsp.action;

import org.eclipse.glsp.server.actions.ResponseAction;

public class MyCustomResponseAction extends ResponseAction {
	   public static final String KIND = "myCustomKind"; 
	   private String additionalInformation;
	   private String elementId;

	   public MyCustomResponseAction() {
	      super(KIND);
	   }
	   
	   public String getElementId() { return elementId; }

	   public void setElementId(final String elementId) {
	      this.elementId = elementId;
	   }
	   
	   public String getAdditionalInformation() { return additionalInformation; }

	   public void setAdditionalInformation(final String additionalInformation) {
	      this.additionalInformation = additionalInformation;
	   }
	}