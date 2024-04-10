package it.unibz.deltabpmn.verification.mcmt.translation;

import it.unibz.deltabpmn.dataschema.core.DataSchema;
import it.unibz.deltabpmn.dataschema.elements.RepositoryRelation;

/**
 * A class providing methods for translating {@link RepositoryRelation} objects into MCMT code.
 */
class RepositoryTranslator implements MCMTRepositoryRelations{

    private final DataSchema Data_Schema;

    public RepositoryTranslator(DataSchema dataSchema) {
        this.Data_Schema = dataSchema;
    }

    //ToDo: add an MCMT template characterizing generated declarations

    @Override
    /**
     * @return A string representing a list of MCMT declarations of all
     * Repository relations from the given {@code DataSchema} object.
     */
    public String getAllElementDeclarations() {
        String result = "";
        for (RepositoryRelation rr : this.Data_Schema.getRepositoryRelations()) {
            result += rr.getMCMTDeclaration();
        }
        return result;
    }

    //ToDo: add an MCMT template characterizing generated declarations

    @Override
    /**
     * @return A string representing initialization declarations of all Repository relations
     * from the given {@code DataSchema} object in MCMT syntax.
     */
    public String getInitializationDeclaration() {
        String result = "";
        for (RepositoryRelation rr : this.Data_Schema.getRepositoryRelations()) {
            result += rr.getMCMTInitializationDeclaration();
        }
        return result;
    }


}
