package it.unibz.deltabpmn.verification.mcmt.translation;

import it.unibz.deltabpmn.dataschema.core.DataSchema;
import it.unibz.deltabpmn.dataschema.elements.Constant;

/**
 * A class providing methods for translating {@link Constant} objects into MCMT code.
 */
class ConstantTranslator implements MCMTConstants {

    private final DataSchema Data_Schema;

    public ConstantTranslator(DataSchema dataSchema) {
        this.Data_Schema = dataSchema;
    }

    @Override
    /**
     * @return A string representing a list of MCMT definitions
     * (characterised by {@code :smt (define [constantName]::[constantSort])} definition clauses)
     * of all constants from the given {@code DataSchema} object.
     */
    public String getAllElementDefinitions() {
        String result = "";
        for (Constant c : this.Data_Schema.getConstants().values()) {
            //if (isNotDefinedInTheory(c))
            result += c.getMCMTDeclaration();
        }
        return result;
    }

    @Override
    /**
     * @return A string representing an MCMT declaration (characterised by {@code ::db_constants} declaration clause)
     * of all constantsfrom the given {@code DataSchema} object.
     */
    public String getAllElementDeclarations() {
        String result = ":db_constants ";
        for (Constant c : this.Data_Schema.getConstants().values())
            //if (isNotDefinedInTheory(c))
            result += c + " ";
        return result + "\n";
    }

    //ToDo: implement a better method for checking whether a constant belongs to some background theory

//    /**
//     * A method that checks if a given constant is NOT supported by a background theory.
//     * If yes, then this constant has to be declared.
//     *
//     * @param c The constant.
//     * @return
//     */
//    private boolean isNotDefinedInTheory(Constant c) {
//        String value = c.getName();
//        //check on all integer numbers
//        if (value.matches("[0..9]+"))
//            return false;
//        return true;
//    }
}
