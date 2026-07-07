@echo off
setlocal enabledelayedexpansion

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
set APP_HOME=%DIRNAME%

if exist "C:\Program Files\Amazon Corretto\jdk25.0.3_9\bin\java.exe" (
  set "JAVA_HOME=C:\Program Files\Amazon Corretto\jdk25.0.3_9"
)

if "%JAVA_HOME%"=="" (
  set JAVA_EXE=java.exe
) else (
  set JAVA_EXE=%JAVA_HOME%\bin\java.exe
)

set WRAPPER_JAR=%APP_HOME%.mvn\wrapper\maven-wrapper.jar

"%JAVA_EXE%" -classpath "%WRAPPER_JAR%" -Dmaven.multiModuleProjectDirectory=%APP_HOME% org.apache.maven.wrapper.MavenWrapperMain %*

if %ERRORLEVEL% NEQ 0 exit /b %ERRORLEVEL%
endlocal
