/*
Copyright (c) 2015 Yellow Brick Systems LLC

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

	DocumentObjectFields
	
	This class defines a custom Ant Task which is used to document the fields
	for a specific object.  It will produce a CSV file with the output.
	
	
	Author:  Jeff Bohanek (jeff@yellowbricksystems.com)
 */
package com.yellowbricksystems.ant;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.tools.ant.BuildException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sforce.soap.metadata.FileProperties;
import com.sforce.soap.metadata.ListMetadataQuery;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.soap.partner.DescribeGlobalResult;
import com.sforce.soap.partner.DescribeGlobalSObjectResult;
import com.sforce.soap.partner.DescribeLayout;
import com.sforce.soap.partner.DescribeLayoutComponent;
import com.sforce.soap.partner.DescribeLayoutItem;
import com.sforce.soap.partner.DescribeLayoutResult;
import com.sforce.soap.partner.DescribeLayoutRow;
import com.sforce.soap.partner.DescribeLayoutSection;
import com.sforce.soap.partner.DescribeSObjectResult;
import com.sforce.soap.partner.Field;
import com.sforce.soap.partner.LayoutComponentType;
import com.sforce.soap.tooling.QueryResult;
import com.sforce.soap.tooling.sobject.CustomField;
import com.sforce.soap.tooling.sobject.SObject;

public class DocumentObjectFields extends SalesforceTask {
	
	public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("M/d/yyyy");
	public static final String COMMA = ",";
	public static final String NEW_LINE = "\n";
	public static final String QUOTES = "\"";
	
	protected String sourceDirectory;
	protected String destinationDirectory;
	protected String objectName;
	
	protected Map<String, ObjectField> objectFieldMap = new HashMap<String, ObjectField>();
	protected List<String> layoutNames = new ArrayList<String>();
	
	public static final String SF_OBJECT_API_NAME = "sf.objectApiName";
	
	public String getSourceDirectory() {
		return sourceDirectory;
	}

	public void setSourceDirectory(String sourceDirectory) {
		this.sourceDirectory = sourceDirectory;
	}

	public String getDestinationDirectory() {
		return destinationDirectory;
	}

	public void setDestinationDirectory(String destinationDirectory) {
		this.destinationDirectory = destinationDirectory;
	}

	
	@Override
	public void init() throws BuildException {

		super.init();
	}

	@Override
	public void execute() throws BuildException {
		if ((sourceDirectory == null) || (sourceDirectory.length() < 1) ||
				(destinationDirectory == null) || (destinationDirectory.length() < 1)) {
			throw new BuildException("Please specify Source and Destination directories.");
		}
		objectName = getProject().getProperty(SF_OBJECT_API_NAME);
		if ((objectName == null) || (objectName.length() < 1)) {
			throw new BuildException("Please specify an Object API Name.");
		}
		processDocumentObjectFields();
	}

	/**
	 * Document the Object Fields.
	 */
	protected void processDocumentObjectFields()
	{
		try
		{
			initSalesforceConnection();
			
			DescribeSObjectResult[] describeSObjectResults = partnerConnection.describeSObjects(new String[] {objectName});
			if (describeSObjectResults != null && describeSObjectResults.length > 0) {
				DescribeSObjectResult describeSObjectResult = describeSObjectResults[0];
				Field[] fields = describeSObjectResult.getFields();
				for (int i = 0; i < fields.length; i++) {
					Field describeField = fields[i];
					
					String fieldApiName = describeField.getName();
					ObjectField field = new ObjectField();
					objectFieldMap.put(fieldApiName.toLowerCase(), field);
					
					field.fieldApiName = fieldApiName;
					field.fieldLabel = describeField.getLabel();
					field.dataType = describeField.getType().toString();
					field.formula = describeField.getCalculatedFormula();
					field.helpText = describeField.getInlineHelpText();
					if (describeField.isUnique()) {
						if (describeField.isCaseSensitive()) {
							field.unique = "Case Sensitive";
						} else {
							field.unique = "Case Insensitive";
						}
					}
					field.externalId = describeField.isExternalId();
				}
			}
			
			populateMetadataValues();
			populateCreatedDate();
			populateLayouts();
			populateFieldTrip();
			
			writeCSV();
		}
		catch (Exception ex)
		{
			String error = "Error trying to document object fields\n";
			error += ex.getMessage();
			ex.printStackTrace();
			throw new BuildException(error, ex);
		}
	}
	
	protected void populateMetadataValues() throws Exception {
		String fileName = sourceDirectory + "/objects/" + objectName + ".object";
		File xmlFile = new File(fileName);
		
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(xmlFile);
		doc.getDocumentElement().normalize();
		
		NodeList objectNodeList = doc.getElementsByTagName("CustomObject");
		for (int i=0; i < objectNodeList.getLength(); i++) {
			Node objectNode = objectNodeList.item(i);
			if (objectNode.getNodeType() == Node.ELEMENT_NODE) {
				Element objectElement = (Element) objectNode;
				
				// Custom Fields
				// Loop through and see if any should be removed (based on ignore/namespace)
				NodeList customFieldNodes = objectElement.getElementsByTagName("fields");
				if (customFieldNodes != null) {
					for (int j=0; j < customFieldNodes.getLength(); j++) {
						Node customFieldNode = customFieldNodes.item(j);
						if (customFieldNode.getNodeType() == Node.ELEMENT_NODE) {
							Element customFieldElement = (Element) customFieldNode;
							
							String fieldApiName = ProcessorUtilities.getTagValue(customFieldElement, "fullName");
							String description = ProcessorUtilities.getTagValue(customFieldElement, "description");
							String trackHistory = ProcessorUtilities.getTagValue(customFieldElement, "trackHistory");
							String trackFeedHistory = ProcessorUtilities.getTagValue(customFieldElement, "trackFeedHistory");
							String required = ProcessorUtilities.getTagValue(customFieldElement, "required");
							String length = ProcessorUtilities.getTagValue(customFieldElement, "length");
							String precision = ProcessorUtilities.getTagValue(customFieldElement, "precision");
							String scale = ProcessorUtilities.getTagValue(customFieldElement, "scale");
							String type = ProcessorUtilities.getTagValue(customFieldElement, "type");
							
							ObjectField field = objectFieldMap.get(fieldApiName.toLowerCase());
							if (field != null) {
								field.description = description;
								field.trackHistory = Boolean.valueOf(trackHistory);
								field.trackFeedHistory = Boolean.valueOf(trackFeedHistory);
								field.required = Boolean.valueOf(required);
								if (length != null && length.trim().length() > 0) {
									field.dataTypeLength = length;
								} else if (precision != null && precision.trim().length() > 0) {
									field.dataTypeLength = precision + ", " + scale;
								}
								if (type != null && type.trim().length() > 0) {
									field.dataType = type;
								}
							}
						}
					}
				}
			}
		}
	}
	
	protected void populateCreatedDate() throws Exception {
		String tableEnumOrId = objectName;
		if (objectName.endsWith("__c")) {
			// Custom object - we need to find Id
			String tableQuery = "select Id, DeveloperName from CustomObject where DeveloperName = '" + objectName.substring(0, objectName.length()-3) + "'";
			QueryResult qr = toolingConnection.query(tableQuery);
			SObject[] records = qr.getRecords();
			for (SObject so : records) {
				tableEnumOrId = (String) so.getId();
			}
		}
		if (tableEnumOrId.equals("Task") || tableEnumOrId.equals("Event")) {
			// Task/Event fields are actually on Activity Object
			tableEnumOrId = "Activity";
		}
		String fieldQuery = "select Id, DeveloperName, CreatedDate from CustomField where TableEnumOrId = '" + tableEnumOrId + "'";
		QueryResult qr = toolingConnection.query(fieldQuery);
		SObject[] records = qr.getRecords();
		for (SObject so : records) {
			if (so instanceof CustomField) {
				CustomField cf = (CustomField) so;
				String developerName = cf.getDeveloperName() + "__c";
				Calendar createdDate = cf.getCreatedDate();
				ObjectField field = objectFieldMap.get(developerName.toLowerCase());
				if (field != null) {
					field.createdDate = createdDate;
				}
			}
		}
	}
	
	protected void populateLayouts() throws Exception {
		String layoutsDirectoryName = sourceDirectory + "/layouts";
		File layoutsDirectory = new File(layoutsDirectoryName);
		String[] files = layoutsDirectory.list();
		if (files != null) {
			for (String fileName : files) {
				int dashIndex = fileName.indexOf("-");
				String sObjName = fileName.substring(0,dashIndex);
				String layoutName = fileName.substring(dashIndex + 1);
				// Remove .layout
				layoutName = layoutName.substring(0, layoutName.length() - 7);
				if (!sObjName.equals(objectName)) {
					// Only need to process layouts for the requested object
					continue;
				}
				layoutNames.add(layoutName);
				
				String fullFileName = layoutsDirectoryName + "/" + fileName;
				File xmlFile = new File(fullFileName);
				DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
				Document doc = dBuilder.parse(xmlFile);
				doc.getDocumentElement().normalize();

				NodeList layoutNodeList = doc.getElementsByTagName("Layout");
				for (int i=0; i < layoutNodeList.getLength(); i++) {
					Node layoutNode = layoutNodeList.item(i);
					if (layoutNode.getNodeType() == Node.ELEMENT_NODE) {
						Element layoutElement = (Element) layoutNode;
						
						NodeList layoutItemsNodes = layoutElement.getElementsByTagName("layoutItems");
						if (layoutItemsNodes != null) {
							for (int j=0; j < layoutItemsNodes.getLength(); j++) {
								Node layoutItemsNode = layoutItemsNodes.item(j);
								
								if (layoutItemsNode.getNodeType() == Node.ELEMENT_NODE) {
									Element layoutItemsElement = (Element) layoutItemsNode;
									
									String fieldName = ProcessorUtilities.getTagValue(layoutItemsElement, "field");
									ObjectField field = objectFieldMap.get(fieldName.toLowerCase());
									if (field != null) {
										field.layoutNames.add(layoutName);
									}
								}
							}
						}
					}
				}
			}
			Collections.sort(layoutNames);
		}
	}
	
	protected void populateFieldTrip() throws Exception {
		String fieldTripObjectName = "Field_Trip__Object_Analysis__c";
		Boolean hasFieldTrip = false;
		
		// Make the describeGlobal() call
		DescribeGlobalResult describeGlobalResult = partnerConnection.describeGlobal();

		// Get the sObjects from the describe global result
		DescribeGlobalSObjectResult[] sobjectResults = describeGlobalResult.getSobjects();
		for (DescribeGlobalSObjectResult sobjectResult : sobjectResults) {
			if(sobjectResult.getName().equals(fieldTripObjectName)) {
				hasFieldTrip = true;
				break;
			}
		}
		
		if (hasFieldTrip) {
			String objectQuery = "SELECT Field_Trip__Last_Analyzed__c,Field_Trip__Object_Label__c,Field_Trip__Object_Name__c,Id,Name FROM Field_Trip__Object_Analysis__c WHERE Field_Trip__Object_Name__c = '" +
					objectName + "' AND Field_Trip__Last_Analyzed__c != null ORDER BY Field_Trip__Last_Analyzed__c DESC NULLS LAST";
			com.sforce.soap.partner.QueryResult objectQueryResult = partnerConnection.query(objectQuery);
			com.sforce.soap.partner.sobject.SObject[] objectRecords = objectQueryResult.getRecords();
			if (objectRecords != null && objectRecords.length > 0) {
				// Use the first one
				com.sforce.soap.partner.sobject.SObject objectAnalysis = objectRecords[0];
				String objectAnalysisId = objectAnalysis.getId();
				
				String fieldQuery = "SELECT Field_Trip__Populated_On_Percent__c,Field_Trip__Populated_On__c,Id,Name FROM Field_Trip__Field_Analysis__c WHERE Field_Trip__Object_Analysis__c = '" +
						objectAnalysisId + "'";
				com.sforce.soap.partner.QueryResult fieldQueryResult = partnerConnection.query(fieldQuery);
				com.sforce.soap.partner.sobject.SObject[] fieldRecords = fieldQueryResult.getRecords();
				for (com.sforce.soap.partner.sobject.SObject fieldAnalysis : fieldRecords) {
					String fieldName = (String) fieldAnalysis.getField("Name");
					ObjectField field = objectFieldMap.get(fieldName.toLowerCase());
					if (field != null) {
						Double numberPopulated = new Double((String) fieldAnalysis.getField("Field_Trip__Populated_On__c"));
						field.numberPopulated = numberPopulated.intValue();
						Double percentPopulated = new Double((String) fieldAnalysis.getField("Field_Trip__Populated_On_Percent__c"));
						field.percentPopulated = percentPopulated.doubleValue();
					}
				}
				
				
			}
			
		}
	}
	
	protected void writeCSV() throws Exception {
		String fileName = destinationDirectory + "/" + objectName + ".csv";
		FileWriter fileWriter = new FileWriter(fileName);
		
		String header = "Object";
		header += ", Field Label";
		header += ", Field API Name";
		header += ", Created Date";
		header += ", # Populated";
		header += ", % Populated";
		header += ", Data Type";
		header += ", Length";
		header += ", Required";
		header += ", External Id";
		header += ", Unique, Track History";
		header += ", Track Feed History";
		header += ", Formula";
		header += ", Description";
		header += ", Help Text";
		header += ", # Layouts";
		for (String layoutName : layoutNames) {
			header += ", " + layoutName;
		}
		
		fileWriter.append(header);
		fileWriter.append(NEW_LINE);
		
		ArrayList<String> fieldNames = new ArrayList<String>(objectFieldMap.keySet());
		Collections.sort(fieldNames);
		for (String fieldApiName : fieldNames) {
			ObjectField field = objectFieldMap.get(fieldApiName.toLowerCase());
			
			csvAddColumn(fileWriter, objectName, false);
			csvAddColumn(fileWriter, field.fieldLabel, true);
			csvAddColumn(fileWriter, field.fieldApiName, true);
			if (field.createdDate != null) {
				String createdDate = DATE_FORMAT.format(field.createdDate.getTime());
				csvAddColumn(fileWriter, createdDate, true);
			} else {
				csvAddColumn(fileWriter, "", true);
			}
			csvAddColumn(fileWriter, Integer.toString(field.numberPopulated), true);
			csvAddColumn(fileWriter, Double.toString(field.percentPopulated) + "%", true);
			csvAddColumn(fileWriter, field.dataType, true);
			csvAddColumn(fileWriter, field.dataTypeLength, true);
			csvAddColumn(fileWriter, (field.required ? "X" : ""), true);
			csvAddColumn(fileWriter, (field.externalId ? "X" : ""), true);
			csvAddColumn(fileWriter, field.unique, true);
			csvAddColumn(fileWriter, (field.trackHistory ? "X" : ""), true);
			csvAddColumn(fileWriter, (field.trackFeedHistory ? "X" : ""), true);
			csvAddColumn(fileWriter, field.formula, true);
			csvAddColumn(fileWriter, field.description, true);
			csvAddColumn(fileWriter, field.helpText, true);
			csvAddColumn(fileWriter, Integer.toString(field.layoutNames.size()), true);
			for (String layoutName : layoutNames) {
				csvAddColumn(fileWriter, (field.layoutNames.contains(layoutName) ? "X" : ""), true);
			}
			
			fileWriter.append(NEW_LINE);
		}
		fileWriter.flush();
		fileWriter.close();
	}
	
	protected void csvAddColumn(FileWriter fileWriter, String value, Boolean addComma) throws Exception {
		if (addComma) {
			fileWriter.append(COMMA);
		}
		fileWriter.append(QUOTES);
		if (value != null) {
			fileWriter.append(value.replace("\"", ""));
		}
		fileWriter.append(QUOTES);
	}
	/*
	 * Inner class used to store values for each field that is being documented.  These values will
	 * then be written to the CSV file.
	 * 
			- Object
			- Field Label
			- Field API Name
			- Created Date
			- # Populated
			- % Populated
			- Data Type
			- Length
			- Required
			- ExternalId
			- Unique
			- Formula
			- Description
			- Help Text
			
			- # Layouts
			- Layout XXX
	 * 
	 * 
	 */
	public class ObjectField {
		String fieldLabel;
		String fieldApiName;
		Calendar createdDate;
		int numberPopulated;
		double percentPopulated;
		String dataType;
		String dataTypeLength; // Includes Precision/Scale
		boolean required;
		boolean externalId;
		String unique;
		boolean trackHistory;
		boolean trackFeedHistory;
		String formula;
		String description;
		String helpText;
		Set<String> layoutNames = new HashSet<String>();
	}
}
