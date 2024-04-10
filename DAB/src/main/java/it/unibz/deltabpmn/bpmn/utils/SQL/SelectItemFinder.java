package it.unibz.deltabpmn.bpmn.utils.SQL;

import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.statement.values.ValuesStatement;

import java.util.ArrayList;
import java.util.List;

//ToDo: implement it for SelectItemVisitor
public class SelectItemFinder implements SelectVisitor {

    private List<String> columns;

    public List<String> getSelectItems(Select select){
        columns = new ArrayList<String>();
        select.getSelectBody().accept(this);
        return columns;
    }

    @Override
    public void visit(PlainSelect plainSelect) {
        List<SelectItem> selectItems = plainSelect.getSelectItems();
        for(SelectItem item : selectItems)
            columns.add(item.toString());
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
}
