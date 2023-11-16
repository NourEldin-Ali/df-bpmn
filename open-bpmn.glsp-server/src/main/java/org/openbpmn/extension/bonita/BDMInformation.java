package org.openbpmn.extension.bonita;

import org.openbpmn.bpmn.BPMNModel;
import org.openbpmn.bpmn.elements.BPMNProcess;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * This class to import the Bonita BDM from the path added to the process. We
 * can access all the objects, and all their field.
 * 
 * @author Ali Nour Eldin
 *
 */
public class BDMInformation {
	BPMNModel model;
	Document document;

	public BDMInformation(BPMNModel model) {
		this.model = model;
		document = loadBDMfile();
	}

	/**
	 * Loading BDM file
	 * 
	 * @return
	 */
	private Document loadBDMfile() {
		File xmlFile = new File(model.openDefaultProces().getAttribute("bonitaProjectPath") + "/bdm/bom.xml");
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.parse(xmlFile);
			return document;
		} catch (Exception e) {
//			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Get all the BDM object name.
	 * 
	 * @return
	 */
	public String[] getAllBusinessObjects() {
		String[] strarray = {};
		if (document == null) {
			return strarray;
		}
		List<String> listOfBDMObject = new ArrayList<String>();
		NodeList businessObjects = document.getElementsByTagName("businessObject");

		for (int i = 0; i < businessObjects.getLength(); i++) {
			Node businessObject = businessObjects.item(i);
			if (businessObject.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) businessObject;
				String qualifiedName = element.getAttribute("qualifiedName");
				listOfBDMObject.add(qualifiedName);
			}
		}

		strarray = new String[listOfBDMObject.size()];
		listOfBDMObject.toArray(strarray);
		return strarray;
	}

	/**
	 * Get BDM object fields based on the BDM object name
	 * 
	 * @param targetQualifiedName
	 * @return
	 */
	public String[] getFieldsForQualifiedName(String targetQualifiedName) {
		String[] strarray = {};
		if (document == null) {
			return strarray;
		}
		List<String> listOfBDMObject = new ArrayList<String>();
		NodeList businessObjects = document.getElementsByTagName("businessObject");

		for (int i = 0; i < businessObjects.getLength(); i++) {
			Node businessObject = businessObjects.item(i);
			if (businessObject.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) businessObject;
				String qualifiedName = element.getAttribute("qualifiedName");

				if (qualifiedName.equals(targetQualifiedName)) {
					NodeList relationFields = element.getElementsByTagName("relationField");
					for (int k = 0; k < relationFields.getLength(); k++) {
						Node relationField = relationFields.item(k);
						if (relationField.getNodeType() == Node.ELEMENT_NODE) {
							Element relationFieldElement = (Element) relationField;
							String name = relationFieldElement.getAttribute("name");
							listOfBDMObject.add(name);
						}
					}

					NodeList fields = element.getElementsByTagName("field");
					for (int j = 0; j < fields.getLength(); j++) {
						Node field = fields.item(j);
						if (field.getNodeType() == Node.ELEMENT_NODE) {
							Element fieldElement = (Element) field;
							String name = fieldElement.getAttribute("name");
							listOfBDMObject.add(name);
						}
					}

					break;
				}
			}
		}
		strarray = new String[listOfBDMObject.size()];
		listOfBDMObject.toArray(strarray);
		return strarray;
	}

	/**
	 * Get BDM object fields based on the BDM object name
	 * 
	 * @param targetQualifiedName
	 * @param fieldName
	 * @return
	 */
	public String getFieldTypeForQualifiedNameAndField(String targetQualifiedName, String fieldName) {

		if (document == null) {
			return "string";
		}
		NodeList businessObjects = document.getElementsByTagName("businessObject");

		for (int i = 0; i < businessObjects.getLength(); i++) {
			Node businessObject = businessObjects.item(i);
			if (businessObject.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) businessObject;
				String qualifiedName = element.getAttribute("qualifiedName");

				if (qualifiedName.equals(targetQualifiedName)) {
					NodeList fields = element.getElementsByTagName("field");

					for (int j = 0; j < fields.getLength(); j++) {
						Node field = fields.item(j);
						if (field.getNodeType() == Node.ELEMENT_NODE) {
							Element fieldElement = (Element) field;
							if (fieldElement.getAttribute("name").equals(fieldName)) {
								String type = fieldElement.getAttribute("type");
								return type;
							}
						}
					}

					NodeList relationFields = element.getElementsByTagName("relationField");

					for (int k = 0; k < relationFields.getLength(); k++) {
						Node relationField = relationFields.item(k);
						if (relationField.getNodeType() == Node.ELEMENT_NODE) {
							Element relationFieldElement = (Element) relationField;
							if (relationFieldElement.getAttribute("name").equals(fieldName)) {
							String reference = relationFieldElement.getAttribute("reference");
							return reference;
							}
						}
					}

					break;
				}
			}
		}

		return "string";
	}
}
