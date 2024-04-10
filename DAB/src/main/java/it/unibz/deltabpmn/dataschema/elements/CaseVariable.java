package it.unibz.deltabpmn.dataschema.elements;

/**
 * A interface for representing sorted (or typed) case variables.
 * Each case variable is defined by its name, sort and type (one-case or multi-case).
 */
public interface CaseVariable extends Variable {

    boolean isOneCase();

    /**
     * @return An integer value indicating whether the case variable is a normal case variable (0),
     * a control case variable (1) or the control case variable of the root process (2)
     */
    int getLifeCycle();

    /**
     * A method for setting the case variable as a normal case variable (0),
     * a control case variable (1) or the control case variable of the root process (2).
     *
     * @param code A code value of the case variable to be set.
     */
    void setLifeCycle(int code);
}
