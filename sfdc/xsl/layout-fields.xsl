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
        <th>Section</th>        
        <th>Field</th>
        <th>Behaviour</th>
      </tr>
      <xsl:for-each select="b:Layout/b:layoutSections">
        <xsl:for-each select="b:layoutColumns">
          <xsl:for-each select="b:layoutItems">
            <tr>
              <td><xsl:value-of select="$filename" /></td>
              <td><xsl:value-of select="../../b:label"/></td>              
              <xsl:if test="b:field!=''">
                <td><xsl:value-of select="b:field"/></td>
              </xsl:if>
              <xsl:if test="not(b:field)">
                <td><xsl:value-of select="b:customLink"/></td>
              </xsl:if>
              <td><xsl:value-of select="b:behavior"/></td>
            </tr>
          </xsl:for-each>
        </xsl:for-each>
      </xsl:for-each>
    </table>
  </body>
  </html>
</xsl:template>
</xsl:stylesheet>