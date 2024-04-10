package it.unibz.deltabpmn.datalogic;

import it.unibz.deltabpmn.dataschema.core.DataSchema;
import it.unibz.deltabpmn.dataschema.core.SystemConstants;
import it.unibz.deltabpmn.dataschema.elements.*;
import it.unibz.deltabpmn.exception.EevarOverflowException;
import it.unibz.deltabpmn.exception.EmptyGuardException;
import it.unibz.deltabpmn.exception.InvalidInputException;
import it.unibz.deltabpmn.exception.UnmatchingSortException;
import it.unibz.deltabpmn.processschema.core.NameProcessor;
import it.unibz.deltabpmn.processschema.core.State;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * A class representing a DAB insert transition.
 */
public class InsertTransition implements ComplexTransition {

    private ConjunctiveSelectQuery precondition;
    private Map<String, String> eevarAssociation;
    private Map<CaseVariable, String> setTable;
    private String name;
    private String guard;
    private String localUpdate = "";
    private String localStatic = "";//ToDo: what is this variable?
    private DataSchema dataSchema;

    /**
     * Creates an insert transition with a precondition.
     *
     * @param name         The insert transition name.
     * @param precondition The precondition.
     * @param dataSchema   The data schema reference.
     */
    public InsertTransition(String name, ConjunctiveSelectQuery precondition, DataSchema dataSchema) throws EmptyGuardException {
        this.precondition = precondition;
        this.eevarAssociation = precondition.getRefManager();
        //ToDo: change the last if-else section with proper exception handling for empty preconditions
        if (!precondition.getMCMTTranslation().equals(""))
            this.guard = precondition.getMCMTTranslation();
        else
            throw new EmptyGuardException("The guard of transition " + name + " cannot be empty!");//this.guard = "(= " + SystemConstants.TRUE.getName() + " " + SystemConstants.TRUE.getName() + ")";
        this.setTable = new HashMap<CaseVariable, String>();
        this.name = name;
        this.dataSchema = dataSchema;
    }

    //ToDo: change this constructor and avoid creating many "empty" objects

    /**
     * Creates an insert transition without a precondition (that is, the precondition in this case is simply going to be {@code true}).
     *
     * @param name       The insert transition name.
     * @param dataSchema The data schema reference.
     */
    public InsertTransition(String name, DataSchema dataSchema) throws EevarOverflowException {
        this.precondition = new ConjunctiveSelectQuery(dataSchema);
        this.eevarAssociation = precondition.getRefManager();
        this.guard = "";//"(= " + SystemConstants.TRUE.getName() + " " + SystemConstants.TRUE.getName() + ")";
        this.setTable = new HashMap<CaseVariable, String>();
        this.name = name;
        this.dataSchema = dataSchema;
    }


    //ToDo: describe when exceptions may happen

    /**
     * A method that creates an insert part ({@code INSERT v1,..,vn INTO R}) of the insert transition.
     *
     * @param relation The relation that gets updated.
     * @param values   The v1,...,vn values.
     * @throws InvalidInputException
     * @throws UnmatchingSortException
     */
    public void insert(RepositoryRelation relation, Term... values) throws InvalidInputException, UnmatchingSortException {
        // first control that the attributes inserted matches the relation's arity
        if (relation.arity() != values.length) {
            throw new InvalidInputException("No matching between arity of the relation and the number of inserted values. The number of inserted values should be " + relation.arity());
        }
        // update part
        for (RepositoryRelation rep : this.dataSchema.getRepositoryRelations()) {
            if (rep.equals(relation)) {
                for (int i = 0; i < relation.arity(); i++) {
                    // control if it is eevar or constant; if not, throw an exception
                    boolean eevar = this.eevarAssociation.containsKey(values[i].getName());
                    boolean caseVar = this.dataSchema.getCaseVariableAssociations().keySet().contains(values[i].getName());
                    if (!eevar && !caseVar && !this.dataSchema.getConstants().containsKey(values[i].getName()) && !values[i].getName().toLowerCase().equals("null"))
                        throw new InvalidInputException(values[i].getName() + " is neither an answer of the precondition nor a constant");


                    // control the sorts of eevars/constants in the SELECT statement to see if they match those of the relation they reference to
                    if (eevar)
                        checkEevarSorts(relation.getAttributeByIndex(i).getSort().getSortName(), values[i].getName());
                    else if (caseVar)
                        checkCaseVarSorts(relation.getAttributeByIndex(i).getSort().getSortName(), values[i].getName());
                    else
                        checkConstantSorts(relation.getAttributeByIndex(i).getSort().getSortName(), values[i].getName());

                    if (!precondition.isIndexPresent())
                        this.guard += "(= " + relation.getName() + (i + 1) + "[x] " + SystemConstants.NULL.getName() + "_" + relation.getAttributeByIndex(i).getSort().getSortName() + ") ";
                    else
                        this.guard += "(= " + relation.getName() + (i + 1) + "[y] " + SystemConstants.NULL.getName() + "_" + relation.getAttributeByIndex(i).getSort().getSortName() + ") ";

                    if (eevar)
                        this.localUpdate += ":val " + this.eevarAssociation.get(values[i].getName()) + "\n";
                    else if (caseVar)
                        this.localUpdate += ":val " + values[i].getName() + "\n";
                    else {//if it's not an eevar and there was no exception generated, then it's a constant
                        if (values[i].getName().toLowerCase().equals("null"))//manage separately NULLs
                            this.localUpdate += ":val " + SystemConstants.NULL.getName() + "_" + relation.getAttributeByIndex(i).getSort().getSortName() + "\n";
                        else
                            this.localUpdate += ":val " + values[i].getName() + "\n";
                    }
                    this.localStatic += ":val " + rep.getName() + (i + 1) + "[j]\n";
                }
            } else {
                for (int i = 0; i < rep.arity(); i++) {
                    this.localUpdate += ":val " + rep.getName() + (i + 1) + "[j]\n";
                    this.localStatic += ":val " + rep.getName() + (i + 1) + "[j]\n";
                }
            }
        }

    }

    //A method that throws an exception if a given variable sort does not match with a sort of some given element.
    private void checkEevarSorts(String elementSortName, String varName) throws UnmatchingSortException {
        if (!elementSortName
                .equals(EevarManager.getSortByVariable(this.eevarAssociation.get(varName)).getSortName()))
            throw new UnmatchingSortException("No matching between sorts: " + elementSortName + " VS " + EevarManager.getSortByVariable(this.eevarAssociation.get(varName)).getSortName());
    }

    //A method that throws an exception if a given variable sort does not match with a sort of some given element.
    private void checkCaseVarSorts(String elementSortName, String varName) throws UnmatchingSortException {
        if (!elementSortName.equals(this.dataSchema.getCaseVariableAssociations().get(varName).getSort().getSortName()))
            throw new UnmatchingSortException("No matching between sorts: " + elementSortName + " VS " + this.dataSchema.getCaseVariableAssociations().get(varName).getSort().getSortName());
    }

    //A method that throws an exception if a given eevar sort does not match with a sort of some given element.
    private void checkConstantSorts(String elementSortName, String constantName) throws UnmatchingSortException {
        if (!constantName.toLowerCase().equals("null"))
            if (!elementSortName.equals(this.dataSchema.getConstants().get(constantName).getSort().getSortName()))
                throw new UnmatchingSortException("No matching between sorts: " + elementSortName + " VS " + this.dataSchema.getConstants().get(constantName).getSort().getSortName());
    }


    @Override
    /**
     * A method that adds an assignment expression of the form {@code x = v} into the set part ({@code SET x1 = v1,...,xn = vn}) of the insert transition.
     * Informally, this expression updates the {@link Constant} value of the case variable {@code x}.
     * Normally, these values should be taken from those enlisted in {@link State}.
     * However, one can also use a generically defined {@link Constant} for defining, for example, an error.
     *
     * @param variable  The case variable {@code x}.
     * @param newValue The new state value {@code v} assigned to {@code x}.
     * @throws InvalidInputException
     * @throws UnmatchingSortException
     */
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


    @Override
    /**
     * A method that adds an assignment expression of the form {@code x = v} into the set part ({@code SET x1 = v1,...,xn = vn}) of the insert transition.
     * Informally, this expression updates the value of the case variable {@code x} by assigning to it a value from one of the attributes appearing in
     * the {@code SELECT} statement of the precondition query of the transition.
     *
     * @param variable  The case variable {@code x}.
     * @param attr The attribute from the {@code SELECT} query.
     * @throws InvalidInputException
     * @throws UnmatchingSortException
     */
    public void setControlCaseVariableValue(CaseVariable variable, Attribute attr) throws InvalidInputException, UnmatchingSortException {
        // first step: control that the case variable is in the collection of changes
        // insert it if it is not present
        if (this.setTable.containsKey(variable)) {
            System.out.println("CaseVariable already set " + variable.getName() + " in " + this.name);
            return;
        }
        //check if the attribute assigned is an eevar (i.e., appears in the SELECT query of the precondition)
        String attrName = attr.getName();
        if (!eevarAssociation.containsKey(attrName))
            throw new InvalidInputException("the second operand in " + variable.getName() + "=" + attr.getName() + " is not appearing in the SELECT query of the transition precondition!");
        //check whether sorts are matching
        checkEevarSorts(variable.getSort().getSortName(), attrName);

        //populate the table of variable assignments with the x=v expression
        this.setTable.put(variable, this.eevarAssociation.get(attr.getName()));
    }


    @Override
    /**
     * A method that adds an assignment expression of the form {@code x = v} into the set part ({@code SET x1 = v1,...,xn = vn}) of the insert transition.
     * Informally, this expression updates the value of the case variable {@code x} by assigning to it a value from one of the newly defined variables
     * appearing in [var v : Type] statements of the transition.
     *
     * @param variable  The case variable {@code x}.
     * @param newVar The new variable that has been defined appeared in the update block.
     * @throws InvalidInputException
     * @throws UnmatchingSortException
     */
    public void setControlCaseVariableValue(CaseVariable variable, CaseVariable newVar) throws InvalidInputException, UnmatchingSortException, EevarOverflowException {
        // first step: control that the case variable is in the collection of changes
        // insert it if it is not present
        if (this.setTable.containsKey(variable)) {
            System.out.println("CaseVariable already set " + variable.getName() + " in " + this.name);
            return;
        }
        //check if the new variable assigned is an eevar
        String varName = newVar.getName();
        if (!eevarAssociation.containsKey(varName))
            addReferenceVariable(newVar); //add the new variable to the list of eevars

        //check whether sorts are matching
        checkEevarSorts(variable.getSort().getSortName(), varName);

        //populate the table of variable assignments with the x=v expression
        this.setTable.put(variable, this.eevarAssociation.get(varName));
    }

    /**
     * A method that performs the eevar association process.
     *
     * @param var The attribute for which the association process is performed.
     * @throws EevarOverflowException
     */
    private void addReferenceVariable(CaseVariable var) throws EevarOverflowException {

        // 1 ) check if in the global manager there are eevars with that sort
        Collection<String> eevarAvailable = EevarManager.getEevarWithSort(var.getSort());

        if (eevarAvailable.isEmpty()) {
            // add eevar to the global eevar manager
            String global_reference = EevarManager.addEevar(var.getSort());
            // add association locally
            this.eevarAssociation.put(var.getName(), global_reference);
        }
        // 2) process the array of eevars and see whether there is one that is free (it means it is not in the local map)
        else {
            for (String glb_name : eevarAvailable) {
                // case in which current one is not already used, I can use it
                if (!this.eevarAssociation.containsValue(glb_name)) {
                    this.eevarAssociation.put(var.getName(), glb_name);
                    return;
                }
            }
            // case in which all eevar already used
            String global_reference = EevarManager.addEevar(var.getSort());
            this.eevarAssociation.put(var.getName(), global_reference);
        }
    }

    //ToDo: check whether the description of the method is correct

    /**
     * @return A string containing a part of MCMT code representing updates for elements with :global declarations
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
     * @return {@code True}, if it is one case; {@code False}, otherwise.
     */
    private boolean isOneCase() {
        if (this.localUpdate.equals(""))
            return true;
        else
            return false;
    }

    @Override
    /**
     * @return A string containing an MCMT translation of the insert transition.
     */
    public String getMCMTTranslation() {
        String finalMCMT = ":comment " + NameProcessor.getTransitionName(this.name) + "\n:transition\n:var j\n";
        // control of the indexes
        if (this.precondition.isIndexPresent()) {
            finalMCMT += ":var x\n";
            if (!this.isOneCase())
                finalMCMT += ":var y\n";
        } else if (!this.isOneCase())
            finalMCMT += ":var x\n";

        finalMCMT += ":guard " + this.guard + "\n";

        // one case
        if (this.isOneCase())
            finalMCMT += this.generateOneCaseMCMT() + "\n";
            // two cases
        else {
            if (this.precondition.isIndexPresent())
                finalMCMT += ":numcases 2\n:case (= j y)\n";
            else
                finalMCMT += ":numcases 2\n:case (= j x)\n";

            finalMCMT += this.localUpdate + "\n";
            finalMCMT += this.generateGlobalMCMT();
            finalMCMT += "\n:case\n";
            finalMCMT += this.localStatic + "\n";
            finalMCMT += this.generateGlobalMCMT() + "\n";
        }

        return finalMCMT;
    }

    /**
     * A method for setting an MCMT translation of the precondition represented  as the guard in the MCMT transition.
     *
     * @param guard The MCMT translation of the precondition.
     */
    public void setGuard(String guard) {
        this.guard = guard;
    }


    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void addTaskGuard(String guard) {
        this.guard += guard;
    }


}