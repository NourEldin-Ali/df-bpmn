package it.unibz.deltabpmn.datalogic;


import it.unibz.deltabpmn.dataschema.core.DataSchema;
import it.unibz.deltabpmn.dataschema.core.SystemConstants;
import it.unibz.deltabpmn.dataschema.elements.Attribute;
import it.unibz.deltabpmn.dataschema.elements.CaseVariable;
import it.unibz.deltabpmn.dataschema.elements.Constant;
import it.unibz.deltabpmn.dataschema.elements.RepositoryRelation;
import it.unibz.deltabpmn.exception.EevarOverflowException;
import it.unibz.deltabpmn.exception.InvalidInputException;
import it.unibz.deltabpmn.exception.UnmatchingSortException;
import it.unibz.deltabpmn.processschema.core.NameProcessor;
import it.unibz.deltabpmn.processschema.core.State;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


/**
 * A class representing the DAB conditional update.
 * Conceptually, a conditional update object is represented as a tree consisting of inner nodes (conditions)
 * and leaves (each leaf is a set operations on a considered repository relation that has to be updated).
 */
public class BulkUpdate implements ComplexTransition {

    private ConjunctiveSelectQuery precondition;
    private Map<String, String> eevarAssociation;
    private String name;
    //private CaseVariable toSet; // only used in case when there is a case variable to be changed, the lifecycle of a current task
    private String guard;
    private String finalMCMT = "";
    private int numCases = 0;
    private String caseUpdate = "";
    private RepositoryRelation toUpdate;
    public BulkCondition root;
    private DataSchema dataSchema;
    private Map<CaseVariable, String> setTable;


    /***
     * @param name         The name of the transition.
     * @param precondition The precondition guard of the transition that defines the used eevars and the MCMT guard translation.
     * @param toUpdate     The repository relation that has to be updated.
     */
    public BulkUpdate(String name, ConjunctiveSelectQuery precondition, RepositoryRelation toUpdate, DataSchema dataSchema) throws InvalidInputException {
        this.precondition = precondition;
        this.eevarAssociation = precondition.getRefManager();
        this.guard = precondition.getMCMTTranslation();
        this.name = name;
        this.toUpdate = toUpdate;
        this.dataSchema = dataSchema;
        this.root = new BulkCondition(toUpdate, dataSchema);
        this.root.setEevarAssociation(this.eevarAssociation);
        this.setTable = new HashMap<CaseVariable, String>();

    }

    //ToDo: change management of TRUE preconditions

    /***
     * @param name         The name of the transition.
     * @param toUpdate     The repository relation that has to be updated.
     */
    public BulkUpdate(String name, RepositoryRelation toUpdate, DataSchema dataSchema) throws InvalidInputException {
        this.precondition = new ConjunctiveSelectQuery(dataSchema);
        this.eevarAssociation = precondition.getRefManager();
        this.guard = "(= " + SystemConstants.TRUE.getName() + " " + SystemConstants.TRUE.getName() + ")";
        this.name = name;
        this.toUpdate = toUpdate;
        this.dataSchema = dataSchema;
        this.root = new BulkCondition(toUpdate, dataSchema);
        this.root.setEevarAssociation(this.eevarAssociation);
        this.setTable = new HashMap<CaseVariable, String>();
    }


//    /**
//     * This method is only used in cases when there is a case variable to be changed that represents the lifecycle variable of a given task.
//     */
//    public void defineLifecycleVariable(CaseVariable variable) {
//        this.toSet = variable;
//    }



    /**
     * @return A string containing a part of MCMT code representing updates for elements with :global declarations (i.e., case variables)
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

    //ToDo: come up with an interface that would have a getMCMTTranslation method that doesn't throw exceptions

    @Override
    /**
     * A method generating a translation of the bulk update into MCMT code.
     * In MCMT the bulk update is represented as a transition.
     *
     * @return String representing the MCMT translation of the bulk update.
     */
    public String getMCMTTranslation() throws InvalidInputException {
        this.finalMCMT += ":comment " + NameProcessor.getTransitionName(this.name) + "\n:transition\n:var j\n";
        // control of the indexes
        if (this.precondition.isIndexPresent())
            this.finalMCMT += ":var x\n";
        this.finalMCMT += ":guard " + this.guard + "\n";

        //here to iterate through the three and print corresponding declaration
        processTree();
        this.finalMCMT += ":numcases " + this.numCases + "\n";
        this.finalMCMT += this.caseUpdate;

        return this.finalMCMT;
    }

    /**
     * A method for recursively traversing the tree and generating some MCMT code for the bulk update.
     */
    private void processTree() throws InvalidInputException {
        processTree(this.root, "");
    }

    /**
     * A method that recursively performs the DFS algorithm to process the tree and
     * generate some MCMT code corresponding to the bullk update.
     *
     * @param node      A current node of the tree.
     * @param condition An accumulator variable that keeps trace of conditions along the visited path.
     */
    private void processTree(BulkCondition node, String condition) throws InvalidInputException {
        //base case
        if (node.isLeaf()) {
            this.caseUpdate += ":case" + condition + "\n";
            this.caseUpdate += node.getLocalUpdateMCMTTranslation() + "\n";
            this.caseUpdate += this.generateGlobalMCMT() + "\n";
            this.numCases++;
            return;
        }
        processTree(node.getTrueNode(), condition + node.getCondition());
        for (String s : node.getFalseList()) {
            processTree(node.getFalseNode(), condition + s);
        }
    }

    /**
     * @return The name of the conditional update.
     */
    public String getName() {
        return this.name;
    }

    @Override
    /**
     * A method for modifying the task guard adding some conditions. Useful in the process schema translation, for adding
     * information about control case variables.
     *
     * @param toAdd A string representing the condition to add.
     */
    public void addTaskGuard(String toAdd) {
        this.guard += toAdd;
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


    //A method that throws an exception if a given variable sort does not match with a sort of some given element.
    private void checkEevarSorts(String elementSortName, String varName) throws UnmatchingSortException {
        if (!elementSortName
                .equals(EevarManager.getSortByVariable(this.eevarAssociation.get(varName)).getSortName()))
            throw new UnmatchingSortException("No matching between sorts: " + elementSortName + " VS " + EevarManager.getSortByVariable(this.eevarAssociation.get(varName)).getSortName());
    }


    //A method that throws an exception if a given eevar sort does not match with a sort of some given element.
    private void checkConstantSorts(String elementSortName, String constantName) throws UnmatchingSortException {
        if (!constantName.toLowerCase().equals("null"))
            if (!elementSortName.equals(this.dataSchema.getConstants().get(constantName).getSort().getSortName()))
                throw new UnmatchingSortException("No matching between sorts: " + elementSortName + " VS " + this.dataSchema.getConstants().get(constantName).getSort().getSortName());
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


    /*@Override
    public void setControlCaseVariableValue(CaseVariable variable, Constant newValue) throws InvalidInputException, UnmatchingSortException {
        try {
            throw new UnsupportedOperationException();
        } catch (UnsupportedOperationException ex) {
            System.out.println("Method not supported for bulk update [" + this.name + "]: bulk updates can't set case variable values!");
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
    }*/

}