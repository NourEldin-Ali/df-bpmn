package it.unibz.deltabpmn.bpmn.utils.SQL;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.operators.relational.ComparisonOperator;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.statement.values.ValuesStatement;

import java.util.ArrayList;
import java.util.List;

public class WhereConditionFinder implements SelectVisitor {

    private List<BinaryExpression> conditions;

    public List<BinaryExpression> getWhereConditions(Select select) {
        conditions = new ArrayList<BinaryExpression>();
        select.getSelectBody().accept(this);
        return conditions;
    }

    @Override
    public void visit(PlainSelect plainSelect) {
        Expression expr = plainSelect.getWhere();
        expr.accept(new ExpressionVisitorAdapter() {
            @Override
            protected void visitBinaryExpression(net.sf.jsqlparser.expression.BinaryExpression expr) {
                if (expr instanceof ComparisonOperator)
                    conditions.add(new BinaryExpression(expr.getLeftExpression().toString(), expr.getStringExpression(), expr.getRightExpression().toString()));
                super.visitBinaryExpression(expr);
            }
        });
    }

    @Override
    public void visit(SetOperationList setOpList) {

    }

    @Override
    public void visit(WithItem withItem) {

    }

    @Override
    public void visit(ValuesStatement aThis) {

    }


    private class ExpressionFinder extends ExpressionVisitorAdapter {


    }
}
