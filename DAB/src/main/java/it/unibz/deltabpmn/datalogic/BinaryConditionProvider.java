package it.unibz.deltabpmn.datalogic;

import it.unibz.deltabpmn.dataschema.elements.Term;

/**
 * A class that generates binary conditions used in {@code WHERE} clauses of {@link ConjunctiveSelectQuery} objects.
 */
public final class BinaryConditionProvider {

    private BinaryConditionProvider() {
    }

    public static BinaryCondition equality(Term left, Term right) {
        return new DABBinaryCondition(left, right, OperatorType.EQUALITY);
    }

    public static BinaryCondition inequality(Term left, Term right) {
        return new DABBinaryCondition(left, right, OperatorType.INEQUALITY);
    }

    public static BinaryCondition greaterThan(Term left, Term right) {
        return new DABBinaryCondition(left, right, OperatorType.GREATER_THAN);
    }

    public static BinaryCondition lessThan(Term left, Term right) {
        return new DABBinaryCondition(left, right, OperatorType.LESS_THAN);
    }
}
