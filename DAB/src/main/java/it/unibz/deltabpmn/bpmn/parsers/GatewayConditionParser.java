package it.unibz.deltabpmn.bpmn.parsers;

import it.unibz.deltabpmn.bpmn.utils.SQL.SelectParser;
import it.unibz.deltabpmn.datalogic.ConjunctiveSelectQuery;
import it.unibz.deltabpmn.dataschema.core.DataSchema;
import org.camunda.bpm.model.bpmn.instance.ExtensionElements;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperties;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperty;

import java.util.Collection;

public class GatewayConditionParser {

    private static final String condKey = "cond";

    //ToDo:how do we know which condition appears first? there are not lables or anything like that; we first get an element that was first added to the diagram (and this could be any flow!)
    //this parsing should be done for certain types of blocks with gates;
    // gates get all the upper block first-> as soon as the upper block is created, get a link between it and the gate, and for that link acquire the condition
    //get all condition expressions from flow elements


    //    public static ConjunctiveSelectQuery parseXORCondition(ExtensionElements extensionElements, DataSchema dataSchema) {
    public static ConjunctiveSelectQuery parseGatewayCondition(String condition, DataSchema dataSchema) throws Exception {
//        Collection<CamundaProperty> properties = extensionElements.getElementsQuery()
//                .filterByType(CamundaProperties.class)
//                .singleResult()
//                .getCamundaProperties();
//        String value = null;
//        for (CamundaProperty property : properties) {
//            String name = property.getCamundaName();
//            if (name.equals(condKey))
//                value = property.getCamundaValue();
//        }
        return parseCondition(condition, dataSchema);
    }

    private static ConjunctiveSelectQuery parseCondition(String precondition, DataSchema dataSchema) throws Exception {
        ConjunctiveSelectQuery query;
        if (precondition.contains("SELECT"))
            //deal with a query that contains a SELECT part
            query = SelectParser.parse(precondition, dataSchema);
        else if(precondition.equals("TRUE"))
        {
            //deal with a case in which
            //ToDo: add more meaningful treatment of queries that consist only of TRUE
            return new ConjunctiveSelectQuery(dataSchema);
        }
        else {
            //deal with a query that doesn't have a SELECT part
            query = new ConjunctiveSelectQuery(dataSchema);
            for (String expr : precondition.split("AND"))
                query.addBinaryCondition(BinaryExpressionParser.parse(expr, dataSchema));
        }
        return query;
    }
}
