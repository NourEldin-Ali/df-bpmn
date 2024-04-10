package it.unibz.deltabpmn.dataschema.elements;


// This interface is used so that both classes can be used as "in_relation" attribute of an object of type DABAttribute.
// Both Catalog and Repository relations are type of Relation.

import com.healthmarketscience.sqlbuilder.dbspec.basic.DbTable;

import java.util.List;

public interface Relation {
    Attribute addAttribute(String name, Sort sort);

    String getName();

    List<Attribute> getAttributes();

    DbTable getDbTable();

    int arity();

    Attribute getAttributeByIndex(int index);

    int getAttributesIndex(Attribute attr);
}
