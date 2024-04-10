package it.unibz.deltabpmn.dataschema.elements;

import com.healthmarketscience.sqlbuilder.dbspec.basic.DbColumn;

public interface Attribute extends Variable, Value {

    String getMCMTDeclarationWithEEVAR(String eevar);

    String getFunctionalView();

    void setFunctionalView(String functionName);

    Relation getRelation();

    DbColumn getDbColumn();
}
