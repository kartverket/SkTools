:
: SETTER ENVIRONMENT VARIABLE FOR PROSJEKT
:

@echo off
@mode con codepage select=865

: Setter hovedkatalog for prosjektet. PROJECT_ROOT inneholder '\' til slutt derfor må det stå en '.' til slutt
rem %~dp0 is name of current script under NT
set PROJECT_ROOT=%~dp0.

: leser inn bruker-spesifike settings
if exist setEnv_personal.cmd call setEnv_personal.cmd


: Window title
if "%TITLE%"=="" (
   for /D %%P in (%PROJECT_ROOT%) do (
      set TITLE=SktoolsKode - %%~nxP
   )
)
title %TITLE%


: Java Setup
@echo JAVA_HOME=%JAVA_HOME%
if not "%JAVA_HOME%"=="" goto CONFIGURE_JAVA_HOME
  @echo   JAVA_HOME environment variable er ikke satt. Har du glem å sette den i setEnv_personal.cmd?
  set ERROR=true
  goto END_JAVA_HOME
:CONFIGURE_JAVA_HOME
  set PATH=%JAVA_HOME%\bin;%PATH%
:END_JAVA_HOME


: Gradle Setup
@echo GRADLE_HOME=%GRADLE_HOME%
if not "%GRADLE_HOME%"=="" goto CONFIGURE_GRADLE_HOME
  @echo   GRADLE_HOME environment variable er ikke satt. Har du glem å sette den i setEnv_personal.cmd?
  set ERROR=true
  goto END_GRADLE_HOME
:CONFIGURE_GRADLE_HOME
  set PATH=%GRADLE_HOME%\bin;%PATH%
  set GRADLE_OPTS=-XX:MaxPermSize=256m
:END_GRADLE_HOME


: Groovy Setup (optional)
if "%GROOVY_HOME%"=="" goto END_GROOVY_HOME
:CONFIGURE_END_GROOVY_HOME
  set PATH=%PATH%;%GROOVY_HOME%\bin
:END_GROOVY_HOME



: Project Setup
if "%ORG_GRADLE_PROJECT_sktools_versjon%"=="" (
   for /D %%P in (%PROJECT_ROOT%) do (
      set ORG_GRADLE_PROJECT_sktools_versjon=%%~nxP
      @echo Setter sktools_versjon til '%%~nxP'
   )
)



: Weglogic Setup
if "%WEBLOGIC_HOME%"=="" (
  @echo   WEBLOGIC_HOME environment variable er ikke satt. Har du glem å sette den i setEnv_personal.cmd?
  set ERROR=true
)
if "%WEBLOGIC_VERSION%"=="" (
  @echo   WEBLOGIC_VERSION environment variable er ikke satt. Har du glem å sette den i setEnv_personal.cmd?
  set ERROR=true
)


: Maven Repo & Nexus Setup
if "%MAVEN_REPO%"=="" (
   @echo Setter std Maven Repository for utvikling [felles]
   set MAVEN_REPO=http://nexus.statkart.no:8090/nexus/content/groups/public/
)
if "%REPO_UPLOAD_RELEASES%"=="" (
   @echo Setter std Maven Repository for utvikling releases
   set REPO_UPLOAD_RELEASES=http://admin:admin123@nexus.statkart.no:8090/nexus/content/repositories/releases/
   set ORG_GRADLE_PROJECT_REPO_UPLOAD_RELEASES_USERNAME=admin
   set ORG_GRADLE_PROJECT_REPO_UPLOAD_RELEASES_PASSWORD=admin123
)


if defined ERROR (
  color 04
  echo.
  echo Feil i oppsett!
  goto exit
)

@gradle --version

:exit
