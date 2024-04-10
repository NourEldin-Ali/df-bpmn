package it.unibz.deltabpmn.datalogic;


import it.unibz.deltabpmn.dataschema.core.DataSchema;
import it.unibz.deltabpmn.dataschema.core.SystemConstants;
import it.unibz.deltabpmn.dataschema.elements.*;
import it.unibz.deltabpmn.exception.InvalidInputException;
import it.unibz.deltabpmn.exception.UnmatchingSortException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//ToDo: do we need the comment regarding "impact on the cases"??

/**
 * A class used for representing a node of the bulk update tree implementing
 * the tree structure of the conditional update of DABs.
 * Here, each node is either an inner node, if it is characterized by a condition,
 * or a leaf node, if it is characterized by a set rule.
 * Each condition added will have impact on the cases of the MCMT transitions.
 *
 * @author Davide Cremonini
 */
public class BulkCondition {

    //ToDo: add comments explaining what are these variables needed for
    private RepositoryRelation relationToUpdate;
    private String condition;
    private String negatedCondition;
    private boolean isLeaf;
    private BulkCondition true_node;
    private BulkCondition false_node;
    private List<String> falseList;
    private Map<String, String> eevarAssociation;
    private Map<String, String> setTable;
    private DataSchema dataSchema;


    /**
     * @param relationToUpdate indicates the relation subject of the update effect
     */
    public BulkCondition(RepositoryRelation relationToUpdate, DataSchema dataSchema) throws InvalidInputException {
        this.relationToUpdate = relationToUpdate;
        this.falseList = new ArrayList<String>();
        this.eevarAssociation = new HashMap<String, String>();
        this.setTable = new HashMap<String, String>();
        this.condition = "";
        this.negatedCondition = "";
        this.isLeaf = false;
        this.dataSchema = dataSchema;
    }

    //ToDo: add MCMT representation format
    //ToDo: why to throw this exception? isn't it possible to make sure that we use only attributes appearing in the relations to update?

    /**
     * Method that checks if the attribute passed as a parameter belongs to a relation that has to be updated
     * and returns its corresponding MCMT representation (that will refer to a local array variable in the overall
     * MCMT specification of the DAB).
     *
     * @param attr The attribute to be checked.
     * @return An MCMT representation of the attribute.
     * @throws InvalidInputException Appears if the attribute does not refer to a
     *                               correct repository relation specified in the update.
     */
    private String prepareAttribute(Attribute attr) throws InvalidInputException {
        if (!(attr.getRelation() instanceof RepositoryRelation) || !((RepositoryRelation) attr.getRelation() == this.relationToUpdate))
            throw new InvalidInputException("DABAttribute " + attr.getName() + " should refer to correct Repository relation: " + relationToUpdate.getName());
        String result = "";
        for (int i = 0; i < relationToUpdate.getAttributes().size(); i++) {
            if (relationToUpdate.getAttributeByIndex(i) == attr) {
                result = relationToUpdate.getName() + (i + 1) + "[j]";
                break;
            }//ToDo: remove break and replace with another logic!
        }
        return result;
    }

    //ToDo: why is an Object used here? can't we use a constant/variable?
    //ToDo: this method may return wrong results as toString methods of Constant and CaseVariable objects are not returning MCMT specifications!

    //ToDo: remove the method

    /**
     * A method for building a condition "greater than" between an attribute and an integer number.
     *
     * @param attr  The attribute (of the relation that has to be updated) to insert in the condition.
     * @param value The integer number appearing in the condition.
     */
    public BulkCondition addGreaterThanCondition(Attribute attr, int value) throws InvalidInputException {
        this.condition += " (> " + prepareAttribute(attr) + " " + value + ")";
        this.falseList.add(" (not (> " + prepareAttribute(attr) + " " + value + "))");
        return this;
    }

    /**
     * A method for building a condition "greater than" between an attribute and an integer number.
     *
     * @param attr   The attribute (of the relation that has to be updated) to insert in the condition.
     * @param object The integer number appearing in the condition.
     */
    public BulkCondition addGreaterThanCondition(Attribute attr, Constant object) throws InvalidInputException {
        this.condition += " (> " + prepareAttribute(attr) + " " + object.getName() + ")";
        this.falseList.add(" (not (> " + prepareAttribute(attr) + " " + object.getName() + "))");
        return this;
    }

    //ToDo: remove the method

    /**
     * A method for building a condition "less than" between an attribute and an integer number.
     *
     * @param attr  The attribute (of the relation that has to be updated) to insert in the condition.
     * @param value The integer number appearing in the condition.
     */
    public BulkCondition addLessThanCondition(Attribute attr, int value) throws InvalidInputException {
        this.condition += " (< " + prepareAttribute(attr) + " " + value + ")";
        this.falseList.add(" (not (< " + prepareAttribute(attr) + " " + value + "))");
        return this;
    }

    /**
     * A method for building a condition "less than" between an attribute and an integer number.
     *
     * @param attr   The attribute (of the relation that has to be updated) to insert in the condition.
     * @param object The integer number appearing in the condition.
     */
    public BulkCondition addLessThanCondition(Attribute attr, Constant object) throws InvalidInputException {
        this.condition += " (< " + prepareAttribute(attr) + " " + object.getName() + ")";
        this.falseList.add(" (not (< " + prepareAttribute(attr) + " " + object.getName() + "))");
        return this;
    }

    /**
     * A method for building the equality condition between an attribute and a constant.
     *
     * @param attr   The attribute (of the relation that has to be updated) to insert in the condition.
     * @param object The constant appearing in the condition.
     */
    public BulkCondition addEqualsCondition(Attribute attr, Constant object) throws InvalidInputException {
        this.condition += " (= " + prepareAttribute(attr) + " " + object.getName() + ")";
        this.falseList.add(" (not (= " + prepareAttribute(attr) + " " + object.getName() + "))");
        return this;
    }

    /**
     * A method for building the equality condition between an attribute and a constant.
     *
     * @param attr   The attribute (of the relation that has to be updated) to insert in the condition.
     * @param object The constant appearing in the condition.
     */
    public BulkCondition addNotEqualsCondition(Attribute attr, Constant object) throws InvalidInputException {
        this.condition += " (not (= " + prepareAttribute(attr) + " " + object.getName() + "))";
        this.falseList.add(" (= " + prepareAttribute(attr) + " " + object.getName() + ")");
        return this;
    }

    //ToDo: remove the method

    /**
     * A method for building the equality condition between an attribute and an integer number.
     *
     * @param attr  The attribute (of the relation that has to be updated) to insert in the condition.
     * @param value The integer number appearing in the condition.
     */
    public BulkCondition addEqualsCondition(Attribute attr, int value) throws InvalidInputException {
        this.condition += " (= " + prepareAttribute(attr) + " " + value + ")";
        this.falseList.add(" (not (= " + prepareAttribute(attr) + " " + value + "))");
        return this;
    }

    /**
     * A method for building a condition which checks that a given tuple of attributes
     * is in a particular catalog relation.
     *
     * @param cat        The catalog relation.
     * @param attributes An array of the attributes that should be checked.
     * @throws InvalidInputException Appears if there is no matching between the arity of the relation and the size of the array of attributes.
     */
    public BulkCondition addInRelationCondition(CatalogRelation cat, Attribute... attributes) throws InvalidInputException {
        // first control arity
        if (cat.arity() != attributes.length) {
            throw new InvalidInputException(
                    "No matching between arity of the relation and number of values. The number of values should be "
                            + cat.arity() + " instead of " + attributes.length);
        }
        for (int i = 0; i < attributes.length; i++) {
            if (i == 0 && attributes[i] != null) {
                this.condition += " (not (= " + prepareAttribute(attributes[0]) + " " + SystemConstants.NULL.getName() + "_" + attributes[0].getSort().getSortName() + "))";
            } else {
                if (attributes[i] != null) {
                    this.condition += " (= (" + cat.getAttributeValueSignature((i + 1), prepareAttribute(attributes[0])) + ") " + prepareAttribute(attributes[i]) + ")";
                }
            }
        }
        return this;
    }

    /**
     * A method that adds a true-valued Child node in the tree of this {@code BulkUpdate} object.
     *
     * @return A BulkCondition object with added a true-valued child node.
     */
    public BulkCondition addTrueChild() throws InvalidInputException {
        BulkCondition newCondition = new BulkCondition(this.relationToUpdate, this.dataSchema);
        newCondition.setEevarAssociation(this.eevarAssociation);
        this.true_node = newCondition;
        return newCondition;
    }

    /**
     * A method that adds a true-valued Child node in the tree of this {@code BulkUpdate} object.
     *
     * @return A BulkCondition object with added a false-valued child node.
     */
    public BulkCondition addFalseChild() throws InvalidInputException {
        BulkCondition newCondition = new BulkCondition(this.relationToUpdate, this.dataSchema);
        newCondition.setEevarAssociation(this.eevarAssociation);
        this.false_node = newCondition;
        return newCondition;
    }

    //ToDo: name changes, set --> updateAttributeValue
    //ToDo: can we say something about how this method affects the tree construction?

    /**
     * A method that sets an update of an attribute of the repository relation by assigning to it a new value.
     *
     * @param attr     The attribute to be set.
     * @param newValue The value to be assigned to the attribute.
     * @throws InvalidInputException Appears if the attribute to be set is not an attribute of the considered relation to update or it has been already set.
     */
    public void updateAttributeValue(Attribute attr, String newValue) throws InvalidInputException, UnmatchingSortException {
        // control whether the attribute belongs to the updated repository relation
        if (((RepositoryRelation) attr.getRelation()) != relationToUpdate) {
            throw new InvalidInputException("DABAttribute to set is not an attribute of the considered relation " + relationToUpdate.getName());
        }
        //ToDo: what are these controls needed for??

        if (!eevarAssociation.containsKey(newValue) && !dataSchema.getConstants().containsKey(newValue))
            dataSchema.newConstant(newValue,attr.getSort());
        // control of eevar or constant newValue
        boolean eevar = isEevar(newValue);
        // control of sorts
        checkSorts(eevar, attr.getSort(), newValue);

        // check attribute not already set
        if (this.setTable.containsKey(prepareAttribute(attr)))
            throw new InvalidInputException("DABAttribute " + attr.getName() + " already set");

        this.setTable.put(prepareAttribute(attr), newValue);
        this.isLeaf = true;
    }

    //ToDo: add an MCMT template characterizing generated specification

    /**
     * A method that generates a string representation of the local update specification
     * using MCMT syntax.
     */
    public String getLocalUpdateMCMTTranslation() throws InvalidInputException {
        String result = "";

        // iterate through the repository relations in the data schema and find the relation used in the update
        for (RepositoryRelation rep : this.dataSchema.getRepositoryRelations()) {
            // case in which the relation is the one to be updated
            if (rep == relationToUpdate) {
                for (Attribute att : rep.getAttributes()) {
                    String corresponding_name = prepareAttribute(att);
                    if (this.setTable.containsKey(corresponding_name)) {
                        result += ":val " + this.setTable.get(corresponding_name) + "\n";
                    } else {
                        result += ":val " + corresponding_name + "\n";
                    }
                }
            }
            // case in which the relation is not the one to be updated
            else {
                for (int i = 0; i < rep.getAttributes().size(); i++) {
                    result += ":val " + rep.getName() + (i + 1) + "[j]\n";
                }
            }
        }
        return result;
    }


    /**
     * A method that checks for matching sorts.
     *
     * @param isEevar A boolean value that indicates whether the check is applied to an eevar variable.
     * @param first   A sort to be checked.
     * @param second  Name of the second object to check.
     * @throws UnmatchingSortException Appears if there is no matching between two sorts.
     */
    private void checkSorts(boolean isEevar, Sort first, String second) throws UnmatchingSortException {
        if (isEevar && !first.getSortName().equals(EevarManager.getSortByVariable(this.eevarAssociation.get(second)).getSortName())) {
            throw new UnmatchingSortException("No matching between sorts: " + first.getSortName() + " VS "
                    + EevarManager.getSortByVariable(eevarAssociation.get(second)).getSortName());
        }
        if (!isEevar && !first.getSortName().equals(dataSchema.getConstants().get(second).getSort().getSortName())) {
            throw new UnmatchingSortException("No matching between sorts: " + first.getSortName() + " VS "
                    + dataSchema.getConstants().get(second).getSort().getSortName());
        }
    }

    /**
     * A method that checks whether the value of the string passed as a parameter is an answer variable (that is, an eevar or a constant).
     *
     * @param value A value to be checked.
     * @return {@code True} if the value is an eevar; {@code False} if the value is a constant.
     * @throws InvalidInputException Appears if the value passed as a parameter is neither an eevar nor a constant.
     */
    private boolean isEevar(String value) throws InvalidInputException {
        //check special values
        if (value.toLowerCase().equals(SystemConstants.NULL.getName().toLowerCase()) || value.toLowerCase().equals(SystemConstants.TRUE.getName().toLowerCase()) || value.toLowerCase().equals(SystemConstants.FALSE.getName().toLowerCase()))
            return false;
        if (!eevarAssociation.containsKey(value)) {
            if (!dataSchema.getConstants().containsKey(value)) {
                throw new InvalidInputException(value + " is not an answer of the precondition or a constant");
            }
            return false;
        }
        return true;
    }


    /**
     * @return A string representing the bulk condition.
     */
    public String getCondition() {
        return this.condition;
    }


    /**
     * A Method for checking if the condition is a leaf node or not.
     *
     * @return {@code True} if the condition is a leaf node; {@code False} otherwise.
     */
    public boolean isLeaf() {
        return isLeaf;
    }


    //ToDo: explain better what's the eevar association.

    /**
     * @return A {@code Map} object representing the eevar association.
     */
    public Map<String, String> getEevarAssociation() {
        return eevarAssociation;
    }
    //ToDo: explain better what's the eevar association.

    /**
     * A method for setting the eevar associaton.
     *
     * @param eevar_association A {@code Map} object containing the eevar association.
     */
    public void setEevarAssociation(Map<String, String> eevar_association) {
        this.eevarAssociation = eevar_association;
    }

    //ToDo: explain what's the list of false conditions.

    /***
     * @return A {@code List} containing false conditions..
     */
    public List<String> getFalseList() {
        return falseList;
    }


    public BulkCondition getTrueNode() {
        return this.true_node;
    }

    public BulkCondition getFalseNode() {
        return this.false_node;
    }
}