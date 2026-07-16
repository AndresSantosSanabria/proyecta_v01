@echo off
echo ============================================================
echo   DIAGNOSTICO DE CORREOS - PROYECTA
echo ============================================================

echo.
echo [1] Verificando conexion SMTP con Office 365...
curl.exe -s --url "smtp://smtp.office365.com:587" --ssl-reqd ^
  --mail-from "notificaciones_proyecta@cundinamarca.gov.co" ^
  --mail-rcpt "notificaciones_proyecta@cundinamarca.gov.co" ^
  --upload-file nul ^
  --user "notificaciones_proyecta@cundinamarca.gov.co:kAx7WABR2gus" 2>&1 | findstr /i "235 authentication failed error"

echo.
echo [2] Ultimos envios de correo (base de datos)...
set PGPASSWORD=admin
"C:\laragon\bin\postgresql\14.5\bin\psql.exe" -h localhost -p 5432 -U postgres -d postgres -c ^
  "SET search_path TO proyecta_db; SELECT event_code, recipient, status, failure_reason, created_at FROM notification_audit WHERE channel='EMAIL' ORDER BY created_at DESC LIMIT 10;" 2>nul || echo [PSQL no disponible directamente - use CheckMail.java]

echo.
echo [3] Proceso de Spring Boot en puerto 8082:
netstat -ano | findstr :8082 | findstr LISTENING
echo.
echo [4] RECUERDA: Debes REINICIAR el backend para aplicar cambios de configuracion.
echo ============================================================
pause
