[Webstart Gradle Plugin](src/main/groovy/no/statkart/sktools/gradle/plugins/webstart/WebstartPlugin.groovy)
------------------

Generering av webstart klienter og java applikasjons-distribusjoner.
Pluginen har funksjonalitet for generering av jnlp-filer, jar-ressurser, signering og enkel war distribuering.
Det forutsetter en Webstart med versjon 1.6 eller nyere.

Installation
------------

Build script snippet for new plugin DSL syntax:

    plugins {
        id 'sktools.webstart' version '5.7'
    }

Build script snippet for use in all versions:

    buildscript {
        repositories {
            maven { url 'https://nexus.statkart.no/repository/public/' }
        }
        dependencies {
            classpath 'no.statkart.sktools.gradle:webstart-plugin:5.7'
            // or
            classpath 'no.statkart.sktools.gradle:gradle-plugins:5.7'
        }
    }
    apply plugin: 'sktools-webstart-plugin'


Changelog
------------
## Unreleased Changes

## 1.3.0 Release Notes
* [SKTOOLS-170] Utvide JarSigner task med ekstra parameter `storetype`
* [SKTOOLS-120] Støtte for javaFX. Uppet jnlp syntax version til 1.6+ (fra 1.5+)
* [SKTOOLS-118] Angivelse av main jar-fil i JNLP
* [SKTOOLS-116] Konfigurasjon av signeringsalgoritme og utvidet manifest attributter

## 1.2.0 Release Notes
* [SKIF-188] Forbedret konfigrasjon av dependencies. Deklarasjon av dependency streng lagt til.
* [SKIF-209] Bugfix: Retter feil der signerte jarfiler ikke ble oppdaterte
* [SKIF-209] Bugfix: Jar-filer med snapshot versjon kommer nå med kun èn gang
* Konfigurerbart version-felt for jnlp element
* SKIF-228 (build 45) Må kunne takle artefaktnavn på den formen SKIF benytter
* Kompatibel med Gradle 1.0

## 1.1.0 Release Notes
* Total omskriving av plugin. Nytt er:
  *  Utvidet, fremoverkompatibel konfigurasjon
  *  Konfigurasjon av flere klienter
  *  Re-signering av jar filer kun når filen faktisk er endret
  *  Vedlikehold av version.xml
  *  Mulighet for enkel integrasjon med War tasks.
  *  Mulighet for parameterisering av signerings-parametere.
  *  Task for enkel innlemmelse av JnlpServlet - se JnlpServletWarTask
  *  Håndtering av sertifikater og versjonering
  *  Håndtering av SNAPSHOT versjoner
* Demo prosjekt og tester

## 1.0.0 Release Notes
* Lagt til dokumentasjon


Bruk
----

## Sjekkliste

* Manifiest attributt 'Permissions' for hoved-jar som holder applikasjonen.
  * Legg til dette i gradle fil for prosjketet:
    ```groovy
        jar {
            manifest {
                attributes 'Permissions': 'all-permissions'
            }
        }
    ```

* Oppsett av jnlp og signering av ressurs-filer
  *  Signering settes opp med kodesigneringssertifikat. Bruk self signed for lokal utvikling og testing.


## Task :: [JarSigner](src/main/groovy/no/statkart/sktools/gradle/plugins/webstart/JarSigner.java)
JarSigner tar inn et sett med filer og eventuelt signerer dem.
Signerte filer tas vare på slik at de ikke trenger å signeres på nytt dersom de ikke endres.
Denne cachen deles på tvert av taskene, men er som standard lokalisert innenfor subprosjektet.


## Task :: [WebstartTask](src/main/groovy/no/statkart/sktools/gradle/plugins/webstart/WebstartTask.groovy)
Lager jnlp-filer for gitte jnlp-definisjoner.
Alle jnlp-filer laget av samme task vil dele de samme jar-filene, men man kan godt ha flere WebstartTask i et og samme prosjekt.



Configuration
------------

    webstart {
        client {
            mainJar 'matrikkelklient'
            jarDependencies configurations.webstartLibs
            sign(keystore: keystore, alias: keystore_alias, password: keystore_password)
            jnlp {
                jnlpFilename 'matrikkeljava.jnlp'
                title "Matrikkelen ver. ${matrikkel_tjener_versjon}"
                version rootProject.getVersion()
                vendor 'Statens kartverk'
                homepage 'matrikkel.html'
                description 'Nasjonalt system for matrikkelføring i Norge.'
                resources {
                    javaRuntime '1.8.* 1.8+', '64m', '256m', 'http://java.sun.com/products/autodl/j2se'
                    systemProperties 'generic.client.serverUrl' : project.properties['fast.skif.serverUrl']
                    systemProperties 'generic.client.name' : 'Innsynsklient for Fast Eiendom i Grunnboka'
                }
                application.mainClass 'no.statkart.matrikkel.presentasjon.mainframe.MainFrameLauncher'
                addServerURLArgument true
            }
        }
    }

Se
 * [ClientConfiguration](src/main/groovy/no/statkart/sktools/gradle/plugins/webstart/WebstartConvention.groovy)
 * [JnlpConfiguration](src/main/groovy/no/statkart/sktools/gradle/plugins/webstart/WebstartConvention.groovy)
 * [ResourcesConfiguration](src/main/groovy/no/statkart/sktools/gradle/plugins/webstart/WebstartConvention.groovy)


## Signering
Signering er aktivert som standard.
Keystore kan parameteriseres og settes på flere forskjellige måter.

1. Innebygd sertifikat
2. Via properties i bygget/prosjektet. (se Bruk)
3. Via anngivelse av properties på WebstartTask
   ```
    def genWebstart = project.tasks.genWebstart
    genWebstart.keystoreFile = file('some.jks')
    genWebstart.alias = 'devsign'
    genWebstart.password = 'revealed'
   ```
4. Kan også deaktivere signering via
   ```
    client {
        signJars false //false deaktiverer signering av jarfiler
        ...
    }
   ```

## JavaRuntimes
Dersom man ikke deklarerer noen runtime, så legges det automatisk en 1.6+ java runtime til for utviklingsformål.
```
    client {
        ...
        jnlp {
            ...
            resources {
                javaRuntime '1.6+', '64m' //shortcut notation
                javaRuntime '1.6+', '64m', '128m', http://java.sun.com/products/autodl/j2se' //same as above, showing default values
                javaRuntime {   //same as above but only with vmArgs.
                    version '1.6+'
                    href 'http://java.sun.com/products/autodl/j2se'
                    xms '64m'
                    xmx '128m'
                    vmArgs '-verbose -enableassertions'
                }
            }
        }
    }
```
Eksempelet over definerer opp tre like runtimes. Merk at den siste også setter vmArgs, mens de andre ikke gjør det.

JNLP standarden gir mulighet til å definere flere runtimes. I tilfeller med flere runtimes vil runtimes bli valgt i foretrukket rekkefølge; alt ettersom hva som finnes installert på klient-maskinen.
En kan med andre ord spesifisere en (eller flere) foretrukkede versjoner samt fallback versjoner om ønskelig.

JNLP standarden spesifiserer også mulighet til å gruppere runtimes i version feltet:

| Syntax                       | Description                                                                                               |
|------------------------------|-----------------------------------------------------------------------------------------------------------|
| `1.5+`                       | Alle runtimes med versjon 1.5 eller nyere                                                                 |
| `1.5*&1.5.0_10+`             | Alle 1.5x runtimes fra og med versjon 1.5.0_10                                                            |
| `1.5.0_15 1.6.0_20 1.6.0_21` | Betyr en runtime i gruppen. Webstart vil velge den nyeste versjonen som er installert på klient-maskinen. |


