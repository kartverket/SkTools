# Migrering til Gradle 9

SkTools bruker nå Gradle 9.1.0 (se `gradle/wrapper/gradle-wrapper.properties`). Dette dokumentet
beskriver det ene bruddet som påvirker vanlig bruk av byggscript: DSL-endringen i
`sktools.dbtools`.

## For konsumenter av `sktools.dbtools`: DSL-brudd

Tidligere kunne man kalle `configureDatabasePlugin { ... }` og `taskSequence(...)` direkte på
prosjektet, uten prefiks:

```groovy
// FØR (Gradle < 9)
configureDatabasePlugin {
    toolset(name: 'eksempel', type: 'hsqldb', prefix: 'db') {
        ...
    }

    taskSequence('createViews') {
        dependsOn dbToolSets['eksempel'].tasks['CreateSchema']
    }
}
```

Dette virket fordi disse metodene ble registrert via Gradle sin gamle `Project.getConvention()` /
`Convention`-mekanisme, som "flatet ut" metoder fra et convention-objekt direkte på `project` (dvs.
`project.configureDatabasePlugin(...)` var identisk med `project.db.configureDatabasePlugin(...)`).
`Project.getConvention()` er fjernet i Gradle 9. Erstatningen, `ExtensionContainer`
(`project.getExtensions()`), gjør **kun** selve objektet tilgjengelig som en navngitt property
(`project.db`) — den flater ikke ut metoder på `project`. Dette er en villet, permanent
design-endring fra Gradle sin side (de anser method-flattening som "for magisk"), så det finnes
ingen "fiks" i pluginen som gjenoppretter den gamle syntaksen uten en kompatibilitets-shim.

Vi har valgt å oppdatere DSL-en fremfor å legge til en shim. Løsningen er å prefikse kallene med
`db.`:

```groovy
// ETTER (Gradle 9+)
db.configureDatabasePlugin {
    toolset(name: 'eksempel', type: 'hsqldb', prefix: 'db') {
        ...
    }

    db.taskSequence('createViews') {
        dependsOn dbToolSets['eksempel'].tasks['CreateSchema']
    }
}
```

Merk:
* Kun **topp-nivå** kall (direkte på `project`) trenger `db.`-prefiks.
* `taskSequence(...)` kalt inni en `toolset { }` eller `patch { }`-blokk trenger **ikke** endres,
  siden disse løses via closure-delegation til riktig objekt og ikke via `Project`-flattening.
* Ingen andre endringer i selve DSL-en (toolset-, patch- og task-konfigurasjon er uendret).

Denne endringen påvirker kun `sktools.dbtools`. Ingen av de andre pluginene i dette repoet
(`sktools.properties`, `sktools.wsdl-customizer`, `sktools.wsdlgen`, `sktools.wsdoc`,
`sktools.wsgen`, `sktools.wsimport`, `sktools.xjc`) brukte denne convention-baserte
method-flattening-mekanismen.
