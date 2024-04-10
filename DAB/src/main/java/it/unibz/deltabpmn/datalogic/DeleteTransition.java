package it.unibz.deltabpmn.datalogic;


import it.unibz.deltabpmn.dataschema.core.DataSchema;
import it.unibz.deltabpmn.dataschema.core.SystemConstants;
import it.unibz.deltabpmn.dataschema.elements.*;
import it.unibz.deltabpmn.exception.EevarOverflowException;
import it.unibz.deltabpmn.exception.InvalidInputException;
import it.unibz.deltabpmn.exception.RepoRelationOverflowException;
import it.unibz.deltabpmn.exception.UnmatchingSortException;
import it.unibz.deltabpmn.processschema.core.NameProcessor;
import it.unibz.deltabpmn.processschema.core.State;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


public class DeleteTransition implements ComplexTransition {

    private ConjunctiveSelectQuery precondition;
    private Map<String, String> eevarAssociation;
    private Map<CaseVariable, String> setTable;
    private String name;
    private String guard;
    private String localUpdate = "";
    private String localStatic = "";
    private DataSchema dataSchema;

    public DeleteTransition(String name, ConjunctiveSelectQuery precondition, DataSchema dataSchema) {
        this.precondition = precondition;
        this.eevarAssociation = precondition.getRefManager();
        this.guard = precondition.getMCMTTranslation();
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
    public DeleteTransition(String name, DataSchema dataSchema) {
        this.precondition = new ConjunctiveSelectQuery(dataSchema);
        this.eevarAssociation = precondition.getRefManager();
        this.guard = "(= " + SystemConstants.TRUE.getName() + " " + SystemConstants.TRUE.getName() + ")";
        this.setTable = new HashMap<CaseVariable, String>();
        this.name = name;
        this.dataSchema = dataSchema;
    }

    //ToDo: describe when exceptions may happen

    //ToDo: change the EEVAR assocaition management and use proper CaseVariable objects instead of their names

    /**
     * A method that creates a delete part ({@code DEL v1,..,vn FROM R}) of the delete transition.
     *
     * @param relation The relation that gets updated.
     * @param values   The v1,...,vn values.
     * @throws InvalidInputException
     * @throws UnmatchingSortException
     */
    public void delete(RepositoryRelation relation, Term... values) throws UnmatchingSortException, InvalidInputException, RepoRelationOverflowException {
        // first control that the attributes inserted matches the relation's arity
        if (relation.arity() != values.length) {
            throw new InvalidInputException("No matching between arity of the relation and the number of deleted values. The number of inserted values should be " + relation.arity());
        }
        // update part
        for (RepositoryRelation rep : this.dataSchema.getRepositoryRelations()) {
            if (rep.equals(relation)) {
                for (int i = 0; i < relation.arity(); i++) {
                    // control if it is eevar or constant; if not, throw an exception
                    boolean eevar = this.eevarAssociation.containsKey(values[i].getName());
                    if (!eevar && !this.dataSchema.getConstants().containsKey(values[i].getName()))
                        throw new InvalidInputException(values[i].getName() + " is neither an answer of the precondition nor a constant");

                    // control the sorts of eevars/constants in the SELECT statement to see if they match those of the relation they reference to
                    if (eevar)
                        checkEevarSorts(relation.getAttributeByIndex(i).getSort().getSortName(), values[i].getName());
                    else
                        checkConstantSorts(relation.getAttributeByIndex(i).getSort().getSortName(), values[i].getName());

                    if (!this.precondition.isIndexPresent()) {
                        if (eevar)
                            this.guard += "(= " + relation.getName() + (i + 1) + "[x] " + this.eevarAssociation.get(values[i].getName())
                                    + ") ";
                        else
                            this.guard += "(= " + relation.getName() + (i + 1) + "[x] " + values[i].getName() + ") ";
                    } else //if there are repository relations in the FROM clause of the SELECT query, we need to make sure that we refer to correct index variables
                    {
                        if (eevar) {
                            if (!Arrays.asList(this.precondition.getSelectedAttributes()).contains(values[i]))//this check alows to remove needless atomic expressions from the MCMT transition guard
                                this.guard += "(= " + relation.getName() + (i + 1) + "[" + this.precondition.getIndexVarForRepositoryRelation(relation) + "] " + this.eevarAssociation.get(values[i].getName())
                                        + ") ";
//                            this.guard += "(= " + relation.getName() + (i + 1) + "[y] " + this.eevarAssociation.get(values[i].getName())
//                                    + ") ";
                        } else
                            guard += "(= " + relation.getName() + (i + 1) + "[" + this.precondition.getIndexVarForRepositoryRelation(relation) + "] " + values[i].getName() + ") ";
//                            guard += "(= " + relation.getName() + (i + 1) + "[y] " + values[i].getName() + ") ";
                    }

                    this.localUpdate += ":val " + SystemConstants.NULL.getName() + "_" + relation.getAttributeByIndex(i).getSort().getSortName() + "\n";
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


    @Override
    /**
     * A method that adds an assignment expression of the form {@code x = v} into the set part ({@code SET x1 = v1,...,xn = vn}) of the delete transition.
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
        //if the constant is NULL, then add the variable's sort name to it
        if (newValue.equals(SystemConstants.NULL))
            cName += "_" + variable.getSort().getSortName();

        //populate the table of variable assignments with the x=v expression
        this.setTable.put(variable, cName);
    }


    @Override
    /**
     * A method that adds an assignment expression of the form {@code x = v} into the set part ({@code SET x1 = v1,...,xn = vn}) of the delete transition.
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


    //A method that throws an exception if a given variable sort does not match with a sort of some given element.
    private void checkEevarSorts(String elementSortName, String varName) throws UnmatchingSortException {
        if (!elementSortName
                .equals(EevarManager.getSortByVariable(this.eevarAssociation.get(varName)).getSortName()))
            throw new UnmatchingSortException("No matching between sorts: " + elementSortName + " VS " + EevarManager.getSortByVariable(this.eevarAssociation.get(varName)).getSortName());
    }

    //A method that throws an exception if a given eevar sort does not match with a sort of some given element.
    private void checkConstantSorts(String elementSortName, String constantName) throws UnmatchingSortException {
        if (!elementSortName.equals(this.dataSchema.getConstants().get(constantName).getSort().getSortName())) {
            throw new UnmatchingSortException("No matching between sorts: " + elementSortName + " VS " + this.dataSchema.getConstants().get(constantName).getSort().getSortName());
        }
    }

    //ToDo: check whether the description of the method is correct

    /**
     * @return A string containing a part of MCMT code representing global updates.
     */
    private String generateGlobalMCMT() {
        String result = "";

        for (CaseVariable caseVar : this.dataSchema.getCaseVariables()) {
            // control if the user set a new value for the current case variable
            if (this.setTable.containsKey(caseVar)) {
                result += ":val " + this.setTable.get(caseVar) + "\n";
            } else
                result += ":val " + caseVar.getName() + "\n";
        }
        return result;
    }


    //ToDo: which kind of one state are we talking about here?

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
        String final_mcmt = ":comment " + NameProcessor.getTransitionName(this.name) + "\n:transition\n:var j\n";
        // control of the indexes
        if (this.precondition.isIndexPresent()) {
            final_mcmt += ":var x\n";
            if (!this.isOneCase() && this.precondition.getRepositoryRelationCount() == 2) //add index variable y only if there are two repository relations in the SELECT query
                final_mcmt += ":var y\n";
        } else if (!this.isOneCase())
            final_mcmt += ":var x\n";

        final_mcmt += ":guard " + this.guard + "\n";

        // one case
        if (this.isOneCase())
            final_mcmt += this.generateOneCaseMCMT();
            // two cases
        else {
            if (this.precondition.isIndexPresent() && this.precondition.getRepositoryRelationCount() == 2)// use two cases with index variable y only if we have two repository relations
                final_mcmt += ":numcases 2\n:case (= j y)\n";
            else
                final_mcmt += ":numcases 2\n:case (= j x)\n";
            final_mcmt += this.localUpdate + "\n";
            final_mcmt += this.generateGlobalMCMT();
            final_mcmt += "\n:case\n";
            final_mcmt += this.localStatic + "\n";
            final_mcmt += this.generateGlobalMCMT();
        }
        return final_mcmt;
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


    @Override
    public void addTaskGuard(String toAdd) {
        this.guard += toAdd;
    }

}
