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

	PostRetrieveProcessor
	
	This class defines a custom Ant Task which is used to update the retrieved 
	Metadata files prior to commiting them to version control.  The following
	updates are made to the Metadata files:
	
	1) Filter Objects:  The objects Metadata is updated to allow us to retrieve the
		entire object Metadata and then filter it based on the property settings.
		If we only include the object parts (ex. Custom Fields, List Views, etc) in
		the package.xml then there is not enough Metadata to deploy the object.  You 
		need to include the object itself, which then will retrieve everything in the 
		object, regardless of the other items listed (or omitted) in the package.xml.
		The objects Metadata is updated as follows:
		- actionOverrides are removed if the SF_INCLUDE_ACTION_OVERRIDES property is
			not set.
		- businessProcesses are removed if the SF_INCLUDE_BUSINESS_PROCESSES property is
			not set or if the businessProcess is being ignored.
		- compactLayouts are removed if the SF_INCLUDE_COMPACT_LAYOUTS property is
			not set.
		- fields are removed if the SF_INCLUDE_CUSTOM_FIELDS property is not set or
			if the field is being ignored.
		- fieldSets are removed if the SF_INCLUDE_FIELD_SETS property is not set or
			if the fieldSet is being ignored.
		- listViews are removed if the SF_INCLUDE_LIST_VIEWS property is not set or
			if the listView is being ignored (or filtered based on prefix).
		- recordTypes are removed if the SF_INCLUDE_RECORD_TYPES property is not set or
			if the recordType is being ignored.
		- searchLayouts are removed if the SF_INCLUDE_SEARCH_LAYOUTS property is 
			not set.
		- sharingReasons are removed if the SF_INCLUDE_SHARING_REASONS property is not
			set or if the sharingReason is being ignored.
		- sharingRecalculations are removed if the SF_INCLUDE_SHARING_RECALCULATIONS property
			is not set.
		- validationRules are removed if the SF_INCLUDE_VALIDATION_RULES property is
			not set or if the validationRule is being ignored.
		- webLinks are removed if the SF_INCLUDE_WEBLINKS property is not set or if the
			weblink is being ignored.
	
	2) Filter Sites:  Remove the siteAdmin and subdomain from all sites Metadata.  This is
		done to prevent Sandbox specific settings from being committed to version control.
	
	3) Filter Permission Sets:  Remove any userPermission that is listed in the 
		SF_EXCLUDE_USER_PERMISSIONS property.  This is done to prevent Production-only
		userPermissions from being included in the build and thus preventing a deploy to an
		environment (i.e. Sandbox) that does not have that userPermission.
	
	4) Filter Profiles:  Remove any userPermission that is listed in the SF_EXCLUDE_USER_PERMISSIONS
		property and remove the personAccountDefault setting under recordTypeVisibilities.  The
		personAccountDefault setting was preventing deployments in certain situations.
	
	Author:  Jeff Bohanek (jeff@yellowbricksystems.com)
 */
package com.yellowbricksystems.ant;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.tools.ant.BuildException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class PostRetrieveProcessor extends SalesforceTask {

	protected String retrieveTarget;
	
	public static final String SF_EXCLUDE_USER_PERMISSIONS = "sf.excludeUserPermissions";
	protected HashSet<String> excludeUserPermissions = new HashSet<String>();
	
	public String getRetrieveTarget() {
		return retrieveTarget;
	}
	
	public void setRetrieveTarget(String retrieveTarget) {
		this.retrieveTarget = retrieveTarget;
	}
	
	@Override
	public void init() throws BuildException {

		super.init();
	}

	@Override
	public void execute() throws BuildException {
		if ((retrieveTarget == null) || (retrieveTarget.length() < 1)) {
			throw new BuildException("Please specify a retrieveTarget directory to process.");
		}
		
		processRetrieveTarget();
	}

	/**
	 * Perform the Post-Retrieve processing on the retrieveTarget directory.
	 */
	protected void processRetrieveTarget()
	{
		try
		{
			String excludeUserPermissionsProperty = getProject().getProperty(SF_EXCLUDE_USER_PERMISSIONS);
			if (excludeUserPermissionsProperty != null && excludeUserPermissionsProperty.length() > 0) {
				for (String permission : excludeUserPermissionsProperty.split(";")) {
					excludeUserPermissions.add(permission);
				}
				
			}
			
			filterObjects();
			filterSites();
			filterPermissionSets();
			filterProfiles();
			
		}
		catch (Exception ex)
		{
			throw new BuildException("Error trying to perform post-retrieve processing", ex);
		}
	}

	protected void filterPermissionSets() throws Exception {
		
		if (getPropertyBoolean(SF_INCLUDE_PERMISSION_SETS)) {
			String permissionSetsDirectoryName = retrieveTarget + "/permissionsets";
			File permissionSetsDirectory = new File(permissionSetsDirectoryName);
			String[] files = permissionSetsDirectory.list();
			if (files != null) {
				for (String fileName : files) {
					String fullFileName = permissionSetsDirectoryName + "/" + fileName;
					File xmlFile = new File(fullFileName);
					DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
					DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
					Document doc = dBuilder.parse(xmlFile);
					doc.getDocumentElement().normalize();

					NodeList permissionSetNodeList = doc.getElementsByTagName("PermissionSet");
					for (int i=0; i < permissionSetNodeList.getLength(); i++) {

						List<Node> removeNodes = new ArrayList<Node>();
						Node permissionSetNode = permissionSetNodeList.item(i);
						if (permissionSetNode.getNodeType() == Node.ELEMENT_NODE) {
							Element permissionSetElement = (Element) permissionSetNode;

							NodeList userPermissionsNodes = permissionSetElement.getElementsByTagName("userPermissions");
							if (userPermissionsNodes != null) {
								for (int j=0; j < userPermissionsNodes.getLength(); j++) {
									Node userPermissionsNode = userPermissionsNodes.item(j);

									if (userPermissionsNode.getNodeType() == Node.ELEMENT_NODE) {
										Element userPermissionsElement = (Element) userPermissionsNode;

										NodeList nameNodes = userPermissionsElement.getElementsByTagName("name");
										if (nameNodes != null && nameNodes.getLength() == 1) {
											Node nameNode = nameNodes.item(0);

											String userPermissionsName = nameNode.getTextContent();
											if (excludeUserPermissions.contains(userPermissionsName)) {
												removeNodes.add(userPermissionsNode);
											}
										}
									}
								}
								if (removeNodes.size() > 0) {
									ProcessorUtilities.removeNodes(doc, permissionSetNode, removeNodes);
								}
							}
						}
					}
					// Always save so that we don't get "phantom" changes
					ProcessorUtilities.saveDocument(doc, fullFileName);
				}
			}
		}
	}

	protected void filterProfiles() throws Exception {
		if (getPropertyBoolean(SF_INCLUDE_PROFILES)) {
			String profilesDirectoryName = retrieveTarget + "/profiles";
			File profilesDirectory = new File(profilesDirectoryName);
			String[] files = profilesDirectory.list();
			if (files != null) {
				for (String fileName : files) {
					String fullFileName = profilesDirectoryName + "/" + fileName;
					File xmlFile = new File(fullFileName);
					DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
					DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
					Document doc = dBuilder.parse(xmlFile);
					doc.getDocumentElement().normalize();

					NodeList profileNodeList = doc.getElementsByTagName("Profile");
					for (int i=0; i < profileNodeList.getLength(); i++) {

						List<Node> removeNodes = new ArrayList<Node>();
						Node profileNode = profileNodeList.item(i);
						if (profileNode.getNodeType() == Node.ELEMENT_NODE) {
							Element profileElement = (Element) profileNode;

							NodeList userPermissionsNodes = profileElement.getElementsByTagName("userPermissions");
							if (userPermissionsNodes != null) {
								List<Node> removeUserPermissionsNodes = new ArrayList<Node>();
								for (int j=0; j < userPermissionsNodes.getLength(); j++) {
									Node userPermissionsNode = userPermissionsNodes.item(j);

									if (userPermissionsNode.getNodeType() == Node.ELEMENT_NODE) {
										Element userPermissionsElement = (Element) userPermissionsNode;

										NodeList nameNodes = userPermissionsElement.getElementsByTagName("name");
										if (nameNodes != null && nameNodes.getLength() == 1) {
											Node nameNode = nameNodes.item(0);

											String userPermissionsName = nameNode.getTextContent();
											if (excludeUserPermissions.contains(userPermissionsName)) {
												removeUserPermissionsNodes.add(userPermissionsNode);
											}
										}
									}
								}
								if (removeUserPermissionsNodes.size() > 0) {
									ProcessorUtilities.removeNodes(doc, profileNode, removeUserPermissionsNodes);
								}
							}
							
							NodeList recordTypeNodes = profileElement.getElementsByTagName("recordTypeVisibilities");
							if (recordTypeNodes != null) {
								for (int j=0; j < recordTypeNodes.getLength(); j++) {
									Node recordTypeNode = recordTypeNodes.item(j);

									if (recordTypeNode.getNodeType() == Node.ELEMENT_NODE) {
										Element recordTypeElement = (Element) recordTypeNode;

										NodeList personAccountNodes = recordTypeElement.getElementsByTagName("personAccountDefault");
										if (personAccountNodes != null && personAccountNodes.getLength() == 1) {
											removeNodes.add(personAccountNodes.item(0));
											ProcessorUtilities.removeNodes(doc, recordTypeNode, removeNodes);
											removeNodes.clear();
										}
									}
								}
							}
						}
					}
					// Always save so that we don't get "phantom" changes
					ProcessorUtilities.saveDocument(doc, fullFileName);
				}
			}
		}
	}
	
	protected void filterObjects() throws Exception {
		String packageXmlFileName = retrieveTarget + "/package.xml";
		Map<String, List<String>> packageTypeMap = PackageUtilities.parsePackageXmlFile(packageXmlFileName);
			
		String objectsDirectoryName = retrieveTarget + "/objects";
		File objectsDirectory = new File(objectsDirectoryName);
		String[] files = objectsDirectory.list();
		if (files != null) {
			for (String fileName : files) {
				String objectName = fileName.split("\\.")[0];
				String fullFileName = objectsDirectoryName + "/" + fileName;
				File xmlFile = new File(fullFileName);
				DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
				Document doc = dBuilder.parse(xmlFile);
				doc.getDocumentElement().normalize();

				NodeList objectNodeList = doc.getElementsByTagName("CustomObject");
				for (int i=0; i < objectNodeList.getLength(); i++) {

					List<Node> removeNodes = new ArrayList<Node>();
					Node objectNode = objectNodeList.item(i);
					if (objectNode.getNodeType() == Node.ELEMENT_NODE) {
						Element objectElement = (Element) objectNode;
						
						// Action Overrides
						if (!getPropertyBoolean(SF_INCLUDE_ACTION_OVERRIDES)) {
							// Remove all ActionOverride sections
							NodeList actionOverrideNodes = objectElement.getElementsByTagName("actionOverrides");
							for (int j=0; j < actionOverrideNodes.getLength(); j++) {
								Node actionOverrideNode = actionOverrideNodes.item(j);
								removeNodes.add(actionOverrideNode);
							}
						}
						// Business Process
						// Loop through and see if any should be removed (based on ignore/namespace)
						NodeList businessProcessNodes = objectElement.getElementsByTagName("businessProcesses");
						List<String> packageBusinessProcesses = packageTypeMap.get("BusinessProcess");
						if (businessProcessNodes != null) {
							for (int j=0; j < businessProcessNodes.getLength(); j++) {
								Node businessProcessNode = businessProcessNodes.item(j);
								if (!getPropertyBoolean(SF_INCLUDE_BUSINESS_PROCESSES) || packageBusinessProcesses == null) {
									removeNodes.add(businessProcessNode);
								} else {
									if (businessProcessNode.getNodeType() == Node.ELEMENT_NODE) {
										Element businessProcessElement = (Element) businessProcessNode;
										NodeList fullNameNodes = businessProcessElement.getElementsByTagName("fullName");
										if (fullNameNodes != null && fullNameNodes.getLength() == 1) {
											Node fullNameNode = fullNameNodes.item(0);
											String fullName = objectName + "." + fullNameNode.getTextContent();
											if (!packageBusinessProcesses.contains(fullName)) {
												removeNodes.add(businessProcessNode);
											}
										}
									}
								}
							}
						}
						// Compact Layouts
						if (!getPropertyBoolean(SF_INCLUDE_COMPACT_LAYOUTS)) {
							// Remove all compactLayouts sections
							NodeList compactLayoutsNodes = objectElement.getElementsByTagName("compactLayouts");
							for (int j=0; j < compactLayoutsNodes.getLength(); j++) {
								Node compactLayoutsNode = compactLayoutsNodes.item(j);
								removeNodes.add(compactLayoutsNode);
							}
						}
						// Custom Fields
						// Loop through and see if any should be removed (based on ignore/namespace)
						NodeList customFieldNodes = objectElement.getElementsByTagName("fields");
						List<String> packageCustomFields = packageTypeMap.get("CustomField");
						if (customFieldNodes != null) {
							for (int j=0; j < customFieldNodes.getLength(); j++) {
								Node customFieldNode = customFieldNodes.item(j);
								if (!getPropertyBoolean(SF_INCLUDE_CUSTOM_FIELDS) || packageCustomFields == null) {
									removeNodes.add(customFieldNode);
								} else {
									if (customFieldNode.getNodeType() == Node.ELEMENT_NODE) {
										Element customFieldElement = (Element) customFieldNode;
										NodeList fullNameNodes = customFieldElement.getElementsByTagName("fullName");
										if (fullNameNodes != null && fullNameNodes.getLength() == 1) {
											Node fullNameNode = fullNameNodes.item(0);
											String fullName = objectName + "." + fullNameNode.getTextContent();
											if (!packageCustomFields.contains(fullName)) {
												removeNodes.add(customFieldNode);
											}
										}
									}
								}
							}
						}
						// Field Sets
						// Loop through and see if any should be removed (based on ignore/namespace)
						NodeList fieldSetNodes = objectElement.getElementsByTagName("fieldSets");
						List<String> packageFieldSets = packageTypeMap.get("FieldSet");
						if (fieldSetNodes != null) {
							for (int j=0; j < fieldSetNodes.getLength(); j++) {
								Node fieldSetNode = fieldSetNodes.item(j);
								if (!getPropertyBoolean(SF_INCLUDE_FIELD_SETS) || packageFieldSets == null) {
									removeNodes.add(fieldSetNode);
								} else {
									if (fieldSetNode.getNodeType() == Node.ELEMENT_NODE) {
										Element customFieldElement = (Element) fieldSetNode;
										NodeList fullNameNodes = customFieldElement.getElementsByTagName("fullName");
										if (fullNameNodes != null && fullNameNodes.getLength() == 1) {
											Node fullNameNode = fullNameNodes.item(0);
											String fullName = objectName + "." + fullNameNode.getTextContent();
											if (!packageFieldSets.contains(fullName)) {
												removeNodes.add(fieldSetNode);
											}
										}
									}
								}
							}
						}
						// List Views
						// Loop through and see if any should be removed (based on ignore/namespace/prefix)
						NodeList listViewNodes = objectElement.getElementsByTagName("listViews");
						List<String> packageListViews = packageTypeMap.get("ListView");
						if (listViewNodes != null) {
							for (int j=0; j < listViewNodes.getLength(); j++) {
								Node listViewNode = listViewNodes.item(j);
								if (!getPropertyBoolean(SF_INCLUDE_LIST_VIEWS) || packageListViews == null) {
									removeNodes.add(listViewNode);
								} else {
									if (listViewNode.getNodeType() == Node.ELEMENT_NODE) {
										Element listViewElement = (Element) listViewNode;
										NodeList fullNameNodes = listViewElement.getElementsByTagName("fullName");
										if (fullNameNodes != null && fullNameNodes.getLength() == 1) {
											Node fullNameNode = fullNameNodes.item(0);
											String fullName = objectName + "." + fullNameNode.getTextContent();
											if (!packageListViews.contains(fullName)) {
												removeNodes.add(listViewNode);
											}
										}
									}
								}
							}
						}
						// RecordType
						// Loop through and see if any should be removed (based on ignore/namespace)
						NodeList recordTypeNodes = objectElement.getElementsByTagName("recordTypes");
						List<String> packageRecordTypes = packageTypeMap.get("RecordType");
						if (recordTypeNodes != null) {
							for (int j=0; j < recordTypeNodes.getLength(); j++) {
								Node recordTypeNode = recordTypeNodes.item(j);
								if (!getPropertyBoolean(SF_INCLUDE_RECORD_TYPES) || packageRecordTypes == null) {
									removeNodes.add(recordTypeNode);
								} else {
									if (recordTypeNode.getNodeType() == Node.ELEMENT_NODE) {
										Element recordTypeElement = (Element) recordTypeNode;
										NodeList fullNameNodes = recordTypeElement.getElementsByTagName("fullName");
										if (fullNameNodes != null && fullNameNodes.getLength() == 1) {
											Node fullNameNode = fullNameNodes.item(0);
											String fullName = objectName + "." + fullNameNode.getTextContent();
											if (!packageRecordTypes.contains(fullName)) {
												removeNodes.add(recordTypeNode);
											}
										}
									}
								}
							}
						}
						// Search Layouts
						if (!getPropertyBoolean(SF_INCLUDE_SEARCH_LAYOUTS)) {
							// Remove all searchLayouts sections
							NodeList searchLayoutsNodes = objectElement.getElementsByTagName("searchLayouts");
							for (int j=0; j < searchLayoutsNodes.getLength(); j++) {
								Node searchLayoutsNode = searchLayoutsNodes.item(j);
								removeNodes.add(searchLayoutsNode);
							}
						}
						// SharingReason
						// Loop through and see if any should be removed (based on ignore/namespace)
						NodeList sharingReasonNodes = objectElement.getElementsByTagName("sharingReasons");
						List<String> packageSharingReasons = packageTypeMap.get("SharingReason");
						if (sharingReasonNodes != null) {
							for (int j=0; j < sharingReasonNodes.getLength(); j++) {
								Node sharingReasonNode = sharingReasonNodes.item(j);
								if (!getPropertyBoolean(SF_INCLUDE_SHARING_REASONS) || packageSharingReasons == null) {
									removeNodes.add(sharingReasonNode);
								} else {
									if (sharingReasonNode.getNodeType() == Node.ELEMENT_NODE) {
										Element sharingReasonElement = (Element) sharingReasonNode;
										NodeList fullNameNodes = sharingReasonElement.getElementsByTagName("fullName");
										if (fullNameNodes != null && fullNameNodes.getLength() == 1) {
											Node fullNameNode = fullNameNodes.item(0);
											String fullName = objectName + "." + fullNameNode.getTextContent();
											if (!packageSharingReasons.contains(fullName)) {
												removeNodes.add(sharingReasonNode);
											}
										}
									}
								}
							}
						}
						// Sharing Recalculations
						if (!getPropertyBoolean(SF_INCLUDE_SHARING_RECALCULATIONS)) {
							// Remove all sharingRecalculations sections
							NodeList sharingRecalculationsNodes = objectElement.getElementsByTagName("sharingRecalculations");
							for (int j=0; j < sharingRecalculationsNodes.getLength(); j++) {
								Node sharingRecalculationsNode = sharingRecalculationsNodes.item(j);
								removeNodes.add(sharingRecalculationsNode);
							}
						}
						// Validation Rules
						// Loop through and see if any should be removed (based on ignore/namespace)
						NodeList validationRulesNodes = objectElement.getElementsByTagName("validationRules");
						List<String> packageValidationRules = packageTypeMap.get("ValidationRule");
						if (validationRulesNodes != null) {
							for (int j=0; j < validationRulesNodes.getLength(); j++) {
								Node validationRulesNode = validationRulesNodes.item(j);
								if (!getPropertyBoolean(SF_INCLUDE_VALIDATION_RULES) || packageValidationRules == null) {
									removeNodes.add(validationRulesNode);
								} else {
									if (validationRulesNode.getNodeType() == Node.ELEMENT_NODE) {
										Element validationRuleElement = (Element) validationRulesNode;
										NodeList fullNameNodes = validationRuleElement.getElementsByTagName("fullName");
										if (fullNameNodes != null && fullNameNodes.getLength() == 1) {
											Node fullNameNode = fullNameNodes.item(0);
											String fullName = objectName + "." + fullNameNode.getTextContent();
											if (!packageValidationRules.contains(fullName)) {
												removeNodes.add(validationRulesNode);
											}
										}
									}
								}
							}
						}
						// Weblinks
						// Loop through and see if any should be removed (based on ignore/namespace)
						NodeList weblinkNodes = objectElement.getElementsByTagName("webLinks");
						List<String> packageWeblinks = packageTypeMap.get("WebLink");
						if (weblinkNodes != null) {
							for (int j=0; j < weblinkNodes.getLength(); j++) {
								Node weblinkNode = weblinkNodes.item(j);
								if (!getPropertyBoolean(SF_INCLUDE_WEBLINKS) || packageWeblinks == null) {
									removeNodes.add(weblinkNode);
								} else {
									if (weblinkNode.getNodeType() == Node.ELEMENT_NODE) {
										Element weblinkElement = (Element) weblinkNode;
										NodeList fullNameNodes = weblinkElement.getElementsByTagName("fullName");
										if (fullNameNodes != null && fullNameNodes.getLength() == 1) {
											Node fullNameNode = fullNameNodes.item(0);
											String fullName = objectName + "." + fullNameNode.getTextContent();
											if (!packageWeblinks.contains(fullName)) {
												removeNodes.add(weblinkNode);
											}
										}
									}
								}
							}
						}
						if (removeNodes.size() > 0) {
							ProcessorUtilities.removeNodes(doc, objectNode, removeNodes);
						}
					}
				}
				// Always save so that we don't get "phantom" changes
				ProcessorUtilities.saveDocument(doc, fullFileName);
			}
		}
	}

	/**
	 * Remove the siteAdmin and subdomain elements from the sites meta-data.
	 * @throws Exception
	 */
	protected void filterSites() throws Exception {
		if (getPropertyBoolean(SF_INCLUDE_SITES)) {
			String sitesDirectoryName = retrieveTarget + "/sites";
			File objectsDirectory = new File(sitesDirectoryName);
			String[] files = objectsDirectory.list();
			if (files != null) {
				for (String fileName : files) {
					String fullFileName = sitesDirectoryName + "/" + fileName;
					File xmlFile = new File(fullFileName);
					DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
					DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
					Document doc = dBuilder.parse(xmlFile);
					doc.getDocumentElement().normalize();

					NodeList objectNodeList = doc.getElementsByTagName("CustomSite");
					for (int i=0; i < objectNodeList.getLength(); i++) {

						List<Node> removeNodes = new ArrayList<Node>();

						Node siteNode = objectNodeList.item(i);
						if (siteNode.getNodeType() == Node.ELEMENT_NODE) {
							Element objectElement = (Element) siteNode;

							NodeList siteAdminNodes = objectElement.getElementsByTagName("siteAdmin");
							if (siteAdminNodes != null) {
								for (int j=0; j < siteAdminNodes.getLength(); j++) {
									Node siteAdminNode = siteAdminNodes.item(j);
									removeNodes.add(siteAdminNode);
								}
							}

							NodeList subdomainNodes = objectElement.getElementsByTagName("subdomain");
							if (subdomainNodes != null) {
								for (int j=0; j < subdomainNodes.getLength(); j++) {
									Node subdomainNode = subdomainNodes.item(j);
									removeNodes.add(subdomainNode);
								}
							}
						}

						ProcessorUtilities.removeNodes(doc, siteNode, removeNodes);
					}
					// Always save so that we don't get "phantom" changes
					ProcessorUtilities.saveDocument(doc, fullFileName);
				}
			}
		}
	}

}
