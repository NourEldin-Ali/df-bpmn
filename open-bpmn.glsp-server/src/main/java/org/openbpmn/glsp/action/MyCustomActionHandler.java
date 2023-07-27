package org.openbpmn.glsp.action;

import java.util.List;

import org.eclipse.glsp.server.actions.AbstractActionHandler;
import org.eclipse.glsp.server.actions.Action;

public class MyCustomActionHandler extends AbstractActionHandler<MyCustomResponseAction> {

	   @Override
	   protected List<Action> executeAction(final MyCustomResponseAction actualAction) {
	      // implement your custom logic to handle the action
//		   System.out.println(actualAction.toString());
		   System.out.println("---------------------------");
	      // Finally issue response actions
	      // If no response actions should be issued 'none()' can be used;
	      return listOf(new MyCustomResponseAction());
	   }

	}
