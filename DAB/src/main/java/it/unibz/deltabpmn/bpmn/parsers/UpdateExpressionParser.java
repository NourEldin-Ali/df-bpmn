package it.unibz.deltabpmn.bpmn.parsers;

import it.unibz.deltabpmn.bpmn.utils.SQL.SelectParser;
import it.unibz.deltabpmn.datalogic.*;
import it.unibz.deltabpmn.dataschema.core.DataSchema;
import it.unibz.deltabpmn.dataschema.elements.*;
import it.unibz.deltabpmn.exception.DuplicateDeclarationException;
import it.unibz.deltabpmn.exception.EevarOverflowException;
import it.unibz.deltabpmn.exception.InvalidInputException;
import it.unibz.deltabpmn.exception.UnmatchingSortException;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.openbpmn.bpmn.BPMNTypes;
import org.openbpmn.bpmn.elements.Activity;
import org.openbpmn.bpmn.elements.DataFlowExtension;
import org.openbpmn.bpmn.elements.DataInputObjectExtension;
import org.openbpmn.bpmn.elements.DataObjectAttributeExtension;
import org.openbpmn.bpmn.elements.DataOutputObjectExtension;
import org.openbpmn.bpmn.elements.core.BPMNElementNode;
import org.w3c.dom.Element;

import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class UpdateExpressionParser {

	private static final String effKey = "eff";
	private static final String preKey = "pre";
	private static final String varKey = "var";

	public static ComplexTransition parse(String taskName, Set<Element> conditionalEventDefinition,
			DataSchema dataSchema) throws Exception {
		String precondition = "";
		List<String> effects = new ArrayList<>();
		List<String> newVariableDeclarations = new ArrayList<>();// list of variable declaration appearing in var
																	// clauses
		// 1. extract all the elements of the update expression

		for (Element definition : conditionalEventDefinition) {
			String name = definition.getAttribute("language");
			String value = definition.getAttribute("expression");
			if (name.equals(preKey))
				precondition += value;
			if (name.equals(effKey))
				effects.add(value);
			if (name.equals(varKey))
				newVariableDeclarations.add(value);
		}

		// 2. start by adding variable declarations (if any exist)
		for (String varDeclaration : newVariableDeclarations)
			dataSchema = parseVariableDeclarations(varDeclaration, dataSchema);
		// 3. parse the precondition
		ConjunctiveSelectQuery query = null;
		if (!precondition.trim().equals("TRUE")) {
			if (precondition.trim().isEmpty())
				System.out.println("empty precondition from task/event " + taskName);
//                query = new ConjunctiveSelectQuery();//
			else {
				query = parsePrecondition(precondition, dataSchema);
				System.out.println(precondition);
			}

		}
		// 4. parse one of the effect types and generate the final transition object
		// (NB: if we have only variables to set, then this is going to be an insert
		// transition; otherwise, variable updates have to be added to a created
		// transition)
		// ToDo always check if query is null
		ComplexTransition transition = null;
		String mainStatement = effects.stream().filter(str -> str.contains("INSERT")).findAny().orElse("")
				+ effects.stream().filter(str -> str.contains("DELETE")).findAny().orElse("")
				+ effects.stream().filter(str -> str.contains("UPDATE")).findAny().orElse("");
		// if (effects.stream().anyMatch(str -> str.contains("INSERT")))
		if (mainStatement.contains("INSERT")) {
			if (query == null)
				transition = new InsertTransition(taskName + "INSERT", dataSchema);
			else
				transition = new InsertTransition(taskName + "INSERT", query, dataSchema);
			Pair<Term[], String> insertComponents = parseInsertExpression(mainStatement, dataSchema);
			((InsertTransition) transition).insert(
					dataSchema.getRepositoryRelationAssociations().get(insertComponents.getValue()),
					insertComponents.getKey());
			System.out.println(mainStatement);
		}
		// if (effects.stream().anyMatch(str -> str.contains("DELETE")))
		if (mainStatement.contains("DELETE")) {
			if (query == null)
				transition = new DeleteTransition(taskName + "DELETE", dataSchema);
			else
				transition = new DeleteTransition(taskName + "DELETE", query, dataSchema);
			Pair<Term[], String> deleteComponents = parseDeleteExpression(mainStatement, dataSchema);
			((DeleteTransition) transition).delete(
					dataSchema.getRepositoryRelationAssociations().get(deleteComponents.getValue()),
					deleteComponents.getKey());
			System.out.println(mainStatement);
		}
		// if (effects.stream().anyMatch(str -> str.contains("UPDATE")))
		if (mainStatement.contains("UPDATE")) {
			// manage the update statement
			transition = parseUpdateExpression(query, taskName + "UPDATE", mainStatement, dataSchema);
			System.out.println(mainStatement);
		}

		// remove from effects the processes string and generate
		effects.remove(mainStatement);

		// deal with process variable updates (if any)
		if (transition == null) {// if transition hasn't been created, create a simple insert transition (the
									// type of transition doesn't matter as we need to update variable values only)
			if (query == null)
				transition = new InsertTransition(taskName + "SET", dataSchema);
			else
				transition = new InsertTransition(taskName + "SET", query, dataSchema);
		}
		for (String update : effects) {
			transition = parseVariableUpdate(update, dataSchema, transition);
		}
		return transition;
	}

	public static ComplexTransition parse(String taskName, Activity activity, DataSchema dataSchema) throws Exception {
		String precondition = "";
		List<String> effects = new ArrayList<>();

		// 1. extract all var elements => user data elements (if any exist)
		List<DataInputObjectExtension> varData = activity.getDataInputObjects().stream()
				.filter((data) -> data.getElementNode().getLocalName()
						.contentEquals(BPMNTypes.DATA_INPUT_OBJECT_ENVIRONMENT_DATA_USER)
						|| data.getElementNode().getLocalName().contentEquals(BPMNTypes.DATA_INPUT_OBJECT_DEPENDENCY))
				.collect(Collectors.toList());// list user data = list of variable declaration appearing in var clauses

		varData.forEach((data) -> {
			if (data.getDataAttributesList().size() == 0) {
				try {
					System.out.println("new variable name: " + data.getName().trim());
					System.out.println("new variable sort: " + data.getAttribute("type").trim());
					System.out.println();
					Sort varSort = dataSchema.newSort(data.getAttribute("type").trim());
					dataSchema.newCaseVariable(data.getName().trim(), varSort, true);
					dataSchema.addEevar(data.getName().trim(), varSort);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					System.exit(0);
				}
			} else {
				data.getDataAttributesList().forEach((name, type) -> {
					try {
						System.out.println("new variable name: " + name.trim());
						System.out.println("new variable sort: " + type.trim());
						System.out.println();
						Sort varSort = dataSchema.newSort(type.trim());
						dataSchema.newCaseVariable(name.trim(), varSort, true);
						dataSchema.addEevar(name.trim(), varSort);
					} catch (EevarOverflowException | DuplicateDeclarationException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						System.exit(0);
					}
				});
			}
		});
		// 2. parse the precondition => SELECT data from database
		precondition = parserPrecondition(activity);

		// 3. parse the precondition
		ConjunctiveSelectQuery query = null;
		if (!precondition.trim().equals("TRUE")) {
			if (precondition.trim().isEmpty())
				System.out.println("empty precondition from task/event " + taskName);
			else {
				System.out.print("precondition: ");
				System.out.println(precondition.trim());
				System.out.println();
				query = parsePrecondition(precondition.trim(), dataSchema);
			}
		}
		// 4. parse one of the effect types and generate the final transition object
		// (NB: if we have only variables to set, then this is going to be an insert
		// transition; otherwise, variable updates have to be added to a created
		// transition)
		// ToDo always check if query is null

		// convert data to effects
		// TODO: create new code without use effects
		effects.addAll(EffectExtract(activity));

		ComplexTransition transition = null;
		String mainStatement = effects.stream().filter(str -> str.contains("INSERT")).findAny().orElse("")
				+ effects.stream().filter(str -> str.contains("DELETE")).findAny().orElse("")
				+ effects.stream().filter(str -> str.contains("UPDATE")).findAny().orElse("");

		// if (effects.stream().anyMatch(str -> str.contains("INSERT")))
		if (mainStatement.contains("INSERT")) {
			System.out.print("Mainstatement: ");
			System.out.println(mainStatement);
			System.out.println();
			if (query == null)
				transition = new InsertTransition(taskName + "INSERT", dataSchema);
			else
				transition = new InsertTransition(taskName + "INSERT", query, dataSchema);
			Pair<Term[], String> insertComponents = parseInsertExpression(mainStatement, dataSchema);
			((InsertTransition) transition).insert(
					dataSchema.getRepositoryRelationAssociations().get(insertComponents.getValue()),
					insertComponents.getKey());

		}
		// if (effects.stream().anyMatch(str -> str.contains("DELETE")))
		if (mainStatement.contains("DELETE")) {
			System.out.print("Mainstatement: ");
			System.out.println(mainStatement);
			System.out.println();
			if (query == null)
				transition = new DeleteTransition(taskName + "DELETE", dataSchema);
			else
				transition = new DeleteTransition(taskName + "DELETE", query, dataSchema);
			Pair<Term[], String> deleteComponents = parseDeleteExpression(mainStatement, dataSchema);
			((DeleteTransition) transition).delete(
					dataSchema.getRepositoryRelationAssociations().get(deleteComponents.getValue()),
					deleteComponents.getKey());

		}
		// if (effects.stream().anyMatch(str -> str.contains("UPDATE")))
		if (mainStatement.contains("UPDATE")) {
			System.out.print("Mainstatement: ");
			System.out.println(mainStatement);
			System.out.println();
			// manage the update statement
			transition = parseUpdateExpression(query, taskName + "UPDATE", mainStatement, dataSchema);

		}

		// remove from effects the processes string and generate
		effects.remove(mainStatement);

		// deal with process variable updates (if any)
		if (transition == null) {// if transition hasn't been created, create a simple insert transition (the
									// type of transition doesn't matter as we need to update variable values only)
			if (query == null)
				transition = new InsertTransition(taskName + "SET", dataSchema);
			else
				transition = new InsertTransition(taskName + "SET", query, dataSchema);
		}
		for (String update : effects) {
			transition = parseVariableUpdate(update, dataSchema, transition);
		}
		return transition;
	}

	private static ComplexTransition parseVariableUpdate(String effect, DataSchema dataSchema,
			ComplexTransition transition) throws Exception {
		System.out.print("effect: ");
		System.out.println(effect);
		System.out.println();
		String[] operands = effect.split("=");
		CaseVariable first = (CaseVariable) TermProcessor.processTerm(operands[0].trim(), dataSchema);// first element
																										// is always #
																										// for case
																										// variables
		Term second = TermProcessor.processTerm(operands[1].trim(), dataSchema);
		if (second instanceof Constant)
			transition.setControlCaseVariableValue(first, (Constant) second);
		else if (second instanceof Attribute)
			transition.setControlCaseVariableValue(first, (Attribute) second);
		else if (second instanceof CaseVariable)
			transition.setControlCaseVariableValue(first, (CaseVariable) second);
		else if (second == null) {
			// else, it's a constant that we don't know about ==> add it to dataSchema
			Constant c = dataSchema.newConstant(operands[1].trim(), first.getSort());
			transition.setControlCaseVariableValue(first, c);
		}
		return transition;
	}

	// returns a pair consisting of values to be deleted and the name of the
	// relation appearing in the FROM clause
	private static Pair<Term[], String> parseDeleteExpression(String deleteExpr, DataSchema dataSchema)
			throws Exception {
		Pattern argumentsPattern = Pattern.compile("DELETE(.*)FROM", Pattern.DOTALL);
		Matcher argMatcher = argumentsPattern.matcher(deleteExpr);
		Term[] toDelete = null;
		if (argMatcher.find()) {
			// extract attributes from the SELECT clause
			String[] values = argMatcher.group(1).split(",");
			toDelete = new Term[values.length];
			for (int i = 0; i < values.length; i++)
				toDelete[i] = TermProcessor.processTerm(values[i].trim(), dataSchema);
		} else
			throw new Exception("Empty DELETE clause!");
		String relationName;
		Pattern fromPattern = Pattern.compile("FROM(.*)", Pattern.DOTALL);
		Matcher fromMatcher = fromPattern.matcher(deleteExpr);
		if (fromMatcher.find()) {
			relationName = fromMatcher.group(1).trim();
		} else
			throw new Exception("Empty FROM clause in the DELETE statement!");
		return new ImmutablePair<>(toDelete, relationName);
	}

	// returns a pair consisting of values to be inserted and the name of the
	// relation appearing in the INTO clause
	private static Pair<Term[], String> parseInsertExpression(String insertExpr, DataSchema dataSchema)
			throws Exception {
		Pattern argumentsPattern = Pattern.compile("INSERT(.*)INTO", Pattern.DOTALL);
		Matcher argMatcher = argumentsPattern.matcher(insertExpr);
		Term[] toInsert = null;
		if (argMatcher.find()) {
			// extract attributes from the SELECT clause
			String[] values = argMatcher.group(1).split(",");
			toInsert = new Term[values.length];
			for (int i = 0; i < values.length; i++)
				toInsert[i] = TermProcessor.processTerm(values[i].trim(), dataSchema);
		} else
			throw new Exception("Empty INSERT clause!");
		String relationName;
		Pattern intoPattern = Pattern.compile("INTO(.*)", Pattern.DOTALL);
		Matcher intoMatcher = intoPattern.matcher(insertExpr);
		if (intoMatcher.find()) {
			relationName = intoMatcher.group(1).trim();
		} else
			throw new Exception("Empty FROM clause in the DELETE statement!");
		return new ImmutablePair<>(toInsert, relationName);
	}

	// returns a pair consisting of values to be inserted and the name of the
	// relation appearing in the INTO clause
	private static BulkUpdate parseUpdateExpression(ConjunctiveSelectQuery query, String taskName, String updateExpr,
			DataSchema dataSchema) throws Exception {
		// extract the name of the relation to update
		Pattern relationPattern = Pattern.compile("UPDATE(.*)SET", Pattern.DOTALL);
		Matcher relMatcher = relationPattern.matcher(updateExpr);
		String relation = null;
		if (relMatcher.find()) {
			String[] values = relMatcher.group(1).split(",");
			relation = values[0].trim();
		} else
			throw new Exception("Empty UPDATE clause!");
		// create the update object
		BulkUpdate bulkUpdate = null;
		if (query == null)
			bulkUpdate = new BulkUpdate(taskName, dataSchema.getRepositoryRelationAssociations().get(relation),
					dataSchema);
		else
			bulkUpdate = new BulkUpdate(taskName, query, dataSchema.getRepositoryRelationAssociations().get(relation),
					dataSchema);

		// ToDo: check if extracting only attributes without relations will be helpful
		// extract elements that have to be updated
		// List<String> referenceVariables = new ArrayList<>();
		Map<String, Attribute> refVaribaleAttributeAssociations = new HashMap<>();
		Pattern updVarsPattern = Pattern.compile("SET(.*)WHERE", Pattern.DOTALL);
		Matcher updVarsMatcher = updVarsPattern.matcher(updateExpr);
		if (updVarsMatcher.find()) {
			// extract attributes from the SET clause
			String[] values = updVarsMatcher.group(1).split(",");
			Arrays.stream(values).forEach(c -> refVaribaleAttributeAssociations.put(c.substring(c.indexOf("@")).trim(),
					dataSchema.getAllAttributes().get(c.substring(c.indexOf(".") + 1, c.indexOf("=")).trim())));
		} else
			throw new Exception("Empty SET clause!");

		// process WHEN-THEN clauses
		Pattern casePattern = Pattern.compile("(WHEN.*?THEN.*?(?=(WHEN|ELSE)))|(?<=ELSE).*", Pattern.DOTALL);
		Matcher caseMatcher = casePattern.matcher(updateExpr);
		// create a list that stores partially processed conditional update statements
		// from WHEN-THEN clauses

		BulkCondition lastFalseNode = null;
		boolean init = true;
		while (caseMatcher.find()) {// ToDo: check this part!
			String clause = caseMatcher.group(0).replaceFirst("WHEN ", "").trim();
			String[] values = clause.split("THEN");
			if (init) {
				bulkUpdate.root = BulkConditionParser.parseAndUpdate(bulkUpdate.root, values[0].trim(), dataSchema);
				BulkCondition trueNode = bulkUpdate.root.addTrueChild();
				// get all @v=u expressions from the THEN part of the clause
				parseUpdateList(trueNode, values[1], refVaribaleAttributeAssociations);
				lastFalseNode = bulkUpdate.root.addFalseChild();// create a false child object that can be then
																// iteratively populated with further conditions
				init = false;
			} else {
				if (clause.contains("THEN")) {
					lastFalseNode = BulkConditionParser.parseAndUpdate(lastFalseNode, values[0].trim(), dataSchema);
					BulkCondition trueNode = lastFalseNode.addTrueChild();
					// get all @v=u expressions from the THEN part of the clause
					parseUpdateList(trueNode, values[1], refVaribaleAttributeAssociations);
					lastFalseNode = lastFalseNode.getFalseNode();
				} else
					// manage the ELSE case
					parseUpdateList(lastFalseNode, values[0], refVaribaleAttributeAssociations);
			}
		}
		return bulkUpdate;
	}

	private static void parseUpdateList(BulkCondition node, String expression,
			Map<String, Attribute> refVaribaleAttributeAssociations)
			throws InvalidInputException, UnmatchingSortException {
		String[] attributeUpdates = expression.split(",");
		for (String upd : attributeUpdates) {
			String[] operands = upd.split("=");
			node.updateAttributeValue(refVaribaleAttributeAssociations.get(operands[0].trim()), operands[1].trim());
		}
		// return node;
	}

	private static ConjunctiveSelectQuery parsePrecondition(String precondition, DataSchema dataSchema)
			throws Exception {
		ConjunctiveSelectQuery query = null;
		if (precondition.contains("SELECT")) {
			// deal with a query that contains a SELECT part
			query = SelectParser.parse(precondition, dataSchema);
		} else {
			// deal with a query that doesn't have a SELECT part
			query = new ConjunctiveSelectQuery(dataSchema);
			for (String expr : precondition.split("AND"))
				query.addBinaryCondition(BinaryExpressionParser.parse(expr, dataSchema));
		}
		return query;
	}

	private static List<String> EffectExtract(Activity activity) throws Exception {
		List<String> effects = new ArrayList<>();
		// data store statement
		// check if delete statement
		if (activity.getAllDeleteObjectData().size() > 0) {
			DataOutputObjectExtension dataToDelete = activity.getAllDeleteObjectData().get(0);
			String column = "";
			// get all data attributes
			Map<String, Object> dataStoreVariables = activity.getBpmnProcess().getDataStoresExtensions()
					.get(dataToDelete.getName());
//			get object need to delete attribute (table column)
//			List<String> attributeSort = new ArrayList<String>();
//			System.out.println(dataStoreVariables.get("attributes").toString());
			for (DataObjectAttributeExtension dataAtt : (Set<DataObjectAttributeExtension>) dataStoreVariables
					.get("attributes")) {
				column += dataAtt.getName() + ",";
			}
			column = column.substring(0, column.lastIndexOf(","));
//			System.out.println(column);
			effects.add("DELETE " + column + " FROM " + dataToDelete.getAttribute("type"));

		} else

		// check if insert statement
		if ((activity.getDataOutputObjects().stream().filter((data) -> {
			return data.getAttribute("state").contentEquals("init")
					&& data.getElementNode().getLocalName().contentEquals(BPMNTypes.DATA_OUTPUT_OBJECT_DATA_STORE);
		}).count() > 0)) {
			DataOutputObjectExtension dataToInsert = activity.getDataOutputObjects().stream().filter((data) -> {
				return data.getAttribute("state").contentEquals("init")
						&& data.getElementNode().getLocalName().contentEquals(BPMNTypes.DATA_OUTPUT_OBJECT_DATA_STORE);
			}).collect(Collectors.toList()).iterator().next();
			// get all data attributes
			Map<String, Object> dataStoreVariables = activity.getBpmnProcess().getDataStoresExtensions()
					.get(dataToInsert.getName());

			// order of attributes
			List<String> allAttributes = new ArrayList<String>();
			((Set<DataObjectAttributeExtension>) dataStoreVariables.get("attributes")).forEach((att) -> {
				allAttributes.add(att.getName());
			});

			String query = "";
			// to make sure that I have order attributes
			List<DataObjectAttributeExtension> currentAttributes = dataToInsert.getDataAttributes().stream()
					.collect(Collectors.toList());
			int i = 0;
			for (String selectedAttribute : allAttributes) {
				if (currentAttributes.stream().filter((a) -> a.getName().contentEquals(selectedAttribute)).findAny()
						.orElse(null) != null // check if the attribute in the current attribute list
						&& currentAttributes.get(i).getName().contentEquals(selectedAttribute) // check if the index is
																								// ok of the order of
																								// the attributes
				) {
					DataObjectAttributeExtension dataAtt = currentAttributes.get(i);

					String sourceRef = activity.getDataFlows().stream()
							.filter((dataflow) -> dataflow.getTargetRef().contentEquals(dataAtt.getId())).findFirst()
							.orElseThrow().getSourceRef();
					BPMNElementNode sourceElement = (BPMNElementNode) activity.findElementById(sourceRef);
					// check type of source object
					if (sourceElement instanceof DataInputObjectExtension) {
						if (sourceElement.getElementNode().getLocalName().equals(BPMNTypes.DATA_INPUT_OBJECT_PROCESS)) {
							query += "#" + sourceElement.getName() + "";
						} else if (sourceElement.getElementNode().getLocalName()
								.equals(BPMNTypes.DATA_INPUT_OBJECT_LOCAL)) {
							query += "" + sourceElement.getAttribute("value") + "";
						} else {
							query += sourceElement.getName();
						}
					} else if (sourceElement instanceof DataObjectAttributeExtension) {
						if (sourceElement.getElementNode().getParentNode().getLocalName()
								.equals(BPMNTypes.DATA_INPUT_OBJECT_PROCESS)) {
							query += "#" + sourceElement.getName() + "";
						} else if (sourceElement.getElementNode().getParentNode().getLocalName()
								.equals(BPMNTypes.DATA_INPUT_OBJECT_LOCAL)) {
							query += sourceElement.getAttribute("value");
						} else {
							query += sourceElement.getName();
						}
					} else {
						query += sourceElement.getName();
					}
					query += ", ";
					i++;
				} else {
					query += "NULL, ";
				}
			}

			query = "INSERT " + query.substring(0, query.lastIndexOf(", ")) + " INTO "
					+ dataToInsert.getAttribute("type");
			effects.add(query);

		} else

		// check if update statement
		if ((activity.getDataOutputObjects().stream().filter((data) -> {
			return data.getAttribute("state").contentEquals("update")
					&& data.getElementNode().getLocalName().contentEquals(BPMNTypes.DATA_OUTPUT_OBJECT_DATA_STORE);
		}).count() > 0)) {
			DataOutputObjectExtension dataToUpdate = activity.getDataOutputObjects().stream().filter((data) -> {
				return data.getAttribute("state").contentEquals("update")
						&& data.getElementNode().getLocalName().contentEquals(BPMNTypes.DATA_OUTPUT_OBJECT_DATA_STORE);
			}).findFirst().orElse(null);
			if (dataToUpdate != null) {
				String query = "";
				if (dataToUpdate.getDataAttributes().size() > 0) {
					int i = 1;
					for (DataObjectAttributeExtension dataAtt : dataToUpdate.getDataAttributes().stream()
							// check incaming dataflow == attributes
							.filter((att) -> activity.getDataFlows().stream()
									.filter((dataflow) -> dataflow.getTargetRef().contentEquals(att.getId()))
									.findFirst().orElse(null) != null)
							.collect(Collectors.toList())) {
						// it should be a data processing operator
						BPMNElementNode dataProcessing = (BPMNElementNode) activity
								.findElementById(activity.getDataFlows().stream()
										.filter((dataflow) -> dataflow.getTargetRef().contentEquals(dataAtt.getId()))
										.findFirst().orElseThrow().getSourceRef());
						if (!dataProcessing.getElementNode().getLocalName().equals(BPMNTypes.DATA_PROCESSING)) {
							throw new Exception("IN UPDATE WE SHOULD HAVE CONDITIONS");
						}

						String gherkinInput = dataProcessing.getAttribute("gherkin");
						if (gherkinInput == "") {
							throw new Exception("GHERKIN IS NOT DEFINED");
						}
						gherkinInput = gherkinInput.replace("\n", " ").replace("\r", "");
						String[] givenMatcher = gherkinInput.split("(?i)given");
						if (givenMatcher.length != 3) {
							throw new Exception("INVALIDE GHERKIN");
						}

						String[] thenMatcher = givenMatcher[2].split("(?i)then");
						String defaultResult = thenMatcher[thenMatcher.length - 1].trim();

						// Extract conditions and results from cases part
						String[] caseLines = givenMatcher[1].split("(?i)when");
						List<String> caseStatements = new ArrayList<>();
						for (int j = 1; j < caseLines.length; j++) {
							String line = caseLines[j];
							if (!line.trim().isEmpty()) {
								String condition = line.split("(?i)then")[0].trim();
								String result = line.split("(?i)then")[1].trim();
								caseStatements.add("WHEN " + condition + " THEN @v" + i + " = " + result);
							}
						}

						// Extract condition and result from default part

						String defaultStatement = " ELSE @v" + i + " = " + defaultResult;

						// Join the case statements
						String casesStatement = String.join(" ", caseStatements);

						// Concatenate the default statement
						String finalStatement = "CASE " + casesStatement + defaultStatement;

						query += dataToUpdate.getAttribute("type") + "." + dataAtt.getName() + " = @v" + i + " WHERE "
								+ finalStatement;

						if (dataProcessing instanceof DataInputObjectExtension) {
							if (dataProcessing.getElementNode().getLocalName()
									.equals(BPMNTypes.DATA_INPUT_OBJECT_PROCESS)) {
								query += "#" + dataProcessing.getName() + "";
							} else if (dataProcessing.getElementNode().getLocalName()
									.equals(BPMNTypes.DATA_INPUT_OBJECT_LOCAL)) {
								query += "\"" + dataProcessing.getAttribute("value") + "\"";
							} else {
								query += "" + dataProcessing.getName() + "";
							}
						} else if (dataProcessing instanceof DataObjectAttributeExtension) {
							if (dataProcessing.getElementNode().getParentNode().getLocalName()
									.equals(BPMNTypes.DATA_INPUT_OBJECT_PROCESS)) {
								query += "#" + dataProcessing.getName() + "";
							} else if (dataProcessing.getElementNode().getParentNode().getLocalName()
									.equals(BPMNTypes.DATA_INPUT_OBJECT_LOCAL)) {
								query += "\"" + dataProcessing.getAttribute("value") + "\"";
							} else {
								query += "" + dataProcessing.getName() + "";
							}
						}
						query += " AND ";
						i++;
					}
				}
				if (query != "") {
					query = "UPDATE " + dataToUpdate.getAttribute("type") + " SET "
							+ query.substring(0, query.lastIndexOf(" AND "));
					effects.add(query);
				}
			}
		}

		// other statement (process variables)
		for (DataFlowExtension dataflow : activity.getDataFlows()) {
			BPMNElementNode elementTargetNode = (BPMNElementNode) activity.findElementById(dataflow.getTargetRef());
			if (elementTargetNode.getElementNode().getLocalName().equals(BPMNTypes.DATA_OUTPUT_OBJECT_DATA_STORE)) {
				continue;
			} else if (elementTargetNode.getElementNode().getParentNode().getLocalName()
					.equals(BPMNTypes.DATA_OUTPUT_OBJECT_DATA_STORE)) {
				continue;
			} else if (elementTargetNode.getElementNode().getLocalName().equals(BPMNTypes.DATA_PROCESSING)) {
				continue;
			}
			BPMNElementNode elementSourceNode = (BPMNElementNode) activity.findElementById(dataflow.getSourceRef());
			if (elementSourceNode.getElementNode().getLocalName()
					.equals(BPMNTypes.DATA_INPUT_OBJECT_ENVIRONMENT_DATA_USER)
					|| elementSourceNode.getElementNode().getParentNode().getLocalName()
							.equals(BPMNTypes.DATA_INPUT_OBJECT_ENVIRONMENT_DATA_USER)) {
				effects.add("#" + elementTargetNode.getName() + "=" + elementSourceNode.getName());
			} else if (elementSourceNode.getElementNode().getLocalName().equals(BPMNTypes.DATA_INPUT_OBJECT_LOCAL)) {
				effects.add("#" + elementTargetNode.getName() + "=" + elementSourceNode.getAttribute("value"));
			} else {
				effects.add("#" + elementTargetNode.getName() + "=" + elementSourceNode.getName());
			}
		}

		return effects;
	}

	private static DataSchema parseVariableDeclarations(String declaration, DataSchema dataSchema)
			throws EevarOverflowException, DuplicateDeclarationException {
		String[] declarationElements = declaration.split(":");
		System.out.println("new variable name: " + declarationElements[0].trim());
		System.out.println("new variable sort: " + declarationElements[1].trim());
		System.out.println();
		// we would actually don't need to create here a new case variable
		String varName = declarationElements[0].trim();
		Sort varSort = dataSchema.newSort(declarationElements[1].trim());
		dataSchema.newCaseVariable(varName, varSort, true);
		dataSchema.addEevar(varName, varSort);
		return dataSchema;
	}

	private static String parserPrecondition(Activity activity) throws Exception {
		String precondition = "";
		String select = "";
		String where = "";
		String table = "";

		List<BPMNElementNode> preconditionData = activity.getDataOutputObjects().stream().filter((data) -> {
			return data.getElementNode().getLocalName().contentEquals(BPMNTypes.DATA_OUTPUT_OBJECT_DATA_STORE)
					&& data.getElementNode().getAttribute("state").contentEquals("read");
		}).collect(Collectors.toList());

		preconditionData.addAll(activity.getDataInputObjects().stream().filter((data) -> {
			return data.getElementNode().getLocalName().contentEquals(BPMNTypes.DATA_INPUT_OBJECT_DATA_STORE);
		}).collect(Collectors.toList()));
		for (BPMNElementNode cond : preconditionData) {
			if (cond instanceof DataOutputObjectExtension) {
				// get read data store
				DataOutputObjectExtension preConditionOut = (DataOutputObjectExtension) cond;
				if (preConditionOut.getDataAttributes().size() > 0) {
					// get selected attributes
					for (DataObjectAttributeExtension dataAtt : preConditionOut.getDataAttributes().stream()
							.collect(Collectors.toList())) {

						select += dataAtt.getName() + ", ";
					}

					// get where conditions
					for (DataObjectAttributeExtension dataAtt : preConditionOut.getDataAttributes().stream()
							// check incaming dataflow == attributes
							.filter((att) -> activity.getDataFlows().stream()
									.filter((dataflow) -> dataflow.getTargetRef().contentEquals(att.getId()))
									.findFirst().orElse(null) != null)
							.collect(Collectors.toList())) {
						BPMNElementNode dataInput = (BPMNElementNode) activity.findElementById(activity.getDataFlows()
								.stream().filter((dataflow) -> dataflow.getTargetRef().contentEquals(dataAtt.getId()))
								.findFirst().orElseThrow().getSourceRef());
						String tempWhere = "=" + dataAtt.getName();
						if (dataInput instanceof DataInputObjectExtension) {
							if (dataInput.getElementNode().getLocalName().equals(BPMNTypes.DATA_INPUT_OBJECT_PROCESS)) {
								tempWhere = "#" + dataInput.getName() + tempWhere;
							} else if (dataInput.getElementNode().getLocalName()
									.equals(BPMNTypes.DATA_INPUT_OBJECT_LOCAL)) {
								tempWhere = "\"" + dataInput.getAttribute("value") + "\"" + tempWhere;
							} else {
								tempWhere = "" + dataInput.getName() + "" + tempWhere;
							}
						} else if (dataInput instanceof DataObjectAttributeExtension) {
							if (dataInput.getElementNode().getParentNode().getLocalName()
									.equals(BPMNTypes.DATA_INPUT_OBJECT_PROCESS)) {
								tempWhere = "#" + dataInput.getName() + "" + tempWhere;
							} else if (dataInput.getElementNode().getParentNode().getLocalName()
									.equals(BPMNTypes.DATA_INPUT_OBJECT_LOCAL)) {
								tempWhere = "\"" + dataInput.getAttribute("value") + "\"" + tempWhere;
							} else {
								tempWhere = "" + dataInput.getName() + "" + tempWhere;
							}
						}
						where += tempWhere + " AND ";
					}
					table += cond.getAttribute("type") + ", ";
				}

			} else {
				// get data store inputs
				// work on one data
				// TODO: multi data
				DataInputObjectExtension preConditionIn = activity.getDataInputObjects().stream().filter((data) -> {
					return data.getElementNode().getLocalName().contentEquals(BPMNTypes.DATA_INPUT_OBJECT_DATA_STORE);
				}).findFirst().orElse(null);

				if (preConditionIn != null) {
					if (preConditionIn.getDataAttributes().size() > 0) {
						// get selected attributes
						for (DataObjectAttributeExtension dataAtt : preConditionIn.getDataAttributes().stream()
								.filter((att) -> {
									// check outgouting dataflow == attribue
									return activity.getDataFlows().stream()
											.filter((dataflow) -> dataflow.getSourceRef().contentEquals(att.getId()))
											.findFirst().orElse(null) != null;
								}).collect(Collectors.toList())) {

							select += dataAtt.getName() + ", ";
						}

						table += cond.getAttribute("type") + ", ";
					}
				}
			}
		}
		if (preconditionData.size() > 0) {
			if (select != "") {
				if (table != "") {
					table = table.substring(0, table.lastIndexOf(", "));

				} else {
					throw new Exception("NO TABLE ADDED IN THIS EXAMPLE");
				}
				select = select.substring(0, select.lastIndexOf(", "));
				precondition = "SELECT " + select + " FROM " + table + "";
			} else {
				throw new Exception("NO ATTRIBUTE USED IN THIS EXAMPLE");
			}

			if (where != "") {
				where = where.substring(0, where.lastIndexOf(" AND "));
				precondition += " WHERE " + where;
			}
		}
		return precondition;
	}
}
