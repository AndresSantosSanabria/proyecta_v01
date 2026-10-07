@echo off
setlocal enabledelayedexpansion

set "DIRNAME=%~dp0"
if "%DIRNAME%"=="" set "DIRNAME=."
set "APP_HOME=%DIRNAME%"

set "JAVA_EXE="

if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"

if not defined JAVA_EXE if exist "%USERPROFILE%\.jdks" (
  for /d %%D in ("%USERPROFILE%\.jdks\jdk*") do (
    if not defined JAVA_EXE if exist "%%~fD\bin\javac.exe" set "JAVA_EXE=%%~fD\bin\java.exe"
  )
)

if not defined JAVA_EXE if exist "%USERPROFILE%\.jdks" (
  for /d %%D in ("%USERPROFILE%\.jdks\jre*") do (
    if not defined JAVA_EXE if exist "%%~fD\bin\java.exe" set "JAVA_EXE=%%~fD\bin\java.exe"
  )
)

if not defined JAVA_EXE (
  for %%P in (
    "C:\Program Files\Amazon Corretto\jdk25.0.3_9"
    "C:\Program Files\Eclipse Adoptium\jdk-25"
    "C:\Program Files\Java\jdk-25"
  ) do (
    if not defined JAVA_EXE if exist "%%~P\bin\java.exe" set "JAVA_EXE=%%~P\bin\java.exe"
  )
)

if not defined JAVA_EXE set "JAVA_EXE=java.exe"

set "PROJECT_DIR=%APP_HOME:~0,-1%"
if "%PROJECT_DIR:~-1%"==":" set "PROJECT_DIR=%APP_HOME%"

set "WRAPPER_JAR=%APP_HOME%.mvn\wrapper\maven-wrapper.jar"
if not exist "%WRAPPER_JAR%" (
  echo [ERROR] No se encuentra el Maven Wrapper: "%WRAPPER_JAR%"
  exit /b 1
)

"%JAVA_EXE%" -classpath "%WRAPPER_JAR%" "-Dmaven.multiModuleProjectDirectory=%PROJECT_DIR%" org.apache.maven.wrapper.MavenWrapperMain %*
set "EXIT_CODE=%ERRORLEVEL%"

if not "%EXIT_CODE%"=="0" exit /b %EXIT_CODE%
endlocal
