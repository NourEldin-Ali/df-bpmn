package org.openbpmn.glsp.action;

import org.eclipse.glsp.server.actions.ResponseAction;

public class MyCustomResponseAction extends ResponseAction {
	   public static final String KIND = "myCustomKind";

	   public MyCustomResponseAction() {
	      super(KIND);
	   }
	}