Verkøy som genererer dokumentasjon av JAX-WS implementerte webservicer.

Verktøyet er implementert som en plugin til `javac` - se Pluggable Annotation Processing (JSR 269).

# Artifakt
Artefaktet har maven koordinat `"no.statkart.sktools:wsdocgen:5.7"`

# Changelog
## Unreleased Changes

## 1.4.0 Release Notes
* [SKTOOLS-135] bug: nullpointer ved manglende kildekode
* [SKTOOLS-134] bug: description element manglet for parameter-element i xml

## 1.3.0 Release Notes
* [SKTOOLS-105] Mulighet for generering av index
* [SKTOOLS-101] Re-implementert verktøyet i Pluggable Annotation Processing API - et plugin til javac
  Fleksibelt output konfigurerbart via parameterisert XSLT fil.

## 1.2.0 Release Notes
* Samkjører versjonsnumre i hele SKTools

## 0.3.0 Release Notes
* Opprettet dokumentasjon samt tester


# Bruk
Dette er en plugin implementert i for kjøring sammen med `javac`.

Koden genererer html-filer, en for hver service funnet.
Navnet på genererte filer blir `<servicenavn>.html` hvor servicenavn er navnet definert i verdien til
`@javax.jws.WebService` annotasjon.


Strukturen til html-filer styres ved å parameterisere en egendefinert xslt transformasjonsfil.

## Konfigurering
Javac konfigureres via følgende "compiler args":

| Argument                                                                 | Use      | Description                                                             |
|--------------------------------------------------------------------------|----------|-------------------------------------------------------------------------|
| `-processor no.statkart.sktools.utils.wsdocgen.processor.WSDocProcessor` | Required | Activates this plugin.                                                  |
| `-Axslt=<xsl:transform.file>`                                            | Required | Stylesheet for transform of output.                                     |
| `-AindexXslt=<xsl:transform.file> `                                      | Optional | Stylesheet for transform of output.                                     |
| `-AjavaDocLookupPath=../javadoc/index.html`                              | Optional | Path to generated javadoc for JAXB classes referenced from web service. |
| `-proc:only`                                                             | Optional | Processing of annotation only (no compile).                             |

## PS: XSLT må spesifiseres
Merk at pluginen ikke genererer xsl stilsett selv, dette parameteriseres ifra byggesystemet.
Se kildekode og
[tester for eksempel-implementasjon](src/test/groovy/no/statkart/sktools/utils/wsdocgen/processor/WSDocProcessorTest.groovy)
samt utvidet dokumentasjon av virkemåte.

## XML-struktur
Se [WSDocProcessor.java](src/main/java/no/statkart/sktools/utils/wsdocgen/processor/WSDocProcessor.java)
for struktur av XML-data for transform.

## Hva er JSR 269?
Pluggable Annotation Processing API er en utvidelse av java apiet som er ment for håndtering av annotasjoner. APIet er skrevet for å være fremoverkompatibelt, men visse utvidelser i form av nye metoder og egenskaper må påberegnes.
Fra og med Java6 så har javac støtte for å hekte inn slike plugin ved kompilering av kildekoden.

Se offisiell [dokumentasjon her](https://www.jcp.org/en/jsr/detail?id=269).


# Historie
Forgjengeren til JSR-269 som kom i var Annotation Processing Tool (APT) som ble deprekert i JDK7 og fjernet i JDK8.
Første versjon av pluginet ble implementert med APT apiet.

### Hva var APT?
APT sto for Anntoation Procession Tool og ble introdusert som en del av Java 5 JDK. Bruksmønsteret for apt var å lage plugins som leste annoteringer i kildekoden før kompilering. En kunne dermed ekstrahere informasjon som ikke fantes runtime.
Gode eksempler er autogenerering av kildekode og dokumentasjon.