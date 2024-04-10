package it.unibz.deltabpmn.dataschema.core;

import com.healthmarketscience.sqlbuilder.dbspec.basic.DbColumn;
import it.unibz.deltabpmn.dataschema.elements.Attribute;
import it.unibz.deltabpmn.dataschema.elements.Relation;
import it.unibz.deltabpmn.dataschema.elements.Sort;

/**
 * Class responsible for creating sorted (or typed) attributes.
 * Each attribute is characterized by particular name and sort
 * as well as a relation it belongs to and a column it relates to in that relation.
 */
class DABAttribute implements Attribute {
    private String name;
    private Sort sort;
    private DbColumn dbColumn; //a column used to associate the attribute to in the database
    private Relation inRelation; //a relation to which the attribute belongs (we assume that an attribute can belong to only one relation)
    private String function_name = ""; //a name of a function corresponding to the functional algebraic representation of the attribute in MCMT

    /**
     * @param name     A specific name of the attribute
     * @param sort     A sort (or a type) of the attribute
     * @param relation A relation the attribute belongs to
     * @param column   A database column the attribute corresponds to
     */
    public DABAttribute(String name, Sort sort, Relation relation, DbColumn column) {
        this.name = name;
        this.sort = sort;
        this.dbColumn = column;
        this.inRelation = relation;
    }

    @Override
    /**
     * @return Name of the attribute
     */
    public String getName() {
        return name;
    }

    @Override
    /**
     * @return The sort of the attribute
     */
    public Sort getSort() {
        return sort;
    }

    @Override
    /**
     * @return The relation object to which the given attribute belongs
     */
    public Relation getRelation() {
        return inRelation;
    }

    @Override
    /**
     * @return The database column object represented by the attribute
     */
    public DbColumn getDbColumn() {
        return this.dbColumn;
    }

    @Override
    /**
     * Assigns to the attribute a string with its functional algebraic representation in MCMT.
     *
     * @param functionName
     */
    public void setFunctionalView(String functionName) {
        this.function_name = functionName;
    }

    @Override
    /**
     * @return The functional algebraic representation of the attribute in MCMT.
     */
    public String getFunctionalView() {
        return function_name;
    }


    @Override
    public String toString() {
        return name + ":" + sort.toString();
    }

    //ToDo: this is not really a "declaration" ==> find another name and create a corresponding interface

    @Override
    /**
     * Generates an MCMT declaration.
     *
     * @return The attribute declaration in MCMT
     */
    public String getMCMTDeclaration() {
        String result = "";
        if (this.getFunctionalView().equals("") || (this.getRelation() instanceof DABRepositoryRelation))
            result += this.name;
        else {
            result += "(" + this.function_name + " " + ((DABCatalogRelation) this.getRelation()).getPrimaryKey().getName() + ")";
        }

        return result;
    }

    //ToDo: check the JavaDoc of this method

    @Override
    /**
     * Generates an MCMT declaration in case the attribute is affected by an eevar
     *
     * @param eevar An eevar variable name corresponding to the attribute (used in transitions)
     * @return The attribute declaration in MCMT
     */
    public String getMCMTDeclarationWithEEVAR(String eevar) {
        String result = "";

        if (this.getFunctionalView().equals("") || (this.getRelation() instanceof DABRepositoryRelation))
            result += this.name;
        else {
            result += "(" + this.function_name + " " + eevar + ")";
        }

        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof DABAttribute))
            return false;
        DABAttribute obj = (DABAttribute) o;
        return name.equals(obj.name) && inRelation.getName().equals(obj.inRelation.getName());
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + sort.hashCode();
        return result;
    }
}
