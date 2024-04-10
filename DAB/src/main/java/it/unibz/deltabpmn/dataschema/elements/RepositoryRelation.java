package it.unibz.deltabpmn.dataschema.elements;

import it.unibz.deltabpmn.verification.mcmt.MCMTDeclarable;

/**
 * A interface for representing Repository relations.
 * Each Repository relation is defined by its name and a corresponding DB schema {@link com.healthmarketscience.sqlbuilder.dbspec.basic.DbSchema}.
 */
public interface RepositoryRelation extends Relation, MCMTDeclarable {
    String getMCMTInitializationDeclaration();
}
