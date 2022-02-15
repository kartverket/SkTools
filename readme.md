SkTools - Felles byggeverktøy i Kartverket
-------------------------------------------

Dette prosjektet består av en samling frittstående byggeverktøy for tooling i byggesystemene.
Fortrinnsvis er disse skrevet for Gradle, men Java baserte versjoner finnes også.

Gradle plugins
--------------
* [sktools.dbtools](gradle-plugins/dbtools-plugin/readme.md)
* [sktools.filter-resources](gradle-plugins/filter-resources-plugin/readme.md)
* [sktools.properties](gradle-plugins/properties-plugin/readme.md)
* [sktools.provided](gradle-plugins/provided-plugin/readme.md)
* [sktools.weblogic-deploy](gradle-plugins/weblogic-deploy-plugin/readme.md)
* [sktools.weblogic-wsclient](gradle-plugins/weblogic-wsclient-plugin/readme.md)
* [sktools.weblogic-wswar](gradle-plugins/weblogic-wswar-plugin/readme.md)
* [sktools.webstart](gradle-plugins/webstart-plugin/readme.md)
* [sktools.wsdl-customizer](gradle-plugins/wsdl-customizer-plugin/readme.md)
* [sktools.wsdoc](gradle-plugins/wsdocgen-plugin/readme.md)
* [sktools.wsgen](gradle-plugins/wsgen-plugin/readme.md)
* [sktools.wsimport](gradle-plugins/wsimport-plugin/readme.md)
* [sktools.xjc](gradle-plugins/xjc-plugin/readme.md)


Java verktøy
------------
* [db-tools](build-utils/db-tools/README.md)
* [wsdocgen](build-utils/wsdocgen/README.md)


Baseline
--------
Gradle 4.2 og nyere.

Java 7 og Java 8.

Weblogic 10.3.5.x og nyere.



Se [jenkinsfile for detaljert oversikt over testede versjoner](Jenkinsfile)


## Utviklingsmiljø

UTF-8 for all kildekode

JDK 7 - 1.7.0_0_171 (latest)

IntelliJ
 1. Importer som gradle prosjekt.
 2. Gradle versjon velges fra spesifisert sted på disk (ikke gradle wrapper)



## Utvikleroppsett
Opprett gradle.properties med innhold, feks:
```
WEBLOGIC_HOME=C:/bea1213
WEBLOGIC_VERSION=12.1.3

!unikt prosjektnavn for vindu i IntelliJ
project_name=sktools-1.5
```

## Releasetesting
For å teste virkemåten til de ulike plugins finnes er jenkins satt opp til å teste med noen kombinasjoner av følgende:
* Gradle versjon (runtime)
* JDK versjon (runtime)


### Jenkins pipeline
Til prosjektet er det instrumentert _continuous integration and testing_ i Jenkins.
Jenkins finner du her [http://jenkins.statkart.no:8021/jenkins/job/sktools/](http://jenkins.statkart.no:8021/jenkins/job/sktools/)

Hver jobb automatiserer bygging, testing og publisering av hver versjon.

Flyten illustreres slik:

1. Kompilering, enhetstesting
2. Publisering til felles repo (nexus)



