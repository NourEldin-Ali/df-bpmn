package it.unibz.deltabpmn.dataschema.core;

//ToDo: add information about types of case variables regarding special codes

import it.unibz.deltabpmn.dataschema.elements.CaseVariable;
import it.unibz.deltabpmn.dataschema.elements.Sort;

/**
 * An implementation of the {@class CaseVariable} interface.
 * The class allows to construct case variables given their names, sorts and types (one-case or multi-case).
 */
class DABCaseVariable implements CaseVariable {
    private String name;
    private Sort sort;
    private boolean oneCase;//true if the process is one-case
    // 0 stands for normal case variable,
    // 1 for lifecycle variable,
    // 2 for lifecycle of the root process block.
    // this distinction is necessary for initialization
    private int lifecycle;

    //ToDo: is multiple-case translation supported now?

    /**
     * @param name    Name of the case variable.
     * @param sort    Sort of the case variable.
     * @param oneCase A boolean flag indicating if we are working with one case or multiple cases.
     */
    public DABCaseVariable(String name, Sort sort, boolean oneCase) {
        this.name = name;
        this.sort = sort;
        this.oneCase = oneCase;
        this.lifecycle = 0;
    }

    @Override
    /**
     * @return The name of the case variable.
     */
    public String getName() {
        return this.name;
    }

    @Override
    /**
     * @return The sort of the case variable.
     */
    public Sort getSort() {
        return this.sort;
    }

    @Override
    /**
     * @return {@code True}, if the case variable is used for a single-case scenario. {@code False} otherwise.
     */
    public boolean isOneCase() {
        return this.oneCase;
    }

    @Override
    /**
     * {@inheritDoc}
     */
    public int getLifeCycle() {
        return this.lifecycle;
    }

    //ToDo: add exception to wrong code values or create a class with an enumeration of allowed code values

    @Override
    /**
     * {@inheritDoc}
     */
    public void setLifeCycle(int code) {
        this.lifecycle = code;
    }

    @Override
    /**
     * Generates an MCMT declaration of the case variable.
     * @return String representing the MCMT declaration of case variable
     */
    public String getMCMTDeclaration() {
        if (this.oneCase)
            return ":global " + this.name + " " + this.sort.toString() + "\n";
        else
            return ":local " + this.name + " " + this.sort.toString() + "\n";
    }

    @Override
    public String toString() {
        return this.name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof DABCaseVariable))
            return false;
        DABCaseVariable obj = (DABCaseVariable) o;
        return name.equals(obj.name);  //we assume that all the variables have distinct names
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + sort.hashCode();
        result = 31 * result + lifecycle;
        return result;
    }
}