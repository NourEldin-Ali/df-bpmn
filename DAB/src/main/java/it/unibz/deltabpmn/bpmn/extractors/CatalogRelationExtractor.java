package it.unibz.deltabpmn.bpmn.extractors;

import it.unibz.deltabpmn.dataschema.core.DataSchema;
import it.unibz.deltabpmn.dataschema.elements.CatalogRelation;
import it.unibz.deltabpmn.dataschema.elements.RepositoryRelation;
import it.unibz.deltabpmn.dataschema.elements.Sort;
import org.openbpmn.bpmn.BPMNModel;
import org.openbpmn.bpmn.elements.BPMNProcess;
import org.openbpmn.bpmn.elements.DataObjectAttributeExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class CatalogRelationExtractor {

	private final static String CATALOG_ID = "CatalogID";

	/**
	 * Extracts catalog relations from the DF-BPMN model.
	 *
	 * @param modelInstance
	 * @param dataSchema
	 * @return The updated data schema object contains all the extracted catalog
	 *         relations.
	 */
	public static DataSchema extract(BPMNModel modelInstance, DataSchema dataSchema) {

		BPMNProcess openProcess = modelInstance.openDefaultProces();
		Map<String, Map<String, Object>> dataStoreVariables = openProcess.getDataStoresExtensions();

		dataStoreVariables.forEach((name, values) -> {
			if (Boolean.parseBoolean((String) values.get("readonly")) == true) {
				String catalogRelationName = ((String) values.get("type")).trim();
				CatalogRelation catalogRelation = dataSchema.newCatalogRelation(catalogRelationName);

				((Set<DataObjectAttributeExtension>) values.get("attributes")).forEach((attr) -> {
					Sort attrSort = dataSchema.newSort(attr.getAttribute("type"));
					catalogRelation.addAttribute(attr.getName(), attrSort);
				});
			}
		});
		return dataSchema;

	}
}
