Verkøy for patching og oppsett av database.

Verktøyet benytter JDBC for kommunikasjon mot database og krever driver vedlagt på java classpath.

# Artifakt
Artefaktet har maven koordinat `"no.statkart.sktools:db-tools:5.7"`

# Changelog
## Unreleased Changes

## 1.2.0 Release Notes
* [SKTOOLS-33] Database-patcher funksjonalitet ifra matrikkelen
* Samkjører versjonsnumre i hele SKTools

## 1.0.0 Release Notes
* Versjon 1 - flyttet kildekode og endret plugin-navn og namespace


# Bruk
Se [no.statkart.sktools.utils.databasepatcher.DatabasePatcher.printUsage()](src/main/java/no/statkart/sktools/utils/databasepatcher/DatabasePatcher.java)

## DatabasePatcher :: Patch
Eksekvering patcher opp databasen til siste patchversjon og patchnummer.
Dette krever at patchnummer allerede er definert for basen - unntatt dersom `DB.MIN.VERSION=<any>`

Dersom eksekvering feiler med sql-feil kan denne overstyres med `failOnError=true` eller `failOnWarning=true`.


## DatabasePatcher :: SyncPatch
Rekjører enkelte typer pacher opp til gjeldende patchversjon og patchnummer.
Dette krever at utvalgte patchblokker er skrevet slik at man ikke får feil ved rekjøring av disse blokkene.
Visse feil blir håndtert uten at database-patcheren feiler.
Se `failOnWarning`-parameteren for feiltilstander som blir håndtert som standard.


## Systemvariabler
Følgende System.variable kan settes:

    failOnError - Styrer om eksekveringen skal feile ved sql-feil eller ikke. Default er true.
    failOnWarning - Styrer om eksekveringen skal feile ved sql-warnings eller ikke. Default er true.

    Følgende sql-feilmeldinger er definert som warnings:
        ORA-00942 - "table or view does not exist"
        ORA-00955 - "name is already used by an existing object"
        ORA-01418 - "specified index does not exist"
        ORA-02275 - "such a referential constraint already exists in the table"

        ORA-02443 - "can not drop constraint - nonexistent constraint"

    singlestep - Styrer om man skal legge til en og en patchblokk per eksekvering.


## Ordliste
### Patch
 En samling av patchblokker for å oppgradere skjema struktur til neste patchversjon. Patchen avgrenses til å gjelde for en komponent.

### Patchblokk
En logisk samling av SQL setninger. Hver patchblokk identifiseres med patchversjon og patchnummer.
Det er definert to subtyper patchblokker; skjema- og index patchblokker. I tillegg finnes always.

### Skjema patchblokk-type
Patchblokk for generelle endringer.

### Index patchblokk-type
Index patchblokker er skilt ut da denne har et noe annet bruksmønster.
Ved import/export av en database så følger typiskt ikke indekser med. Det finnes funksjonalitet for enkelt å legge på alle indekser igjen.

### Always patchblokk-type
Patchblokker av denne typen blir alltid eksekvert. Denne bruken er tiltenkt setting av skjema for patching mm.
Dette kan f.eks. gjøres mot Oracle slik: `ALTER SESSION SET CURRENT_SCHEMA = "@substituert.verdi@";`


### Patchversjon (DBVersion)
Beskriver versjon av systemet. Dette er som regel det samme som hovedversjonen av applikasjonen som går mot skjemaet.

### Patchnummer
Nummer som identifiserer patchblokk. I tillegg til patchversjon så beskriver denne antall patcher som er lagt inn til gjeldende patchversjon.

### Komponent
Avgrensing som inndeler skjemaet inn i forskjellige moduler. Dette gjør at man kan splitte opp omfanget og avtrense ansvarsområdet for patching til ovenliggende implementerende applikasjoner.



## Konfigurering

### Database patcher

#### SQL syntax til patchefil
Innholdet i filen er skrevet slik at man skal kunne kjøre hele eller deler av filen manuellt som ren SQL via andre verktøy.

Kommentarer kan legges inn via linjer som starter på `--`

Man har mulighet til å anngi minste patchversjon patchfilen forutsetter. `<any>` angir alle databaseversjoner:
```sql
-- Denne patchfilen er uavhengig av evt tidligere patcher
-- PATCH DB.MIN.VERSION="<any>"
```

Alle patchblokker må ha stigende patchversjon og patchnummer.

Syntaks for patchblokk er: `-- PATCH (DATA|SCHEMA|INDEX|ALWAYS|<custom>) DB.VERSION="<string>" PATCH.NO="<number>" ["<kommentar>"]`

Eksempel patch.sql:
```sql
-- PATCH ALWAYS DB.VERSION="1.0" PATCH.NO="0" "Setter skjema for påfølgende patcher"
ALTER SESSION SET CURRENT_SCHEMA = "@substituert.verdi@";

-- PATCH DATA DB.VERSION="1.0" PATCH.NO="1" "JIRA-1: Oppretter brukertabell"
create table bruker
 (
   brukerId number(19,0) not null,
   brukernavn varchar(64) not null,
   primary key (brukeridId)
 )
;

-- PATCH INDEX DB.VERSION="1.0" PATCH.NO="3" "JIRA-2: Optimalisering av brukerservice"
-- Indeks for spørring etter brukernavn
create index bruker_IX_brukernavn on bruker(brukernavn)
```


## Strategi for patching av komponenter
En har enkelte ganger noen script som alltid er oppdaterte. Disse er da avhengig av siste patchversjon.

Eksempler på dette kan være indexer hvor en har som strategi IKKE å inkludere disse i dumpfiler ved import/export.
Algoritmen her er da å legge til siste versjon av disse ved import. Ved patching så skal indexer legges til fortløpende.
Dette fører til en dobbeltføring av indexer, men en har til en hver tid full oversikt over alle indexer som gjelder til siste patchversjon.

Et annet eksempel er deklarering av view og triggere. Her finnes samme syntax for oppdatering som kreering.
Utfordringen er altså å kjøre inn riktig versjon av scriptene ihht hvilken versjon som er gjeldende.

I gradle har man mulighet til å skippe eksekvering av tasker via onlyIf().
En har her mulighet til å hoppe over eksekvering dersom innholdet i denne evalueres til false rett før kjøring.
I et fler-modul system så må man her sammenstille versjonen for flere moduler og sjekke at disse møter bestemte krav.

Dersom man i importen har views inne og en ønsker å patche opp disse blir algoritmen slik at man først dropper alle aktuelle views for deretter å legge disse inn igjen.
En må da her utvikle tasks med tilhørende script for både sletting og kreering av views for hver produksjonsversjon.

Tilsvarende vil det være for indekser.

Det kan være en god ide å integrere bruk av disse avleverings-scriptene til tools branchen for bruk ved deployment mm.
Strategien bør da være å lenke inn scriptene ifra hver branch til gradle-bygget som finnes der.



# Historie
Forgjengeren til dette vertøyet kom fra matrikkelen og ble benyttet bla i API over Grunnboka og senere i Grunnboka.
