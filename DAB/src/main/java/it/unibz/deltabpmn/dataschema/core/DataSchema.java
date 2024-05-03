package it.unibz.deltabpmn.dataschema.core;

import com.healthmarketscience.sqlbuilder.dbspec.basic.DbSchema;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbSpec;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbTable;
import it.unibz.deltabpmn.datalogic.EevarManager;
import it.unibz.deltabpmn.dataschema.elements.*;
import it.unibz.deltabpmn.dataschema.elements.providers.*;
import it.unibz.deltabpmn.exception.DuplicateDeclarationException;
import it.unibz.deltabpmn.exception.EevarOverflowException;
import it.unibz.deltabpmn.processschema.core.State;

import java.util.*;

public final class DataSchema implements SortProvider, ConstantProvider, RepositoryRelationProvider, CatalogRelationProvider, CaseVariableProvider {
    private static DataSchema dataSchema = new DataSchema();
    //a collection of all generated sorts (required for preserving the uniqueness of every created Sort object)
    private Map<String, Sort> sorts;
    private Map<String, Constant> constants;
    private Map<String, CatalogRelation> catalog;
    private Map<String, RepositoryRelation> repository;
    private Map<String, CaseVariable> caseVariables;
    private Map<String, Attribute> attributes;
    private Map<String, String> eevars;
    private DbSpec spec;
    private DbSchema schema;


    public DataSchema() {
        //1. initialize all the components needed for sorts
        this.sorts = new HashMap<String, Sort>();
        initializeSorts();
        //2. add all the constants required by the general encoding
        this.constants = new HashMap<String, Constant>();
        intializeConstants();
        //3. initialize all the components needed for managing relations
        this.catalog = new HashMap<String, CatalogRelation>();
        this.repository = new HashMap<String, RepositoryRelation>();
        this.spec = new DbSpec();
        this.schema = spec.addDefaultSchema();
        //4. initialize all the case variables and eevars
        this.caseVariables = new HashMap<String, CaseVariable>();
        this.eevars = new HashMap<>();
        //a map for collecting data about the attributes of all the relations
        this.attributes = new HashMap<>();
        //create a special lifecylce variable for managing empty Blocks
        newCaseVariable(SystemVariables.EMPTY.getName(), SystemVariables.EMPTY.getSort(), SystemVariables.EMPTY.isOneCase());
    }

    /**
     * @return The data schema object.
     */
    public static DataSchema getInstance() {
        return dataSchema;
    }

    public Map<String, Attribute> getAllAttributes() {
        if (this.attributes.isEmpty()) {
            for (CatalogRelation catalogRelation : getCatalogRelations())//populate a map containing all attributes
                catalogRelation.getAttributes().stream().forEach(attr -> this.attributes.put(attr.getName(), attr));
            for (RepositoryRelation repositoryRelation : getRepositoryRelations())//populate a map containing all attributes
                repositoryRelation.getAttributes().stream().forEach(attr -> this.attributes.put(attr.getName(), attr));
        }
        return this.attributes;
    }

    @Override
    /**
     * If a sort does not exist, then the method creates a new one.
     * Otherwise, it returns a link to an already created {@code Sort} object.
     * @param name A sort name.
     * @return A {@code Sort} object.
     */
    public Sort newSort(String name) {
        if (sorts.containsKey(name))
            return sorts.get(name);
        else {
            Sort sort = new DABSort(name);
            sorts.put(name, sort);
            return sort;
        }
    }

    @Override
    /**
     * If a constant does not exist, then the method creates a new one.
     * Otherwise, it returns a link to an already created {@code Constant} object.
     *
     * @param name A constant name.
     * @param sort A constant sort.
     * @return A {@code Constant} object.
     */
    public Constant newConstant(String name, Sort sort) {
        if (constants.containsKey(name))
            return constants.get(name);
        else {
            Constant c = new DABConstant(name, sort);
            constants.put(name, c);
            return c;
        }
    }

    /**
     * A method that adds basic sorts that have to be specified in every MCMT program.
     */
    private void initializeSorts() {
        sorts.put(SystemSorts.BOOL.getSortName(), SystemSorts.BOOL);
        sorts.put(SystemSorts.INT.getSortName(), SystemSorts.INT);
        sorts.put(SystemSorts.STRING.getSortName(), SystemSorts.STRING);
    }

    public void removeSort(Sort sort) {
        this.sorts.remove(sort.getSortName());
    }

    /**
     * A method that adds constants that have to be specified in every MCMT program working with DABs.
     */
    private void intializeConstants() {
        State[] systemStates = State.values();
        for(int i=0;i<systemStates.length;i++)
            newConstant(systemStates[i].getName(),systemStates[i].getSort());
//        newConstant(State.IDLE.getName(), State.IDLE.getSort());
//        newConstant(State.ENABLED.getName(), State.ENABLED.getSort());
//        newConstant(State.ACTIVE.getName(), State.ACTIVE.getSort());
//        newConstant(State.COMPLETED.getName(), State.COMPLETED.getSort());
//        newConstant(State.ACTIVE_SINGLE.getName(), State.ACTIVE_SINGLE.getSort());
//        newConstant(State.ACTIVE_ALL.getName(), State.ACTIVE_ALL.getSort());
//        newConstant(State.ERROR.getName(), State.ERROR.getSort());

        newConstant(SystemConstants.TRUE.getName(), SystemConstants.TRUE.getSort());
        newConstant(SystemConstants.FALSE.getName(), SystemConstants.FALSE.getSort());
        //newConstant(SystemConstants.NULL.getName(), SystemConstants.NULL.getSort());
    }

    @Override
    /**
     * Creates a Catalog relation. If a relation does not exist, then the method creates a new one.
     * Otherwise, it returns a link to an already created {@code DABCatalogRelation} object.
     *
     * @param name A {@code DABCatalogRelation} relation name.
     * @return A {@code DABCatalogRelation} relation object.
     */
    public CatalogRelation newCatalogRelation(String name) {
        if (this.catalog.containsKey(name))
            return this.catalog.get(name);
        else {
            CatalogRelation catRelation = new DABCatalogRelation(name, this);
            this.catalog.put(name, catRelation);
            return catRelation;
        }
    }

    @Override
    /**
     * Creates a Repository relation. If a relation does not exist, then the method creates a new one.
     * Otherwise, it returns a link to an already created {@code DABRepositoryRelation} object.
     *
     * @param name A {@code DABRepositoryRelation} relation name.
     * @return A {@code DABRepositoryRelation} object.
     */
    public RepositoryRelation newRepositoryRelation(String name) {
        if (this.repository.containsKey(name))
            return this.repository.get(name);
        else {
            DABRepositoryRelation repRelation = new DABRepositoryRelation(name, this);
            this.repository.put(name, repRelation);
            return repRelation;
        }
    }

    @Override
    /**
     * If a case variable does not exist, then the method creates a new one.
     * Otherwise, it returns a link to an already created {@code DABCaseVariable} object.
     *
     * @param name A case variable name.
     * @param sort A case variable sort.
     * @param type {@code true} is the case variable is used in the one-case setting. {@code false} otherwise.
     * @return A {@CaseVariable} object.
     */
    public CaseVariable newCaseVariable(String name, Sort sort, boolean type) {
        if (this.caseVariables.containsKey(name))
            return caseVariables.get(name);
        else {
            CaseVariable variable = new DABCaseVariable(name, sort, type);
            caseVariables.put(name, variable);
            return variable;
        }
    }

    /**
     * @return A a list of all sorts contained in the data schema.
     */
    public List<Sort> getSorts() {
        return new ArrayList<Sort>(this.sorts.values());
    }

    /**
     * @return A map of all constants (together with their string representations) contained in the data schema.
     */
    public Map<String, Constant> getConstants() {
        return this.constants;
    }

    /**
     * @return A  list of all Catalog relations contained in the data schema.
     */
    public List<CatalogRelation> getCatalogRelations() {
        return new ArrayList<CatalogRelation>(this.catalog.values());
    }

    /**
     * @return A map with (catalog relation name, object) pairs
     */
    public Map<String, CatalogRelation> getCatalogRelationAssociations() {
        return this.catalog;
    }


    /**
     * @return A list of all Repository relations contained in the data schema.
     */
    public List<RepositoryRelation> getRepositoryRelations() {
        return new ArrayList<RepositoryRelation>(this.repository.values());
    }

    /**
     * @return A map with (repository relation name, object) pairs
     */
    public Map<String, RepositoryRelation> getRepositoryRelationAssociations() {
        return this.repository;
    }

    /**
     * @return A list of all case variables contained in the data schema.
     */
    public List<CaseVariable> getCaseVariables() {
        return new ArrayList<CaseVariable>(this.caseVariables.values());
    }

    /**
     * @return A map of (CaseVariable_Name,CaseVariable_Object) pairs
     */
    public Map<String, CaseVariable> getCaseVariableAssociations() {
        return this.caseVariables;
    }

    /**
     * A method that updates the database schema component
     * {@link com.healthmarketscience.sqlbuilder.dbspec.basic.DbSchema} of the given data schema
     * by creating a table and adding it to the database schema component.
     *
     * @param tableName The table name.
     * @return The created table.
     */
    public DbTable addTableToDbSchema(String tableName) {
        return this.schema.addTable(tableName);
    }

    /**
     * A method for remembering variables declared in {@code var} clauses (these variables are essentially eevars)
     *
     * @param name
     */
    public void addEevar(String name, Sort sort) throws EevarOverflowException, DuplicateDeclarationException {

        // 1 ) check if the declared variable has been already declared before
        if (this.eevars.containsKey(name))
            throw new DuplicateDeclarationException("Variable with name " + name + " has been already declared in hte process model!");

        // 2 ) check if in the global manager there are eevar with that sort
        Collection<String> eevarAvailable = EevarManager.getEevarWithSort(sort);

        if (eevarAvailable.isEmpty()) {
            // add eevar to the global eevar manager
            String globalReference = EevarManager.addEevar(sort);
            // add association locally
            this.eevars.put(name, globalReference);
        }
        // 3 process the array and look if one is free (it means it is not in the local map)
        else {
            for (String globalEevar : eevarAvailable) {
            	System.out.println(globalEevar);
                // case in which current one is not already used, I can use it
                if (!this.eevars.containsValue(globalEevar)) {
                    this.eevars.put(name, globalEevar);
                    return;
                }
            }
            // case in which all eevar already used
            String globalReference = EevarManager.addEevar(sort);
            this.eevars.put(name, globalReference);
        }
    }

    //ToDo: make List/Set outputs homogeneous for all class getters

    /**
     * @return The list of newly declared variables
     */
    public Set<String> getNewlyDeclaredVariables() {
        return this.eevars.keySet();
    }

    /**
     * @param varName
     * @return The name of an eevar associate to the newly declared (case) variable with name {@code varName}
     */
    public String getEeevarByCaseVarName(String varName) {
        return this.eevars.get(varName);
    }

    /**
     * Removes eevars from case variable declarations
     */
    public void eevarsOut() {
        for (String eevarName : this.eevars.keySet()) {
            this.caseVariables.remove(eevarName);
        }
    }
}
