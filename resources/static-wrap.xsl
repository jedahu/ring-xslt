<xsl:stylesheet version='2.0'
  xmlns:xsl='http://www.w3.org/1999/XSL/Transform'
  xmlns:rx='http://github.com/jedahu/ring-xslt/'
  exclude-result-prefixes='rx'>

  <xsl:variable name='rxns' select='"http://github.com/jedahu/ring-xslt/"'/>

  <xsl:template match='@*|node()'>
    <xsl:copy copy-namespaces='no'>
      <xsl:apply-templates select='@*|node()'/>
    </xsl:copy>
  </xsl:template>

  <xsl:template match='rx:include'>
    <xsl:variable name='resource'
      select='document(resolve-uri(@href, base-uri(current())))'/>
    <xsl:apply-templates select='$resource'/>
  </xsl:template>

  <xsl:template match='rx:include' mode='wrapper'>
    <xsl:variable name='resource'
      select='document(resolve-uri(@href, base-uri(/rx:wrap)))'/>
    <xsl:apply-templates select='$resource'/>
  </xsl:template>

  <xsl:template match='rx:wrap'>
    <xsl:variable name='wrapper'
      select='document(resolve-uri(@href, base-uri(current())))'/>
    <xsl:variable name='temp'>
      <xsl:copy-of copy-namespaces='no' select='$wrapper'/>
      <rx:wrap href='{@href}' xml:base='{base-uri()}'>
        <xsl:copy-of copy-namespaces='no' select='node()'/>
      </rx:wrap>
    </xsl:variable>
    <xsl:apply-templates mode='wrapper' select='$temp'/>
  </xsl:template>

  <xsl:template mode='wrapper' match='rx:wrap'/>

  <xsl:template mode='wrapper' match='rx:use[@name]'>
    <xsl:copy-of copy-namespaces='no'
      select='/rx:wrap/rx:provide[@name = current()/@name][1]/node()'/>
  </xsl:template>

  <xsl:template mode='wrapper' match='rx:use'>
    <xsl:variable name='nameless'
      select='/rx:wrap/rx:provide[not(@name)][1]'/>
    <xsl:choose>
      <xsl:when test='$nameless'>
        <xsl:copy-of copy-namespaces='no'
          select='$nameless/node()'/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:copy-of copy-namespaces='no'
          select='/rx:wrap/*[namespace-uri() != $rxns][1]'/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template mode='wrapper' match='@*|node()'>
    <xsl:copy copy-namespaces='no'>
      <xsl:apply-templates mode='wrapper' select='@*|node()'/>
    </xsl:copy>
  </xsl:template>

</xsl:stylesheet>
