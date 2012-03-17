@rem
@rem SETTER BRUKER/MASKIN SPESEFIKKE ENVIRONMENT VARIABLE FOR HOVEDPROSJEKTET
@rem

@rem Java, Gradle
set JAVA_HOME=C:\Program Files\Java\jdk1.6.0_21
set GRADLE_HOME=C:\bin\gradle-1.0-milestone-7

@rem Groovy (optional)
set GROOVY_HOME=C:\bin\groovy-1.8.4

@rem Set Weblogic
set WEBLOGIC_HOME=C:\bea_wls10.3.1

@rem Definer lokal nexus repo for offline utvikling (optional)
@rem set REPO_PUBLIC=http://localhost:8081/nexus/content/repositories/statkart/

@rem Setup Perforce windows integrasjon (optional)
p4 set P4USER=%USERNAME%
p4 set P4CLIENT=%P4USER%-%COMPUTERNAME%
p4 set P4PORT=skrivap42.statkart.no:1666

