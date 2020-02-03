# Fremgangsmåte for migrering av Perforce kildekode repo til GIT
Dette prosjektet inneholder rutine for portering av kildekode til git.
Fremgangsmåten forutsetter at perforce repo avsluttes og fryses. Videre utvikling skjer på git. 

Kun aktuelle brancher tas med. Evt annet må labes i perforce slik at dette kommer over som git tags.

# Vertøy
GIT har en mengde scripts som tilbyr ekstrafunksjonalitet.   
Disse ligger i prosjektet [for GIT på github](https://github.com/git/git) og er kopiert inn i [bin mappen](bin).
For å kjøre disse kreves det at Python er installert.


# Forutsetninger

Denne runtinen er testet med:
 * Perforce client: P4/NTX64/2018.2/1751184
 * Git: 2.23.0.windows.1
 * Python: 2.7.16

Git config må minst ha:

    $ git config --global --list
    user.name=skumag
    user.email=skumag@kartverket.no
    core.autocrlf=input
    git-p4.retries=100
    git-p4.pathencoding=iso8859-1
    git-p4.port=perforce.statkart.no:1666
    
For å sette manglende instillinger:

    git config --global core.autocrlf input
    git config --global git-p4.retries 100
    git config --global git-p4.pathencoding iso8859-1
    git config --global git-p4.port perforce.statkart.no:1666


# Oppskrift

Perforce credentials settes best via environment variable:

    export P4USER=lislei
    export P4PASSWD=***
    
Det kreves også at alle brancher er nevnt i branch-mappings.    
Det kan være at man må fjerne utgåtte branch-mappinger. 
En hadde hell med å samle de til en felles branch mapping for SKToolsKode slik: 

    //sktools/SKToolsKode/trunk/... //sktools/SKToolsKode/1.2/...
    //sktools/SKToolsKode/trunk/... //sktools/SKToolsKode/1.3/...
    //sktools/SKToolsKode/trunk/... //sktools/SKToolsKode/1.4/...
    //sktools/SKToolsKode/trunk/... //sktools/SKToolsKode/1.5/...
    //sktools/SKToolsKode/trunk/... //sktools/SKToolsKode/5.x/...

## Kjøring
Det anbefales å ta vare på konverteringsrutinen for å dokumentere hvordan commitene ble konvertert.
Dette gjøres ved å sjekke den inn på en "perforce-migrering" branch. Til slutt tagges denne sammen med scriptet som ble kjørt.

    git checkout --orphan perforce-migrering


Kjør [script for migrering](konverter_sktools.sh) i git bash og lagre output til fil.
Se [konverter_sktools.log](konverter_sktools.log).

