package it.unibz.deltabpmn.dataschema.core;


import it.unibz.deltabpmn.dataschema.elements.Constant;
import it.unibz.deltabpmn.dataschema.elements.Sort;

/**
 * An implementation of the {@class Constant} interface. The class allows to construct constants given their names and sorts.
 */
class DABConstant implements Constant {

    private String name;
    private Sort sort;

    public DABConstant(String name, Sort sort) {
        this.name = name;
        this.sort = sort;
    }

    @Override
    public String toString() {
        return this.name;
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = result * 37 + this.name.hashCode();
        result = result * 37 + this.sort.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof DABConstant))
            return false;
        DABConstant obj = (DABConstant) o;
        return this.name.equals(obj.name) && this.sort.equals(obj.sort);
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Sort getSort() {
        return this.sort;
    }

    @Override
    /**
     * Generates an MCMT declaration of the constant.
     *
     * @return The constant declaration in MCMT.
     */
    public String getMCMTDeclaration() {
        return ":smt (define " + this.name + " ::" + this.sort.getSortName() + ")\n";
    }
}
