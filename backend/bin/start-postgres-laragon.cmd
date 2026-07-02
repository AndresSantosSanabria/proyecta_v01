@echo off
setlocal EnableExtensions EnableDelayedExpansion

rem Laragon PostgreSQL starter for this project.
rem - Removes stale postmaster.pid when PostgreSQL is not running.
rem - Starts the PostgreSQL 17.9 cluster used by Laragon.
rem - Falls back to the older pgsql layout if needed.

set "DATA_DIR=C:\laragon\data\postgresql"
set "LOG_FILE=%DATA_DIR%\postgresql-start.log"
set "PG_CTL=C:\laragon\bin\postgresql\pgsql-17.9\bin\pg_ctl.exe"

if not exist "%PG_CTL%" set "PG_CTL=C:\laragon\bin\postgresql\pgsql\bin\pg_ctl.exe"

if not exist "%PG_CTL%" (
    echo No se encontro pg_ctl.exe en Laragon.
    echo Revisa que PostgreSQL este instalado en C:\laragon\bin\postgresql.
    exit /b 1
)

if not exist "%DATA_DIR%" (
    echo No se encontro el directorio de datos: "%DATA_DIR%"
    exit /b 1
)

"%PG_CTL%" -D "%DATA_DIR%" status >nul 2>&1
if not errorlevel 1 (
    echo PostgreSQL ya esta activo en 127.0.0.1:5432
    exit /b 0
)

if exist "%DATA_DIR%\postmaster.pid" (
    echo Detectado postmaster.pid huerfano. Limpiando bloqueo...
    del /F /Q "%DATA_DIR%\postmaster.pid"
)

echo Iniciando PostgreSQL desde Laragon...
"%PG_CTL%" -D "%DATA_DIR%" -l "%LOG_FILE%" start
if errorlevel 1 (
    echo Fallo el arranque. Revisa el log:
    echo "%LOG_FILE%"
    exit /b 1
)

timeout /t 3 /nobreak >nul

"%PG_CTL%" -D "%DATA_DIR%" status >nul 2>&1
if errorlevel 1 (
    echo PostgreSQL no quedo activo. Revisa el log:
    echo "%LOG_FILE%"
    exit /b 1
)

echo PostgreSQL listo en 127.0.0.1:5432
exit /b 0
