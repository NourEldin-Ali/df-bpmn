package it.unibz.deltabpmn.datalogic;

import it.unibz.deltabpmn.dataschema.elements.Term;


class DABBinaryCondition implements BinaryCondition {

    private final Term leftAttr;
    private final Term rightAttr;
    private final OperatorType op;

    public DABBinaryCondition(Term left, Term right, OperatorType op) {
        this.leftAttr = left;
        this.rightAttr = right;
        this.op = op;
    }

    @Override
    public OperatorType getOperator() {
        return this.op;
    }

    @Override
    public Term getLeft() {
        return this.leftAttr;
    }

    @Override
    public Term getRight() {
        return this.rightAttr;
    }
}
