<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:output method="xml" version="1.0" indent="yes"
                doctype-public="-//W3C//DTD XHTML 1.0 Strict//EN"
                doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd"
                media-type="text/html"
                omit-xml-declaration="no" />

    <xsl:template match="/services/service">

        <html>
            <head>
                <title><xsl:value-of select="@name"/></title>

                <style media="screen" type="text/css">

html {
    margin : 0;
    padding : 0;
}

body {
    background-color: #ffffff;
    color: #000000;
    left: 15px;
    font-family: Verdana, sans-serif;
    width: 100ex;
    margin-left: auto;
    margin-right: auto;
    padding: 3em 6em
}

h3 {
    font-size: 24;
    font-weight: bold;
    font-family: Garamond, serif;
}


h4 {
    font-size: 20;
    font-weight: bold;
}

h5 {
    font-size: 16;
    font-weight: bold;
}

div#header {
    margin-bottom: 3em;
    border:0.1ex solid black;
    padding: 3em 2ex;
}

div#hoved {
    border:0.1ex solid black;
    margin-top: 3ex;
}

div.metode {
    border-bottom:0.1ex solid gray;
    padding: 3em 2ex;
}

                </style>
            </head>

            <body>

                name=<span><xsl:value-of select="@name"/></span>
                description=<span><xsl:value-of select="@description"/></span>
                namespace=<span><xsl:value-of select="@namespace"/></span>

                <div>
                    <ul>
                        <xsl:for-each select="methods/method">
                            <xsl:sort select="@name"/><!-- ordered TOC by name -->
                            <li><a href=""><xsl:value-of select="@name"/></a></li>
                        </xsl:for-each>
                    </ul>
                </div>
                <div>
                    <xsl:for-each select="methods/method">
                        <div>
                            <h4><xsl:value-of select="@name"/></h4>
                            <p><xsl:value-of select="@description"/></p>

                            <h5>Input</h5>
                            <ul>
                                <xsl:for-each select="parameters/parameter">
                                    <li>
                                        <span><xsl:value-of select="@name"/></span>
                                        <span><xsl:value-of select="@description"/></span>
                                        <div>
                                            <span><xsl:value-of select="type/@name"/></span>
                                            <span><xsl:value-of select="type/@namespace"/></span>
                                            <span><xsl:value-of select="type/@javadocPath"/></span>
                                        </div>
                                    </li>
                                </xsl:for-each>
                            </ul>

                            <h5>Response</h5>
                            <ul>
                                <xsl:for-each select="returns/parameter">
                                    <li>
                                        <span><xsl:value-of select="@name"/></span>
                                        <span><xsl:value-of select="@description"/></span>
                                        <div>
                                            <span><xsl:value-of select="type/@name"/></span>
                                            <span><xsl:value-of select="type/@namespace"/></span>
                                            <span><xsl:value-of select="type/@javadocPath"/></span>
                                        </div>
                                    </li>
                                </xsl:for-each>
                                <xsl:for-each select="exceptions/exception">
                                    <li>
                                        <span><xsl:value-of select="@name"/></span>
                                        <span><xsl:value-of select="@description"/></span>
                                        <div>
                                            <span><xsl:value-of select="type/@name"/></span>
                                            <span><xsl:value-of select="type/@namespace"/></span>
                                            <span><xsl:value-of select="type/@javadocPath"/></span>
                                        </div>
                                    </li>
                                </xsl:for-each>
                            </ul>
                        </div>
                    </xsl:for-each>
                </div>

            </body></html>

    </xsl:template>

</xsl:stylesheet>