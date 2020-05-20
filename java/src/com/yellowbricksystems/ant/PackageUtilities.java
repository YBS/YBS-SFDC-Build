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

	PackageUtilities
	
	This class contains utilities to read and write package.xml and destructiveChanges.xml files.
	
	Author:  Jeff Bohanek (jeff@yellowbricksystems.com)
 */
package com.yellowbricksystems.ant;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.tools.ant.BuildException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class PackageUtilities {

	public static void createPackageXmlFile(String packageFileName, double asOfVersion,
											Map<String, List<String>> typesMap) throws IOException {
		createPackageXmlFile(packageFileName, asOfVersion, typesMap, null);
	}

	public static void createPackageXmlFile(String packageFileName, double asOfVersion,
			Map<String, List<String>> typesMap, String[] comments) throws IOException {

		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(packageFileName),"UTF-8"));		
		writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		writer.write("<Package xmlns=\"http://soap.sforce.com/2006/04/metadata\">\n");

		if (comments != null && comments.length > 0) {
			writer.write("<!-- \n");
			for (String comment : comments) {
				writer.write("    " + comment + "\n");
			}
			writer.write("-->\n");
		}

		List<String> typeList = new ArrayList<String>(typesMap.keySet());
		Collections.sort(typeList);
		for (String typeName : typeList) {
			List<String> memberList = typesMap.get(typeName);
			if (memberList != null) {
				writer.write("  <types>\n");

				Collections.sort(memberList);
				for (String memberName : memberList) {
					writer.write("	<members>" + memberName + "</members>\n");
				}
				writer.write("	<name>" + typeName + "</name>\n");
				writer.write("  </types>\n");
			}
		}
		writer.write("  <version>" + asOfVersion + "</version>\n");
		writer.write("</Package>\n");
		writer.close();
	}

	public static Map<String, List<String>> parsePackageXmlFile(String packageFileName) {
		Map<String, List<String>> typesMap = new HashMap<String, List<String>>();

		try {
			File xmlFile = new File(packageFileName);
			if (xmlFile.exists()) {
				DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
				Document doc = dBuilder.parse(xmlFile);
				doc.getDocumentElement().normalize();

				NodeList nodeList = doc.getElementsByTagName("types");
				for (int i=0; i < nodeList.getLength(); i++) {

					Node typesNode = nodeList.item(i);
					if (typesNode.getNodeType() == Node.ELEMENT_NODE) {
						Element typesElement = (Element) typesNode;

						NodeList nameNodes = typesElement.getElementsByTagName("name");
						if (nameNodes != null && nameNodes.getLength() == 1) {
							Node nameNode = nameNodes.item(0);
							String typeName = nameNode.getChildNodes().item(0).getNodeValue();
							List<String> typeMembers = new ArrayList<String>();
							typesMap.put(typeName, typeMembers);

							NodeList memberNodes = typesElement.getElementsByTagName("members");
							if (memberNodes != null && memberNodes.getLength() > 0) {
								for (int j=0; j < memberNodes.getLength(); ++j) {
									Node memberNode = memberNodes.item(j);
									String memberName = memberNode.getChildNodes().item(0).getNodeValue();
									typeMembers.add(memberName);
								}
							}
						}
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new BuildException("Exception trying to parse package.xml file " + packageFileName);
		}

		return typesMap;
	}
}
