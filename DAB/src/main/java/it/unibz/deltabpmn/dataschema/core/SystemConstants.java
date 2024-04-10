package it.unibz.deltabpmn.dataschema.core;

import it.unibz.deltabpmn.dataschema.elements.Constant;
import it.unibz.deltabpmn.dataschema.elements.Sort;

/**
 * Stores constants representing boolean values {@code TRUE} and {@code FALSE}.
 * Also stores a special constant {@code NULL} to denote an undefined value in any domain of any sort.
 */
public enum SystemConstants implements Constant {

    TRUE("True") {
        @Override
        public Sort getSort() {
            return SystemSorts.BOOL;
        }
    },

    FALSE("False") {
        @Override
        public Sort getSort() {
            return SystemSorts.BOOL;
        }
    },

    NULL("NULL") {
        @Override
        public Sort getSort() {
            return SystemSorts.STRING;
        }
    };

    private final String name;


    private SystemConstants(String name) {
        this.name = name;
    }


    @Override

    public String getName() {
        return this.name;
    }


    @Override
    public String getMCMTDeclaration() {
        return ":smt (define " + getName() + " ::" + getSort().getSortName() + ")\n";
    }
}
