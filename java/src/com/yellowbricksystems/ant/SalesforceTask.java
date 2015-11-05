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

	SalesforceTask
	
	This class is the ancestor class for the custom Salesforce tasks and is used
	to centralize constants and connection information.
	
	Author:  Jeff Bohanek (jeff@yellowbricksystems.com)
 */
package com.yellowbricksystems.ant;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.soap.tooling.ToolingConnection;
import com.sforce.ws.ConnectorConfig;

public class SalesforceTask extends Task {

	public static final String SF_USER_PROPERTY_NAME = "sf.username";
	public static final String SF_PASSWORD_PROPERTY_NAME = "sf.password";
	public static final String SF_SERVER_URL_PROPERTY_NAME = "sf.serverurl";

	protected PartnerConnection partnerConnection = null;
	protected MetadataConnection metadataConnection = null;
	protected ToolingConnection toolingConnection = null;
	
	protected double asOfVersion = 35.0;

	// Property Names for salesforce.properties file to control metadata that is
	// retrieved/deployed
	
	// Apex
	public static final String SF_INCLUDE_CLASSES = "sf.includeClasses";
	public static final String SF_INCLUDE_COMPONENTS = "sf.includeComponents";
	public static final String SF_INCLUDE_PAGES = "sf.includePages";
	public static final String SF_INCLUDE_TRIGGERS = "sf.includeTriggers";
	public static final String SF_INCLUDE_FLEXI_PAGES = "sf.includeFlexiPages";
	public static final String SF_INCLUDE_FLOWS = "sf.includeFlows";
	public static final String SF_INCLUDE_SCONTROLS = "sf.includeScontrols";
	public static final String SF_INCLUDE_STATIC_RESOURCES = "sf.includeStaticResources";
	public static final String SF_INCLUDE_AURA_DEFINITION_BUNDLES = "sf.includeAuraDefinitionBundles";
	public static final String SF_INCLUDE_PLATFORM_CACHE_PARTITIONS = "sf.includePlatformCachePartitions";
	
	// App
	public static final String SF_INCLUDE_APP_MENUS = "sf.includeAppMenus";
	public static final String SF_INCLUDE_CONNECTED_APPS = "sf.includeConnectedApp";
	public static final String SF_INCLUDE_APPLICATIONS = "sf.includeApplications";
	public static final String SF_INCLUDE_APPLICATION_COMPONENTS = "sf.includeApplicationComponents";
	public static final String SF_INCLUDE_LABELS = "sf.includeLabels";
	public static final String SF_INCLUDE_TABS = "sf.includeTabs";
	public static final String SF_INCLUDE_DOCUMENTS = "sf.includeDocuments";
	public static final String SF_INCLUDE_DOCUMENTS_FOLDER_PREFIX = "sf.includeDocumentsFolderPrefix";
	public static final String SF_INCLUDE_CUSTOM_PAGE_WEBLINKS = "sf.includeCustomPageWeblinks";
	public static final String SF_INCLUDE_HOME_PAGE_COMPONENTS = "sf.includeHomePageComponents";
	public static final String SF_INCLUDE_HOME_PAGE_LAYOUTS = "sf.includeHomePageLayouts";
	public static final String SF_INCLUDE_INSTALLED_PACKAGES = "sf.includeInstalledPackages";
	public static final String SF_INCLUDE_TRANSLATIONS = "sf.includeTranslations";
	
	//Object
	public static final String SF_INCLUDE_ACTION_OVERRIDES = "sf.includeActionOverrides";
	public static final String SF_INCLUDE_BUSINESS_PROCESSES = "sf.includeBusinessProcesses";
	public static final String SF_INCLUDE_COMPACT_LAYOUTS = "sf.includeCompactLayouts";
	public static final String SF_INCLUDE_CUSTOM_FIELDS = "sf.includeCustomFields";
	public static final String SF_INCLUDE_FIELD_SETS = "sf.includeFieldSets";
	public static final String SF_INCLUDE_LIST_VIEWS = "sf.includeListViews";
	public static final String SF_INCLUDE_LIST_VIEWS_PREFIX = "sf.includeListViewsPrefix";
	public static final String SF_INCLUDE_RECORD_TYPES = "sf.includeRecordTypes";
	public static final String SF_INCLUDE_SEARCH_LAYOUTS = "sf.includeSearchLayouts";
	public static final String SF_INCLUDE_SHARING_REASONS = "sf.includeSharingReasons";
	public static final String SF_INCLUDE_SHARING_RECALCULATIONS = "sf.includeSharingRecalculations";
	public static final String SF_INCLUDE_VALIDATION_RULES = "sf.includeValidationRules";
	public static final String SF_INCLUDE_WEBLINKS = "sf.includeWeblinks";
	
	public static final String SF_INCLUDE_OBJECT_TRANSLATIONS = "sf.includeObjectTranslations";
	public static final String SF_INCLUDE_EXTERNAL_DATA_SOURCES = "sf.includeExternalDataSources";
	public static final String SF_INCLUDE_LAYOUTS = "sf.includeLayouts";
	public static final String SF_INCLUDE_PUBLISHER_ACTIONS = "sf.includePublisherActions";
	public static final String SF_INCLUDE_ACTION_LINK_GROUP_TEMPLATES = "sf.includeActionLinkGroupTemplates";
	
	// Reporting
	public static final String SF_INCLUDE_ANALYTIC_SNAPSHOTS = "sf.includeAnalyticSnapshots";
	public static final String SF_INCLUDE_DASHBOARDS = "sf.includeDashboards";
	public static final String SF_INCLUDE_DASHBOARDS_FOLDER_PREFIX = "sf.includeDashboardsFolderPrefix";
	public static final String SF_INCLUDE_REPORTS = "sf.includeReports";
	public static final String SF_INCLUDE_REPORTS_FOLDER_PREFIX = "sf.includeReportsFolderPrefix";
	public static final String SF_INCLUDE_REPORTS_UNFILED_PUBLIC = "sf.includeReportsUnfiledPublic";
	public static final String SF_INCLUDE_REPORT_TYPES = "sf.includeReportTypes";
	
	// Security/Admin
	public static final String SF_INCLUDE_PROFILES = "sf.includeProfiles";
	public static final String SF_INCLUDE_PROFILES_PREFIX = "sf.includeProfilesPrefix";
	public static final String SF_INCLUDE_PERMISSION_SETS = "sf.includePermissionSets";
	public static final String SF_INCLUDE_ROLES = "sf.includeRoles";
	public static final String SF_INCLUDE_GROUPS = "sf.includeGroups";
	public static final String SF_INCLUDE_QUEUES = "sf.includeQueues";
	public static final String SF_INCLUDE_TERRITORIES = "sf.includeTerritories";
	public static final String SF_INCLUDE_AUTH_PROVIDERS = "sf.includeAuthProviders";
	public static final String SF_INCLUDE_REMOTE_SITE_SETTINGS = "sf.includeRemoteSiteSettings";
	public static final String SF_INCLUDE_SAML_SSO_CONFIGS = "sf.includeSamlSsoConfigs";
	public static final String SF_INCLUDE_CUSTOM_PERMISSIONS = "sf.includeCustomPermissions";
	public static final String SF_INCLUDE_MATCHING_RULES = "sf.includeMatchingRules";
	public static final String SF_INCLUDE_NAMED_CREDENTIALS = "sf.includeNamedCredentials";
	public static final String SF_INCLUDE_PATH_ASSISTANTS = "sf.includePathAssistants";
	public static final String SF_INCLUDE_SHARING_RULES = "sf.includeSharingRules";
	public static final String SF_INCLUDE_SYNONYM_DICTIONARY = "sf.includeSynonymDictionary";
	// Service
	public static final String SF_INCLUDE_CALL_CENTERS = "sf.includeCallCenters";
	public static final String SF_INCLUDE_DATA_CATEGORY_GROUPS = "sf.includeDataCategoryGroups";
	public static final String SF_INCLUDE_ENTITLEMENT_TEMPLATES = "sf.includeEntitlementTemplates";
	public static final String SF_INCLUDE_LIVE_CHAT_AGENT_CONFIGS = "sf.includeLiveChatAgentConfigs";
	public static final String SF_INCLUDE_LIVE_CHAT_BUTTONS = "sf.includeLiveChatButtons";
	public static final String SF_INCLUDE_LIVE_CHAT_DEPLOYMENTS = "sf.includeLiveChatDeployments";
	public static final String SF_INCLUDE_MILESTONE_TYPES = "sf.includeMilestoneTypes";
	public static final String SF_INCLUDE_SKILLS = "sf.includeSkills";
	
	// Settings
	public static final String SF_INCLUDE_ACCOUNT_SETTINGS = "sf.includeAccountSettings";
	public static final String SF_INCLUDE_ACTIVITIES_SETTINGS = "sf.includeActivitiesSettings";
	public static final String SF_INCLUDE_ADDRESS_SETTINGS = "sf.includeAddressSettings";
	public static final String SF_INCLUDE_BUSINESS_HOURS_SETTINGS = "sf.includeBusinessHoursSettings";
	public static final String SF_INCLUDE_CASE_SETTINGS = "sf.includeCaseSettings";
	public static final String SF_INCLUDE_CHATTER_ANSWERS_SETTINGS = "sf.includeChatterAnswersSettings";
	public static final String SF_INCLUDE_COMPANY_SETTINGS = "sf.includeCompanySettings";
	public static final String SF_INCLUDE_CONTRACT_SETTINGS = "sf.includeContractSettings";
	public static final String SF_INCLUDE_ENTITLEMENT_SETTINGS = "sf.includeEntitlementSettings";
	public static final String SF_INCLUDE_FORECASTING_SETTINGS = "sf.includeForecastingSettings";
	public static final String SF_INCLUDE_IDEAS_SETTINGS = "sf.includeIdeasSettings";
	public static final String SF_INCLUDE_KNOWLEDGE_SETTINGS = "sf.includeKnowledgeSettings";
	public static final String SF_INCLUDE_LEAD_CONVERT_SETTINGS = "sf.includeLeadConvertSettings";
	public static final String SF_INCLUDE_LIVE_AGENT_SETTINGS = "sf.includeLiveAgentSettings";
	public static final String SF_INCLUDE_MOBILE_SETTINGS = "sf.includeMobileSettings";
	public static final String SF_INCLUDE_NAME_SETTINGS = "sf.includeNameSettings";
	public static final String SF_INCLUDE_OPPORTUNITY_SETTINGS = "sf.includeOpportunitySettings";
	public static final String SF_INCLUDE_ORDER_SETTINGS = "sf.includeOrderSettings";
	public static final String SF_INCLUDE_PATH_ASSISTANT_SETTINGS = "sf.includePathAssistantSettings";
	public static final String SF_INCLUDE_PRODUCT_SETTINGS = "sf.includeProductSettings";
	public static final String SF_INCLUDE_QUOTE_SETTINGS = "sf.includeQuoteSettings";
	public static final String SF_INCLUDE_SECURITY_SETTINGS = "sf.includeSecuritySettings";
	
	// Sites
	public static final String SF_INCLUDE_COMMUNITIES = "sf.includeCommunities";
	public static final String SF_INCLUDE_SITES = "sf.includeSites";
	public static final String SF_INCLUDE_NETWORKS = "sf.includeNetworks";
	public static final String SF_INCLUDE_PORTALS = "sf.includePortals";
	public static final String SF_INCLUDE_SHARING_SETS = "sf.includeSharingSets";
	public static final String SF_INCLUDE_SITE_DOT_COMS = "sf.includeSiteDotComs";
	
	// Workflows
	public static final String SF_INCLUDE_APPROVAL_PROCESSES = "sf.includeApprovalProcesses";
	public static final String SF_INCLUDE_ASSIGNMENT_RULES = "sf.includeAssignmentRules";
	public static final String SF_INCLUDE_AUTO_RESPONSE_RULES = "sf.includeAutoResponseRules";
	public static final String SF_INCLUDE_EMAILS = "sf.includeEmails";
	public static final String SF_INCLUDE_EMAILS_FOLDER_PREFIX = "sf.includeEmailsFolderPrefix";
	public static final String SF_INCLUDE_EMAILS_UNFILED_PUBLIC = "sf.includeEmailsUnfiledPublic";
	public static final String SF_INCLUDE_LETTERHEADS = "sf.includeLetterheads";
	public static final String SF_INCLUDE_POST_TEMPLATES = "sf.includePostTemplates";
	public static final String SF_INCLUDE_WORKFLOWS = "sf.includeWorkflows";
	public static final String SF_INCLUDE_WORKFLOW_ALERTS = "sf.includeWorkflowAlerts";
	public static final String SF_INCLUDE_WORKFLOW_FIELD_UPDATES = "sf.includeWorkflowFieldUpdates";
	public static final String SF_INCLUDE_WORKFLOW_FLOW_ACTIONS = "sf.includeWorkflowFlowActions";
	public static final String SF_INCLUDE_WORKFLOW_KNOWLEDGE_PUBLISHES = "sf.includeWorkflowKnowledgePublishes";
	public static final String SF_INCLUDE_WORKFLOW_OUTBOUND_MESSAGES = "sf.includeWorkflowOutboundMessages";
	public static final String SF_INCLUDE_WORKFLOW_RULES = "sf.includeWorkflowRules";
	public static final String SF_INCLUDE_WORKFLOW_TASKS = "sf.includeWorkflowTasks";
	

	protected PartnerConnection getPartnerConnection() {
		if (partnerConnection == null) {
			initSalesforceConnection();
		}
		return partnerConnection;
	}

	protected MetadataConnection getMetadataConnection() {
		if (metadataConnection == null) {
			initSalesforceConnection();
		}
		return metadataConnection;
	}

	protected ToolingConnection getToolingConnection() {
		if (toolingConnection == null) {
			initSalesforceConnection();
		}
		return toolingConnection;
	}

	@Override
	public void init() throws BuildException {
		super.init();
	}
	
	/**
	 * getPropertyBoolean(String propertyName)
	 * This method will return true if the property is set to either TRUE or YES (case insensitive), otherwise
	 * it returns false.
	 * 
	 * @param propertyName
	 * @return
	 */
	protected boolean getPropertyBoolean(String propertyName) {
		boolean propertyBoolean = false;
		String propertyString = getProject().getProperty(propertyName);
		if (propertyString != null && 
			(propertyString.toUpperCase().equals("YES") || propertyString.toUpperCase().equals("TRUE"))) {
			propertyBoolean = true;
		}
		return propertyBoolean;
	}
	
	protected void initSalesforceConnection() {
		try {
			String username = getProject().getProperty(SF_USER_PROPERTY_NAME);
			String password = getProject().getProperty(SF_PASSWORD_PROPERTY_NAME);
			String serverUrl = getProject().getProperty(SF_SERVER_URL_PROPERTY_NAME);

			if (username == null || username.length() < 1) {
				throw new BuildException("The " + SF_USER_PROPERTY_NAME + " Salesforce username property is not set.");
			}
			if (password == null || password.length() < 1) {
				throw new BuildException("The " + SF_PASSWORD_PROPERTY_NAME + " Salesforce password property is not set.");
			}
			if (serverUrl == null || serverUrl.length() < 1) {
				throw new BuildException("The " + SF_SERVER_URL_PROPERTY_NAME + " Salesforce server url property is not set.");
			}

			ConnectorConfig config = new ConnectorConfig();
			config.setUsername(username);
			config.setPassword(password);
			config.setAuthEndpoint(serverUrl);

			partnerConnection = new PartnerConnection(config);
			
			String serviceEndpoint = config.getServiceEndpoint();
			
			ConnectorConfig metadataConfig = new ConnectorConfig();
			metadataConfig.setSessionId(partnerConnection.getSessionHeader().getSessionId());
			String metaEndpoint = serviceEndpoint.replace("/u/", "/m/");
			metadataConfig.setServiceEndpoint(metaEndpoint);

			metadataConnection = new MetadataConnection(metadataConfig);

			ConnectorConfig toolingConfig = new ConnectorConfig();
			toolingConfig.setSessionId(partnerConnection.getSessionHeader().getSessionId());
			String toolingEndpoint = serviceEndpoint.replace("/u/", "/T/");
			toolingConfig.setServiceEndpoint(toolingEndpoint);

			toolingConnection = new ToolingConnection(toolingConfig);
			
			int lastSlash = serverUrl.lastIndexOf("/");
			if (lastSlash > 0) {
				String asOfVersionString = serverUrl.substring(lastSlash + 1);
				asOfVersion = Double.parseDouble(asOfVersionString);
			}

		} catch (Exception ex) {
			ex.printStackTrace();
			throw new BuildException("Error trying to connect to Salesforce.");
		}
	}

}
