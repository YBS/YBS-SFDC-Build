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
	<th>field</th> 
	<th>editable</th> 
	<th>readable</th>
	      </tr>
      <xsl:for-each select="b:Profile/b:fieldPermissions">        
        <tr>
          <td><xsl:value-of select="$filename" /></td>              
          <td><xsl:value-of select="b:field"/></td>
	  <td><xsl:value-of select="b:editable"/></td>
	  <td><xsl:value-of select="b:readable"/></td>
	    </tr>
      </xsl:for-each>
    </table>
  </body>
  </html>
</xsl:template>
</xsl:stylesheet>