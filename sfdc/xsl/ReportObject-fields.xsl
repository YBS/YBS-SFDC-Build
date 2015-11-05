<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:b="http://soap.sforce.com/2006/04/metadata">
<xsl:output method="html" />
<xsl:template match="/">
  <xsl:param name="filename" />
  <html>
  <body>
    <table border="1">
      <tr bgcolor="#9acd32">
        <th>Report Name</th>
        <th>Field</th> 
	    <th>Report Type</th>
      </tr>
      <xsl:for-each select="b:Report/b:columns">        
        <tr>
          <td><xsl:value-of select="$filename" /></td>              
          <td><xsl:value-of select="b:field"/></td>
          <td><xsl:value-of select="../b:reportType"/></td>
        </tr>
      </xsl:for-each>
      <xsl:for-each select="b:Report/b:groupingsAcross">        
        <tr>
          <td><xsl:value-of select="$filename" /></td>              
          <td><xsl:value-of select="b:field"/></td>
          <td><xsl:value-of select="../b:reportType"/></td>
        </tr>
      </xsl:for-each>
      <xsl:for-each select="b:Report/b:groupingsDown">        
        <tr>
          <td><xsl:value-of select="$filename" /></td>              
          <td><xsl:value-of select="b:field"/></td>
          <td><xsl:value-of select="../b:reportType"/></td>
        </tr>
      </xsl:for-each>
    </table>
  </body>
  </html>
</xsl:template>
</xsl:stylesheet>