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

	ProcessorUtilities
	
	This class contains utilities to write Metadata XML files that have been updated.
	
	Author:  Jeff Bohanek (jeff@yellowbricksystems.com)
 */
package com.yellowbricksystems.ant;

import java.io.File;
import java.util.List;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ProcessorUtilities {

	public static void removeNodes(Document doc, Node parentNode, List<Node> removeNodes) throws Exception {
		if (doc != null && parentNode != null && removeNodes != null && removeNodes.size() > 0) {
			for (Node removeNode : removeNodes) {
				parentNode.removeChild(removeNode);
			}
			cleanDocument(doc, parentNode);
		}
	}

	public static void cleanDocument(Document doc, Node parentNode) throws Exception {
		doc.getDocumentElement().normalize();
		XPathFactory xpathFactory = XPathFactory.newInstance();
		// XPath to find empty text nodes.
		XPathExpression xpathExp = xpathFactory.newXPath().compile("//text()[normalize-space(.) = '']");
		NodeList emptyTextNodes = (NodeList)
				xpathExp.evaluate(parentNode, XPathConstants.NODESET);

		// Remove each empty text node from document.
		for (int k = 0; k < emptyTextNodes.getLength(); k++) {
			Node emptyTextNode = emptyTextNodes.item(k);
			if (emptyTextNode != null) {
				Node emptyTextParentNode = emptyTextNode.getParentNode();
				if (emptyTextParentNode != null) {
					emptyTextParentNode.removeChild(emptyTextNode);
				}
			}
		}
	}

	public static void saveDocument(Document doc, String fullFileName) throws Exception {
		doc.setXmlStandalone(true);
		doc.getDocumentElement().normalize();
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(new File(fullFileName));
		transformer.transform(source, result);
	}
	
	public static String getTagValue(Element element, String tagName) {
		String tagValue = "";
		if (element != null) {
			NodeList nodes = element.getElementsByTagName(tagName);
			if (nodes != null && nodes.getLength() == 1) {
				Node node = nodes.item(0);
				tagValue = node.getTextContent();
			}
		}
		return tagValue;
		
	}

}
