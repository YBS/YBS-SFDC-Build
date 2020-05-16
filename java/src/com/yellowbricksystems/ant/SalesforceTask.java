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

import java.util.HashSet;
import java.util.Hashtable;
import java.util.concurrent.TimeUnit;

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
	
	protected double asOfVersion = 48.0;

	public static final String SF_IGNORE_PREFIX = "sf.ignore";
	public static final String SF_PACKAGE_IGNORE_PREFIX = "sf.package.ignore";

	protected HashSet<String> ignoreList = new HashSet<String>();
	protected HashSet<String> packageIgnoreList = new HashSet<String>();
	
	protected HashSet<String> managedPackageTypes = new HashSet<String>();
	
	// Property Names for salesforce.properties file to control metadata that is
	// retrieved/deployed
	
	public static final String SF_INCLUDE_MANAGED_PACKAGE_TYPES = "sf.includeManagedPackageTypes";
	
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
	public static final String SF_INCLUDE_EMAIL_SERVICES_FUNCTIONS = "sf.includeEmailServicesFunctions";
	
	// App
	public static final String SF_INCLUDE_APP_MENUS = "sf.includeAppMenus";
	public static final String SF_INCLUDE_CONNECTED_APPS = "sf.includeConnectedApp";
	public static final String SF_INCLUDE_APPLICATIONS = "sf.includeApplications";
	public static final String SF_INCLUDE_APPLICATION_COMPONENTS = "sf.includeApplicationComponents";
	public static final String SF_INCLUDE_LABELS = "sf.includeLabels";
	public static final String SF_INCLUDE_TABS = "sf.includeTabs";
	public static final String SF_INCLUDE_DOCUMENTS = "sf.includeDocuments";
	public static final String SF_INCLUDE_DOCUMENTS_FOLDER_PREFIX = "sf.includeDocumentsFolderPrefix";
	public static final String SF_INCLUDE_DOCUMENTS_FOLDERS = "sf.includeDocumentsFolders";
	public static final String SF_INCLUDE_CUSTOM_PAGE_WEBLINKS = "sf.includeCustomPageWeblinks";
	public static final String SF_INCLUDE_HOME_PAGE_COMPONENTS = "sf.includeHomePageComponents";
	public static final String SF_INCLUDE_HOME_PAGE_LAYOUTS = "sf.includeHomePageLayouts";
	public static final String SF_INCLUDE_INSTALLED_PACKAGES = "sf.includeInstalledPackages";
	public static final String SF_INCLUDE_TRANSLATIONS = "sf.includeTranslations";
	public static final String SF_INCLUDE_CHATTER_EXTENSIONS = "sf.includeChatterExtensions";
	public static final String SF_INCLUDE_LIGHTNING_BOLTS = "sf.includeLightningBolts";
	
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
	public static final String SF_INCLUDE_CUSTOM_METADATA = "sf.includeCustomMetadata";
	public static final String SF_INCLUDE_GLOBAL_VALUE_SETS = "sf.includeGlobalValueSets";
	public static final String SF_INCLUDE_GLOBAL_VALUE_SET_TRANSLATIONS = "sf.includeGlobalValueSetTranslations";
	public static final String SF_INCLUDE_STANDARD_VALUE_SETS = "sf.includeStandardValueSets";
	public static final String SF_INCLUDE_TOPICS_FOR_OBJECTS = "sf.includeTopicsForObjects";
	
	// Reporting/Analytics
	public static final String SF_INCLUDE_ANALYTIC_SNAPSHOTS = "sf.includeAnalyticSnapshots";
	public static final String SF_INCLUDE_DASHBOARDS = "sf.includeDashboards";
	public static final String SF_INCLUDE_DASHBOARDS_FOLDER_PREFIX = "sf.includeDashboardsFolderPrefix";
	public static final String SF_INCLUDE_DASHBOARDS_FOLDERS = "sf.includeDashboardsFolders";
	public static final String SF_INCLUDE_REPORTS = "sf.includeReports";
	public static final String SF_INCLUDE_REPORTS_FOLDER_PREFIX = "sf.includeReportsFolderPrefix";
	public static final String SF_INCLUDE_REPORTS_FOLDERS = "sf.includeReportsFolders";
	public static final String SF_INCLUDE_REPORTS_UNFILED_PUBLIC = "sf.includeReportsUnfiledPublic";
	public static final String SF_INCLUDE_REPORT_TYPES = "sf.includeReportTypes";
	public static final String SF_INCLUDE_ANALYTIC_MAP_CHARTS = "sf.includeAnalyticMapCharts";
	public static final String SF_INCLUDE_WAVE_APPLICATIONS = "sf.includeWaveApplications";
	public static final String SF_INCLUDE_WAVE_DASHBOARDS = "sf.includeWaveDashboards";
	public static final String SF_INCLUDE_WAVE_DATAFLOWS = "sf.includeWaveDataflows";
	public static final String SF_INCLUDE_WAVE_DATASETS = "sf.includeWaveDatasets";
	public static final String SF_INCLUDE_WAVE_LENSES = "sf.includeWaveLenses";
	public static final String SF_INCLDUE_WAVE_TEMPLATE_BUNDLES = "sf.includeWaveTemplateBundles";
	public static final String SF_INCLUDE_WAVE_XMDS = "sf.includeWaveXmds";
	
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
	public static final String SF_INCLUDE_CERTIFICATES = "sf.includeCertificates";
	public static final String SF_INCLUDE_CLEAN_DATA_SERVICES = "sf.includeCleanDataServices";
	public static final String SF_INCLUDE_CORS_WHITELIST_ORIGINS = "sf.includeCORSWhitelistOrigins";
	public static final String SF_INCLUDE_DELEGATE_GROUPS = "sf.includeDelegateGroups";
	public static final String SF_INCLUDE_DUPLICATE_RULES = "sf.includeDuplicateRules";
	public static final String SF_INCLUDE_EXTERNAL_SERVICE_REGISTRATIONS = "sf.includeExternalServiceRegistrations";
	public static final String SF_INCLUDE_PROFILE_PASSWORD_POLICIES = "sf.includeProfilePasswordPolicies";
	public static final String SF_INCLUDE_PROFILE_SESSION_SETTINGS = "sf.includeProfileSessionSettings";
	public static final String SF_INCLUDE_TERRITORIES2 = "sf.includeTerritories2";
	public static final String SF_INCLUDE_TERRITORY2_MODELS = "sf.includeTerritory2Models";
	public static final String SF_INCLUDE_TERRITORY2_RULES = "sf.includeTerritory2Rules";
	public static final String SF_INCLUDE_TERRITORY2_TYPES = "sf.includeTerritory2Types";
	public static final String SF_INCLUDE_TRANSACTION_SECURITY_POLICIES = "sf.includeTransactionSecurityPolicies";
	
	// Service
	public static final String SF_INCLUDE_CALL_CENTERS = "sf.includeCallCenters";
	public static final String SF_INCLUDE_DATA_CATEGORY_GROUPS = "sf.includeDataCategoryGroups";
	public static final String SF_INCLUDE_ENTITLEMENT_TEMPLATES = "sf.includeEntitlementTemplates";
	public static final String SF_INCLUDE_LIVE_CHAT_AGENT_CONFIGS = "sf.includeLiveChatAgentConfigs";
	public static final String SF_INCLUDE_LIVE_CHAT_BUTTONS = "sf.includeLiveChatButtons";
	public static final String SF_INCLUDE_LIVE_CHAT_DEPLOYMENTS = "sf.includeLiveChatDeployments";
	public static final String SF_INCLUDE_MILESTONE_TYPES = "sf.includeMilestoneTypes";
	public static final String SF_INCLUDE_SKILLS = "sf.includeSkills";
	public static final String SF_INCLUDE_CASE_SUBJECT_PARTICLES = "sf.includeCaseSubjectParticles";
	public static final String SF_INCLUDE_CUSTOM_FEED_FILTERS = "sf.includeCustomFeedFilters";
	public static final String SF_INCLUDE_EMBEDDED_SERVICE_BRANDINGS = "sf.includeEmbeddedServiceBrandings";
	public static final String SF_INCLUDE_EMBEDDED_SERVICE_CONFIGS = "sf.includeEmbeddedServiceConfigs";
	public static final String SF_INCLUDE_EMBEDDED_SERVICE_LIVE_AGENTS = "sf.includeEmbeddedServiceLiveAgents";
	public static final String SF_INCLUDE_ENTITLEMENT_PROCESSES = "sf.includeEntitlementProcesses";
	public static final String SF_INCLUDE_LIVE_CHAT_SENSITIVE_DATA_RULES = "sf.includeLiveChatSensitiveDataRules";
	
	// Settings
	public static final String SF_INCLUDE_ACCOUNT_SETTINGS = "sf.includeAccountSettings";
	public static final String SF_INCLUDE_ACTIVITIES_SETTINGS = "sf.includeActivitiesSettings";
	public static final String SF_INCLUDE_ADDRESS_SETTINGS = "sf.includeAddressSettings";
	public static final String SF_INCLUDE_BUSINESS_HOURS_SETTINGS = "sf.includeBusinessHoursSettings";
	public static final String SF_INCLUDE_CASE_SETTINGS = "sf.includeCaseSettings";
	public static final String SF_INCLUDE_COMPANY_SETTINGS = "sf.includeCompanySettings";
	public static final String SF_INCLUDE_CONTRACT_SETTINGS = "sf.includeContractSettings";
	public static final String SF_INCLUDE_ENTITLEMENT_SETTINGS = "sf.includeEntitlementSettings";
	public static final String SF_INCLUDE_FILE_UPLOAD_AND_DOWNLOAD_SECURITY_SETTINGS = "sf.includeFileUploadAndDownloadSecuritySettings";
	public static final String SF_INCLUDE_FORECASTING_SETTINGS = "sf.includeForecastingSettings";
	public static final String SF_INCLUDE_IDEAS_SETTINGS = "sf.includeIdeasSettings";
	public static final String SF_INCLUDE_KNOWLEDGE_SETTINGS = "sf.includeKnowledgeSettings";
	public static final String SF_INCLUDE_LEAD_CONVERT_SETTINGS = "sf.includeLeadConvertSettings";
	public static final String SF_INCLUDE_LIVE_AGENT_SETTINGS = "sf.includeLiveAgentSettings";
	public static final String SF_INCLUDE_MOBILE_SETTINGS = "sf.includeMobileSettings";
	public static final String SF_INCLUDE_NAME_SETTINGS = "sf.includeNameSettings";
	public static final String SF_INCLUDE_OPPORTUNITY_SETTINGS = "sf.includeOpportunitySettings";
	public static final String SF_INCLUDE_ORDER_SETTINGS = "sf.includeOrderSettings";
	public static final String SF_INCLUDE_ORG_PREFERENCE_SETTINGS = "sf.includeOrgPreferenceSettings";
	public static final String SF_INCLUDE_PATH_ASSISTANT_SETTINGS = "sf.includePathAssistantSettings";
	public static final String SF_INCLUDE_PRODUCT_SETTINGS = "sf.includeProductSettings";
	public static final String SF_INCLUDE_QUOTE_SETTINGS = "sf.includeQuoteSettings";
	public static final String SF_INCLUDE_SEARCH_SETTINGS = "sf.includeSearchSettings";
	public static final String SF_INCLUDE_SECURITY_SETTINGS = "sf.includeSecuritySettings";
	public static final String SF_INCLUDE_SOCIAL_CUSTOMER_SERVICE_SETTINGS = "sf.includeSocialCustomerServiceSettings";
	public static final String SF_INCLUDE_TERRITORY2_SETTINGS = "sf.includeTerritory2Settings";
	
	// Sites
	public static final String SF_INCLUDE_COMMUNITIES = "sf.includeCommunities";
	public static final String SF_INCLUDE_SITES = "sf.includeSites";
	public static final String SF_INCLUDE_NETWORKS = "sf.includeNetworks";
	public static final String SF_INCLUDE_PORTALS = "sf.includePortals";
	public static final String SF_INCLUDE_SHARING_SETS = "sf.includeSharingSets";
	public static final String SF_INCLUDE_SITE_DOT_COMS = "sf.includeSiteDotComs";
	public static final String SF_INCLUDE_BRANDING_SETS = "sf.includeBrandingSets";
	public static final String SF_INCLUDE_CMS_CONNECT_SOURCES = "sf.includeCMSConnectSources";
	public static final String SF_INCLUDE_COMMUNITY_TEMPLATE_DEFINITIONS = "sf.includeCommunityTemplateDefinitions";
	public static final String SF_INCLUDE_COMMUNITY_THEME_DEFINITIONS = "sf.includeCommunityThemeDefinitions";
	public static final String SF_INCLUDE_CONTENT_ASSETS = "sf.includeContentAssets";
	public static final String SF_INCLUDE_KEYWORD_LISTS = "sf.includeKeywordLists";
	public static final String SF_INCLUDE_MANAGED_TOPICS = "sf.includeManagedTopics";
	public static final String SF_INCLUDE_MODERATION_RULES = "sf.includeModerationRules";
	public static final String SF_INCLUDE_NETWORK_BRANDINGS = "sf.includeNetworkBrandings";
	public static final String SF_INCLUDE_AUDIENCES = "sf.includeAudiences";
	public static final String SF_INCLUDE_LIGHTNING_EXPERIENCE_THEMES = "sf.includeLightningExperienceThemes";
	
	// Workflows
	public static final String SF_INCLUDE_APPROVAL_PROCESSES = "sf.includeApprovalProcesses";
	public static final String SF_INCLUDE_ASSIGNMENT_RULES = "sf.includeAssignmentRules";
	public static final String SF_INCLUDE_AUTO_RESPONSE_RULES = "sf.includeAutoResponseRules";
	public static final String SF_INCLUDE_EMAILS = "sf.includeEmails";
	public static final String SF_INCLUDE_EMAILS_FOLDER_PREFIX = "sf.includeEmailsFolderPrefix";
	public static final String SF_INCLUDE_EMAILS_FOLDERS = "sf.includeEmailsFolders";
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

	//Einstein
	public static final String SF_INCLUDE_BOTS = "sf.includeBots";
	public static final String SF_INCLUDE_BOT_VERSIONS = "sf.includeBotVersions";
	public static final String SF_INCLUDE_MI_DOMAINS = "sf.includeMIDomains";

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
		loadIgnoreValues();
		loadManagedPackageTypes();
		
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
	
	protected void loadIgnoreValues() {
		ignoreList.clear();
		packageIgnoreList.clear();

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
			if (propertyKey != null && propertyKey.startsWith(SF_PACKAGE_IGNORE_PREFIX)) {
				// This is a package ignore property
				String ignoreProperty = projectProperties.get(propertyKey);
				if (ignoreProperty != null && ignoreProperty.trim().length() > 0) {
					for (String ignore : ignoreProperty.split(";")) {
						packageIgnoreList.add(ignore);
					}
				}
			}
		}
	}
	
	protected void loadManagedPackageTypes() {
		managedPackageTypes.clear();
		
		String managedPackageTypesProperty = getProject().getProperty(SF_INCLUDE_MANAGED_PACKAGE_TYPES);
		if (managedPackageTypesProperty != null && managedPackageTypesProperty.trim().length() > 0) {
			for (String type : managedPackageTypesProperty.split(";")) {
				managedPackageTypes.add(type);
			}
		}
		
	}
	
	protected void initSalesforceConnection() {
		try {
			long startTime = System.nanoTime();
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
			long elapsedTime = System.nanoTime() - startTime;
			log("Connected to Salesforce [" + TimeUnit.NANOSECONDS.toMillis(elapsedTime) + " ms]");
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new BuildException("Error trying to connect to Salesforce.");
		}
	}

}
