# SkTools - Felles byggeverktøy i kartverket

# Baseline
Gradle 5.0 og nyere. 

Java 8.

Weblogic 12.1.3.x og nyere.



Se [jenkinsfile for detaljert oversikt over testede versjoner](Jenkinsfile)


## Utviklingsmiljø

UTF-8 for all kildekode

JDK 12 / JDK 1.8




## Utvikleroppsett
Opprett gradle.properties med innhold, feks:
```
WEBLOGIC_HOME=C:/wls1221
WEBLOGIC_VERSION=12.2.1

!! Weblogic 12.1 requires JDK8 !!
#WEBLOGIC_HOME=C:/bea1213
#WEBLOGIC_VERSION=12.1.3

!unikt prosjektnavn for vindu i IntelliJ
project_name=sktools-1.5
```

## Releasetesting 
For å teste virkemåten til de ulike plugins finnes er jenkins satt opp til å teste med noen kombinasjoner av følgende:
* `WEBLOGIC_VERSION` styrer default weblogic classpath.
* `WEBLOGIC_HOME` dersom ikke en angir WEBLOGIC_VERSION kan denne brukes eksplisitt
* Gradle versjon (runtime)
* JDK versjon (runtime)


### Jenkins pipeline
Til prosjektet er det instrumentert _continuous integration and testing_ i Jenkins. 
Jenkins finner du her [http://jenkins.statkart.no:8021/jenkins/job/sktools/](http://jenkins.statkart.no:8021/jenkins/job/sktools/) 

Hver jobb automatiserer bygging, testing og publisering av hver versjon.

Flyten illustreres slik:

1. Kompilering, enhetstesting 
2. Publisering til felles repo (nexus)
  
   

