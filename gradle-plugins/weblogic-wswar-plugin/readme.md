[Weblogic-wswar Gradle Plugin](src/main/groovy/no/statkart/sktools/gradle/plugins/weblogic/wswar/WeblogicWsWarPlugin.groovy)
------------------

Generates web service modules for Weblogic Server.

Installation
------------

Build script snippet for new plugin DSL syntax:

    plugins {
        id 'sktools.weblogic-wswar' version '5.7'
    }

Build script snippet for use in all versions:

    buildscript {
        repositories {
            maven { url 'https://nexus.statkart.no/repository/public/' }
        }
        dependencies {
            classpath 'no.statkart.sktools.gradle:weblogic-wswar-plugin:5.7'
            // or
            classpath 'no.statkart.sktools.gradle:gradle-plugins:5.7'
        }
    }
    apply plugin: 'sktools-weblogic-wswar-plugin'


Changelog
------------
## Unreleased Changes

## 5.6 Release Notes
* [SKTOOLS-226] Fjerne bruk av deprecated `compile` og `runtime` konfigurasjoner

## 1.4.0 Release Notes
* [SKTOOLS-121] Fjernet overflødig WeblogicWarTask

## 1.2.0 Release Notes
* [SKIF-197] Forbedret integrasjon med idea - generert kode blir nå kompilert og lagt i separate kataloger
* [SKIF-197] Eksponerer war task for enkel konfigurasjon
* [SKIF-171] bug: Retter kompile feil når man refererte til annen kildekode innen samme SourceSet
* [SKIF-179] bug: Retter feil i intern struktur av war fil
* [SKIF-213] bug: Unngår provided dependencies i konfigurasjon for war artifakt
* Kompatibel med Gradle 1.0

## 1.1.0 Release Notes
* Refaktorert plugin til valgfritt å kunne integreres med JavaPlugin. Baserer seg nå på JavaBasePlugin.
* Compile og resource tasks får nå satt source og target filer per konvensjon.
* Forbedret konfigurering og lagt til egen Configuration for weblogic avhengigheter.
* Tester

## 1.0.0 Release Notes
* Lagt til dokumentasjon


Bruk
-----
Pluginen integrerer med andre [SourceSet](https://docs.gradle.org/current/dsl/org.gradle.api.tasks.SourceSet.html)
og [configurations](https://docs.gradle.org/current/dsl/org.gradle.api.artifacts.Configuration.html).

Det blir lagt til et sourceSet med navn `weblogic`. All weblogic spesifik implementasjon legges her.
Dersom man ønsker å skille ut felles kode for bruk på klienter ol. kan dette legges til main sourceSet (aktivering av JavaPlugin)

## Dependency håndtering

Pluginen legger til en konfigurasjoner `weblogicRuntime` og `weblogicCompile`.
Egen konfigurasjon med navn `weblogic` legges til for publisering av war artifakt.
Det legges til arv slik at `weblogic` arver ifra `weblogicRuntime` som igjen arver ifra `weblogicCompile`

Dersom JavaPlugin er aktivert, vil dependencies som legges til main sin konfigurasjon bli arvet. Dvs at weblogicCompile vil arve ifra compile.
Dependencies kan således legges til konfigurasjonene weblogicCompile eller compile.

Biblioteker for `tools.jar` og weblogic legges til konfigurasjon med navn `weblogicProvided`


## Kjente feil
> java.lang.RuntimeException: Parsing source file [xxxx.java] failed!
> CAUSE:
> An error has occurred while invoking com.sun.tools.javac.main.JavaCompiler
> to inspect your source files.
> We use the JavaCompiler to obtain import declarations when there are
> STATIC-IMPORT declarations in your source files.
> This situation elicits what is believed to the JavaCompiler has been loaded
> in a different class loader before this time calling. One common case in
> which this happens is when using the 'ant' tool, which uses a special context
> classloader to load classes from tools.jar.

Fiksen her er er å fjerne alle static imports frå alle WSBean-klasser som er input til jwsc, ikkje berre den eine klassa i feilmeldinga.


## Tasks
[genWeblogic](src/main/groovy/no/statkart/sktools/gradle/plugins/weblogic/wswar/WeblogicWsCompileTask.java)
[warWeblogic](https://docs.gradle.org/current/dsl/org.gradle.api.tasks.bundling.War.html)


## Artifakt

Det blir lagt til et war artifakt med weblogic som appendix i filnavnet. Dette blir generert via task

Configuration
------------

    dependencies {
        weblogicCompile project(':server-prosjekt')
    }

    sourceSets {
        weblogic {
            java.srcDir 'src/java'
        }
    }

    warWeblogic {
        from 'src/webapp'
    }

