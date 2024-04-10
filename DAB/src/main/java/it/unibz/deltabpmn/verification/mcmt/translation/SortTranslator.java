package it.unibz.deltabpmn.verification.mcmt.translation;

import it.unibz.deltabpmn.dataschema.core.DataSchema;
import it.unibz.deltabpmn.dataschema.elements.Sort;

/**
 * A class providing methods for translating {@link Sort} objects into MCMT code.
 */
class SortTranslator implements MCMTSorts{
    private final DataSchema Data_Schema;

    public SortTranslator(DataSchema dataSchema) {
        this.Data_Schema = dataSchema;
    }

    @Override
    /**
     * @return A string representing a list of MCMT definitions
     * (characterised by {@code :smt (define--type [sortName])} definition clauses)
     * of all sorts from the given {@code DataSchema} object.
     */
    public String getAllElementDefinitions() {
        String result = "";
        for (Sort s : this.Data_Schema.getSorts()) {
            result += s.getMCMTDeclaration();
        }
        return result;
    }

    @Override
    /**
     * @return A string representing an MCMT declaration (characterised by {@code ::db_sorts} declaration clause)
     * of all sorts from the given {@code DataSchema} object.
     */
    public String getAllElementDeclarations() {
        String result = ":db_sorts ";
        for (Sort s : this.Data_Schema.getSorts()) {
            result += s.toString() + " ";
        }
        return result;
    }


}
