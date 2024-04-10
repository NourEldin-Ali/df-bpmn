package it.unibz.deltabpmn.bpmn.extractors;

import it.unibz.deltabpmn.dataschema.core.DataSchema;
import it.unibz.deltabpmn.dataschema.elements.Sort;
import org.openbpmn.bpmn.BPMNModel;
import org.openbpmn.bpmn.elements.BPMNProcess;

import java.util.Map;
import java.util.Optional;

public final class CaseVariableExtractor {

	private final static String PROCESS_VARIABLES_ID = "ProcessVariablesID";

	/**
	 * Extracts process (or case) variables from the DF-BPMN model.
	 *
	 * @return The updated data schema object contains all the extracted case
	 *         variables.
	 */
	public static DataSchema extract(BPMNModel modelInstance, DataSchema dataSchema) {
		BPMNProcess openProcess = modelInstance.openDefaultProces();
		Map<String, String> caseVariables = openProcess.getDataObjectsExtensions();
		
		caseVariables.forEach((key, type) -> {
//            System.out.println("Key: " + key + ", Value: " + type);
            
            // ToDo: manage system sorts correctly!
			Sort varSort = dataSchema.newSort(type);
			// ToDo: add a way to account for multiple-case variables
			dataSchema.newCaseVariable("#"+key.trim(), varSort, true);
			
        });
		
		return dataSchema;
	}
}
