package it.unibz.deltabpmn.datalogic;

import it.unibz.deltabpmn.dataschema.elements.Term;

/**
 * An interface for representing binary conditions used in {@code WHERE} clauses of {@link ConjunctiveSelectQuery} objects.
 */
public interface BinaryCondition {

    OperatorType getOperator();

    Term getLeft();

    Term getRight();
}
