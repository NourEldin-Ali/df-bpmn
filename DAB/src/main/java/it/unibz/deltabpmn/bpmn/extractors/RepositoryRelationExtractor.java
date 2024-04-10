package it.unibz.deltabpmn.bpmn.extractors;

import it.unibz.deltabpmn.dataschema.core.DataSchema;
import it.unibz.deltabpmn.dataschema.elements.CatalogRelation;
import it.unibz.deltabpmn.dataschema.elements.RepositoryRelation;
import it.unibz.deltabpmn.dataschema.elements.Sort;
import org.openbpmn.bpmn.BPMNModel;
import org.openbpmn.bpmn.elements.BPMNProcess;
import org.openbpmn.bpmn.elements.DataObjectAttributeExtension;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class RepositoryRelationExtractor {

    private final static String REPOSITORY_ID = "RepositoryID";

    
    /**
     * Extracts repository relations from the DF-BPMN model.
     *
     * @return The updated data schema object contains all the extracted repository relations.
     */
    public static DataSchema extract(BPMNModel modelInstance, DataSchema dataSchema) {
    	
    	BPMNProcess openProcess = modelInstance.openDefaultProces();
		Map<String, Map<String, Object>> dataStoreVariables = openProcess.getDataStoresExtensions();

		dataStoreVariables.forEach((name, values) -> {
			String repRelationName = ((String) values.get("type")).trim();
		    RepositoryRelation repRelation = dataSchema.newRepositoryRelation(repRelationName);
		    
			((Set<DataObjectAttributeExtension>) values.get("attributes"))
			.forEach((attr) -> {
				Sort attrSort = dataSchema.newSort(attr.getAttribute("type"));
				repRelation.addAttribute(attr.getName(), attrSort);
			});
		});
		return dataSchema;
		    }

}


