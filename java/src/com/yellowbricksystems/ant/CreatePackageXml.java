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
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
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
import com.sforce.soap.tooling.ToolingConnection;
import com.sforce.soap.tooling.sobject.ApexClass;
import com.sforce.soap.tooling.sobject.ApexComponent;
import com.sforce.soap.tooling.sobject.ApexPage;
import com.sforce.soap.tooling.sobject.ApexTrigger;
import com.sforce.soap.tooling.sobject.CustomField;
import com.sforce.soap.tooling.sobject.Layout;
import com.sforce.soap.tooling.sobject.RecordType;

public class CreatePackageXml extends SalesforceTask {

	public static final String BUILD_VERSION = "37.1";
	
	public static final String SF_IGNORE_PREFIX = "sf.ignore";

	protected String packageFileName;
	
	protected Boolean includeManagedPackages = false;

	protected Map<String, List<String>> typesMap = new HashMap<String, List<String>>();

	protected HashSet<String> ignoreList = new HashSet<String>();
	
	// Object related collections
	protected List<String> objectNames = new ArrayList<String>();
	// Object API Name => TableEnumOrId
	protected Map<String, String> objectNameEnumIdMap = new HashMap<String, String>();
	
	public String getPackageFileName() {
		return packageFileName;
	}

	public void setPackageFileName(String packageFileName) {
		this.packageFileName = packageFileName;
	}

	@Override
	public void init() throws BuildException {
		loadIgnoreValues();

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
			
			// Initial types should not include any managed package records
			includeManagedPackages = false;
			// Apex Types
			addToolingType(SF_INCLUDE_CLASSES, "ApexClass");
			addToolingType(SF_INCLUDE_COMPONENTS, "ApexComponent");
			addToolingType(SF_INCLUDE_PAGES, "ApexPage");
			addToolingType(SF_INCLUDE_TRIGGERS, "ApexTrigger");
			addType(SF_INCLUDE_FLEXI_PAGES, "FlexiPage");
			// Due to a listMetadata bug that doesn't return the correct fullName for active Flows, we
			// can't use listMetadata to retrieve the list of Flows.  Instead we can only add it as a 
			// "*" in package.xml to retrieve all Flows.
			// addType(SF_INCLUDE_FLOWS, "Flow");
			if (getPropertyBoolean(SF_INCLUDE_FLOWS)) {
				List<String> memberList = new ArrayList<String>();
				memberList.add("*");
				typesMap.put("Flow", memberList);
			}
			addType(SF_INCLUDE_SCONTROLS, "Scontrol");
			addType(SF_INCLUDE_STATIC_RESOURCES, "StaticResource");
			addType(SF_INCLUDE_AURA_DEFINITION_BUNDLES, "AuraDefinitionBundle");
			addType(SF_INCLUDE_PLATFORM_CACHE_PARTITIONS, "PlatformCachePartition");

			// App Types
			addType(SF_INCLUDE_APP_MENUS, "AppMenu");
			addType(SF_INCLUDE_CONNECTED_APPS, "ConnectedApp");
			addType(SF_INCLUDE_APPLICATIONS, "CustomApplication");
			addType(SF_INCLUDE_APPLICATION_COMPONENTS, "CustomApplicationComponent");
			addType(SF_INCLUDE_LABELS, "CustomLabel");
			addType(SF_INCLUDE_CUSTOM_PAGE_WEBLINKS, "CustomPageWebLink");
			addType(SF_INCLUDE_TABS, "CustomTab");
			addFolderType(SF_INCLUDE_DOCUMENTS, SF_INCLUDE_DOCUMENTS_FOLDER_PREFIX, null,
					"Document", "Document", "FolderId");
			addType(SF_INCLUDE_HOME_PAGE_COMPONENTS, "HomePageComponent");
			addType(SF_INCLUDE_HOME_PAGE_LAYOUTS, "HomePageLayout");
			addType(SF_INCLUDE_INSTALLED_PACKAGES, "InstalledPackage");
			addType(SF_INCLUDE_TRANSLATIONS, "Translations");

			// Object types
			// Problem deploying if you break out objects into pieces.  We need to keep the objects whole
			// in addition to listing out the pieces so that we can figure out which pieces need
			// to be deleted.
			includeManagedPackages = getPropertyBoolean(SF_INCLUDE_MANAGED_PACKAGES);
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

				addType(SF_INCLUDE_CUSTOM_FIELDS, "CustomField");
				// Can't use Tooling API for Custom Fields because deleted (and even erased)
				// fields still come back in the query and cannot be filtered
				//addObjectToolingType(SF_INCLUDE_CUSTOM_FIELDS, "CustomField");
				addType(SF_INCLUDE_RECORD_TYPES, "RecordType");
				// The following objects don't properly support managed package
				// retrieves, so don't allow the managed package components to be
				// included for now
				includeManagedPackages = false;
				addType(SF_INCLUDE_BUSINESS_PROCESSES, "BusinessProcess");
				addType(SF_INCLUDE_FIELD_SETS, "FieldSet");
				addType(SF_INCLUDE_LIST_VIEWS, "ListView", SF_INCLUDE_LIST_VIEWS_PREFIX);
				addType(SF_INCLUDE_SHARING_REASONS, "SharingReason");
				addType(SF_INCLUDE_VALIDATION_RULES, "ValidationRule");
				addType(SF_INCLUDE_WEBLINKS, "WebLink");
			}
			// Finished objects, so exclude managed packages
			includeManagedPackages = false;
			
			addType(SF_INCLUDE_OBJECT_TRANSLATIONS, "CustomObjectTranslation");
			addType(SF_INCLUDE_EXTERNAL_DATA_SOURCES, "ExternalDataSource");
			addObjectToolingType(SF_INCLUDE_LAYOUTS, "Layout");
			addType(SF_INCLUDE_PUBLISHER_ACTIONS, "QuickAction");
			addType(SF_INCLUDE_ACTION_LINK_GROUP_TEMPLATES, "ActionLinkGroupTemplate");

			// Reporting
			addType(SF_INCLUDE_ANALYTIC_SNAPSHOTS, "AnalyticSnapshot");
			addFolderType(SF_INCLUDE_DASHBOARDS, SF_INCLUDE_DASHBOARDS_FOLDER_PREFIX, null,
					"Dashboard", "Dashboard", "FolderId");
			addFolderType(SF_INCLUDE_REPORTS, SF_INCLUDE_REPORTS_FOLDER_PREFIX, SF_INCLUDE_REPORTS_UNFILED_PUBLIC,
					"Report", "Report", "OwnerId");
			addType(SF_INCLUDE_REPORT_TYPES, "ReportType");


			// Security/Admin Related
			addType(SF_INCLUDE_PROFILES, "Profile", SF_INCLUDE_PROFILES_PREFIX);
			addType(SF_INCLUDE_PERMISSION_SETS, "PermissionSet");
			addType(SF_INCLUDE_ROLES, "Role");
			addType(SF_INCLUDE_GROUPS, "Group");
			addType(SF_INCLUDE_QUEUES, "Queue");
			addType(SF_INCLUDE_TERRITORIES, "Territory");
			addType(SF_INCLUDE_AUTH_PROVIDERS, "AuthProvider");
			addType(SF_INCLUDE_REMOTE_SITE_SETTINGS, "RemoteSiteSetting");
			addType(SF_INCLUDE_SAML_SSO_CONFIGS, "SamlSsoConfig");
			addType(SF_INCLUDE_CUSTOM_PERMISSIONS, "CustomPermission");
			addType(SF_INCLUDE_MATCHING_RULES, "MatchingRule");
			addType(SF_INCLUDE_NAMED_CREDENTIALS, "NamedCredential");
			addType(SF_INCLUDE_PATH_ASSISTANTS, "PathAssistant");
			addType(SF_INCLUDE_SHARING_RULES, "SharingRules");
			addType(SF_INCLUDE_SYNONYM_DICTIONARY, "SynonymDictionary");

			// Service Types
			addType(SF_INCLUDE_CALL_CENTERS, "CallCenter");
			addType(SF_INCLUDE_DATA_CATEGORY_GROUPS, "DataCategoryGroup");
			addType(SF_INCLUDE_ENTITLEMENT_TEMPLATES, "EntitlementTemplate");
			addType(SF_INCLUDE_LIVE_CHAT_AGENT_CONFIGS, "LiveChatAgentConfig");
			addType(SF_INCLUDE_LIVE_CHAT_BUTTONS, "LiveChatButtons");
			addType(SF_INCLUDE_LIVE_CHAT_DEPLOYMENTS, "LiveChatDeployments");
			addType(SF_INCLUDE_MILESTONE_TYPES, "MilestoneType");
			addType(SF_INCLUDE_SKILLS, "Skill");

			// Settings
			if (getPropertyBoolean(SF_INCLUDE_ACCOUNT_SETTINGS)) {
				addTypeMember("Settings", "Account");
			}
			if (getPropertyBoolean(SF_INCLUDE_ACTIVITIES_SETTINGS)) {
				addTypeMember("Settings", "Activities");
			}
			if (getPropertyBoolean(SF_INCLUDE_ADDRESS_SETTINGS)) {
				addTypeMember("Settings", "Address");
			}
			if (getPropertyBoolean(SF_INCLUDE_BUSINESS_HOURS_SETTINGS)) {
				addTypeMember("Settings", "BusinessHours");
			}
			if (getPropertyBoolean(SF_INCLUDE_CASE_SETTINGS)) {
				addTypeMember("Settings", "Case");
			}
			if (getPropertyBoolean(SF_INCLUDE_CHATTER_ANSWERS_SETTINGS)) {
				addTypeMember("Settings", "ChatterAnswersSettings");
			}
			if (getPropertyBoolean(SF_INCLUDE_COMPANY_SETTINGS)) {
				addTypeMember("Settings", "Company");
			}
			if (getPropertyBoolean(SF_INCLUDE_CONTRACT_SETTINGS)) {
				addTypeMember("Settings", "Contract");
			}
			if (getPropertyBoolean(SF_INCLUDE_ENTITLEMENT_SETTINGS)) {
				addTypeMember("Settings", "Entitlement");
			}
			if (getPropertyBoolean(SF_INCLUDE_FORECASTING_SETTINGS)) {
				addTypeMember("Settings", "Forecasting");
			}
			if (getPropertyBoolean(SF_INCLUDE_IDEAS_SETTINGS)) {
				addTypeMember("Settings", "Ideas");
			}
			if (getPropertyBoolean(SF_INCLUDE_KNOWLEDGE_SETTINGS)) {
				addTypeMember("Settings", "Knowledge");
			}
			if (getPropertyBoolean(SF_INCLUDE_LEAD_CONVERT_SETTINGS)) {
				addTypeMember("Settings", "LeadConvert");
			}
			if (getPropertyBoolean(SF_INCLUDE_LIVE_AGENT_SETTINGS)) {
				addTypeMember("Settings", "LiveAgent");
			}
			if (getPropertyBoolean(SF_INCLUDE_MOBILE_SETTINGS)) {
				addTypeMember("Settings", "Mobile");
			}
			if (getPropertyBoolean(SF_INCLUDE_NAME_SETTINGS)) {
				addTypeMember("Settings", "Name");
			}
			if (getPropertyBoolean(SF_INCLUDE_OPPORTUNITY_SETTINGS)) {
				addTypeMember("Settings", "Opportunity");
			}
			if (getPropertyBoolean(SF_INCLUDE_ORDER_SETTINGS)) {
				addTypeMember("Settings", "Order");
			}
			if (getPropertyBoolean(SF_INCLUDE_PATH_ASSISTANT_SETTINGS)) {
				addTypeMember("Settings", "PathAssistant");
			}
			if (getPropertyBoolean(SF_INCLUDE_PRODUCT_SETTINGS)) {
				addTypeMember("Settings", "Product");
			}
			if (getPropertyBoolean(SF_INCLUDE_QUOTE_SETTINGS)) {
				addTypeMember("Settings", "Quote");
			}
			if (getPropertyBoolean(SF_INCLUDE_SECURITY_SETTINGS)) {
				addTypeMember("Settings", "Security");
			}

			//Sites
			addType(SF_INCLUDE_COMMUNITIES, "Community");
			addType(SF_INCLUDE_SITES, "CustomSite");
			addType(SF_INCLUDE_NETWORKS, "Network");
			addType(SF_INCLUDE_PORTALS, "Portal");
			addType(SF_INCLUDE_SHARING_SETS, "SharingSet");
			addType(SF_INCLUDE_SITE_DOT_COMS, "SiteDotCom");

			// Workflows
			addType(SF_INCLUDE_APPROVAL_PROCESSES, "ApprovalProcess");
			addType(SF_INCLUDE_ASSIGNMENT_RULES, "AssignmentRule");
			addType(SF_INCLUDE_AUTO_RESPONSE_RULES, "AutoResponseRules");
			addFolderType(SF_INCLUDE_EMAILS, SF_INCLUDE_EMAILS_FOLDER_PREFIX, SF_INCLUDE_EMAILS_UNFILED_PUBLIC,
					"Email", "EmailTemplate", "FolderId");
			addType(SF_INCLUDE_LETTERHEADS, "Letterhead");
			addType(SF_INCLUDE_POST_TEMPLATES, "PostTemplate");
			addType(SF_INCLUDE_WORKFLOWS, "Workflow"); // Backwards compatibility
			addType(SF_INCLUDE_WORKFLOW_ALERTS, "WorkflowAlert");
			addType(SF_INCLUDE_WORKFLOW_FIELD_UPDATES, "WorkflowFieldUpdate");
			addType(SF_INCLUDE_WORKFLOW_FLOW_ACTIONS, "WorkflowFlowAction");
			addType(SF_INCLUDE_WORKFLOW_KNOWLEDGE_PUBLISHES, "WorkflowKnowledgePublish");
			addType(SF_INCLUDE_WORKFLOW_OUTBOUND_MESSAGES, "WorkflowOutboundMessage");
			addType(SF_INCLUDE_WORKFLOW_RULES, "WorkflowRule");
			addType(SF_INCLUDE_WORKFLOW_TASKS, "WorkflowTask");


			PackageUtilities.createPackageXmlFile(packageFileName, asOfVersion, typesMap);
		}
		catch (IOException ioe)
		{
			throw new BuildException("Error trying to write package.xml file");
		}
	}

	protected void addTypeMember(String typeName, String memberName) {
		if (!ignoreList.contains(typeName + "." + memberName)) {
			List<String> memberList = typesMap.get(typeName);
			if (memberList == null) {
				memberList = new ArrayList<String>();
				typesMap.put(typeName, memberList);
			}
			memberList.add(memberName);
		}
	}

	protected void addTypeFromListMetadata(String typeName, String memberPrefix) throws IOException {
		try {
			long startTime = System.nanoTime();
			MetadataConnection metaConnection = getMetadataConnection();
			ListMetadataQuery query = new ListMetadataQuery();
			query.setType(typeName);

			FileProperties[] properties = metaConnection.listMetadata(new ListMetadataQuery[] {query}, asOfVersion);
			for (FileProperties p : properties) {
				String namespace = p.getNamespacePrefix();
				if (includeManagedPackages || namespace == null || namespace.trim().length() == 0) {
					String fullName = p.getFullName();
					String matchName = fullName;
					String[] splitNames = fullName.split("\\.");
					if (splitNames.length == 2) {
						// This is a subset of Object so it include the Object name first and we just want the component name
						String objectName = splitNames[0];
						if (!objectNameEnumIdMap.containsKey(objectName)) {
							// We are not including this object so don't include this component
							continue;
						}
						matchName = splitNames[1];
					}
					if (memberPrefix == null || memberPrefix.trim().length() == 0 ||
							matchName.startsWith(memberPrefix)) {
						addTypeMember(typeName, fullName);
					}
				}
			}
			long elapsedTime = System.nanoTime() - startTime;
			log("Added " + typeName + " from List Metadata [" + TimeUnit.NANOSECONDS.toMillis(elapsedTime) + " ms]");
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new BuildException("Exception trying to add " + typeName + " components.");
		}
	}

	protected void addType(String propertyName, String typeName) throws IOException{
		addType(propertyName, typeName, null);
	}

	protected void addType(String propertyName, String typeName, String memberPrefixPropertyName) throws IOException{
		String memberPrefix = null;
		if (memberPrefixPropertyName != null) {
			memberPrefix = getProject().getProperty(memberPrefixPropertyName);
		}

		if (getPropertyBoolean(propertyName)) {
			addTypeFromListMetadata(typeName, memberPrefix);
		}
	}
	
	protected void loadObjects() {
		try {
			long startTime = System.nanoTime();
			// We need to use List Metadata so we can get a complete list of both
			// Standard and Custom Objects
			objectNames = new ArrayList<String>();
			objectNameEnumIdMap = new HashMap<String, String>();
			ListMetadataQuery query = new ListMetadataQuery();
			query.setType("CustomObject");
			
			FileProperties[] properties = metadataConnection.listMetadata(new ListMetadataQuery[] {query}, asOfVersion);
			for (FileProperties p : properties) {
				String namespace = p.getNamespacePrefix();
				if (includeManagedPackages || namespace == null || namespace.trim().length() == 0) {
					String objectName = p.getFullName();
					String objectEnumId = p.getId();
					if (!objectName.endsWith("__c")) {
						// Standard object so use name as enum
						objectEnumId = objectName;
					}
					objectNames.add(objectName);
					objectNameEnumIdMap.put(objectName,  objectEnumId);
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
			addTypeMember("CustomObject", objectName);
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
					if (includeManagedPackages || namespace == null || namespace.trim().length() == 0) {
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
							if (so instanceof CustomField) {
								CustomField cf = (CustomField) so;
								String namespace = cf.getNamespacePrefix();
								if (namespace != null && namespace.trim().length() > 0) {
									fullName += "." + namespace + "__" + cf.getDeveloperName() + "__c";
								} else {
									fullName += "." + ((CustomField) so).getDeveloperName() + "__c";
								}
							}
							if (so instanceof Layout) {
								Layout l = (Layout) so;
								String namespace = l.getNamespacePrefix();
								if (namespace != null && namespace.trim().length() > 0) {
									fullName += "-" + namespace + "__" + l.getName();
								} else {
									fullName += "-" + l.getName();
								}
								// Need to encode Layout name since it is not a DeveloperName
								fullName = encodePackageMember(fullName);
							}
							addTypeMember(typeName, fullName);
						}
					}
				}
			}
			long elapsedTime = System.nanoTime() - startTime;
			log("Added " + typeName + " from Tooling query [" + TimeUnit.NANOSECONDS.toMillis(elapsedTime) + " ms]");
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
			String query = "select Id, Name, NamespacePrefix from " + typeName;
			if (includeManagedPackages) {
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
							addTypeMember(typeName, fullName);
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
			log("Added " + typeName + " from Tooling query [" + TimeUnit.NANOSECONDS.toMillis(elapsedTime) + " ms]");
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new BuildException("Exception trying to add " + typeName + " components.");
		}
	}
	
	protected void addFolderType(String includePropertyName, String includeFolderPrefixPropertyName,
			String includeUnfiledPublicPropertyName, String folderType, String objectName, String objectFolderFieldName) throws IOException {
		if (getPropertyBoolean(includePropertyName)) {
			try {
				long startTime = System.nanoTime();
				PartnerConnection connection = getPartnerConnection();

				String folderPrefix = getProject().getProperty(includeFolderPrefixPropertyName);
				SObject[] folders = getFolders(folderType, folderPrefix);
				for (SObject folder : folders) {
					String folderId = folder.getId();
					String folderName = (String) folder.getField("DeveloperName");
					addTypeMember(objectName, folderName);

					String soql = "select Id, DeveloperName from " + objectName + " where " +
							objectFolderFieldName + "='" + folderId + "' ";
					QueryResult qr = connection.query(soql);
					SObject[] sobjects = qr.getRecords();
					for (SObject so : sobjects) {
						String developerName = (String) so.getField("DeveloperName");
						addTypeMember(objectName, folderName + "/" + developerName);
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
						addTypeMember(objectName, "unfiled$public/" + developerName);
					}
				}
				long elapsedTime = System.nanoTime() - startTime;
				log("Added " + objectName + " from SOQL query [" + TimeUnit.NANOSECONDS.toMillis(elapsedTime) + " ms]");
			} catch (Exception ex) {
				ex.printStackTrace();
				throw new BuildException("Exception retrieving folder list.");
			}
		}
	}

	protected SObject[] getFolders(String folderType, String folderPrefix) {
		SObject[] folders = new SObject[] {};

		try {
			String soql = "select Id, DeveloperName from Folder where NamespacePrefix = null AND DeveloperName != null " +
					" and Type='" + folderType + "' ";

			PartnerConnection connection = getPartnerConnection();
			QueryResult qr = connection.query(soql);
			SObject[] soqlFolders = qr.getRecords();
			List<SObject> approvedFolders = new ArrayList<SObject>();
			for (SObject so : soqlFolders) {
				String developerName = (String) so.getField("DeveloperName");
				if (folderPrefix == null || folderPrefix.length() == 0 ||
						(developerName != null && developerName.startsWith(folderPrefix))) {
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

	protected void loadIgnoreValues() {
		ignoreList.clear();

		@SuppressWarnings("unchecked")
		Hashtable<String, String> projectProperties = (Hashtable<String, String>) getProject().getProperties();
		for (String propertyKey : projectProperties.keySet()) {
			if (propertyKey != null && propertyKey.startsWith(SF_IGNORE_PREFIX)) {
				// This is an ignore property
				String ignoreProperty = projectProperties.get(propertyKey);
				if (ignoreProperty != null && ignoreProperty.trim().length() > 0) {
					for (String ignore : ignoreProperty.split(";")) {
						ignoreList.add(ignore);
					}
				}
			}
		}
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
			testProject.setProperty(SF_INCLUDE_CLASSES, "yEs");
			testProject.setProperty(SF_INCLUDE_COMPONENTS, "trUE");
			testProject.setProperty(SF_INCLUDE_PAGES, "yEs");
			testProject.setProperty(SF_INCLUDE_TRIGGERS, "trUE");
			testProject.setProperty(SF_INCLUDE_APPLICATIONS, "yEs");
			testProject.setProperty(SF_INCLUDE_LABELS, "yEs");
			testProject.setProperty(SF_INCLUDE_BUSINESS_PROCESSES, "trUE");
			testProject.setProperty(SF_INCLUDE_CUSTOM_FIELDS, "trUE");
			testProject.setProperty(SF_INCLUDE_FIELD_SETS, "trUE");
			testProject.setProperty(SF_INCLUDE_LIST_VIEWS, "trUE");
			testProject.setProperty(SF_INCLUDE_LIST_VIEWS_PREFIX, "A_");
			testProject.setProperty(SF_INCLUDE_RECORD_TYPES, "trUE");
			testProject.setProperty(SF_INCLUDE_SHARING_REASONS, "trUE");
			testProject.setProperty(SF_INCLUDE_VALIDATION_RULES, "trUE");
			testProject.setProperty(SF_INCLUDE_WEBLINKS, "trUE");
			testProject.setProperty(SF_INCLUDE_OBJECT_TRANSLATIONS, "yEs");
			testProject.setProperty(SF_INCLUDE_CUSTOM_PAGE_WEBLINKS, "yEs");
			testProject.setProperty(SF_INCLUDE_SITES, "yEs");
			testProject.setProperty(SF_INCLUDE_TABS, "yEs");
			testProject.setProperty(SF_INCLUDE_DASHBOARDS, "yEs");
			testProject.setProperty(SF_INCLUDE_DASHBOARDS_FOLDER_PREFIX, "Test_");
			testProject.setProperty(SF_INCLUDE_DATA_CATEGORY_GROUPS, "yEs");
			testProject.setProperty(SF_INCLUDE_DOCUMENTS, "yEs");
			testProject.setProperty(SF_INCLUDE_DOCUMENTS_FOLDER_PREFIX, "");
			testProject.setProperty(SF_INCLUDE_EMAILS, "yEs");
			testProject.setProperty(SF_INCLUDE_EMAILS_FOLDER_PREFIX, "");
			testProject.setProperty(SF_INCLUDE_EMAILS_UNFILED_PUBLIC, "true");
			testProject.setProperty(SF_INCLUDE_HOME_PAGE_COMPONENTS, "yEs");
			testProject.setProperty(SF_INCLUDE_HOME_PAGE_LAYOUTS, "yEs");
			testProject.setProperty(SF_INCLUDE_LAYOUTS, "yEs");
			testProject.setProperty(SF_INCLUDE_LETTERHEADS, "yEs");
			testProject.setProperty(SF_INCLUDE_PROFILES, "yEs");
			testProject.setProperty(SF_INCLUDE_PROFILES_PREFIX, "%28");
			testProject.setProperty(SF_INCLUDE_REMOTE_SITE_SETTINGS, "yEs");
			testProject.setProperty(SF_INCLUDE_REPORTS, "yEs");
			testProject.setProperty(SF_INCLUDE_REPORTS_FOLDER_PREFIX, "");
			testProject.setProperty(SF_INCLUDE_REPORTS_UNFILED_PUBLIC, "true");
			testProject.setProperty(SF_INCLUDE_REPORT_TYPES, "yEs");
			testProject.setProperty(SF_INCLUDE_SCONTROLS, "yEs");
			testProject.setProperty(SF_INCLUDE_STATIC_RESOURCES, "yEs");
			testProject.setProperty(SF_INCLUDE_WORKFLOWS, "yEs");

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
