SkTools - Felles byggeverktøy i Kartverket
-------------------------------------------

Dette prosjektet består av en samling frittstående byggeverktøy for tooling i byggesystemene.
Fortrinnsvis er disse skrevet for Gradle, men Java baserte versjoner finnes også.

Gradle plugins
--------------
* [sktools.dbtools](gradle-plugins/dbtools-plugin/readme.md)
* [sktools.properties](gradle-plugins/properties-plugin/readme.md)
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
Gradle 7.0 og nyere.

Java 8.



Se [jenkinsfile for detaljert oversikt over testede versjoner](Jenkinsfile)


## Utviklingsmiljø

UTF-8 for all kildekode

JDK 17+ / JDK 8




## Utvikleroppsett
Opprett gradle.properties med innhold, feks:
```
!unikt prosjektnavn for vindu i IntelliJ
project_name=sktools-7.x
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


## Versjonering
En benytter "trunk based" versjonering. Det vil si at man cherry-picker over endringer mellom levende kodegrener.
Det beste er at nye features først utvikles på trunk slik at man holder den fremtidige koden ren og uten legacy kode.

Publisering av nye versjoner gjøres via jenkins jobb.

Før versjon 5.6 og overgang til git så benyttet "late branching" som strategi for merging av kildekode.
Disse taggene har da versjon som starter med `sktools-` - f.eks. "sktools-5.5.1" og ble bygget manuelt.


## Historikk til kodebasen
Denne kodebasen lå først i VCS systemet Perforce der det ble opprettet.

I 2020 så ble dette konvertert til Git og lagt på BitBucket tjener.
Script for konvertering og loggfiler finnes i teg [perforce-migrering](https://github.com/kartverket/SkTools/tree/perforce-migrering).

I 2023 ble repoet flyttet til GitHub under [kartverket/SkTools](https://github.com/kartverket/SkTools)
