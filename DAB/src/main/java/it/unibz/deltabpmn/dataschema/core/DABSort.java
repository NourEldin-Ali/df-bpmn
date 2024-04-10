package it.unibz.deltabpmn.dataschema.core;


import it.unibz.deltabpmn.dataschema.elements.Sort;

/**
 * A basic implementation of the {@class Sort} interface. The class allows to construct sorts given their names.
 */
class DABSort implements Sort {

    private String name;

    public DABSort(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof DABSort))
            return false;
        DABSort obj = (DABSort) o;
        return name.equals(obj.name);//we assume that all sorts have dinstict names
    }

    @Override
    public String getSortName() {
        return this.name;
    }

    @Override
    public String getMCMTDeclaration() {
        return ":smt (define-type " + name + ")\n";

    }
}
