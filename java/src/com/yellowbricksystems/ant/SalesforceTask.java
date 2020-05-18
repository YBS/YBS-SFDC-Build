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

import java.util.ArrayList;
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

	public static final double API_VERSION = 48.0;
	public static final String BUILD_VERSION = "48.0";

	public static final String SF_USER_PROPERTY_NAME = "sf.username";
	public static final String SF_PASSWORD_PROPERTY_NAME = "sf.password";
	public static final String SF_SERVER_URL_PROPERTY_NAME = "sf.serverurl";

	protected PartnerConnection partnerConnection = null;
	protected MetadataConnection metadataConnection = null;
	protected ToolingConnection toolingConnection = null;

	public static final String SF_IGNORE_PREFIX = "sf.ignore";
	public static final String SF_PACKAGE_IGNORE_PREFIX = "sf.package.ignore";

	public static final String SF_PRINT_UNUSED_TYPES = "sf.printUnusedTypes";

	protected HashSet<String> ignoreList = new HashSet<String>();
	protected HashSet<String> packageIgnoreList = new HashSet<String>();
	
	protected HashSet<String> managedPackageTypes = new HashSet<String>();
	
	// Property Names for salesforce.properties file to control metadata that is
	// retrieved/deployed
	
	public static final String SF_INCLUDE_MANAGED_PACKAGE_TYPES = "sf.includeManagedPackageTypes";

	public static final int ADD_METHOD_LIST_METADATA = 0; // Default
	public static final int ADD_METHOD_FOLDER = 1;
	public static final int ADD_METHOD_TOOLING_API = 2;
	public static final int ADD_METHOD_SETTINGS = 3;
	public static final int ADD_METHOD_QUERY = 4;

	public static ArrayList<PackageType> allPackageTypes = new ArrayList<PackageType>();
	
	// Apex
	public static final PackageType SF_INCLUDE_CLASSES = new PackageType("sf.includeClasses", "ApexClass", ADD_METHOD_TOOLING_API);
	public static final PackageType SF_INCLUDE_COMPONENTS = new PackageType("sf.includeComponents", "ApexComponent", ADD_METHOD_TOOLING_API);
	public static final PackageType SF_INCLUDE_PAGES = new PackageType("sf.includePages", "ApexPage", ADD_METHOD_TOOLING_API);
	public static final PackageType SF_INCLUDE_TRIGGERS = new PackageType("sf.includeTriggers", "ApexTrigger", ADD_METHOD_TOOLING_API);
	public static final PackageType SF_INCLUDE_FLEXI_PAGES = new PackageType("sf.includeFlexiPages", "FlexiPage");
	public static final PackageType SF_INCLUDE_FLOWS = new PackageType("sf.includeFlows", "Flow");
	public static final PackageType SF_INCLUDE_SCONTROLS = new PackageType("sf.includeScontrols", "Scontrol");
	public static final PackageType SF_INCLUDE_STATIC_RESOURCES = new PackageType("sf.includeStaticResources", "StaticResource");
	public static final PackageType SF_INCLUDE_AURA_DEFINITION_BUNDLES = new PackageType("sf.includeAuraDefinitionBundles", "AuraDefinitionBundle");
	public static final PackageType SF_INCLUDE_PLATFORM_CACHE_PARTITIONS = new PackageType("sf.includePlatformCachePartitions", "PlatformCachePartition");
	public static final PackageType SF_INCLUDE_EMAIL_SERVICES_FUNCTIONS = new PackageType("sf.includeEmailServicesFunctions", "EmailServicesFunction");
	
	// App
	public static final PackageType SF_INCLUDE_APP_MENUS = new PackageType("sf.includeAppMenus", "AppMenu");
	public static final PackageType SF_INCLUDE_CONNECTED_APPS = new PackageType("sf.includeConnectedApp", "ConnectedApp");
	public static final PackageType SF_INCLUDE_APPLICATIONS = new PackageType("sf.includeApplications", "CustomApplication");
	public static final PackageType SF_INCLUDE_APPLICATION_COMPONENTS = new PackageType("sf.includeApplicationComponents", "CustomApplicationComponent");
	public static final PackageType SF_INCLUDE_LABELS = new PackageType("sf.includeLabels", "CustomLabel");
	public static final PackageType SF_INCLUDE_CUSTOM_PAGE_WEBLINKS = new PackageType("sf.includeCustomPageWeblinks", "CustomPageWebLink");
	public static final PackageType SF_INCLUDE_TABS = new PackageType("sf.includeTabs", "CustomTab");
	public static final PackageType SF_INCLUDE_DOCUMENTS = new PackageType("sf.includeDocuments", "Document", ADD_METHOD_FOLDER,
			"sf.includeDocumentsFolderPrefix", "sf.includeDocumentsFolders", null, "Document", "FolderId");
	public static final PackageType SF_INCLUDE_HOME_PAGE_COMPONENTS = new PackageType("sf.includeHomePageComponents", "HomePageComponent");
	public static final PackageType SF_INCLUDE_HOME_PAGE_LAYOUTS = new PackageType("sf.includeHomePageLayouts", "HomePageLayout");
	public static final PackageType SF_INCLUDE_INSTALLED_PACKAGES = new PackageType("sf.includeInstalledPackages", "InstalledPackage");
	public static final PackageType SF_INCLUDE_TRANSLATIONS = new PackageType("sf.includeTranslations", "Translations");
	public static final PackageType SF_INCLUDE_CHATTER_EXTENSIONS = new PackageType("sf.includeChatterExtensions", "ChatterExtension");
	public static final PackageType SF_INCLUDE_LIGHTNING_BOLTS = new PackageType("sf.includeLightningBolts", "LightningBolt");
	
	//Object
	public static final PackageType SF_INCLUDE_CUSTOM_FIELDS = new PackageType("sf.includeCustomFields", "CustomField");
	public static final PackageType SF_INCLUDE_RECORD_TYPES = new PackageType("sf.includeRecordTypes", "RecordType");
	public static final PackageType SF_INCLUDE_BUSINESS_PROCESSES = new PackageType("sf.includeBusinessProcesses", "BusinessProcess");
	public static final PackageType SF_INCLUDE_COMPACT_LAYOUTS = new PackageType("sf.includeCompactLayouts", "CompactLayout");
	public static final PackageType SF_INCLUDE_FIELD_SETS = new PackageType("sf.includeFieldSets", "FieldSet");
	public static final PackageType SF_INCLUDE_LIST_VIEWS = new PackageType("sf.includeListViews", "ListView", ADD_METHOD_LIST_METADATA,
			"sf.includeListViewsPrefix", null, null, null, null);
	public static final PackageType SF_INCLUDE_SHARING_REASONS = new PackageType("sf.includeSharingReasons", "SharingReason");
	public static final PackageType SF_INCLUDE_VALIDATION_RULES = new PackageType("sf.includeValidationRules", "ValidationRule");
	public static final PackageType SF_INCLUDE_WEBLINKS = new PackageType("sf.includeWeblinks", "WebLink");
	// @todo: figure out what to do with the following
	public static final String SF_INCLUDE_SEARCH_LAYOUTS = "sf.includeSearchLayouts";
	public static final String SF_INCLUDE_SHARING_RECALCULATIONS = "sf.includeSharingRecalculations";
	public static final String SF_INCLUDE_ACTION_OVERRIDES = "sf.includeActionOverrides";

	public static final PackageType SF_INCLUDE_OBJECT_TRANSLATIONS = new PackageType("sf.includeObjectTranslations", "CustomObjectTranslation");
	public static final PackageType SF_INCLUDE_EXTERNAL_DATA_SOURCES = new PackageType("sf.includeExternalDataSources", "ExternalDataSource");
	public static final PackageType SF_INCLUDE_LAYOUTS = new PackageType("sf.includeLayouts", "Layout");
	public static final PackageType SF_INCLUDE_PUBLISHER_ACTIONS = new PackageType("sf.includePublisherActions", "QuickAction");
	public static final PackageType SF_INCLUDE_ACTION_LINK_GROUP_TEMPLATES = new PackageType("sf.includeActionLinkGroupTemplates", "ActionLinkGroupTemplate");
	public static final PackageType SF_INCLUDE_CUSTOM_METADATA = new PackageType("sf.includeCustomMetadata", "CustomMetadata");
	public static final PackageType SF_INCLUDE_GLOBAL_VALUE_SETS = new PackageType("sf.includeGlobalValueSets", "GlobalValueSet");
	public static final PackageType SF_INCLUDE_GLOBAL_VALUE_SET_TRANSLATIONS = new PackageType("sf.includeGlobalValueSetTranslations", "GlobalValueSetTranslation");
	public static final String SF_INCLUDE_STANDARD_VALUE_SETS = "sf.includeStandardValueSets";
	public static final PackageType SF_INCLUDE_TOPICS_FOR_OBJECTS = new PackageType("sf.includeTopicsForObjects", "TopicsForObjects");
	
	// Reporting/Analytics
	public static final PackageType SF_INCLUDE_ANALYTIC_SNAPSHOTS = new PackageType("sf.includeAnalyticSnapshots", "AnalyticSnapshot");
	public static final PackageType SF_INCLUDE_DASHBOARDS = new PackageType("sf.includeDashboards", "Dashboard", ADD_METHOD_FOLDER,
			"sf.includeDashboardsFolderPrefix", "sf.includeDashboardsFolders", null, "Dashboard", "FolderId");
	public static final PackageType SF_INCLUDE_REPORTS = new PackageType("sf.includeReports", "Report", ADD_METHOD_FOLDER,
			"sf.includeReportsFolderPrefix", "sf.includeReportsFolders", "sf.includeReportsUnfiledPublic", "Report", "OwnerId");
	public static final PackageType SF_INCLUDE_REPORT_TYPES = new PackageType("sf.includeReportTypes", "ReportType");
	public static final PackageType SF_INCLUDE_ANALYTIC_MAP_CHARTS = new PackageType("sf.includeAnalyticMapCharts", "EclairGeoData");
	public static final PackageType SF_INCLUDE_WAVE_APPLICATIONS = new PackageType("sf.includeWaveApplications", "WaveApplication");
	public static final PackageType SF_INCLUDE_WAVE_DASHBOARDS = new PackageType("sf.includeWaveDashboards", "WaveDashboard");
	public static final PackageType SF_INCLUDE_WAVE_DATAFLOWS = new PackageType("sf.includeWaveDataflows", "WaveDataflow");
	public static final PackageType SF_INCLUDE_WAVE_DATASETS = new PackageType("sf.includeWaveDatasets", "WaveDataset");
	public static final PackageType SF_INCLUDE_WAVE_LENSES = new PackageType("sf.includeWaveLenses", "WaveLens");
	public static final PackageType SF_INCLDUE_WAVE_TEMPLATE_BUNDLES = new PackageType("sf.includeWaveTemplateBundles", "WaveTemplateBundle");
	public static final PackageType SF_INCLUDE_WAVE_XMDS = new PackageType("sf.includeWaveXmds", "WaveXmd");
	
	// Security/Admin
	public static final PackageType SF_INCLUDE_PROFILES = new PackageType("sf.includeProfiles", "Profile", ADD_METHOD_LIST_METADATA,
			"sf.includeProfilesPrefix", null, null, null, null);
	public static final String PERMISSION_SET_QUERY = "select Id,Name,NamespacePrefix from PermissionSet where ProfileId = null order by NamespacePrefix, Name";
	public static final PackageType SF_INCLUDE_PERMISSION_SETS = new PackageType("sf.includePermissionSets", "PermissionSet", null, PERMISSION_SET_QUERY, "Name", "NamespacePrefix");
	public static final PackageType SF_INCLUDE_ROLES = new PackageType("sf.includeRoles", "Role");
	public static final PackageType SF_INCLUDE_GROUPS = new PackageType("sf.includeGroups", "Group");
	public static final PackageType SF_INCLUDE_QUEUES = new PackageType("sf.includeQueues", "Queue");
	public static final PackageType SF_INCLUDE_TERRITORIES = new PackageType("sf.includeTerritories", "Territory");
	public static final PackageType SF_INCLUDE_AUTH_PROVIDERS = new PackageType("sf.includeAuthProviders", "AuthProvider");
	public static final PackageType SF_INCLUDE_REMOTE_SITE_SETTINGS = new PackageType("sf.includeRemoteSiteSettings", "RemoteSiteSetting");
	public static final PackageType SF_INCLUDE_SAML_SSO_CONFIGS = new PackageType("sf.includeSamlSsoConfigs", "SamlSsoConfig");
	public static final PackageType SF_INCLUDE_CUSTOM_PERMISSIONS = new PackageType("sf.includeCustomPermissions", "CustomPermission");
	public static final PackageType SF_INCLUDE_MATCHING_RULES = new PackageType("sf.includeMatchingRules", "MatchingRule");
	public static final PackageType SF_INCLUDE_NAMED_CREDENTIALS = new PackageType("sf.includeNamedCredentials", "NamedCredential");
	public static final PackageType SF_INCLUDE_PATH_ASSISTANTS = new PackageType("sf.includePathAssistants", "PathAssistant");
	public static final PackageType SF_INCLUDE_SHARING_RULES = new PackageType("sf.includeSharingRules", "SharingRules");
	public static final PackageType SF_INCLUDE_SYNONYM_DICTIONARY = new PackageType("sf.includeSynonymDictionary", "SynonymDictionary");
	public static final PackageType SF_INCLUDE_CERTIFICATES = new PackageType("sf.includeCertificates", "Certificate");
	public static final PackageType SF_INCLUDE_CLEAN_DATA_SERVICES = new PackageType("sf.includeCleanDataServices", "CleanDataService");
	public static final PackageType SF_INCLUDE_CORS_WHITELIST_ORIGINS = new PackageType("sf.includeCORSWhitelistOrigins", "CorsWhitelistOrigin");
	public static final PackageType SF_INCLUDE_DELEGATE_GROUPS = new PackageType("sf.includeDelegateGroups", "DelegateGroup");
	public static final PackageType SF_INCLUDE_DUPLICATE_RULES = new PackageType("sf.includeDuplicateRules", "DuplicateRule");
	public static final PackageType SF_INCLUDE_EXTERNAL_SERVICE_REGISTRATIONS = new PackageType("sf.includeExternalServiceRegistrations", "ExternalServiceRegistration");
	public static final PackageType SF_INCLUDE_PROFILE_PASSWORD_POLICIES = new PackageType("sf.includeProfilePasswordPolicies", "ProfilePasswordPolicy");
	public static final PackageType SF_INCLUDE_PROFILE_SESSION_SETTINGS = new PackageType("sf.includeProfileSessionSettings", "ProfileSessionSetting");
	public static final PackageType SF_INCLUDE_TERRITORIES2 = new PackageType("sf.includeTerritories2", "Territory2");
	public static final PackageType SF_INCLUDE_TERRITORY2_MODELS = new PackageType("sf.includeTerritory2Models", "Territory2Model");
	public static final PackageType SF_INCLUDE_TERRITORY2_RULES = new PackageType("sf.includeTerritory2Rules", "Territory2Rule");
	public static final PackageType SF_INCLUDE_TERRITORY2_TYPES = new PackageType("sf.includeTerritory2Types", "Territory2Type");
	public static final PackageType SF_INCLUDE_TRANSACTION_SECURITY_POLICIES = new PackageType("sf.includeTransactionSecurityPolicies", "TransactionSecurityPolicy");
	
	// Service
	public static final PackageType SF_INCLUDE_CALL_CENTERS = new PackageType("sf.includeCallCenters", "CallCenter");
	public static final PackageType SF_INCLUDE_DATA_CATEGORY_GROUPS = new PackageType("sf.includeDataCategoryGroups", "DataCategoryGroup");
	public static final PackageType SF_INCLUDE_ENTITLEMENT_TEMPLATES = new PackageType("sf.includeEntitlementTemplates", "EntitlementTemplate");
	public static final PackageType SF_INCLUDE_LIVE_CHAT_AGENT_CONFIGS = new PackageType("sf.includeLiveChatAgentConfigs", "LiveChatAgentConfig");
	public static final PackageType SF_INCLUDE_LIVE_CHAT_BUTTONS = new PackageType("sf.includeLiveChatButtons", "LiveChatButton");
	public static final PackageType SF_INCLUDE_LIVE_CHAT_DEPLOYMENTS = new PackageType("sf.includeLiveChatDeployments", "LiveChatDeployment");
	public static final PackageType SF_INCLUDE_MILESTONE_TYPES = new PackageType("sf.includeMilestoneTypes", "MilestoneType");
	public static final PackageType SF_INCLUDE_SKILLS = new PackageType("sf.includeSkills", "Skill");
	public static final PackageType SF_INCLUDE_CASE_SUBJECT_PARTICLES = new PackageType("sf.includeCaseSubjectParticles", "CaseSubjectParticle");
	public static final PackageType SF_INCLUDE_CUSTOM_FEED_FILTERS = new PackageType("sf.includeCustomFeedFilters", "CustomFeedFilter");
	public static final PackageType SF_INCLUDE_EMBEDDED_SERVICE_BRANDINGS = new PackageType("sf.includeEmbeddedServiceBrandings", "EmbeddedServiceBranding");
	public static final PackageType SF_INCLUDE_EMBEDDED_SERVICE_CONFIGS = new PackageType("sf.includeEmbeddedServiceConfigs", "EmbeddedServiceConfig");
	public static final PackageType SF_INCLUDE_EMBEDDED_SERVICE_LIVE_AGENTS = new PackageType("sf.includeEmbeddedServiceLiveAgents", "EmbeddedServiceLiveAgent");
	public static final PackageType SF_INCLUDE_ENTITLEMENT_PROCESSES = new PackageType("sf.includeEntitlementProcesses", "EntitlementProcess");
	public static final PackageType SF_INCLUDE_LIVE_CHAT_SENSITIVE_DATA_RULES = new PackageType("sf.includeLiveChatSensitiveDataRules", "LiveChatSensitiveDataRule");
	
	// Settings
	public static final PackageType SF_INCLUDE_ACCOUNT_SETTINGS = new PackageType("sf.includeAccountSettings", "Account", ADD_METHOD_SETTINGS);
	public static final PackageType SF_INCLUDE_ACTIVITIES_SETTINGS = new PackageType("sf.includeActivitiesSettings", "Activities", ADD_METHOD_SETTINGS);
	public static final PackageType SF_INCLUDE_ADDRESS_SETTINGS = new PackageType("sf.includeAddressSettings", "Address", ADD_METHOD_SETTINGS);
	public static final PackageType SF_INCLUDE_BUSINESS_HOURS_SETTINGS = new PackageType("sf.includeBusinessHoursSettings", "BusinessHours", ADD_METHOD_SETTINGS);
	public static final PackageType SF_INCLUDE_CASE_SETTINGS = new PackageType("sf.includeCaseSettings", "Case", ADD_METHOD_SETTINGS);
	public static final PackageType SF_INCLUDE_COMPANY_SETTINGS = new PackageType("sf.includeCompanySettings", "Company", ADD_METHOD_SETTINGS);
	public static final PackageType SF_INCLUDE_CONTRACT_SETTINGS = new PackageType("sf.includeContractSettings", "Contract", ADD_METHOD_SETTINGS);
	public static final PackageType SF_INCLUDE_ENTITLEMENT_SETTINGS = new PackageType("sf.includeEntitlementSettings", "Entitlement", ADD_METHOD_SETTINGS);
	public static final PackageType SF_INCLUDE_FILE_UPLOAD_AND_DOWNLOAD_SECURITY_SETTINGS = new PackageType("sf.includeFileUploadAndDownloadSecuritySettings", "FileUploadAndDownloadSecurity", ADD_METHOD_SETTINGS);
	public static final PackageType SF_INCLUDE_FORECASTING_SETTINGS = new PackageType("sf.includeForecastingSettings", "Forecasting", ADD_METHOD_SETTINGS);
	public static final PackageType SF_INCLUDE_IDEAS_SETTINGS = new PackageType("sf.includeIdeasSettings", "Ideas", ADD_METHOD_SETTINGS);
	public static final PackageType SF_INCLUDE_KNOWLEDGE_SETTINGS = new PackageType("sf.includeKnowledgeSettings", "Knowledge", ADD_METHOD_SETTINGS);
	public static final PackageType SF_INCLUDE_LEAD_CONVERT_SETTINGS = new PackageType("sf.includeLeadConvertSettings", "LeadConvertSettings", ADD_METHOD_LIST_METADATA);
	public static final PackageType SF_INCLUDE_LIVE_AGENT_SETTINGS = new PackageType("sf.includeLiveAgentSettings", "LiveAgent", ADD_METHOD_SETTINGS);
	public static final PackageType SF_INCLUDE_MOBILE_SETTINGS = new PackageType("sf.includeMobileSettings", "Mobile", ADD_METHOD_SETTINGS);
	public static final PackageType SF_INCLUDE_NAME_SETTINGS = new PackageType("sf.includeNameSettings", "Name", ADD_METHOD_SETTINGS);
	public static final PackageType SF_INCLUDE_OPPORTUNITY_SETTINGS = new PackageType("sf.includeOpportunitySettings", "Opportunity", ADD_METHOD_SETTINGS);
	public static final PackageType SF_INCLUDE_ORDER_SETTINGS = new PackageType("sf.includeOrderSettings", "Order", ADD_METHOD_SETTINGS);
	public static final PackageType SF_INCLUDE_ORG_PREFERENCE_SETTINGS = new PackageType("sf.includeOrgPreferenceSettings", "OrgPreference", ADD_METHOD_SETTINGS);
	public static final PackageType SF_INCLUDE_PATH_ASSISTANT_SETTINGS = new PackageType("sf.includePathAssistantSettings", "PathAssistant", ADD_METHOD_SETTINGS);
	public static final PackageType SF_INCLUDE_PRODUCT_SETTINGS = new PackageType("sf.includeProductSettings", "Product", ADD_METHOD_SETTINGS);
	public static final PackageType SF_INCLUDE_QUOTE_SETTINGS = new PackageType("sf.includeQuoteSettings", "Quote", ADD_METHOD_SETTINGS);
	public static final PackageType SF_INCLUDE_SEARCH_SETTINGS = new PackageType("sf.includeSearchSettings", "Search", ADD_METHOD_SETTINGS);
	public static final PackageType SF_INCLUDE_SECURITY_SETTINGS = new PackageType("sf.includeSecuritySettings", "Security", ADD_METHOD_SETTINGS);
	public static final PackageType SF_INCLUDE_SOCIAL_CUSTOMER_SERVICE_SETTINGS = new PackageType("sf.includeSocialCustomerServiceSettings", "SocialCustomerService", ADD_METHOD_SETTINGS);
	public static final PackageType SF_INCLUDE_TERRITORY2_SETTINGS = new PackageType("sf.includeTerritory2Settings", "Territory2", ADD_METHOD_SETTINGS);
	
	// Sites
	public static final PackageType SF_INCLUDE_COMMUNITIES = new PackageType("sf.includeCommunities", "Community");
	public static final PackageType SF_INCLUDE_SITES = new PackageType("sf.includeSites", "CustomSite");
	public static final PackageType SF_INCLUDE_NETWORKS = new PackageType("sf.includeNetworks", "Network");
	public static final PackageType SF_INCLUDE_PORTALS = new PackageType("sf.includePortals", "Portal");
	public static final PackageType SF_INCLUDE_SHARING_SETS = new PackageType("sf.includeSharingSets", "SharingSet");
	public static final PackageType SF_INCLUDE_SITE_DOT_COMS = new PackageType("sf.includeSiteDotComs", "SiteDotCom");
	public static final PackageType SF_INCLUDE_BRANDING_SETS = new PackageType("sf.includeBrandingSets", "BrandingSet");
	public static final PackageType SF_INCLUDE_CMS_CONNECT_SOURCES = new PackageType("sf.includeCMSConnectSources", "CMSConnectSource");
	public static final PackageType SF_INCLUDE_COMMUNITY_TEMPLATE_DEFINITIONS = new PackageType("sf.includeCommunityTemplateDefinitions", "CommunityTemplateDefinition");
	public static final PackageType SF_INCLUDE_COMMUNITY_THEME_DEFINITIONS = new PackageType("sf.includeCommunityThemeDefinitions", "CommunityThemeDefinition");
	public static final PackageType SF_INCLUDE_CONTENT_ASSETS = new PackageType("sf.includeContentAssets", "ContentAsset");
	public static final PackageType SF_INCLUDE_KEYWORD_LISTS = new PackageType("sf.includeKeywordLists", "KeywordList");
	public static final PackageType SF_INCLUDE_MANAGED_TOPICS = new PackageType("sf.includeManagedTopics", "ManagedTopics");
	public static final PackageType SF_INCLUDE_MODERATION_RULES = new PackageType("sf.includeModerationRules", "ModerationRule");
	public static final PackageType SF_INCLUDE_NETWORK_BRANDINGS = new PackageType("sf.includeNetworkBrandings", "NetworkBranding");
	public static final PackageType SF_INCLUDE_AUDIENCES = new PackageType("sf.includeAudiences", "Audience");
	public static final PackageType SF_INCLUDE_LIGHTNING_EXPERIENCE_THEMES = new PackageType("sf.includeLightningExperienceThemes", "LightningExperienceTheme");
	
	// Workflows
	public static final PackageType SF_INCLUDE_APPROVAL_PROCESSES =  new PackageType("sf.includeApprovalProcesses", "ApprovalProcess");
	public static final PackageType SF_INCLUDE_ASSIGNMENT_RULES =  new PackageType("sf.includeAssignmentRules", "AssignmentRule");
	public static final PackageType SF_INCLUDE_AUTO_RESPONSE_RULES =  new PackageType("sf.includeAutoResponseRules", "AutoResponseRules");
	public static final PackageType SF_INCLUDE_EMAILS =  new PackageType("sf.includeEmails", "EmailTemplate", ADD_METHOD_FOLDER,
			"sf.includeEmailsFolderPrefix", "sf.includeEmailsFolders", "sf.includeEmailsUnfiledPublic", "Email", "FolderId");
	public static final PackageType SF_INCLUDE_LETTERHEADS =  new PackageType("sf.includeLetterheads", "Letterhead");
	public static final PackageType SF_INCLUDE_POST_TEMPLATES =  new PackageType("sf.includePostTemplates", "PostTemplate");
	public static final PackageType SF_INCLUDE_WORKFLOWS =  new PackageType("sf.includeWorkflows", "Workflow"); // Backwards compatibility
	public static final PackageType SF_INCLUDE_WORKFLOW_ALERTS =  new PackageType("sf.includeWorkflowAlerts", "WorkflowAlert");
	public static final PackageType SF_INCLUDE_WORKFLOW_FIELD_UPDATES =  new PackageType("sf.includeWorkflowFieldUpdates", "WorkflowFieldUpdate");
	public static final PackageType SF_INCLUDE_WORKFLOW_FLOW_ACTIONS =  new PackageType("sf.includeWorkflowFlowActions", "WorkflowFlowAction");
	public static final PackageType SF_INCLUDE_WORKFLOW_KNOWLEDGE_PUBLISHES =  new PackageType("sf.includeWorkflowKnowledgePublishes", "WorkflowKnowledgePublish");
	public static final PackageType SF_INCLUDE_WORKFLOW_OUTBOUND_MESSAGES =  new PackageType("sf.includeWorkflowOutboundMessages", "WorkflowOutboundMessage");
	public static final PackageType SF_INCLUDE_WORKFLOW_RULES =  new PackageType("sf.includeWorkflowRules", "WorkflowRule");
	public static final PackageType SF_INCLUDE_WORKFLOW_TASKS =  new PackageType("sf.includeWorkflowTasks", "WorkflowTask");

	//Einstein
	public static final PackageType SF_INCLUDE_BOTS = new PackageType("sf.includeBots", "Bot");
	public static final PackageType SF_INCLUDE_BOT_VERSIONS = new PackageType("sf.includeBotVersions", "BotVersion");
	public static final PackageType SF_INCLUDE_MI_DOMAINS = new PackageType("sf.includeMIDomains", "MIDomain");

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

	protected boolean getPropertyBoolean(PackageType packageType) {
		if (packageType != null) {
			return getPropertyBoolean(packageType.propertyName);
		} else {
			return false;
		}
	}

	protected void setMemberPrefix(PackageType packageType) {
		if (packageType != null) {
			packageType.memberPrefix = null;
			if (packageType.prefixPropertyName != null) {
				packageType.memberPrefix = getProject().getProperty(packageType.prefixPropertyName);
			}
		}
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
			} else if (!serverUrl.endsWith(".com")) {
				throw new BuildException("The " + SF_SERVER_URL_PROPERTY_NAME + " Salesforce server url must end in .com");
			}

			// Build "correct" serverUrl
			serverUrl += "/services/Soap/u/" + API_VERSION;

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
			
			long elapsedTime = System.nanoTime() - startTime;
			log("Connected to Salesforce [" + TimeUnit.NANOSECONDS.toMillis(elapsedTime) + " ms]");
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new BuildException("Error trying to connect to Salesforce.");
		}
	}

	public static class PackageType {

		public int addMethod;
		public String propertyName;
		public String metadataName;

		// For Folders
		public String prefixPropertyName;
		public String foldersPropertyName;
		public String unfiledPublicPropertyName;
		public String folderType;
		public String objectFolderFieldName;

		// For Queries
		public String query;
		public String nameFieldName;
		public String namespaceFieldName;

		public String memberPrefix;

		// Default for List Metadata (most common)
		public PackageType(String propertyName, String metadataName) {
			this(propertyName, metadataName, ADD_METHOD_LIST_METADATA, null, null, null, null, null);
		}

		public PackageType(String propertyName, String metadataName, int addMethod) {
			this(propertyName, metadataName, addMethod, null, null, null, null, null);
		}

		// For Query Type
		public PackageType(String propertyName, String metadataName, String prefixPropertyName, String query, String nameFieldName, String namespaceFieldName) {
			this(propertyName, metadataName, ADD_METHOD_QUERY, prefixPropertyName, null, null, null, null);
			this.query = query;
			this.nameFieldName = nameFieldName;
			this.namespaceFieldName = namespaceFieldName;
		}

		public PackageType(String propertyName, String metadataName, int addMethod, String prefixPropertyName,
						   String foldersPropertyName, String unfiledPublicPropertyName, String folderType, String objectFolderFieldName) {
			this.propertyName = propertyName;
			this.metadataName = metadataName;
			this.addMethod = addMethod;

			this.prefixPropertyName = prefixPropertyName;
			this.foldersPropertyName = foldersPropertyName;
			this.unfiledPublicPropertyName = unfiledPublicPropertyName;
			this.folderType = folderType;
			this.objectFolderFieldName = objectFolderFieldName;

			allPackageTypes.add(this);
		}

	}
}
