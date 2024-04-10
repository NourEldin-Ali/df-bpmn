package it.unibz.deltabpmn.bpmn.utils.SQL;

import it.unibz.deltabpmn.bpmn.parsers.BinaryExpressionParser;
import it.unibz.deltabpmn.datalogic.ConjunctiveSelectQuery;
import it.unibz.deltabpmn.dataschema.core.DataSchema;
import it.unibz.deltabpmn.dataschema.elements.Attribute;
import it.unibz.deltabpmn.exception.EevarOverflowException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//ToDo: add correct management of the .find() method!
public class SelectParser {
    public static ConjunctiveSelectQuery parse(String query, DataSchema dataSchema) throws Exception {
        Pattern argumentsPattern = Pattern.compile("SELECT(.*)FROM", Pattern.DOTALL);
        Matcher argMatcher = argumentsPattern.matcher(query);
        argMatcher.find();
        //extract attributes from the SELECT clause
        List<Attribute> attributes = new ArrayList<>();
        Arrays.stream(argMatcher.group(1).split(","))
                .forEach(arg -> attributes.add(dataSchema.getAllAttributes().get(arg.trim())));
        ConjunctiveSelectQuery outputQuery = null;
        try {
            outputQuery = new ConjunctiveSelectQuery(dataSchema,attributes.toArray(new Attribute[attributes.size()]));
        } catch (EevarOverflowException e) {
            e.printStackTrace();
        }
        if (query.contains("WHERE")) {
            //extract relations from the FROM clause
            //ToDo: extracting relations is not required if attributes have been previously properly extracted; find a way to control whether relations in the FROM clause correspond to the actual attributes!
            /*Pattern fromPattern = Pattern.compile("FROM(.*)WHERE", Pattern.DOTALL);
            Matcher fromMatcher = fromPattern.matcher(query);
            fromMatcher.find();
            String fromRelations = fromMatcher.group(1);
            for (String rel : fromRelations.split(",")) {
                System.out.println("relation: " + rel.trim());
            }*/
            //extract binary expressions from the WHERE clause
            Pattern wherePattern = Pattern.compile("WHERE(.*)", Pattern.DOTALL);
            Matcher whereMatcher = wherePattern.matcher(query);
            whereMatcher.find();
            List<String> expressions = new ArrayList<>();
            Arrays.stream(whereMatcher.group(1).split("AND")).forEach(expr -> expressions.add(expr));
            for (String expr : expressions)
                outputQuery.addBinaryCondition(BinaryExpressionParser.parse(expr, dataSchema));
        } else {
            //extract relations from the FROM clause
            //ToDo: extracting relations is not required if attributes have been previously properly extracted; find a way to control whether relations in the FROM clause correspond to the actual attributes!
            /*Pattern fromPattern = Pattern.compile("FROM(.*)", Pattern.DOTALL);
            Matcher fromMatcher = fromPattern.matcher(query);
            fromMatcher.find();
            String fromRelations = fromMatcher.group(1);
            for (String rel : fromRelations.split(",")) {
                System.out.println("relation: " + rel.trim());*/
        }
        return outputQuery;
    }
}
