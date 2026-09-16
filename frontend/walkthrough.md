# Walkthrough - Corrección de Reporte Excel de Seguimiento / Avance

Se ha corregido completamente la generación del reporte Excel de seguimiento/avance en Java para resolver el error `#¿NOMBRE?`, añadir las columnas faltantes (X, Y, Z) y la tabla resumen `INDICADORES AL CORTE`.

## Cambios Realizados

### Backend Java (`SeguimientoExcelGenerator.java`)

1. **Corrección de Fórmula `HOY()`**:
   - Se cambió `c17.setCellFormula("TODAY()")` por `c17.setCellFormula("HOY()")`.
   - **Causa raíz**: En Excel en español, la función en XML para la fecha actual requiere `HOY()`. Al usar `TODAY()`, Excel no la reconocía y mostraba `#¿NOMBRE?`. Como todas las columnas de cálculo (S, T, X, Y, Z e indicadores) dependen de la columna R (`HOY`), la falla se propagaba a toda la hoja.

2. **Incorporación de Columnas X, Y, Z**:
   - **Columna X (24)**: `EyE Incluye?` (`=IF(I{r}<=R{r},"Si","No")`)
   - **Columna Y (25)**: `Eficacia Calcula` (`=IF(AND(I{r}<=R{r},J{r}<>"",L{r}=1),"Si","")`)
   - **Columna Z (26)**: `Eficiencia Calcula` (`=IF(AND(I{r}<>"",J{r}<>"",L{r}=1,J{r}<=I{r}),"Si","")`)
   - **Fórmulas de resumen inferior (Fila N+1)**:
     - Columna Y: `=COUNTIFS($Y$5:$Y$N,"Si",$X$5:$X$N,"Si")/COUNTIF($X$5:$X$N,"Si")`
     - Columna Z: `=COUNTIFS($Z$5:$Z$N,"Si",$Y$5:$Y$N,"Si")/COUNTIF($Y$5:$Y$N,"Si")`

3. **Inclusión de la Tabla `INDICADORES AL CORTE`**:
   - Se agregó la sección de indicadores al final del reporte (Filas `N+3` a `N+9`), replicando exactamente la estructura del reporte original:
     - **Fecha de corte**: `=R5`
     - **Programados al corte**: `=COUNTIFS(I5:I{N},"<="&Y{corteRow},I5:I{N},"<>")`
     - **Entregados al corte**: `=COUNTIFS(J5:J{N},"<="&Y{corteRow},J5:J{N},"<>",L5:L{N},1)`
     - **Entregados a tiempo**: `=SUMPRODUCT(--(J5:J{N}<=Y{corteRow}),--(I5:I{N}<>""),--(L5:L{N}=1),--(J5:J{N}<>""),--(J5:J{N}<=I5:I{N}))`
     - **Eficacia**: `=MIN(1,IFERROR(Y{entCorte}/Y{progCorte},0))` (con formato `0.00%` y fondo verde claro)
     - **Eficiencia**: `=MIN(1,IFERROR(Y{entTiempo}/Y{entCorte},0))` (con formato `0.00%` y fondo verde claro)

4. **Nombre de la Hoja**:
   - Se renombró la hoja a `"AVANCE"` para alinearse con la plantilla original.

## Instrucciones para Aplicar en el Backend Local

Copia el archivo actualizado a la carpeta de tu backend Spring Boot:

```powershell
Copy-Item -Path "backend-patch\SeguimientoExcelGenerator.java" -Destination "C:\Users\soporteportal\Pictures\Proyecta\src\main\java\com\proyecta\api_gestion\service\report\SeguimientoExcelGenerator.java" -Force
```

Luego reinicia tu backend backend de Spring Boot y vuelve a descargar el reporte desde la interfaz.
