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
* [sktools.wsdlgen](gradle-plugins/wsdlgen-plugin/readme.md)
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
Gradle 6.0 og nyere.

Java 8.

Weblogic 12.1.3.x og nyere.



Se [jenkinsfile for detaljert oversikt over testede versjoner](Jenkinsfile)


## Utviklingsmiljø

UTF-8 for all kildekode

JDK 11+ / JDK 1.8




## Utvikleroppsett
Opprett gradle.properties med innhold, feks:
```
!unikt prosjektnavn for vindu i IntelliJ
project_name=sktools-6.x
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


