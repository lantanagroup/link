<?xml version="1.0" encoding="UTF-8"?>
<!-- 

Copyright 2020 Lantana Consulting Group

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

-->
<xsl:stylesheet
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        xmlns:fhir="http://hl7.org/fhir"
        xmlns="urn:hl7-org:v3"
        version="1.0"
        exclude-result-prefixes="xsl fhir">

    <!--  <xsl:include href="fhir2cda-includes.xslt" />-->

    <xsl:output method="text" omit-xml-declaration="yes"/>

    <xsl:variable name="vNewline" select="'&#xa;'"/>

    <xsl:template match="/">
        <xsl:choose>
            <xsl:when test="fhir:QuestionnaireResponse">
                <xsl:apply-templates
                        select="fhir:QuestionnaireResponse[fhir:questionnaire/@value = 'http://hl7.org/fhir/us/hai/Questionnaire/hai-questionnaire-covid-19-pt-impact-hosp-capacity']"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:message terminate="yes">This transform can only be run on a FHIR QuestionnaireResponse where
                    questionnaire.value=
                    'http://hl7.org/fhir/us/hai/Questionnaire/hai-questionnaire-covid-19-pt-impact-hosp-capacity'.
                </xsl:message>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template
            match="fhir:QuestionnaireResponse[fhir:questionnaire/@value = 'http://hl7.org/fhir/us/hai/Questionnaire/hai-questionnaire-covid-19-pt-impact-hosp-capacity']">
        <xsl:text>collectionDate,numTotBeds,numBeds,numBedsOcc,numICUBeds,numICUBedsOcc,numVent,numVentUse,numC19HospPats,numC19MechVentPats,numC19HOPats,numC19OverflowPats,numC19OFMechVentPats,numC19Died</xsl:text>
        <xsl:value-of select="$vNewline"/>
        <!-- collection-date -> collectionDate -->
        <xsl:variable name="vDate"
                      select="//fhir:item[fhir:linkId/@value = 'collection-date']/fhir:answer/fhir:valueDate/@value"/>
        <xsl:variable name="vDay" select="substring($vDate, 9, 2)"/>
        <xsl:variable name="vMonth" select="substring($vDate, 6, 2)"/>
        <xsl:variable name="vYear" select="substring($vDate, 1, 4)"/>
        <xsl:value-of select="concat($vDay, '/', $vMonth, '/', $vYear)"/>

        <!-- A blank field indicates no data was entered whereas ‘0’ indicates no suspected/confirmed case. The only required fields are collectionDate and numBeds (hospital-inpatient-beds) -->

        <!-- numTotBeds -->
        <xsl:value-of
                select="concat(',', //fhir:item[fhir:linkId/@value = 'numTotBeds']/fhir:answer/fhir:valueInteger/@value)"/>
        <!-- numBeds -->
        <xsl:value-of
                select="concat(',', //fhir:item[fhir:linkId/@value = 'numBeds']/fhir:answer/fhir:valueInteger/@value)"/>
        <!-- numBedsOcc -->
        <xsl:value-of
                select="concat(',', //fhir:item[fhir:linkId/@value = 'numBedsOcc']/fhir:answer/fhir:valueInteger/@value)"/>
        <!-- numICUBeds -->
        <xsl:value-of
                select="concat(',', //fhir:item[fhir:linkId/@value = 'numICUBeds']/fhir:answer/fhir:valueInteger/@value)"/>
        <!-- numICUBedsOcc -->
        <xsl:value-of
                select="concat(',', //fhir:item[fhir:linkId/@value = 'numICUBedsOcc']/fhir:answer/fhir:valueInteger/@value)"/>
        <!-- numVent -->
        <xsl:value-of
                select="concat(',', //fhir:item[fhir:linkId/@value = 'numVent']/fhir:answer/fhir:valueInteger/@value)"/>
        <!-- numVentUse -->
        <xsl:value-of
                select="concat(',', //fhir:item[fhir:linkId/@value = 'numVentUse']/fhir:answer/fhir:valueInteger/@value)"/>
        <!-- numC19HospPats -->
        <xsl:value-of
                select="concat(',', //fhir:item[fhir:linkId/@value = 'numC19HospPats']/fhir:answer/fhir:valueInteger/@value)"/>
        <!-- numC19MechVentPats -->
        <xsl:value-of
                select="concat(',', //fhir:item[fhir:linkId/@value = 'numC19MechVentPats']/fhir:answer/fhir:valueInteger/@value)"/>
        <!-- numC19HOPats -->
        <xsl:value-of
                select="concat(',', //fhir:item[fhir:linkId/@value = 'numC19HOPats']/fhir:answer/fhir:valueInteger/@value)"/>
        <!-- numC19OverflowPats -->
        <xsl:value-of
                select="concat(',', //fhir:item[fhir:linkId/@value = 'numC19OverflowPats']/fhir:answer/fhir:valueInteger/@value)"/>
        <!-- numC19OFMechVentPats -->
        <xsl:value-of
                select="concat(',', //fhir:item[fhir:linkId/@value = 'numC19OFMechVentPats']/fhir:answer/fhir:valueInteger/@value)"/>
        <!-- numC19Died -->
        <xsl:value-of
                select="concat(',', //fhir:item[fhir:linkId/@value = 'numC19Died']/fhir:answer/fhir:valueInteger/@value)"/>
        <xsl:value-of select="$vNewline"/>
    </xsl:template>

</xsl:stylesheet>
