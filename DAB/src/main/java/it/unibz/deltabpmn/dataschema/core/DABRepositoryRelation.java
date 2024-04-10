package it.unibz.deltabpmn.dataschema.core;

import com.healthmarketscience.sqlbuilder.dbspec.basic.DbTable;
import it.unibz.deltabpmn.dataschema.elements.Attribute;
import it.unibz.deltabpmn.dataschema.elements.RepositoryRelation;
import it.unibz.deltabpmn.dataschema.elements.Sort;

import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * Class responsible for representing the Repository relation component of a data schema of a DAB.
 */
class DABRepositoryRelation implements RepositoryRelation {

    private String name;
    private DbTable relationTable;
    private ArrayList<Attribute> attributes;
    private DataSchema dataSchema;

    public DABRepositoryRelation(String name, DataSchema dataSchema) {
        this.name = name;
        this.attributes = new ArrayList<Attribute>();
        this.dataSchema = dataSchema;
        this.relationTable = this.dataSchema.addTableToDbSchema(name);
        this.dataSchema.addTableToDbSchema(name);
    }

    @Override
    /**
     * Creates and adds an attribute to the Repository relation based on its name and sort.
     *
     * @param name A String representing the attribute name.
     * @param sort A String representing the attribute sort.
     * @return An newly created attribute.
     */
    public Attribute addAttribute(String name, Sort sort) {
        Attribute attribute = new DABAttribute(name, sort,
                this, (this.relationTable.addColumn(name, sort.getSortName(),
                null)));
        this.attributes.add(attribute);
        return attribute;
    }

    //
//    // method for returning a particular column of an attribute given the index
//    public DbColumn getAttributeColumn(int index) {
//        return attributes.get(index).getDbColumn();
//    }
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

    @Override
    /**
     * @return Name of the Repository relation.
     */
    public String getName() {
        return this.name;
    }

    @Override
    /**
     * @return List of attributes of the Repository relation.
     */
    public ArrayList<Attribute> getAttributes() {
        return this.attributes;
    }

    @Override
    /**
     * @return DBTable of the relation.
     */
    public DbTable getDbTable() {
        return this.relationTable;
    }

    @Override
    /**
     * @return Arity of the relation
     */ public int arity() {
        return this.attributes.size();
    }


    /**
     * Generates an MCMT declaration.
     *
     * @return A String with an MCMT declaration of the current Repository relation.
     */
    public String getMCMTDeclaration() {
        String result = "";
        int attr_num = 1;
        for (Attribute attr : attributes) {
            result += ":local " + this.name + attr_num + " " + attr.getSort().getSortName() + "\n";
            attr_num++;
        }
//        for (DABAttribute attr : attributes)
//            result += ":local " + this.name + '_' + attr.getName() + " " + attr.getSort().getName() + "\n";
        return result;
    }

    @Override
    /**
     * Generates an MCMT declaration for initializing the Repository relation.
     *
     * @return A string with an MCMT declaration of the initialization statements for the given Repository relation.
     */
    public String getMCMTInitializationDeclaration() {
        String result = "";
        int attr_num = 1;

        for (Attribute attr : attributes) {
            result += "(= " + this.name + attr_num + "[x] " + SystemConstants.NULL.getName() + "_" + attr.getSort().getSortName() + ") ";
            attr_num++;
        }
//        for (DABAttribute attr : attributes)
//            result += "(= " + this.name + '_' + attr.getName() + "[x] NULL_" + attr.getSort().getName() + ") ";
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
        if (!(o instanceof DABRepositoryRelation))
            return false;
        DABRepositoryRelation obj = (DABRepositoryRelation) o;
        return name.equals(obj.name) && this.attributes.equals(obj.attributes);
    }

}