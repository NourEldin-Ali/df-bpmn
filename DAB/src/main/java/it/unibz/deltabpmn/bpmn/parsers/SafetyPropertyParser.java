package it.unibz.deltabpmn.bpmn.parsers;

import it.unibz.deltabpmn.bpmn.utils.SQL.SelectParser;
import it.unibz.deltabpmn.datalogic.ConjunctiveSelectQuery;
import it.unibz.deltabpmn.dataschema.core.DataSchema;
import org.openbpmn.bpmn.BPMNModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SafetyPropertyParser {

	private static final String verifyKey = "verify";


	public static List<ConjunctiveSelectQuery> parse(BPMNModel bpmnModel, DataSchema dataSchema) throws Exception {

		List<ConjunctiveSelectQuery> toVerify = new ArrayList<>();
		
		String value = bpmnModel.openDefaultProces().getAttribute(verifyKey);
		if(value.isEmpty())
          throw new Exception("The process model contains no properties to verify!");
		
		toVerify.add(parseProperty(value, dataSchema));
		System.out.println("----------------------------");
		System.out.println(value);

		return toVerify;
	}

	private static ConjunctiveSelectQuery parseProperty(String property, DataSchema dataSchema) throws Exception {
		ConjunctiveSelectQuery query = null;
		if (property.contains("SELECT"))
			// deal with a query that contains a SELECT part
			query = SelectParser.parse(property, dataSchema);
		else {
			// deal with a query that doesn't have a SELECT part
			query = new ConjunctiveSelectQuery(dataSchema);
			for (String expr : property.split("AND"))
				query.addBinaryCondition(BinaryExpressionParser.parse(expr, dataSchema));
		}
		return query;
	}
}
