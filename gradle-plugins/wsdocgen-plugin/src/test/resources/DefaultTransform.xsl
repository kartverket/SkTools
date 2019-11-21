<?xml version='1.0' encoding='UTF-8'?><?xar XSLT?>

<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">



    <xsl:output method="xml" version="1.0" indent="yes"
                doctype-public="-//W3C//DTD XHTML 1.0 Strict//EN"
                doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd"
                media-type="text/html" encoding="windows-1252"
                omit-xml-declaration="no"/>

    <!-- struktur og layout for webside -->
    <xsl:template match="/">
        <html>
            <head>
                <title><xsl:value-of select="services/service/@name"/></title>
                <meta http-equiv="Content-Language" content="no-bok"/>
                <meta http-equiv="Content-Type" content="text/html; charset=windows-1252"/>

                <style media="screen" type="text/css">

                    html {
                    margin : 0;
                    padding : 0;
                    }

                    body {
                    background-color: purple;   /** fin farge for lazy testing uten å definere egen transformfil **/
                    color: #000000;
                    left: 15px;
                    font-family: Verdana, sans-serif;
                    width: 100ex;
                    margin-left: auto;
                    margin-right: auto;
                    padding: 3em 6em
                    }

                    h3 {
                    font-size: 24pt;
                    font-weight: bold;
                    font-family: Garamond, serif;
                    }


                    h4 {
                    font-size: 20pt;
                    font-weight: bold;
                    }

                    h5 {
                    font-size: 16pt;
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

                    /**
                    * Stilsett for webservice dokumentasjon
                    */

                    span.javadoc_tag_code {
                    white-space: pre;
                    font-family: monospace;
                    }


                </style>
            </head>

            <body>
                <div id="midtstilltSide">
                    <div>
                        <div class="toppelement1">
                            <img src="/grunnbok/pics/Header-blank.gif" height="64" width="590" alt=""/>
                        </div>

                        <xsl:apply-templates select="services"/>

                    </div>

                </div>
            </body>
        </html>
    </xsl:template>


    <!-- templates for innhold ... -->

    <xsl:template match="services">
        <xsl:apply-templates select="service"/>
    </xsl:template>


    <xsl:template match="service">
        <h3><xsl:value-of select="@name"/></h3>
        <p>
            <xsl:apply-templates select="description"/>
        </p>
        <h5>Namespace: <span><xsl:value-of select="@namespace"/></span></h5>

        <h5>Oversikt</h5>
        <ul>
            <xsl:for-each select="methods/method">
                <xsl:sort select="@name"/><!-- ordered TOC by name -->
                <li><a href="#{@name}"><xsl:value-of select="@name"/></a></li>
            </xsl:for-each>
        </ul>


        <br/>


        <xsl:for-each select="methods/method">
            <a name="{@name}" />
            <div style="margin-top: 6em">
                <h3><xsl:value-of select="@name"/></h3>
                <p>
                    <xsl:apply-templates select="description"/>
                </p>

                <h5>Input</h5>
                <ul>
                    <xsl:for-each select="parameters/parameter">
                        <li>
                            <xsl:value-of select="@name"/> - <xsl:apply-templates select="description"/>
                            <div>
                                <xsl:apply-templates select="type"/>
                            </div>
                        </li>
                    </xsl:for-each>
                </ul>

                <h5>Response</h5>
                <ul>
                    <xsl:for-each select="returns/parameter">
                        <li>
                            <xsl:value-of select="@name"/> - <xsl:apply-templates select="description"/>
                            <div>
                                <xsl:apply-templates select="type"/>
                            </div>
                        </li>
                    </xsl:for-each>
                    <xsl:for-each select="exceptions/exception">
                        <li>
                            <xsl:value-of select="@name"/> - <xsl:apply-templates select="description"/>
                            <div>
                                <xsl:apply-templates select="type"/>
                            </div>
                        </li>
                    </xsl:for-each>
                </ul>
            </div>
        </xsl:for-each>
    </xsl:template>



    <xsl:template match="description">
        <xsl:for-each select="text()|*">
            <xsl:choose>
                <xsl:when test="name(.)">
                    <xsl:apply-templates select="." mode="#default"/><!-- will fail here if no XSLT 2.0 compliant transformer exists on classpath ... -->
                </xsl:when>

                <xsl:otherwise>
                    <xsl:apply-templates select="." mode="noEscapedText"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:for-each>

    </xsl:template>


    <xsl:template match="type">
        <xsl:choose>
            <xsl:when test="@javadocPath != ''">
                <a href="{@javadocPath}">
                    <xsl:apply-templates select="." mode="plain"/>
                </a>
            </xsl:when>
            <xsl:otherwise>
                <xsl:apply-templates select="." mode="plain"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template match="type" mode="plain">
        <span title="{@namespace}/{@name}"><xsl:value-of select="@name"/></span>
    </xsl:template>


    <xsl:template match="span" mode="#all">
        <xsl:element name="span">
            <xsl:attribute name="class" select="@class" />

            <xsl:choose>
                <xsl:when test="@class='javadoc_tag_code'">
                    <xsl:attribute name="debug">span with escaped contents!</xsl:attribute>
                    <xsl:apply-templates mode="#default" />
                </xsl:when>

                <xsl:otherwise>
                    <xsl:attribute name="debug">normal span element</xsl:attribute>
                    <xsl:apply-templates mode="noEscapedText"/>
                </xsl:otherwise>
            </xsl:choose>

        </xsl:element>
    </xsl:template>

    <xsl:template match="text()" mode="noEscapedText">
        <xsl:value-of select="." disable-output-escaping="yes" />
    </xsl:template>


</xsl:stylesheet>