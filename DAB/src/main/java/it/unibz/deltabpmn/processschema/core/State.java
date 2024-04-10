package it.unibz.deltabpmn.processschema.core;

import it.unibz.deltabpmn.dataschema.core.SystemSorts;
import it.unibz.deltabpmn.dataschema.elements.Constant;
import it.unibz.deltabpmn.dataschema.elements.Sort;

/**
 * Stores predefined states of process components such as tasks, events and blocks. Each state is treated as a {@link Constant}.
 */
public enum State implements Constant {


    ACTIVE {
        @Override
        public String getName() {
            return "Active";
        }
    },

    ACTIVE_SINGLE {
        @Override
        public String getName() {
            return "ActiveOnePath";
        }
    },

    ACTIVE_ALL {
        @Override
        public String getName() {
            return "ActiveAllPath";
        }
    },

    COMPLETED {
        @Override
        public String getName() {
            return "Completed";
        }
    },

    ENABLED {
        @Override
        public String getName() {
            return "Enabled";
        }
    },

    ERROR {
        @Override
        public String getName() {
            return "Error";
        }
    },

    IDLE {
        @Override
        public String getName() {
            return "Idle";
        }
    };

    @Override
    public Sort getSort() {
        return SystemSorts.STRING;
    }

    @Override
    public String getMCMTDeclaration() {
        return ":smt (define " + getName() + " ::" + getSort().getSortName() + ")\n";
    }

//    @Override
//    public String toString() {
//        return getName();
//    }
}
