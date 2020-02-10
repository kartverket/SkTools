# SkTools - Felles byggeverktøy i kartverket

# Baseline
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



