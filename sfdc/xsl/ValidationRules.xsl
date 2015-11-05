<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:b="http://soap.sforce.com/2006/04/metadata">
<xsl:output method="html" />
<xsl:template match="/">
  <xsl:param name="filename" />
  <html>
  <body>
    <table border="1">
      <tr bgcolor="#9acd32">
        <th>File</th>
		<th>RuleName</th> 
		<th>Active?</th> 
		<th>Error Condition Formula</th>
		<th>Error Message</th>
	  </tr>
      <xsl:for-each select="b:CustomObject/b:validationRules">        
        <tr>
          	<td><xsl:value-of select="$filename" /></td>              
          	<td><xsl:value-of select="b:fullName"/></td>
	  		<td><xsl:value-of select="b:active"/></td>
	  		<td><xsl:value-of select="b:errorConditionFormula"/></td>
	  		<td><xsl:value-of select="b:errorMessage"/></td>
	    </tr>
      </xsl:for-each>
    </table>
  </body>
  </html>
</xsl:template>
</xsl:stylesheet>