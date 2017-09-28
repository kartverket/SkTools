# SkTools - Felles byggeverktøy i kartverket

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
Jenkins finner du her [http://jenkins.statkart.no:8001/jenkins](http://jenkins.statkart.no:8001/jenkins/view/SKTOOLS) 

Hver jobb automatiserer bygging, enhetstesting og integrasjonstesting av hver versjon.

Flyten illustreres slik:

1. Kompilering, enhetstesting -> publish til lokalt repo
2. Integrasjonstesting ved kjøring av demoer mot artefakter installert lokalt
   3. Integrasjonstesting ulike kompbinasjoner av gradle, weblogic osv
4. Deploy til felles repo (nexus)
  
   

