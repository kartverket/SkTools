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
Gradle 8.13 og nyere.

Java 17.



## Utviklingsmiljø

UTF-8 for all kildekode

JDK 17+ / JDK 11




## Utvikleroppsett
Opprett gradle.properties med innhold, feks:
```
!unikt prosjektnavn for vindu i IntelliJ
project_name=sktools-7.x
```

## Releasetesting
Testene kjøres automatisk ved PR og push til `trunk`.

## Publisering
Pakkene ble tidligere publisert til Nexus. Tidligere pakker vil bli migrert til GitHub Packages.

Nye pakker publiseres til [GitHub Packages](https://github.com/orgs/kartverket/packages?repo_name=SkTools) via [build-push.yml](.github/workflows/build-publish.yml) workflowen.
Ved hver push til `trunk` så vil det bygges og publiseres en ny versjon av alle pluginene og verktøyene i dette repoet.

Flyten illustreres slik:

1. Kompilering, enhetstesting
2. Publisering til GitHub Packages


## Versjonering
Pakkene har versjonsnummer som er av formatet `[Major version].[Date].[SHA]`
Alle pakker har samme versjon, og versjonsnummeret oppdateres ved hver publisering.


[Major version] oppdateres ved breaking changes og kan endres i [build-push.yml](.github/workflows/build-publish.yml) workflowen.


Før versjon 5.6 og overgang til git så benyttet "late branching" som strategi for merging av kildekode.
Disse taggene har da versjon som starter med `sktools-` - f.eks. "sktools-5.5.1" og ble bygget manuelt.


## Historikk til kodebasen
Denne kodebasen lå først i VCS systemet Perforce der det ble opprettet.

I 2020 så ble dette konvertert til Git og lagt på BitBucket tjener.
Script for konvertering og loggfiler finnes i teg [perforce-migrering](https://github.com/kartverket/SkTools/tree/perforce-migrering).

I 2023 ble repoet flyttet til GitHub under [kartverket/SkTools](https://github.com/kartverket/SkTools)
