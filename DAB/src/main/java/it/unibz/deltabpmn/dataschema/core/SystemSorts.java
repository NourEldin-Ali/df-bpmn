package it.unibz.deltabpmn.dataschema.core;

//ToDo: throw exceptions when someone is trying to redefine these sorts

import it.unibz.deltabpmn.dataschema.elements.Sort;

/**
 * A class with sorts initially used within the system.
 */
public final class SystemSorts {
    public static final Sort STRING = new DABSort("StringSort");
    public static final Sort BOOL = new DABSort("Bool");
    public static final Sort INT = new DABSort("int");

    private SystemSorts() {
    }
}
