const ExcelJS = require('exceljs');
const path = require('path');

async function analyzeExcel() {
    const filename = path.join(__dirname, 'reporte-actual-proyecto-PROY-CUN-2026-040 (3).xlsx');
    const workbook = new ExcelJS.Workbook();
    try {
        await workbook.xlsx.readFile(filename);
        console.log("Worksheets:");
        workbook.worksheets.forEach((ws, i) => console.log(`${i}: ${ws.name}`));
        
        const worksheet = workbook.worksheets.find(ws => ws.name !== 'DATOS') || workbook.worksheets[0];
        console.log(`\nAnalyzing Worksheet: ${worksheet.name}`);
        
        worksheet.eachRow((row, rowNumber) => {
            if (rowNumber > 0 && rowNumber < 15) { 
                const rowData = {};
                row.eachCell({ includeEmpty: true }, (cell, colNumber) => {
                    rowData[colNumber] = {
                        value: cell.value,
                        formula: cell.formula,
                        type: cell.type,
                    };
                });
                console.log(`Row ${rowNumber}:`, JSON.stringify(rowData));
            }
        });
        
    } catch (err) {
        console.error("Error reading file:", err);
    }
}

analyzeExcel();
