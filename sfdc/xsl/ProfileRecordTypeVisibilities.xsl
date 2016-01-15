<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:b="http://soap.sforce.com/2006/04/metadata">
<xsl:output method="html" />
<xsl:template match="/">
  <xsl:param name="filename" />
  <html>
  <body>
    <table border="1">
      <tr bgcolor="#9acd32">
        <th>File</th>
	<th>RecordType</th> 
	<th>Visible</th> 
	<th>Default</th>
	      </tr>
      <xsl:for-each select="b:Profile/b:recordTypeVisibilities">        
        <tr>
          <td><xsl:value-of select="$filename" /></td>              
          <td><xsl:value-of select="b:recordType"/></td>
	  <td><xsl:value-of select="b:visible"/></td>
	  <td><xsl:value-of select="b:default"/></td>
	    </tr>
      </xsl:for-each>
    </table>
  </body>
  </html>
</xsl:template>
</xsl:stylesheet>