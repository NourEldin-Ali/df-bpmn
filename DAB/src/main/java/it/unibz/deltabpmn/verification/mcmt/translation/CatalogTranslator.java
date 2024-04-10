package it.unibz.deltabpmn.verification.mcmt.translation;

import it.unibz.deltabpmn.dataschema.core.DataSchema;
import it.unibz.deltabpmn.dataschema.elements.CatalogRelation;


/**
 * A class providing methods for translating {@link CatalogRelation} objects into MCMT code.
 */
class CatalogTranslator implements MCMTCatalogRelations{

    private final DataSchema Data_Schema;

    public CatalogTranslator(DataSchema dataSchema) {
        this.Data_Schema = dataSchema;
    }
    //ToDo: add an MCMT template characterizing generated declarations

    /**
     * @return A string representing a list of MCMT declarations of all
     * Catalog relations from the given {@code DataSchema} object.
     */
    public String getAllElementDeclarations() {
        String result = "";
        for (CatalogRelation cr : this.Data_Schema.getCatalogRelations()) {
            result += cr.getMCMTDeclaration();
        }
        return result;
    }

    //ToDo: add an MCMT template characterizing generated declarations

    /**
     * @return A string representing an MCMT declaration containing names
     * for a functional algebraic representation of Catalog relations
     * (i.e., of all non-PK attributes of all Catalog relations)
     * from the given {@code DataSchema} object in MCMT syntax.
     */
    public String getFunctionalCatalogView() {
        String result = ":db_functions ";
        for (CatalogRelation cr : this.Data_Schema.getCatalogRelations()) {
            result += cr.getFunctionNames();
        }
        return result;
    }
}
