[DbTools Gradle Plugin](src/main/groovy/no/statkart/sktools/gradle/plugins/dbtools/database/DbtoolsPlugin.java)
------------------

Funksjonalitet for enkel konfigurasjon av verktøy for modifisering og oppsett av database. Pluginen håndterer underliggende kommunikasjon og feilhåndtering mot databaser.
Det er implementert støtte for Oracle Database og HyperSQL (HSQLDB).

Pluginen er et universelt verktøy for enkel kjøring av kall mot databasen over JDBC.
Konfigurasjonen er skrevet for å være deklarativ og fleksibel.
Sql tasker får substituert inn toolsettet-properties via ANT syntaks `@propertynavn@` .

Det finnes også tasker for eksport og import av Oracle-dumper.

Installation
------------

Build script snippet for new plugin DSL syntax:

    plugins {
        id 'sktools.dbtools' version '6.0'
    }

Build script snippet for use in all versions:

    buildscript {
        repositories {
            maven { url 'https://nexus.statkart.no/repository/public/' }
        }
        dependencies {
            classpath 'no.statkart.sktools.gradle:dbtools-plugin:6.0'
            // or
            classpath 'no.statkart.sktools.gradle:gradle-plugins:6.0'
        }

    }
    apply plugin: 'sktools-dbtools-plugin'


Changelog
------------
## Unreleased Changes

## 6.0 Release Notes
* [SKTOOLS-202] Property for sql-tasker er nå implementert med `Property` og `Provider` for konvensjonelle verdier.
    - Groovy syntax skal være baokover-kompatibel med tidligere DSL.

## 1.4.0 Release Notes
 * [SKTOOLS-149] Parameteriserbar angivelse av transform for import.
 * [SKTOOLS-148] Autocommit på connection skal være false. Er nå kompatibel med Oracle 12c jdbc drivere.

## 1.3.0 Release Notes
 * [SKTOOLS-88] felles info task for visning av gjeldende konfigurasjon
 * [SKTOOLS-87] setting av siste eksisterende patchversjon
 * [SKTOOLS-86] mulighet for rekjøring av valgfrie patcher
 * [SKTOOLS-84] riktig bruk av failOnError og failOnWarning samt parameterisering av dette for patche-tasker.
 * [SKTOOLS-77] Patching av valgfritt skjema via 'ALWAYS'
 * [SKTOOLS-81] 'checkSQLTasks' task for validering av konfigurasjon. Denne kan kalles via 'check'.
 * [SKTOOLS-78] database-patcher håndterer verdier for db.version satt til "null" og ikke numeriske versjonsnummer.
 * [SKTOOLS-75] sqlparsing for `CREATE [OR REPLACE] PROCEDURE`

## 1.2.0 Release Notes
 * [SKTOOLS-40] Mulighet for angivelse av `-Dparalell=` verdi for OracleImportTask og OracleExportTask.
 * [SKTOOLS-34] Mulighet for patching moduler (deler av schema) samt strategi for dette. Se forøvrig DatabasePatcher for strategi for patching.
 * [SKTOOLS-33] Lagt til mulighet for patching av schema
 * [SKTOOLS-32] Eksponert variabel toolset for aksess på tvers av scope.
 * [SKTOOLS-16] Deklarativ og fleksibel konfigurasjon
 * [SKTOOLS-27] Forbedret property konfigurasjon slik at export og import tasker fungerer
 * [SKTOOLS-27] failOnError attributt for SQLTask
 * [SKTOOLS-21] Setting av encoding
 * [SKTOOLS-30] Include parameter for importTask() og exportTask()
 * Kompatibel med Gradle 1.0

## 1.0.0 Release Notes
 * Støtte for hsqldb
 * Lagt til to demo-prosjekter (simpledemo og multiple_toolsets)'


Configuration
------------

    configureDatabasePlugin {
        toolset(name:<toolset>, type:<oracle|hsqldb>, prefix:<prefix>) {
            sqlTask('CreateSchema', sqlFile: 'src/sql/createSchema.sql')
            sqlTask('CreateIndexes', sqlFile: 'src/sql/createIndexes.sql', failOnError:false) //continues execution of sql in case of error
            //config for toolset here...

            patch {
                //config for database patcher here...
            }
        }

        taskSequence('createViews') {
            dependsOn dbToolSets[<toolset>].tasks['CreateSchema']
            dependsOn dbToolSets[<toolset>].tasks['CreateIndexes']
            //....
        }
    }


Bruk
----
Tasker blir deklarert i konfigurasjonen og legges til hva en kaller 'toolset'.
Hvert toolset kan kobles til en rekke sql-filer som kan kjøre kommandoer til databasen via SQLTask

Utifra konfigurerte tasker har man mulighet til å konfigurere opp samletasker for eksekvering av ønskede operasjoner. (eks automatisk oppsett av en full database)



## Felles tasks
| Name            | Type         | Description                                                     |
|-----------------|--------------|-----------------------------------------------------------------|
| info            |              | Viser gjeldende konfigurasjon (inkl. properties)                |
| checkSQLTasks   |              | Verifiserer konfigurasjon for alle SQLTask og patche tasks      |
|                 | SQLTask      | Eksekverer innhold fra `.sql`-fil                               |
|                 | SequenceTask | Eksekverer tasker angitt via `dependsOn` i deklarert rekkefølge |


## Felles attributter for tasks
| Name          | Default                        | Description                                                          |
|---------------|--------------------------------|----------------------------------------------------------------------|
| `failOnError` | `true` unless `--continue`     | Dersom `false` avbrytes ikke eksekvering av sql statements ved feil  |
| `sqlFile`     |                                | fil med sql setninger (enten `sqlFile` eller `sqlString` kan anngis) |
| `sqlString`   | SQLTask                        | sql setninger (enten `sqlFile` eller `sqlString` kan anngis)         |
| `encoding`    | `system.encoding`, se encoding | optional angivelse av charset                                        |

## Encoding
Standard encoding for sqlFiler for SQLTask blir satt via System properties.
Dette blir gjort via `sql.file.encoding` system-property dersom satt.
Hvis ikke så defaulter man til standard encoding for filer definert av `file.encoding`

For enkelt å sette encoding, så kan man føye til følgende property for rotprosjektet:
`systemProp.sql.file.encoding=UTF8`

## Konfigurasjon :: toolset
Toolset legges til som illustrert under. Hvor
* `toolset` kan være 'oracle' eller 'hsqldb'
* `prefix` er prefikset alle genererte tasker får. Eksempelvis vil prefix 'Db' og navn 'LoadTestdata' generere task med navn dbLoadTestdata
* `name` er navn for toolset


### Patching
For sikkerhetshensyn og fleksibilitet så anbefales det at patching skjer via egen bruker, eventuelt via systembruker.
Dette gjør også drift-situasjonen med oversiktlig.

For å patche med systembruker må man huske å legge til følgende endringer i oppsett. Skjema må defineres slik at patchinfo tabellen plasseres riktig.
Videre må patche-scriptet (`patch.sql`) endres til å angi skjemanavn for tabell-referanser mm.
Dette kan enten gjøres ved å prefikse med skjemanavnet, eller ved å endre sessionen i oracle slik at alle referanser tolkes relativt til spesifisert skjema.

En kan da legge dette inn slik:
```sql
-- PATCH ALWAYS DB.VERSION="0" PATCH.NO="-1" "Definerer skjema for påfølgende patcher"
ALTER SESSION SET CURRENT_SCHEMA = "@target_db_schema@";
```

For standard tasks, se [AbstractDatabaseConvention.patch(Closure)](src/main/groovy/no/statkart/sktools/gradle/plugins/dbtools/database/util/AbstractDatabaseConvention.groovy)

### Konfigurasjon :: patching
Eksempel konfigurasjon:
```groovy
patch {
    name = 'Komponent'
    schema = target_db_schema //trengs kun for delegert patching med annen bruker

    patchTask('Latest', sqlFile:'src/sql/schema1/patch.sql', description: 'Databasepatcher for mySchema') //task med navn '<prefix>PatchKomponentLatest'
    syncPatchTask('RepatchIndexes', sqlFile:'src/sql/schema1/patch.sql', patchTypes:['INDEX'], description: 'Rekjøring av indekser etter import') //task med navn '<prefix>SyncPatchKomponentReapplyIndexes'
 }
```

### Patch tasker

 * [PatchTask](src/main/groovy/no/statkart/sktools/gradle/plugins/dbtools/database/util/tasks/patch/PatchTask.groovy)
   * Definerer og patcher opp skjema.
 * [IndexesInSyncWithPatchTask](src/main/groovy/no/statkart/sktools/gradle/plugins/dbtools/database/util/tasks/patch/IndexesInSyncWithPatchTask.groovy)
   * Flagger om indexer er up to date med skjema eller ikke.
 * [AssertPatchVersionTask](src/main/groovy/no/statkart/sktools/gradle/plugins/dbtools/database/util/tasks/patch/AssertPatchversionTask.groovy)
   * Viser og verifiserer forventet patchersjon for skjema.
 * [DefinePatchVersionTask](src/main/groovy/no/statkart/sktools/gradle/plugins/dbtools/database/util/tasks/patch/DefinePatchversionTask.groovy)
 og [DefineLatestPatchVersionTask](src/main/groovy/no/statkart/sktools/gradle/plugins/dbtools/database/util/tasks/patch/DefineLatestPatchVersionTask.groovy)
   * Sette patchversjon for skjema i databasen. Sistnevnte bruker siste versjon som patchfil (f.eks.`patch.sql`).


## Oracle toolset
Oracle spesifikke tasker som legges til er

| Konfigurasjon  | Beskrivelse                             | Type                                                                                                                   |
|----------------|-----------------------------------------|------------------------------------------------------------------------------------------------------------------------|
| `importTask()` | Import av dump via Oracles eget verktøy | [OracleImportTask](src/main/groovy/no/statkart/sktools/gradle/plugins/dbtools/database/oracle/OracleImportTask.groovy) |
| `exportTask()` | Export av dump via Oracles eget verktøy | [OracleExportTask](src/main/groovy/no/statkart/sktools/gradle/plugins/dbtools/database/oracle/OracleExportTask.groovy) |


Noen konvensjonelle properties legges også til:

| Navn           | Verdi                                                               | Beskrivelse                                                                                  |
|----------------|---------------------------------------------------------------------|----------------------------------------------------------------------------------------------|
| `db_username`  | `credentials.username`                                              | brukernavn                                                                                   |
| `db_password`  | `db_username` dersom property finnes, `credentials.password` ellers | passord                                                                                      |
| `db_schema`    | `properties['db_username']`                                         | schemanavn                                                                                   |
| `db_oradata`   | `project.properties['db_oradata']`                                  | overstyrer plassering av datafiler der en ønsker å samle alle i en katalog (lokal utvikling) |
| `db_oradata01` | `db_oradata`, ellers `F:\Oradata`                                   | fysisk plassering av datafiler for tablespace                                                |
| `db_oradata02` | `db_oradata`, ellers `G:\Oradata`                                   | fysisk plassering av datafiler for tablespace                                                |
| `db_oradata03` | `db_oradata`, ellers `H:\Oradata`                                   | fysisk plassering av datafiler for tablespace                                                |
| `db_oradata04` | `db_oradata01`                                                      | fysisk plassering av datafiler for tablespace                                                |
| `db_oradata05` | `db_oradata02`                                                      | fysisk plassering av datafiler for tablespace                                                |
| `db_oradata06` | `db_oradata03`                                                      | fysisk plassering av datafiler for tablespace                                                |
| `db_oradata07` | `db_oradata04`                                                      | fysisk plassering av datafiler for tablespace                                                |
| `db_oradata08` | `db_oradata05`                                                      | fysisk plassering av datafiler for tablespace                                                |
| `db_oradata09` | `db_oradata06`                                                      | fysisk plassering av datafiler for tablespace                                                |
| `schemas`      | `[<username>]`                                                      | array av schemanavn                                                                          |
| `dumpfile`     | `"<schemas[0]>_dateString.DMP"`                                     | filnavn for dumper (export/import)                                                           |


#### Eksempel på import og export
```groovy

toolset(name: 'eksempel', type: 'oracle', prefix: 'db') {
  exportTask() {
    exclude = ['STATISTICS', 'INDEX']
    compression = 'NONE'
    dumpfile = gradle.startParameter.mergedSystemProperties.get('dumpfile') ?: "${matrikkel_db_schema}.DMP"
    logfile = gradle.startParameter.mergedSystemProperties.get('logfile') ?: "${matrikkel_db_schema}.LOG"

    doFirst {
        askContinue(it, "Eksport av database ${tns} schema ${getSchemas()} til ${getDumpfile()}, vil du fortsette?")
    }
  }
  importTask() {
    include = ['TABLE/TABLE', 'TABLE/TABLE_DATA', 'TABLE/CONSTRAINT']
    dumpfile = gradle.startParameter.mergedSystemProperties.get('dumpfile') ?: "${matrikkel_db_source_schema}.DMP"
    logfile = gradle.startParameter.mergedSystemProperties.get('logfile') ?: "${matrikkel_db_schema}_import.LOG"
    schemas = [matrikkel_db_source_schema]
    schemaMapping = [
            "${matrikkel_db_source_schema}": matrikkel_db_username,
            "${matrikkel_db_source_schema}_FA": "${matrikkel_fa_db_username}",
    ]
    tableExistsAction = 'REPLACE'

    doFirst {
        askContinue(it, "Import til database ${tns} schema ${getSchemas()} fra ${getDumpfile()}, vil du fortsette?")
    }
  }

  properties = [
          tns: "${matrikkel_db_sid}_${matrikkel_db_hostname}", // resolves against tnsnames.ora
          directory: "MATRIKKEL_${matrikkel_db_dump_dir_name}_DIR",
          schemas: [matrikkel_db_username, matrikkel_fa_db_username],
  ]

  credentials.username = db_system_username
  credentials.password = db_system_password

}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Div hjelpefunksjoner
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
boolean askContinue(Task task, def message, boolean stopExecution = true) {
    def svar = System.console().readLine(' ' + message + ' (j/n) ')
    boolean doContinue = svar.equalsIgnoreCase("j") || svar.equalsIgnoreCase("ja")
    if (stopExecution && !doContinue) {
        println "Bruker valgte å avbryte kjøring av " + task.path
        throw new StopExecutionException("Bruker valgte å avbryte kjøring av" + task.path)
    }
    return doContinue
}
``
