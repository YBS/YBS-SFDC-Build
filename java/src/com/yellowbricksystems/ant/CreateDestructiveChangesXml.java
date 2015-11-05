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

	CreateDestructiveChangesXml
	
	This class is used to generate a destructiveChanges.xml file by generating a
	package.xml file in the current destination environment and comparing it to the
	source package.xml file.  Any components that are in the destination package.xml
	that are not in the source package.xml will be added to the destructiveChanges.xml.
	
	Author:  Jeff Bohanek (jeff@yellowbricksystems.com)
 */
package com.yellowbricksystems.ant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.tools.ant.BuildException;

public class CreateDestructiveChangesXml extends SalesforceTask {

	protected String sourcePackageFileName;
	protected String destinationPackageFileName;
	protected String destructiveChangesFileName;

	protected Map<String, List<String>> typesMap = new HashMap<String, List<String>>();

	protected ArrayList<String> noDeleteTypes = new ArrayList<String>();

	@Override
	public void init() throws BuildException {
		noDeleteTypes.add("RecordType");
		super.init();
	}

	@Override
	public void execute() throws BuildException {
		if ((sourcePackageFileName == null) || (sourcePackageFileName.length() < 1)) {
			throw new BuildException("Please specify a sourcePackageFileName to create.");
		}
		if ((destinationPackageFileName == null) || (destinationPackageFileName.length() < 1)) {
			throw new BuildException("Please specify a destinationPackageFileName to create.");
		}
		if ((destructiveChangesFileName == null) || (destructiveChangesFileName.length() < 1)) {
			throw new BuildException("Please specify a destructiveChangesFileName to create.");
		}
		initSalesforceConnection(); // Get correct asOfVersion from url
		createDestructiveChangesFile();
	}

	protected void createDestructiveChangesFile() {
		try {
			Map<String, List<String>> sourceTypeMap = PackageUtilities.parsePackageXmlFile(sourcePackageFileName);
			Map<String, List<String>> destinationTypeMap = PackageUtilities.parsePackageXmlFile(destinationPackageFileName);

			for (String typeName : destinationTypeMap.keySet()) {
				List<String> destinationMemberList = destinationTypeMap.get(typeName);
				List<String> sourceMemberList = sourceTypeMap.get(typeName);

				for (String memberName : destinationMemberList) {
					if (sourceMemberList == null || !sourceMemberList.contains(memberName)) {
						if (noDeleteTypes.contains(typeName)) {
							System.out.println("The " + typeName + " " + memberName + 
									" cannot be deleted through the API.  You will need to delete it manually.");
						} else {
							addTypeMember(typeName, memberName);
						}
					}
				}
			}

			PackageUtilities.createPackageXmlFile(destructiveChangesFileName, asOfVersion, typesMap);
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new BuildException("Exception trying to generate destructiveChanges file.");
		}
	}

	protected void addTypeMember(String typeName, String memberName) {
		List<String> memberList = typesMap.get(typeName);
		if (memberList == null) {
			memberList = new ArrayList<String>();
			typesMap.put(typeName, memberList);
		}
		memberList.add(memberName);
	}

	public String getSourcePackageFileName() {
		return sourcePackageFileName;
	}

	public void setSourcePackageFileName(String sourcePackageFileName) {
		this.sourcePackageFileName = sourcePackageFileName;
	}

	public String getDestinationPackageFileName() {
		return destinationPackageFileName;
	}

	public void setDestinationPackageFileName(String destinationPackageFileName) {
		this.destinationPackageFileName = destinationPackageFileName;
	}

	public String getDestructiveChangesFileName() {
		return destructiveChangesFileName;
	}

	public void setDestructiveChangesFileName(String destructiveChangesFileName) {
		this.destructiveChangesFileName = destructiveChangesFileName;
	}
}
