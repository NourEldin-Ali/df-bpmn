package it.unibz.deltabpmn.dataschema.elements;

import it.unibz.deltabpmn.verification.mcmt.MCMTDeclarable;

/**
 * A interface for representing Catalog relations.
 * Each Catalog relation is defined by its name and a corresponding DB schema {@link com.healthmarketscience.sqlbuilder.dbspec.basic.DbSchema}.
 */
public interface CatalogRelation extends Relation, MCMTDeclarable {
    Attribute getPrimaryKey();

    String getFunctionNames();

    String getAttributeValueSignature(int n, String key);
}
