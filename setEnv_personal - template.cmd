@rem
@rem SETTER BRUKER/MASKIN SPESEFIKKE ENVIRONMENT VARIABLE FOR HOVEDPROSJEKTET
@rem

@rem Java, Gradle
set JAVA_HOME=C:\Program Files\Java\jdk1.6.0_21
set GRADLE_HOME=C:\bin\gradle\gradle-1.0
set GRADLE_HOME=C:\bin\gradle\gradle-1.2
set GRADLE_HOME=C:\bin\gradle\gradle-1.5

@rem Groovy (optional)
rem set GROOVY_HOME=C:\bin\groovy-1.8.6

@rem bestemmer weblogic home
set ORG_GRADLE_PROJECT_WEBLOGIC_HOME=C:\bea_wls10.3.5

@rem bestemmer versjon av prosjektet [optional - må benyttes på ikke numeriske kodebrancher ala trunk]
@rem set ORG_GRADLE_PROJECT_sktools_versjon=1-SNAPSHOT



@rem echo Definerer lokalt Nexus Maven Reposotory repo for offline utvikling [optional]
@rem set ORG_GRADLE_PROJECT_MAVEN_REPO=http://localhost:8081/nexus/content/repositories/statkart/

@rem echo Definerer Maven Repository for releases [optional]
@rem set ORG_GRADLE_PROJECT_REPO_UPLOAD_RELEASES=http://localhost:8081/nexus/content/repositories/releases/
@rem set ORG_GRADLE_PROJECT_REPO_UPLOAD_RELEASES_USERNAME=admin
@rem set ORG_GRADLE_PROJECT_REPO_UPLOAD_RELEASES_PASSWORD=admin123

@rem echo Definerer Maven Repository for snapshots [optional]
@rem set ORG_GRADLE_PROJECT_REPO_UPLOAD_SNAPSHOTS=http://localhost:8081/nexus/content/repositories/snapshots/
@rem set ORG_GRADLE_PROJECT_REPO_UPLOAD_SNAPSHOTS_USERNAME=admin
@rem set ORG_GRADLE_PROJECT_REPO_UPLOAD_SNAPSHOTS_PASSWORD=admin123


@rem Setup Perforce windows integrasjon (optional)
p4 set P4USER=%USERNAME%
p4 set P4CLIENT=%P4USER%-%COMPUTERNAME%
p4 set P4PORT=perforce.statkart.no:1666

