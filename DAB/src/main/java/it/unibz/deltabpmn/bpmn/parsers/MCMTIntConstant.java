package it.unibz.deltabpmn.bpmn.parsers;

import it.unibz.deltabpmn.dataschema.core.SystemSorts;
import it.unibz.deltabpmn.dataschema.elements.Sort;
import it.unibz.deltabpmn.dataschema.elements.Constant;

class MCMTIntConstant implements Constant {
    private int value;

    public MCMTIntConstant(int value) {
        this.value = value;
    }

    @Override
    public String getName() {
        return String.valueOf(this.value);
    }

    @Override
    public Sort getSort() {
        return SystemSorts.INT;
    }

    @Override
    public String getMCMTDeclaration() {
        return String.valueOf(this.value);
    }
}
