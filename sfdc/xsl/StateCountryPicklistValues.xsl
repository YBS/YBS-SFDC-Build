<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:b="http://soap.sforce.com/2006/04/metadata">
<xsl:output method="html" />
<xsl:template match="/">
  <xsl:param name="filename" />
  <html>
  <body>
    <table border="1">
      <tr bgcolor="#9acd32">
        <th>Country Name</th>
        <th>Country</th>
        <th>CountryCode</th>
        <th>Country Active</th>        
        <th>Country Visible</th>        
        <th>State Name</th>
        <th>State</th>
        <th>StateCode</th>
        <th>State Active</th>        
        <th>State Visible</th>        
      </tr>
      <xsl:for-each select="b:AddressSettings/b:countriesAndStates/b:countries">
        <tr>
          <td><xsl:value-of select="b:label"/></td>
          <td><xsl:value-of select="b:integrationValue"/></td>
          <td><xsl:value-of select="b:isoCode"/></td>
          <td><xsl:value-of select="b:active"/></td>
          <td><xsl:value-of select="b:visible"/></td>
          <td> </td>
          <td> </td>
          <td> </td>
          <td> </td>
          <td> </td>
        </tr>
        <xsl:for-each select="b:states">
            <tr>
              <td><xsl:value-of select="../b:label"/></td>
              <td><xsl:value-of select="../b:integrationValue"/></td>
              <td><xsl:value-of select="../b:isoCode"/></td>
              <td><xsl:value-of select="../b:active"/></td>
              <td><xsl:value-of select="../b:visible"/></td>
              <td><xsl:value-of select="b:label"/></td>
              <td><xsl:value-of select="b:integrationValue"/></td>
              <td><xsl:value-of select="b:isoCode"/></td>
              <td><xsl:value-of select="b:active"/></td>
              <td><xsl:value-of select="b:visible"/></td>
            </tr>
        </xsl:for-each>
      </xsl:for-each>
    </table>
  </body>
  </html>
</xsl:template>
</xsl:stylesheet>