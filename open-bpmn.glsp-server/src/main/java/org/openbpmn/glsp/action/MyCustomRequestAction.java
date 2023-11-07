package org.openbpmn.glsp.action;

import org.eclipse.glsp.server.actions.RequestAction;

public class MyCustomRequestAction extends RequestAction<MyCustomResponseAction > {
	   public static final String KIND= "myCustomResponse";

	   public MyCustomRequestAction() {
	      super(KIND);
	   }
	}
