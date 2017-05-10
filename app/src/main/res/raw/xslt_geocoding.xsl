<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
    <xsl:output method="xml" indent="yes"/>
    <xsl:template match="/">
        <xsl:text>&#10;</xsl:text>
        <result>
            <xsl:for-each select="GeocodeResponse/result">
                <xsl:text>&#10;</xsl:text>
                <match>
                    <adress>
                        <xsl:value-of select="formatted_address"/>
                    </adress>
                    <location>
                        <lat>
                            <xsl:value-of select="geometry/location/lat"/>
                        </lat>
                        <lng>
                            <xsl:value-of select="geometry/location/lng"/>
                        </lng>
                    </location>
                </match>
            </xsl:for-each>
        </result>
    </xsl:template>
</xsl:stylesheet>