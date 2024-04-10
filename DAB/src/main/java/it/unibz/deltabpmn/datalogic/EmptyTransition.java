package it.unibz.deltabpmn.datalogic;

import it.unibz.deltabpmn.dataschema.core.DataSchema;
import it.unibz.deltabpmn.dataschema.core.SystemConstants;
import it.unibz.deltabpmn.dataschema.elements.Attribute;
import it.unibz.deltabpmn.dataschema.elements.CaseVariable;
import it.unibz.deltabpmn.dataschema.elements.Constant;
import it.unibz.deltabpmn.dataschema.elements.RepositoryRelation;
import it.unibz.deltabpmn.exception.InvalidInputException;
import it.unibz.deltabpmn.exception.UnmatchingSortException;
import it.unibz.deltabpmn.processschema.core.NameProcessor;

import java.util.HashMap;
import java.util.Map;

public class EmptyTransition implements ComplexTransition {

    private String name;
    private String guard;
    private Map<CaseVariable, String> setTable;
    private DataSchema dataSchema;


    public EmptyTransition(String name, DataSchema dataSchema) {
        this.name = name;
        this.guard = "(= " + SystemConstants.TRUE.getName() + " " + SystemConstants.TRUE.getName() + ")";
        this.setTable = new HashMap<CaseVariable, String>();
        this.dataSchema = dataSchema;

    }

    @Override
    public void setControlCaseVariableValue(CaseVariable variable, Constant newValue) throws InvalidInputException, UnmatchingSortException {
        // first step: control that the case variable is in the collection of changes
        // insert it if it is not present
        if (this.setTable.containsKey(variable)) {
            System.out.println("CaseVariable already set " + variable.getName() + " in " + this.name);
            return;
        }
        //check if the value assigned is a constant
        String cName = newValue.getName();
        if (!this.dataSchema.getConstants().containsKey(cName)) {
            throw new InvalidInputException("the second operand in " + variable.getName() + "=" + cName + " is not a constant!");
        }
        //check whether sorts are matching
        checkConstantSorts(variable.getSort().getSortName(), cName);

        //populate the table of variable assignments with the x=v expression
        this.setTable.put(variable, cName);
    }

    private void checkConstantSorts(String elementSortName, String constantName) throws UnmatchingSortException {
        if (!elementSortName.equals(this.dataSchema.getConstants().get(constantName).getSort().getSortName())) {
            throw new UnmatchingSortException("No matching between sorts: " + elementSortName + " VS " + this.dataSchema.getConstants().get(constantName).getSort().getSortName());
        }
    }

    @Override
    public void setControlCaseVariableValue(CaseVariable variable, Attribute attr) throws InvalidInputException, UnmatchingSortException {
        try {
            throw new UnsupportedOperationException();
        } catch (UnsupportedOperationException ex) {
            System.out.println("Method not supported for bulk update [" + this.name + "]: bulk updates can't set case variable values!");
        }
    }

    @Override
    public void setControlCaseVariableValue(CaseVariable variable, CaseVariable newVar) throws InvalidInputException, UnmatchingSortException {
        try {
            throw new UnsupportedOperationException();
        } catch (UnsupportedOperationException ex) {
            System.out.println("Method not supported for bulk update [" + this.name + "]: bulk updates can't set case variable values!");
        }
    }

    @Override
    /**
     * @return A string containing an MCMT translation of the insert transition.
     */
    public String getMCMTTranslation() {
        String finalMCMT = ":comment " + NameProcessor.getTransitionName(this.name)+ "\n:transition\n:var j\n";
        finalMCMT += ":guard " + this.guard + "\n";
        finalMCMT += this.generateOneCaseMCMT() + "\n";
        return finalMCMT;
    }

    /**
     * @return A string containing an MCMT translation of the insert transition that has an empty insert part (special case).
     */
    private String generateOneCaseMCMT() {
        String result = ":numcases 1\n:case\n";
        for (RepositoryRelation rep : this.dataSchema.getRepositoryRelations()) {
            for (int i = 0; i < rep.arity(); i++) {
                result += ":val " + rep.getName() + (i + 1) + "[j]\n";
            }
        }
        result += "\n";
        result += this.generateGlobalMCMT();
        return result;
    }

    /**
     * @return A string containing a part of MCMT code representing global updates.
     */
    private String generateGlobalMCMT() {
        String result = "";
        for (CaseVariable caseVar : this.dataSchema.getCaseVariables()) {
            // check if the user set a new value for the current case variable
            if (setTable.containsKey(caseVar)) {
                result += ":val " + setTable.get(caseVar) + "\n";
            } else
                result += ":val " + caseVar.getName() + "\n";
        }
        return result;
    }


    @Override
    public void addTaskGuard(String guard) {
        this.guard += guard;
    }
}
