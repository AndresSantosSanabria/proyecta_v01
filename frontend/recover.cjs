const fs = require('fs');
const transcript = fs.readFileSync('C:/Users/soporteportal/.gemini/antigravity-ide/brain/cfa1097a-fa26-4696-8ee9-59db0ec34fb7/.system_generated/logs/transcript.jsonl', 'utf8');
const lines = transcript.split('\n');
let fileLines = {};
for (const line of lines) {
  if (!line) continue;
  if (line.includes('File Path: `file:///c:/Users/soporteportal/Downloads/Proyecta-Frontend/proyecta/src/pages/SecurityConfigPage/SecurityConfigPage.jsx`')) {
    const obj = JSON.parse(line);
    const textStr = JSON.stringify(obj);
    const splitText = textStr.split('\\n');
    for (let cl of splitText) {
      const match = cl.match(/^(\d+): (.*)$/);
      if (match) {
        let text = match[2];
        if (text.endsWith('"}')) text = text.slice(0, -2);
        fileLines[parseInt(match[1])] = text.replace(/\\\\/g, '\\').replace(/\\"/g, '"');
      }
    }
  }
}
const keys = Object.keys(fileLines).map(Number);
if (keys.length === 0) {
  console.log('No lines found!');
  process.exit(1);
}
const maxLine = Math.max(...keys);
let output = '';
for (let i = 1; i <= maxLine; i++) {
  if (fileLines[i] !== undefined) {
    output += fileLines[i] + '\n';
  } else {
    output += '// MISSING LINE ' + i + '\n';
  }
}
fs.writeFileSync('C:/Users/soporteportal/Downloads/Proyecta-Frontend/proyecta/recovered.jsx', output);
console.log('Recovered up to line', maxLine);
