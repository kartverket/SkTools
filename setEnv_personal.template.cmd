:
: SETTER BRUKER/MASKIN SPESEFIKE ENVIRONMENT VARIABLE FOR HOVEDPROSJEKTET
:


: *******************************************************************************************************************
: * Gradle & Project Setup
: *******************************************************************************************************************
set JAVA_HOME=C:\bin\jdk\jdk1.8.0_202
set GRADLE_HOME=C:\bin\gradle\gradle-4.10.2

@rem bestemmer versjon av prosjektet [optional - m� benyttes p� ikke numeriske kodebrancher ala trunk]
@rem set ORG_GRADLE_PROJECT_sktools_versjon=1.6-SNAPSHOT



: *******************************************************************************************************************
: * Weblogic Setup - 12.1.3 (java 8)
: *******************************************************************************************************************
set WEBLOGIC_HOME=C:\bea1213
set WEBLOGIC_VERSION=12.1.3


: *******************************************************************************************************************
: * Maven Repo & Nexus Setup  (optional)
: *******************************************************************************************************************
: Definer lokal nexus repo for offline utvikling (optional)
@rem set MAVEN_REPO=http://localhost:8081/repository/public/

: Definerer repo for deploying av bygget applikasjon (nexus)
@rem set MAVEN_PUBLISH=http://admin:admin123@nexus.statkart.no:8090/nexus/content/repositories/releases/


: *******************************************************************************************************************
: * Perforce for Windows (optional)
: *******************************************************************************************************************
@rem p4 set P4CLIENT=%USERNAME%-%COMPUTERNAME%
@rem p4 set P4PORT=perforce.statkart.no:1666
@rem p4 set P4USER=%USERNAME%

