package it.unibz.deltabpmn.dataschema.core;

import com.healthmarketscience.sqlbuilder.dbspec.basic.DbColumn;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbTable;
import it.unibz.deltabpmn.dataschema.elements.Attribute;
import it.unibz.deltabpmn.dataschema.elements.CatalogRelation;
import it.unibz.deltabpmn.dataschema.elements.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Class responsible for representing the Catalog relation component of a data schema of a DAB.
 */
class DABCatalogRelation implements CatalogRelation {

    private String name;
    private Attribute primaryKey;
    private DbTable relationTable;
    private List<Attribute> attributes;
    private int function_number = 1;
    //private DbSchema schema;
    private DataSchema dataSchema;

    /**
     * Catalog relation constructor. A catalog relation is originally defined by its name and
     * a (reference to a) data schema. All attributes and the Primary Key should be added later on.
     *
     * @param name       A relation name.
     * @param dataSchema A reference to the data schema.
     */
    public DABCatalogRelation(String name, DataSchema dataSchema) {
        this.name = name;
        this.dataSchema = dataSchema;
        this.relationTable = this.dataSchema.addTableToDbSchema(name);
        this.attributes = new ArrayList<Attribute>();
    }

    @Override
    /**
     * @return Name of the Catalog relation
     */
    public String getName() {
        return this.name;
    }

    @Override
    /**
     * @return Primary Key attribute of the Catalog relation
     */
    public Attribute getPrimaryKey() {
        return this.primaryKey;
    }

    @Override
    /**
     * @return List of attributes of the Catalog relation
     */
    public List<Attribute> getAttributes() {
        return attributes;
    }

    @Override
    /**
     * @return DBTable of the relation
     */
    public DbTable getDbTable() {
        return this.relationTable;
    }

    @Override
    /**
     * Method that returns a signature of a function call for retrieving an n-th attribute value of the Catalog relation.
     *
     * @param n   An attribute index used for generating a corresponding function signature
     * @param key A string representing a key which this function is applied on (this is usually an e-variable)
     * @return Function name
     */
    public String getAttributeValueSignature(int n, String key) {
        return this.name + "F" + n + " " + key;
    }

    @Override
    /**
     * @return Arity of the relation
     */
    public int arity() {
        return this.attributes.size();
    }

    @Override
    /**
     * Creates and adds an attribute to the Catalog relation based on its name and sort.
     * The first added attribute is the Primary Key of the relation.
     *
     * @param name A String representing the attribute name.
     * @param sort A String representing the attribute sort.
     * @return An newly created attribute.
     */
    public Attribute addAttribute(String name, Sort sort) {
        Attribute attribute = new DABAttribute(name, sort, this, this.relationTable.addColumn(name, sort.toString(), null));
        if (this.attributes.size() == 0)
            this.primaryKey = attribute;
        else {
            attribute.setFunctionalView(this.name + "F" + function_number);
            function_number++;
        }
        this.attributes.add(attribute);
        return attribute;
    }


//        /**
//         * Method for returning a particular column of an attribute given the index
//         * @param index of the attribute
//         * @return particular column of an attribute given the index
//         */
//        public DbColumn getAttributeColumn(int index) {
//            return list_attributes.get(index).getDbColumn();
//        }
//

    @Override
    /**
     * Method for returning a particular attribute of the Repository relation given the index (enumeration starts from 0).
     * @param index of the attribute.
     * @return The attribute.*/
    public Attribute getAttributeByIndex(int index) {
        return this.attributes.get(index);
    }


    @Override
    public int getAttributesIndex(Attribute attr) {
        return this.attributes.indexOf(attr);
    }

    /**
     * Method for getting an alias of the current catalog relation.
     *
     * @return A Catalog relation being the alias of the current one.
     */
    public CatalogRelation getAlias() {
        //ToDo: why do we need this?
        DbTable alias = dataSchema.addTableToDbSchema(this.name);
        // copy the columns
        for (DbColumn col : this.relationTable.getColumns()) {
            alias.addColumn(col.getName(), col.getTypeNameSQL(), col.getTypeLength());
        }
        CatalogRelation catRelation = new DABCatalogRelation(this.name, this.dataSchema);
        //cat_relation.setDbTable(alias);
        for (Attribute att : this.attributes) {
            catRelation.addAttribute(alias.getAlias() + att.getName(), att.getSort());
        }
        return catRelation;
    }


    /**
     * Generates an MCMT declaration.
     *
     * @return An MCMT declaration of the current Catalog relation.
     */
    public String getMCMTDeclaration() {
        String result = "";
        int function_number = 0;
        for (Attribute attr : attributes) {
            if (function_number != 0)
                result += ":smt (define " + this.name + "F" + function_number + " ::(-> " + this.primaryKey.getSort().toString() + " " + attr.getSort().toString() + "))\n";
            function_number++;
        }
        return result;
    }

    @Override
    /**
     * Method for generating the MCMT declaration function names for the functional algebraic representation
     * of attributes that are not Primary Keys.
     *
     * @return A string of function names for the MCMT declaration
     */
    public String getFunctionNames() {
        String result = "";
        for (Attribute attr : attributes) {
            if (!attr.getFunctionalView().equals(""))
                result += attr.getFunctionalView() + " ";
        }
        return result;
    }


    @Override
    public String toString() {
        String result = this.name +
                "(" + this.attributes.
                stream().
                map(Attribute::toString).
                collect(Collectors.joining(","))
                + ")";
        return result;
    }


    @Override
    public int hashCode() {
        int result = name.hashCode();
        for (Attribute attr : this.attributes)
            result = 31 * result + attr.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof DABCatalogRelation))
            return false;
        DABCatalogRelation obj = (DABCatalogRelation) o;
        return name.equals(obj.name) && this.primaryKey.equals(obj.primaryKey);//we don't need to compare all the attributes, it's enough to check the name and the PK attribute
    }
}
