:
: SETTER ENVIRONMENT VARIABLE FOR PROSJEKT
:

@echo off
@mode con codepage select=865

: Setter hovedkatalog for prosjektet. PROJECT_ROOT inneholder '\' til slutt derfor m� det st� en '.' til slutt
rem %~dp0 is name of current script under NT
set PROJECT_ROOT=%~dp0.
cd /d %PROJECT_ROOT%

rem les inn JAVA_HOME, GRADLE_HOME, MAVEN_HOME og andre maskin/bruker spesifike settings
@if exist setEnv_personal.cmd (
  echo Applying setEnv_personal ...
  call setEnv_personal.cmd
) else (
  echo setEnv_personal.cmd does not exists - continuing with current environment settings ...
)


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
  @echo   JAVA_HOME environment variable er ikke satt. Har du glem � sette den i setEnv_personal.cmd?
  set ERROR=true
  goto END_JAVA_HOME
:CONFIGURE_JAVA_HOME
  set PATH=%JAVA_HOME%\bin;%PATH%
:END_JAVA_HOME


: Gradle Setup
@echo GRADLE_HOME=%GRADLE_HOME%
if not "%GRADLE_HOME%"=="" goto CONFIGURE_GRADLE_HOME
  @echo   GRADLE_HOME environment variable er ikke satt. Har du glem � sette den i setEnv_personal.cmd?
  set ERROR=true
  goto END_GRADLE_HOME
:CONFIGURE_GRADLE_HOME
  set PATH=%GRADLE_HOME%\bin;%PATH%
  set GRADLE_OPTS=-XX:MaxPermSize=512m
:END_GRADLE_HOME

title %title% %GRADLE_HOME%



: Project Setup
if "%ORG_GRADLE_PROJECT_sktools_versjon%"=="" (
   for /D %%P in (%PROJECT_ROOT%) do (
      set ORG_GRADLE_PROJECT_sktools_versjon=%%~nxP
      @echo Setter sktools_versjon til '%%~nxP'
   )
)


: Maven Repo & Nexus Setup
if "%MAVEN_REPO%"=="" (
   @echo Setter std Maven Repository for utvikling [felles]
   set MAVEN_REPO=https://nexus.statkart.no/repository/public/
)


if defined ERROR (
  color 04
  echo.
  echo Feil i oppsett!
  goto exit
)

@gradle --version

:exit
