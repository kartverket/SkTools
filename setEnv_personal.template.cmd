:
: SETTER BRUKER/MASKIN SPESEFIKE ENVIRONMENT VARIABLE FOR HOVEDPROSJEKTET
:


: *******************************************************************************************************************
: * Gradle & Project Setup
: *******************************************************************************************************************
set JAVA_HOME=C:\bin\jdk\jdk1.6.0_45_x86
set GRADLE_HOME=C:\bin\gradle\gradle-2.1

@rem bestemmer versjon av prosjektet [optional - må benyttes på ikke numeriske kodebrancher ala trunk]
@rem set ORG_GRADLE_PROJECT_sktools_versjon=1.4-SNAPSHOT


: *******************************************************************************************************************
: * Weblogic Setup - 10.3.5
: *******************************************************************************************************************
set WEBLOGIC_HOME=C:\bin\bea1035
set WEBLOGIC_VERSION=10.3.5

: *******************************************************************************************************************
: * Weblogic Setup - 10.3.6
: *******************************************************************************************************************
set WEBLOGIC_HOME=C:\bea1036
set WEBLOGIC_VERSION=10.3.6

: *******************************************************************************************************************
: * Weblogic Setup - 12.1.3 (java 8)
: *******************************************************************************************************************
set WEBLOGIC_HOME=C:\bea1213
set WEBLOGIC_VERSION=12.1.3


: *******************************************************************************************************************
: * Maven Repo & Nexus Setup  (optional)
: *******************************************************************************************************************
: Definer lokal nexus repo for offline utvikling (optional)
@rem set MAVEN_REPO=http://localhost:8081/nexus/content/repositories/statkart/

: Definerer repo for deploying av bygget applikasjon (nexus)
@rem set REPO_UPLOAD_RELEASES=http://admin:admin123@nexus.statkart.no:8090/nexus/content/repositories/releases/
@rem set ORG_GRADLE_PROJECT_REPO_UPLOAD_RELEASES_USERNAME=admin
@rem set ORG_GRADLE_PROJECT_REPO_UPLOAD_RELEASES_PASSWORD=admin123


: *******************************************************************************************************************
: * Perforce for Windows (optional)
: *******************************************************************************************************************
@rem p4 set P4CLIENT=%USERNAME%-%COMPUTERNAME%
@rem p4 set P4PORT=perforce.statkart.no:1666
@rem p4 set P4USER=%USERNAME%

