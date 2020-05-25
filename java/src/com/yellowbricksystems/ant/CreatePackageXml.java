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

	CreatePackageXml
	
	This class defines a custom Ant Task which is used to create a package.xml file 
	based on the settings defined by the 'sf.' properties and the contents of the 
	org based on the login credentials which are supplied.
	
	Author:  Jeff Bohanek (jeff@yellowbricksystems.com)
 */
package com.yellowbricksystems.ant;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

import com.sforce.soap.metadata.FileProperties;
import com.sforce.soap.metadata.ListMetadataQuery;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.soap.partner.QueryResult;
import com.sforce.soap.partner.sobject.SObject;
import com.sforce.soap.tooling.sobject.ApexClass;
import com.sforce.soap.tooling.sobject.ApexComponent;
import com.sforce.soap.tooling.sobject.ApexPage;
import com.sforce.soap.tooling.sobject.ApexTrigger;
import com.sforce.soap.tooling.sobject.CustomField;
import com.sforce.soap.tooling.sobject.Layout;

public class CreatePackageXml extends SalesforceTask {

	protected String packageFileName;
	
	protected Map<String, List<String>> typesMap = new HashMap<String, List<String>>();

	public static final String EXCLUDED_PACKAGE_XML_FILENAME = "excludedPackage.xml";
	protected Map<String, List<String>> excludedTypesMap = new HashMap<String, List<String>>();
	public static final String[] EXCLUDED_PACKAGE_XML_COMMENTS = new String[] {
		EXCLUDED_PACKAGE_XML_FILENAME,
		"This file is used to list out the metadata components that would normally be retrieved because of",
		"the salesforce.properties settings but are being excluded due to the " + INCLUDE_PACKAGE_XML_FILENAME,
		"or " + IGNORE_PACKAGE_XML_FILENAME + " values.  This file is generated automatically."
	};

	// Object related collections
	protected List<String> objectNames = new ArrayList<String>();
	// Object API Name => TableEnumOrId
	protected Map<String, String> objectNameEnumIdMap = new HashMap<String, String>();
	// Object API Name => Namespace
	protected Map<String, String> objectNamespaceMap = new HashMap<String, String>();
	
	public String getPackageFileName() {
		return packageFileName;
	}

	public void setPackageFileName(String packageFileName) {
		this.packageFileName = packageFileName;
	}

	@Override
	public void init() throws BuildException {
		super.init();
	}

	@Override
	public void execute() throws BuildException {
		if ((packageFileName == null) || (packageFileName.length() < 1)) {
			throw new BuildException("Please specify a packageFileName to create.");
		}

		createPackageFile();
	}

	/**
	 * Create the package.xml file based on the properties that have been set.
	 */
	protected void createPackageFile()
	{
		try
		{
			log("Creating package.xml using build version " + BUILD_VERSION);

			initSalesforceConnection();

			// Object types
			// Problem deploying if you break out objects into pieces.  We need to keep the objects whole
			// in addition to listing out the pieces so that we can figure out which pieces need
			// to be deleted.
			loadObjects();
			if (getPropertyBoolean(SF_INCLUDE_ACTION_OVERRIDES) || getPropertyBoolean(SF_INCLUDE_BUSINESS_PROCESSES) ||
					getPropertyBoolean(SF_INCLUDE_COMPACT_LAYOUTS) || getPropertyBoolean(SF_INCLUDE_CUSTOM_FIELDS) ||
					getPropertyBoolean(SF_INCLUDE_FIELD_SETS) || getPropertyBoolean(SF_INCLUDE_LIST_VIEWS) ||
					getPropertyBoolean(SF_INCLUDE_RECORD_TYPES) || getPropertyBoolean(SF_INCLUDE_SEARCH_LAYOUTS) ||
					getPropertyBoolean(SF_INCLUDE_SHARING_REASONS) || getPropertyBoolean(SF_INCLUDE_SHARING_RECALCULATIONS) ||
					getPropertyBoolean(SF_INCLUDE_VALIDATION_RULES) || getPropertyBoolean(SF_INCLUDE_WEBLINKS)) {
				// We are retrieving at least some Object component, so add the CustomObject metadata and it will
				// be filtered later in Post-Retrieve
				addObjects();
			}

			HashMap<String, PackageType> listMetadataPackageMap = new HashMap<String, PackageType>();
			for (PackageType packageType : allPackageTypes) {
				if (!getPropertyBoolean(packageType)) {
					// Skip this type - it is not set in the salesforce.properties
					continue;
				}
				if (packageType.addMethod == ADD_METHOD_LIST_METADATA) {
					listMetadataPackageMap.put(packageType.metadataName, packageType);
					if (listMetadataPackageMap.size() >= 3) {
						addTypeFromListMetadata(listMetadataPackageMap);
						listMetadataPackageMap.clear();
					}

				} else if (packageType.addMethod == ADD_METHOD_FOLDER) {
					addFolderType(packageType.propertyName, packageType.prefixPropertyName, packageType.foldersPropertyName,
							packageType.unfiledPublicPropertyName, packageType.folderType, packageType.metadataName, packageType.objectFolderFieldName);

				} else if (packageType.addMethod == ADD_METHOD_TOOLING_API) {
					addToolingType(packageType.propertyName, packageType.metadataName, packageType.prefixPropertyName);

				} else if (packageType.addMethod == ADD_METHOD_SETTINGS) {
					if (getPropertyBoolean(packageType.propertyName)) {
						addTypeMember("Settings", packageType.metadataName, null);
					}

				} else if (packageType.addMethod == ADD_METHOD_QUERY) {
					addQueryType(packageType.propertyName, packageType.metadataName, packageType.prefixPropertyName,
							packageType.query, packageType.nameFieldName, packageType.namespaceFieldName);

				}
			}
			if (listMetadataPackageMap.size() > 0) {
				// Add any stragglers
				addTypeFromListMetadata(listMetadataPackageMap);
				listMetadataPackageMap.clear();
			}

			if (getPropertyBoolean(SF_INCLUDE_STANDARD_VALUE_SETS)) {
				// Need to hard code Standard picklist values - no retrieve call to get them
				addTypeMember("StandardValueSet", "AccountContactMultiRoles", null);
				addTypeMember("StandardValueSet", "AccountContactRole", null);
				addTypeMember("StandardValueSet", "AccountOwnership", null);
				addTypeMember("StandardValueSet", "AccountRating", null);
				addTypeMember("StandardValueSet", "AccountType", null);
				addTypeMember("StandardValueSet", "AssetStatus", null);
				addTypeMember("StandardValueSet", "CampaignMemberStatus", null);
				addTypeMember("StandardValueSet", "CampaignStatus", null);
				addTypeMember("StandardValueSet", "CampaignType", null);
				addTypeMember("StandardValueSet", "CaseContactRole", null);
				addTypeMember("StandardValueSet", "CaseOrigin", null);
				addTypeMember("StandardValueSet", "CasePriority", null);
				addTypeMember("StandardValueSet", "CaseReason", null);
				addTypeMember("StandardValueSet", "CaseStatus", null);
				addTypeMember("StandardValueSet", "CaseType", null);
				addTypeMember("StandardValueSet", "ContactRole", null);
				addTypeMember("StandardValueSet", "ContractContactRole", null);
				addTypeMember("StandardValueSet", "ContractStatus", null);
				addTypeMember("StandardValueSet", "EntitlementType", null);
				addTypeMember("StandardValueSet", "EventSubject", null);
				addTypeMember("StandardValueSet", "EventType", null);
				addTypeMember("StandardValueSet", "FiscalYearPeriodName", null);
				addTypeMember("StandardValueSet", "FiscalYearPeriodPrefix", null);
				addTypeMember("StandardValueSet", "FiscalYearQuarterName", null);
				addTypeMember("StandardValueSet", "FiscalYearQuarterPrefix", null);
				addTypeMember("StandardValueSet", "IdeaMultiCategory", null);
				addTypeMember("StandardValueSet", "IdeaStatus", null);
				addTypeMember("StandardValueSet", "IdeaThemeStatus", null);
				addTypeMember("StandardValueSet", "Industry", null);
				addTypeMember("StandardValueSet", "LeadSource", null);
				addTypeMember("StandardValueSet", "LeadStatus", null);
				addTypeMember("StandardValueSet", "OpportunityCompetitor", null);
				addTypeMember("StandardValueSet", "OpportunityStage", null);
				addTypeMember("StandardValueSet", "OpportunityType", null);
				addTypeMember("StandardValueSet", "OrderType", null);
				addTypeMember("StandardValueSet", "PartnerRole", null);
				addTypeMember("StandardValueSet", "Product2Family", null);
				addTypeMember("StandardValueSet", "QuickTextCategory", null);
				addTypeMember("StandardValueSet", "QuickTextChannel", null);
				addTypeMember("StandardValueSet", "QuoteStatus", null);
				addTypeMember("StandardValueSet", "RoleInTerritory2", null);
				addTypeMember("StandardValueSet", "SalesTeamRole", null);
				addTypeMember("StandardValueSet", "Salutation", null);
				addTypeMember("StandardValueSet", "ServiceContractApprovalStatus", null);
				addTypeMember("StandardValueSet", "SocialPostClassification", null);
				addTypeMember("StandardValueSet", "SocialPostEngagementLevel", null);
				addTypeMember("StandardValueSet", "SocialPostReviewedStatus", null);
				addTypeMember("StandardValueSet", "SolutionStatus", null);
				addTypeMember("StandardValueSet", "TaskPriority", null);
				addTypeMember("StandardValueSet", "TaskStatus", null);
				addTypeMember("StandardValueSet", "TaskSubject", null);
				addTypeMember("StandardValueSet", "TaskType", null);
				addTypeMember("StandardValueSet", "WorkOrderLineItemStatus", null);
				addTypeMember("StandardValueSet", "WorkOrderPriority", null);
				addTypeMember("StandardValueSet", "WorkOrderStatus", null);
				log("Added StandardValueSet");
			}

			PackageUtilities.createPackageXmlFile(packageFileName, API_VERSION, typesMap);
			String basedir = getProject().getProperty("basedir");
			PackageUtilities.createPackageXmlFile(basedir + "/" + EXCLUDED_PACKAGE_XML_FILENAME, API_VERSION, excludedTypesMap, EXCLUDED_PACKAGE_XML_COMMENTS);

			if (getPropertyBoolean(SF_PRINT_UNUSED_TYPES)) {
				printUnusedTypes();
			}
		}
		catch (IOException ioe)
		{
			throw new BuildException("Error trying to write package.xml file");
		}
	}

	protected void printUnusedTypes() {
		ArrayList<PackageType> unusedTypes = new ArrayList<PackageType>();
		for (PackageType packageType : allPackageTypes) {
			if (getPropertyBoolean(packageType) && packageType.addMethod != ADD_METHOD_SETTINGS) {
				// This type is being included in the build, let's see if we added any
				// metadata for it
				List<String> types = typesMap.get(packageType.metadataName);
				if (types == null || types.size() == 0) {
					unusedTypes.add(packageType);
				}
			}
		}
		if (unusedTypes.size() > 0) {
			log("The following types were included in the build but there were no matching values.  You may want to exclude them from the build to save time.");
			for (PackageType packageType : unusedTypes) {
				log("    " + packageType.metadataName + " (" + packageType.propertyName + ")");
			}
		}
	}

	protected boolean addTypeMember(String typeName, String memberName, String namespace) {
		boolean added = false;
		if (includeMetadata(typeName, memberName, namespace)) {
			List<String> memberList = typesMap.get(typeName);
			if (memberList == null) {
				memberList = new ArrayList<String>();
				typesMap.put(typeName, memberList);
			}
			if (!memberList.contains(memberName)) {
				memberList.add(memberName);
			}
			added = true;
		} else {
			// Keep track of which members we are excluding from the package.xml
			List<String> excludedMemberList = excludedTypesMap.get(typeName);
			if (excludedMemberList == null) {
				excludedMemberList = new ArrayList<String>();
				excludedTypesMap.put(typeName, excludedMemberList);
			}
			excludedMemberList.add(memberName);
		}
		return added;
	}

	protected void addTypeFromListMetadata(HashMap<String, PackageType> packageTypeMap) throws IOException {
		String listTypes = "";
		try {
			long startTime = System.nanoTime();
			MetadataConnection metaConnection = getMetadataConnection();

			HashMap<String, Integer> typeCountMap = new HashMap<String, Integer>();
			ArrayList<ListMetadataQuery> queries = new ArrayList<ListMetadataQuery>();
			String clause = "";
			for (PackageType packageType : packageTypeMap.values()) {
				ListMetadataQuery query = new ListMetadataQuery();
				query.setType(packageType.metadataName);
				queries.add(query);

				// Pre-load the memberPrefix so we only do it once
				setMemberPrefix(packageType);

				listTypes += clause + packageType.metadataName;
				clause = ", ";

				typeCountMap.put(packageType.metadataName, 0);
			}

            FileProperties[] properties;
            try {
                properties = metaConnection.listMetadata(queries.toArray(new ListMetadataQuery[0]), API_VERSION);
            } catch (Exception ex1) {
                // Retry once
                log("Retrying List Metadata call...");
				properties = metaConnection.listMetadata(queries.toArray(new ListMetadataQuery[0]), API_VERSION);
            }
			for (FileProperties p : properties) {
				String typeName = p.getType();
				PackageType packageType = packageTypeMap.get(typeName);

				String namespace = p.getNamespacePrefix();
				if (managedPackageTypes.contains(typeName) || namespace == null || namespace.trim().length() == 0) {
					String fullName = p.getFullName();
					String matchName = fullName;
					String[] splitNames = fullName.split("\\.");
					if (splitNames.length == 2) {
						// This is a subset of Object so it include the Object name first and we just want the component name
						String objectName = splitNames[0];
						matchName = splitNames[1];
					}
					if (packageType.memberPrefix == null || packageType.memberPrefix.trim().length() == 0 ||
							matchName.startsWith(packageType.memberPrefix)) {
						if (addTypeMember(typeName, fullName, namespace)) {
							int typeCount = typeCountMap.get(typeName);
							++typeCount;
							typeCountMap.put(typeName, typeCount);
						}
					}
				}
			}
			long elapsedTime = System.nanoTime() - startTime;
			String logMessage = "Added ";
			clause = "";
			for (PackageType packageType : packageTypeMap.values()) {
				int typeCount = typeCountMap.get(packageType.metadataName);
				logMessage += clause + packageType.metadataName + " (" + typeCount + ")";
				clause = ", ";
			}
			logMessage += " from List Metadata [" + TimeUnit.NANOSECONDS.toMillis(elapsedTime) + " ms]";
			log(logMessage);
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new BuildException("Exception trying to add " + listTypes + " List Metadata components.");
		}
	}

	protected void addQueryType(String propertyName, String typeName, String memberPrefixPropertyName, String query, String nameFieldName, String namespaceFieldName) throws IOException{
		String memberPrefix = null;
		if (memberPrefixPropertyName != null) {
			memberPrefix = getProject().getProperty(memberPrefixPropertyName);
		}

		if (getPropertyBoolean(propertyName)) {
			addTypeFromQuery(typeName, memberPrefix, query, nameFieldName, namespaceFieldName);
		}
	}
	
	protected void addTypeFromQuery(String typeName, String memberPrefix, String query, String nameFieldName, String namespaceFieldName) throws IOException {
		try {
			long startTime = System.nanoTime();
			int typeCount = 0;
			Boolean done = false;
			com.sforce.soap.partner.QueryResult qr = partnerConnection.query(query);
			while (!done) {
				com.sforce.soap.partner.sobject.SObject[] records = qr.getRecords();
				for (com.sforce.soap.partner.sobject.SObject so : records) {
					String name = (String) so.getField(nameFieldName);
					String namespacePrefix = (String) so.getField(namespaceFieldName);
					if (name != null) {
						if (managedPackageTypes.contains(typeName) || namespacePrefix == null || namespacePrefix.trim().length() == 0) {
							String fullName = name;
							if (namespacePrefix != null && namespacePrefix.trim().length() > 0) {
								fullName = namespacePrefix + "__" + name;
							}
							String matchName = fullName;
							if (memberPrefix == null || memberPrefix.trim().length() == 0 ||
									matchName.startsWith(memberPrefix)) {
								if (addTypeMember(typeName, fullName, namespacePrefix)) {
									++typeCount;
								}
							}
						}
					}
				}
				if (qr.isDone()) {
					done = true;
				} else {
					qr = partnerConnection.queryMore(qr.getQueryLocator());
				}
			}
			long elapsedTime = System.nanoTime() - startTime;
			log("Added " + typeName + " (" + typeCount + ") from SOQL query [" + TimeUnit.NANOSECONDS.toMillis(elapsedTime) + " ms]");
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new BuildException("Exception trying to add " + typeName + " components.");
		}
	}

	protected void loadObjects() {
		try {
			long startTime = System.nanoTime();
			// We need to use List Metadata so we can get a complete list of both
			// Standard and Custom Objects
			objectNames = new ArrayList<String>();
			objectNameEnumIdMap = new HashMap<String, String>();
			objectNamespaceMap = new HashMap<String, String>();
			ListMetadataQuery query = new ListMetadataQuery();
			query.setType("CustomObject");
			
			FileProperties[] properties = metadataConnection.listMetadata(new ListMetadataQuery[] {query}, API_VERSION);
			for (FileProperties p : properties) {
				String namespace = p.getNamespacePrefix();
				if (managedPackageTypes.contains("CustomObject") || namespace == null || namespace.trim().length() == 0) {
					String objectName = p.getFullName();
					String objectEnumId = p.getId();
					if (!objectName.endsWith("__c") && !objectName.endsWith("__mdt")) {
						// Standard object so use name as enum
						objectEnumId = objectName;
					}
					objectNames.add(objectName);
					objectNameEnumIdMap.put(objectName,  objectEnumId);
					objectNamespaceMap.put(objectName,  namespace);
				}
			}
			long elapsedTime = System.nanoTime() - startTime;
			log("Cached objects from List Metadata [" + TimeUnit.NANOSECONDS.toMillis(elapsedTime) + " ms]");
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new BuildException("Exception trying to load objects.");
		}
		
	}
	
	protected void addObjects() {
		for (String objectName : objectNames) {
			String namespace = objectNamespaceMap.get(objectName);
			addTypeMember("CustomObject", objectName, namespace);
		}
	}
	
	protected void addObjectToolingType(String propertyName, String typeName) throws BuildException{
		if (getPropertyBoolean(propertyName)) {
			addObjectTypeFromToolingQuery(typeName);
		}
	}
	
	protected void addObjectTypeFromToolingQuery(String typeName) throws BuildException {
		try {
			long startTime = System.nanoTime();
			int typeCount = 0;
			Map<String, List<com.sforce.soap.tooling.sobject.SObject>> objectResultMap = new HashMap<String, List<com.sforce.soap.tooling.sobject.SObject>>();
			String query = "select Id, DeveloperName, TableEnumOrId, NamespacePrefix from CustomField order by DeveloperName";
			if (typeName.equals("Layout")) {
				query = "select Id, Name, TableEnumOrId, NamespacePrefix from Layout where LayoutType = 'Standard' order by Name";
			}
			Boolean done = false;
			com.sforce.soap.tooling.QueryResult qr = toolingConnection.query(query);
			while (!done) {
				com.sforce.soap.tooling.sobject.SObject[] records = qr.getRecords();
				String namespace = null;
				for (com.sforce.soap.tooling.sobject.SObject so : records) {
					String tableEnumOrId = null;
					if (so instanceof CustomField) {
						tableEnumOrId = ((CustomField) so).getTableEnumOrId();
						namespace = ((CustomField)so).getNamespacePrefix();
					} else if (so instanceof Layout) {
						tableEnumOrId = ((Layout) so).getTableEnumOrId();
						namespace = ((Layout)so).getNamespacePrefix();
					}
					if (managedPackageTypes.contains(typeName) || namespace == null || namespace.trim().length() == 0) {
						if (tableEnumOrId != null) {
							List<com.sforce.soap.tooling.sobject.SObject> objects = objectResultMap.get(tableEnumOrId);
							if (objects == null) {
								objects = new ArrayList<com.sforce.soap.tooling.sobject.SObject>();
								objectResultMap.put(tableEnumOrId, objects);
							}
							objects.add(so);
						}
					}
				}
				if (qr.isDone()) {
					done = true;
				} else {
					qr = toolingConnection.queryMore(qr.getQueryLocator());
				}
			}
			// Loop through objects in alphabetical order
			for (String objectName : objectNames) {
				String objectEnumOrId = objectNameEnumIdMap.get(objectName);
				if (objectEnumOrId != null) {
					List<com.sforce.soap.tooling.sobject.SObject> objects = objectResultMap.get(objectEnumOrId);
					if (objects != null) {
						for (com.sforce.soap.tooling.sobject.SObject so : objects) {
							String fullName = objectName ;
							String namespace = null;
							if (so instanceof CustomField) {
								CustomField cf = (CustomField) so;
								namespace = cf.getNamespacePrefix();
								if (namespace != null && namespace.trim().length() > 0) {
									fullName += "." + namespace + "__" + cf.getDeveloperName() + "__c";
								} else {
									fullName += "." + ((CustomField) so).getDeveloperName() + "__c";
								}
							}
							if (so instanceof Layout) {
								Layout l = (Layout) so;
								namespace = l.getNamespacePrefix();
								if (namespace != null && namespace.trim().length() > 0) {
									fullName += "-" + namespace + "__" + l.getName();
								} else {
									fullName += "-" + l.getName();
								}
								// Need to encode Layout name since it is not a DeveloperName
								fullName = encodePackageMember(fullName);
							}
							if (addTypeMember(typeName, fullName, namespace)) {
								++typeCount;
							}
						}
					}
				}
			}
			long elapsedTime = System.nanoTime() - startTime;
			log("Added " + typeName + " (" + typeCount + ") from Tooling query [" + TimeUnit.NANOSECONDS.toMillis(elapsedTime) + " ms]");
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new BuildException("Exception trying to add " + typeName + " components.");
		}
	}
	
	protected String encodePackageMember(String fullName) throws UnsupportedEncodingException{
		if (fullName != null) {
			return fullName.replace("%", "%25")
					.replace("!", "%21")
					.replace("\"", "%22")
					.replace("#", "%23")
					.replace("$", "%24")
					.replace("&", "%26")
					.replace("'", "%27")
					.replace("(", "%28")
					.replace(")", "%29")
					.replace("+", "%2B")
					.replace(",", "%2C")
					.replace(".", "%2E")
					.replace("/", "%2F")
					.replace(":", "%3A")
					.replace(";", "%3B")
					.replace("<", "%3C")
					.replace("=", "%3D")
					.replace(">", "%3E")
					.replace("?", "%3F")
					.replace("@", "%40")
					.replace("[", "%5B")
					.replace("\\", "%5C")
					.replace("]", "%5D")
					.replace("^", "%5E")
					.replace("`", "%60")
					;
		}
		return null;
	}
	
	protected void addToolingType(String propertyName, String typeName) throws IOException{
		addToolingType(propertyName, typeName, null);
	}

	protected void addToolingType(String propertyName, String typeName, String memberPrefixPropertyName) throws IOException{
		String memberPrefix = null;
		if (memberPrefixPropertyName != null) {
			memberPrefix = getProject().getProperty(memberPrefixPropertyName);
		}

		if (getPropertyBoolean(propertyName)) {
			addTypeFromToolingQuery(typeName, memberPrefix);
		}
	}
	
	protected void addTypeFromToolingQuery(String typeName, String memberPrefix) throws IOException {
		try {
			long startTime = System.nanoTime();
			int typeCount = 0;
			String query = "select Id, Name, NamespacePrefix from " + typeName;
			if (managedPackageTypes.contains(typeName)) {
				query += " order by Name";
			} else {
				query += " where NamespacePrefix = null order by Name";
			}
			Boolean done = false;
			com.sforce.soap.tooling.QueryResult qr = toolingConnection.query(query);
			while (!done) {
				com.sforce.soap.tooling.sobject.SObject[] records = qr.getRecords();
				for (com.sforce.soap.tooling.sobject.SObject so : records) {
					String name = null;
					String namespacePrefix = null;
					if (so instanceof ApexClass) {
						name = ((ApexClass) so).getName();
						namespacePrefix = ((ApexClass) so).getNamespacePrefix();
					} else if (so instanceof ApexComponent) {
						name = ((ApexComponent) so).getName();
						namespacePrefix = ((ApexComponent) so).getNamespacePrefix();
					} else if (so instanceof ApexPage) {
						name = ((ApexPage) so).getName();
						namespacePrefix = ((ApexPage) so).getNamespacePrefix();
					} else if (so instanceof ApexTrigger) {
						name = ((ApexTrigger) so).getName();
						namespacePrefix = ((ApexTrigger) so).getNamespacePrefix();
					}
					if (name != null) {
						String fullName = name;
						if (namespacePrefix != null && namespacePrefix.trim().length() > 0) {
							fullName = namespacePrefix + "__" + name;
						}
						String matchName = fullName;
						if (memberPrefix == null || memberPrefix.trim().length() == 0 ||
								matchName.startsWith(memberPrefix)) {
							if (addTypeMember(typeName, fullName, namespacePrefix)) {
								++typeCount;
							}
						}
					}
				}
				if (qr.isDone()) {
					done = true;
				} else {
					qr = toolingConnection.queryMore(qr.getQueryLocator());
				}
			}
			long elapsedTime = System.nanoTime() - startTime;
			log("Added " + typeName + " (" + typeCount + ") from Tooling query [" + TimeUnit.NANOSECONDS.toMillis(elapsedTime) + " ms]");
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new BuildException("Exception trying to add " + typeName + " components.");
		}
	}
	
	protected void addFolderType(String includePropertyName, String includeFolderPrefixPropertyName, String includeFoldersPropertyName,
			String includeUnfiledPublicPropertyName, String folderType, String objectName, String objectFolderFieldName) throws IOException {
		if (getPropertyBoolean(includePropertyName)) {
			try {
				long startTime = System.nanoTime();
				int typeCount = 0;
				PartnerConnection connection = getPartnerConnection();
				
				HashSet<String> includeFolders = new HashSet<String>();
				String includeFoldersProperty = getProject().getProperty(includeFoldersPropertyName);
				if (includeFoldersProperty != null && includeFoldersProperty.trim().length() > 0) {
					for (String folder : includeFoldersProperty.split(";")) {
						includeFolders.add(folder);
					}
				}
				
				String folderPrefix = getProject().getProperty(includeFolderPrefixPropertyName);
				SObject[] folders = getFolders(folderType, folderPrefix, includeFolders);
				for (SObject folder : folders) {
					String folderId = folder.getId();
					String folderName = (String) folder.getField("DeveloperName");
					String folderNamespacePrefix = (String) folder.getField("NamespacePrefix");
					if (folderNamespacePrefix != null && folderNamespacePrefix.trim().length() > 0) {
						folderName = folderNamespacePrefix + "__" + folderName;
					}
					if (addTypeMember(objectName, folderName, folderNamespacePrefix)) {
						++typeCount;
					}

					String soql = "select Id, DeveloperName, NamespacePrefix from " + objectName + " where " +
							objectFolderFieldName + "='" + folderId + "' ";
					QueryResult qr = connection.query(soql);
					SObject[] sobjects = qr.getRecords();
					for (SObject so : sobjects) {
						String developerName = (String) so.getField("DeveloperName");
						String namespacePrefix = (String) so.getField("NamespacePrefix");
						if (namespacePrefix != null && namespacePrefix.trim().length() > 0) {
							developerName = namespacePrefix + "__" + developerName;
						}
						if (addTypeMember(objectName, folderName + "/" + developerName, namespacePrefix)) {
							++typeCount;
						}
					}
				}

				if (getPropertyBoolean(includeUnfiledPublicPropertyName)) {
					//addTypeMember(objectName, "unfiled$public");
					String orgId = partnerConnection.getUserInfo().getOrganizationId();
					String soql = "select Id, DeveloperName from " + objectName + " where " +
							objectFolderFieldName + "='" + orgId + "' ";
					QueryResult qr = connection.query(soql);
					SObject[] sobjects = qr.getRecords();
					for (SObject so : sobjects) {
						String developerName = (String) so.getField("DeveloperName");
						if (addTypeMember(objectName, "unfiled$public/" + developerName, null)) {
							++typeCount;
						}
					}
				}
				long elapsedTime = System.nanoTime() - startTime;
				log("Added " + objectName + " (" + typeCount + ") from SOQL query [" + TimeUnit.NANOSECONDS.toMillis(elapsedTime) + " ms]");
			} catch (Exception ex) {
				ex.printStackTrace();
				throw new BuildException("Exception retrieving folder list.");
			}
		}
	}

	protected SObject[] getFolders(String folderType, String folderPrefix, HashSet<String> includeFolders) {
		SObject[] folders = new SObject[] {};

		try {
			String soql = "select Id, DeveloperName, NamespacePrefix from Folder where DeveloperName != null " +
					" and Type='" + folderType + "' ";
			if (!managedPackageTypes.contains(folderType)) {
				soql += " AND NamespacePrefix = null ";
			}
			PartnerConnection connection = getPartnerConnection();
			QueryResult qr = connection.query(soql);
			SObject[] soqlFolders = qr.getRecords();
			List<SObject> approvedFolders = new ArrayList<SObject>();
			for (SObject so : soqlFolders) {
				String developerName = (String) so.getField("DeveloperName");
				String namespacePrefix = (String) so.getField("NamespacePrefix");
				if (namespacePrefix != null && namespacePrefix.trim().length() > 0) {
					developerName = namespacePrefix + "__" + developerName;
				}
				if (((folderPrefix == null || folderPrefix.length() == 0) && includeFolders.size() == 0) ||
						includeFolders.contains(developerName) ||
						(folderPrefix != null && folderPrefix.length() > 0 && developerName != null && developerName.startsWith(folderPrefix))) {
					approvedFolders.add(so);
				}
			}
			folders = approvedFolders.toArray(new SObject[0]);
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new BuildException("Exception retrieving folder list.");
		}

		return folders;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// Default Project for testing
		try {
			CreatePackageXml createPackage = new CreatePackageXml();

			Project testProject = new Project();

			// Login settings
			Properties loginProperties = new Properties();
			InputStream loginPropertiesInputStream = new FileInputStream("login.properties");
			loginProperties.load(loginPropertiesInputStream);
			testProject.setProperty(SF_USER_PROPERTY_NAME, loginProperties.getProperty(SF_USER_PROPERTY_NAME));
			testProject.setProperty(SF_PASSWORD_PROPERTY_NAME, loginProperties.getProperty(SF_PASSWORD_PROPERTY_NAME));
			testProject.setProperty(SF_SERVER_URL_PROPERTY_NAME, loginProperties.getProperty(SF_SERVER_URL_PROPERTY_NAME));

			// Create Settings
			testProject.setProperty(SF_INCLUDE_CLASSES.propertyName, "yEs");
			testProject.setProperty(SF_INCLUDE_COMPONENTS.propertyName, "trUE");
			testProject.setProperty(SF_INCLUDE_PAGES.propertyName, "yEs");
			testProject.setProperty(SF_INCLUDE_TRIGGERS.propertyName, "trUE");
			testProject.setProperty(SF_INCLUDE_APPLICATIONS.propertyName, "yEs");
			testProject.setProperty(SF_INCLUDE_LABELS.propertyName, "yEs");
			testProject.setProperty(SF_INCLUDE_BUSINESS_PROCESSES.propertyName, "trUE");
			testProject.setProperty(SF_INCLUDE_CUSTOM_FIELDS.propertyName, "trUE");
			testProject.setProperty(SF_INCLUDE_FIELD_SETS.propertyName, "trUE");
			testProject.setProperty(SF_INCLUDE_LIST_VIEWS.propertyName, "trUE");
			testProject.setProperty(SF_INCLUDE_LIST_VIEWS.prefixPropertyName, "A_");
			testProject.setProperty(SF_INCLUDE_RECORD_TYPES.propertyName, "trUE");
			testProject.setProperty(SF_INCLUDE_SHARING_REASONS.propertyName, "trUE");
			testProject.setProperty(SF_INCLUDE_VALIDATION_RULES.propertyName, "trUE");
			testProject.setProperty(SF_INCLUDE_WEBLINKS.propertyName, "trUE");
			testProject.setProperty(SF_INCLUDE_OBJECT_TRANSLATIONS.propertyName, "yEs");
			testProject.setProperty(SF_INCLUDE_CUSTOM_PAGE_WEBLINKS.propertyName, "yEs");
			testProject.setProperty(SF_INCLUDE_SITES.propertyName, "yEs");
			testProject.setProperty(SF_INCLUDE_TABS.propertyName, "yEs");
			testProject.setProperty(SF_INCLUDE_DASHBOARDS.propertyName, "yEs");
			testProject.setProperty(SF_INCLUDE_DASHBOARDS.prefixPropertyName, "Test_");
			testProject.setProperty(SF_INCLUDE_DATA_CATEGORY_GROUPS.propertyName, "yEs");
			testProject.setProperty(SF_INCLUDE_DOCUMENTS.propertyName, "yEs");
			testProject.setProperty(SF_INCLUDE_DOCUMENTS.prefixPropertyName, "");
			testProject.setProperty(SF_INCLUDE_EMAILS.propertyName, "yEs");
			testProject.setProperty(SF_INCLUDE_EMAILS.prefixPropertyName, "");
			testProject.setProperty(SF_INCLUDE_EMAILS.unfiledPublicPropertyName, "true");
			testProject.setProperty(SF_INCLUDE_HOME_PAGE_COMPONENTS.propertyName, "yEs");
			testProject.setProperty(SF_INCLUDE_HOME_PAGE_LAYOUTS.propertyName, "yEs");
			testProject.setProperty(SF_INCLUDE_LAYOUTS.propertyName, "yEs");
			testProject.setProperty(SF_INCLUDE_LETTERHEADS.propertyName, "yEs");
			testProject.setProperty(SF_INCLUDE_PROFILES.propertyName, "yEs");
			testProject.setProperty(SF_INCLUDE_PROFILES.prefixPropertyName, "%28");
			testProject.setProperty(SF_INCLUDE_REMOTE_SITE_SETTINGS.propertyName, "yEs");
			testProject.setProperty(SF_INCLUDE_REPORTS.propertyName, "yEs");
			testProject.setProperty(SF_INCLUDE_REPORTS.prefixPropertyName, "");
			testProject.setProperty(SF_INCLUDE_REPORTS.unfiledPublicPropertyName, "true");
			testProject.setProperty(SF_INCLUDE_REPORT_TYPES.propertyName, "yEs");
			testProject.setProperty(SF_INCLUDE_SCONTROLS.propertyName, "yEs");
			testProject.setProperty(SF_INCLUDE_STATIC_RESOURCES.propertyName, "yEs");
			testProject.setProperty(SF_INCLUDE_WORKFLOWS.propertyName, "yEs");

			// Ignore settings
			testProject.setProperty(SF_IGNORE_PREFIX, "Case.Email-to-Case;Lead.STOP EMAIL");

			createPackage.setProject(testProject);

			//createPackage.test();

			createPackage.setPackageFileName("package.xml");
			createPackage.init();
			createPackage.execute();

			PackageUtilities.parsePackageXmlFile("package.xml");
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

}
