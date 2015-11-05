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
	<th>fullName</th> 
	<th>label</th> 
	<th>type</th>
	<th>length</th>
	<th>precision</th>
	<th>scale</th>
	<th>required</th>
	<th>externalId</th>
	<th>unique</th>
	<th>formula</th>
	<th>description</th>
	<th>inlineHelpText</th>
      </tr>
      <xsl:for-each select="b:CustomObject/b:fields">        
        <tr>
          <td><xsl:value-of select="$filename" /></td>              
          <td><xsl:value-of select="b:fullName"/></td>
	  <td><xsl:value-of select="b:label"/></td>
	  <td><xsl:value-of select="b:type"/></td>
	  <td><xsl:value-of select="b:length"/></td>
	  <td><xsl:value-of select="b:precision"/></td>
	  <td><xsl:value-of select="b:scale"/></td>
	  <td><xsl:value-of select="b:required"/></td>
	  <td><xsl:value-of select="b:externalId"/></td>
	  <td><xsl:value-of select="b:unique"/></td>
	  <td><xsl:value-of select="b:formula"/></td>
	  <td><xsl:value-of select="b:description"/></td>
	  <td><xsl:value-of select="b:inlineHelpText"/></td>
        </tr>
      </xsl:for-each>
    </table>
  </body>
  </html>
</xsl:template>
</xsl:stylesheet>