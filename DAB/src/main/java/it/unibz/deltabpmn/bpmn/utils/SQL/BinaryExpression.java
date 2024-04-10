package it.unibz.deltabpmn.bpmn.utils.SQL;

/**
 * A class for storing a string representation of a binary expression
 */
public class BinaryExpression {

    private String left; //left element of an expression
    private String right; //right element of an expression
    private String op; //an expression operator

    public BinaryExpression(String left, String op, String right) {
        this.left = left;
        this.right = right;
        this.op = op;
    }

    public String getLeft() {
        return this.left;
    }

    public String getRight() {
        return right;
    }

    public String getOp() {
        return op;
    }

    @Override
    public String toString() {
        return left + op + right;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((left == null) ? 0 : left.hashCode());
        result = prime * result + ((op == null) ? 0 : op.hashCode());
        result = prime * result + ((right == null) ? 0 : right.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || this.getClass() != obj.getClass()) {
            return false;
        }
        BinaryExpression other = (BinaryExpression) obj;
        String otherStr = other.toString();
        return otherStr.equals(left + op + right) || otherStr.equals(right + op + left);
    }


}
