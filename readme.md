# SkTools - Felles byggeverktøy i kartverket

# Baseline
Gradle 4.2 og nyere. 

Java 7 og Java 8.

Weblogic 10.3.5.x og nyere.



Se [jenkinsfile for detaljert oversikt over testede versjoner](Jenkinsfile)


## Utviklingsmiljø

UTF-8 for all kildekode

JDK 7 - 1.7.0_0_171 (latest)

IntelliJ - kjør gradlew og importer versjonert ipr fil



## Utvikleroppsett
Opprett gradle.properties med innhold, feks:
```
WEBLOGIC_HOME=C:/bea1213
WEBLOGIC_VERSION=12.1.3
```

## Releasetesting 
For å teste virkemåten til de ulike plugins finnes det noen veldig enkle demo prosjekter. 
Disse kan kjøres via `gradle install` og `gradle :gradle-demos:runDemos`

Følgende parametere er aktuelle å teste
* `WEBLOGIC_VERSION` styrer default veblogic classpath.
* `WEBLOGIC_HOME` dersom ikke en angir WEBLOGIC_VERSION kan denne brukes eksplisitt
* Gradle versjon (runtime)
* JDK versjon (runtime)


### Jenkins pipeline
Til prosjektet er det intrumentert _continuous integration and testing_ i Jenkins. 
Jenkins finner du her [http://jenkins.statkart.no:8021/jenkins/job/sktools/](http://jenkins.statkart.no:8021/jenkins/job/sktools/) 

Hver jobb automatiserer bygging, enhetstesting og integrasjonstesting av hver versjon.

Flyten illustreres slik:

1. Kompilering, enhetstesting -> publish til lokalt repo
2. Integrasjonstesting ved kjøring av demoer mot artefakter installert lokalt
   3. Integrasjonstesting ulike kompbinasjoner av gradle, weblogic osv
4. Deploy til felles repo (nexus)
  
   

