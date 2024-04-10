package it.unibz.deltabpmn.verification.mcmt.translation;


import it.unibz.deltabpmn.dataschema.core.DataSchema;
import it.unibz.deltabpmn.dataschema.core.SystemConstants;
import it.unibz.deltabpmn.dataschema.elements.CaseVariable;

class CaseVariableTranslator implements MCMTCaseVariables {

    private final DataSchema Data_Schema;

    public CaseVariableTranslator(DataSchema dataSchema) {
        this.Data_Schema = dataSchema;
    }

    @Override
    /**
     * Method for getting the String representing the case variable in MCMT specification file
     *
     * @return String representing the case variable in MCMT specification file
     */
    public String getAllElementDeclarations() {
        String result = "";
        for (CaseVariable v : this.Data_Schema.getCaseVariables()) {
            result += v.getMCMTDeclaration();
        }
        return result;
    }

    @Override
    /**
     * Method for building the MCMT declaration for initializing the case variables.
     * Normal case variable are initialized with {@code undef}, control case variables with {@code Idle}
     * (except from control case variables of the root process, which is initialized with {@code Enabled}).
     *
     * @return An MCMT declaration of all case variables created so far by the factory.
     */
    public String getInitializationDeclaration() {
        String result = "";
        for (CaseVariable v : this.Data_Schema.getCaseVariables()) {
            if (v.getLifeCycle() == 2)
                result += "(= " + v.toString() + " Enabled) ";
            else if (v.getLifeCycle() == 1)
                result += "(= " + v.toString() + " Idle) ";
            else {
                if (v.getSort().toString().equals("bool"))
                    result += "(= " + v.toString() + " false) ";
                else
                    result += "(= " + v.toString() + " " + SystemConstants.NULL + "_" + v.getSort().toString() + ") ";
            }
        }
        return result;
    }
}
