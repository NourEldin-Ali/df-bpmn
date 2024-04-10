package it.unibz.deltabpmn.dataschema.core;

import it.unibz.deltabpmn.dataschema.elements.CaseVariable;

public final class SystemVariables {

    public static final CaseVariable EMPTY = new DABCaseVariable("lifecycleEmpty", SystemSorts.STRING, true);

    private SystemVariables() {
    }

}
