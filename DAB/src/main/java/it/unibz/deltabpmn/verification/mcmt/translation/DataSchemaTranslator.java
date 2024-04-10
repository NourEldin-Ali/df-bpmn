package it.unibz.deltabpmn.verification.mcmt.translation;

import it.unibz.deltabpmn.dataschema.core.DataSchema;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class DataSchemaTranslator {

    //used to realise the multiton pattern
    private static final ConcurrentMap<DataSchema, DataSchemaTranslator> instances = new ConcurrentHashMap<>();

    private final DataSchema Data_Schema;
    private final MCMTCaseVariables Case_Variable_Translator;
    private final MCMTCatalogRelations Catalog_Translator;
    private final MCMTConstants Constant_Translator;
    private final MCMTRepositoryRelations Repository_Translator;
    private final MCMTSorts Sort_Translator;

    private DataSchemaTranslator(DataSchema dataSchema) {
        this.Data_Schema = dataSchema;
        this.Case_Variable_Translator = new CaseVariableTranslator(this.Data_Schema);
        this.Catalog_Translator = new CatalogTranslator(this.Data_Schema);
        this.Constant_Translator = new ConstantTranslator(this.Data_Schema);
        this.Repository_Translator = new RepositoryTranslator(this.Data_Schema);
        this.Sort_Translator = new SortTranslator(this.Data_Schema);
    }

    public static DataSchemaTranslator getInstance(final DataSchema dataSchema) {
        return instances.computeIfAbsent(dataSchema, DataSchemaTranslator::new);
    }

    public MCMTCaseVariables CaseVariables() {
        return this.Case_Variable_Translator;
    }

    public MCMTCatalogRelations CatalogRelations() {
        return this.Catalog_Translator;
    }

    public MCMTConstants Constants() {
        return this.Constant_Translator;
    }

    public MCMTRepositoryRelations RepositoryRelations() {
        return this.Repository_Translator;
    }

    public MCMTSorts Sorts() {
        return this.Sort_Translator;
    }


}
