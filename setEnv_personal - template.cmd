@rem
@rem SETTER BRUKER/MASKIN SPESEFIKE ENVIRONMENT VARIABLE FOR HOVEDPROSJEKTET
@rem


@rem *******************************************************************************************************************
@rem * Gradle & Project Setup
@rem *******************************************************************************************************************
set JAVA_HOME=C:\Program Files\Java\jdk1.6.0_21
set GRADLE_HOME=C:\bin\gradle\gradle-1.2
set GRADLE_HOME=C:\bin\gradle\gradle-1.5
set GRADLE_HOME=C:\bin\gradle\gradle-1.6

@rem bestemmer versjon av prosjektet [optional - må benyttes på ikke numeriske kodebrancher ala trunk]
@rem set ORG_GRADLE_PROJECT_sktools_versjon=1.2-SNAPSHOT


@rem *******************************************************************************************************************
@rem * Weblogic Setup
@rem *******************************************************************************************************************
set WEBLOGIC_HOME=C:\bin\bea1035
set WEBLOGIC_VERSION=10.3.5
@rem LOCALHOSTNAME bestemmer gyldig hostname for bruk med sertifikater (default til %COMPUTERNAME%.statkart.no)
@rem set LOCALHOSTNAME=%COMPUTERNAME%.statkart.no


@rem *******************************************************************************************************************
@rem * Maven Repo & Nexus Setup  (optional)
@rem *******************************************************************************************************************
@rem Definer lokal nexus repo for offline utvikling (optional)
@rem set MAVEN_REPO=http://localhost:8081/nexus/content/repositories/statkart/

@rem Definerer repo for deploying av bygget applikasjon (nexus)
@rem set REPO_UPLOAD_RELEASES=http://admin:admin123@nexus.statkart.no:8090/nexus/content/repositories/releases/
@rem set ORG_GRADLE_PROJECT_REPO_UPLOAD_RELEASES_USERNAME=admin
@rem set ORG_GRADLE_PROJECT_REPO_UPLOAD_RELEASES_PASSWORD=admin123


@rem *******************************************************************************************************************
@rem * Groovy Setup (optional)
@rem *******************************************************************************************************************
@rem set GROOVY_HOME=C:\bin\groovy-1.8.6


@rem *******************************************************************************************************************
@rem * Perforce for Windows integrasjon (optional)
@rem *******************************************************************************************************************
@rem p4 set P4CLIENT=%USERNAME%-%COMPUTERNAME%
@rem p4 set P4PORT=perforce.statkart.no:1666
@rem p4 set P4USER=%USERNAME%

