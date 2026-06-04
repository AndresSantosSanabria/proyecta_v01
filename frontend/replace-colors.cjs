const fs = require('fs');
const path = require('path');

const cssFiles = [
  'src/pages/SecurityConfigPage/SecurityConfigPage.css',
  'src/pages/RiesgosPage/RiesgosPage.css',
  'src/pages/ReportsPage/ReportsPage.css',
  'src/pages/ProjectsPage/ProjectsPage.css',
  'src/pages/ProjectProgressPage/ProjectProgressPage.css',
  'src/pages/ProjectClosurePage/ProjectClosurePage.css',
  'src/pages/NewProjectPage/NewProjectPage.css',
  'src/pages/CronogramaPage/CronogramaPage.css',
  'src/pages/AnalyticsPage/AnalyticsPage.css',
  'src/App.css',
  'src/components/ReportDocumentPreview/ReportDocumentPreview.css',
  'src/components/layout/SidebarLayout/Sidebar.css',
  'src/components/layout/SidebarLayout/SidebarLayout.css',
  'src/components/features/projects/ProjectTable/ProjectTable.css',
  'src/components/features/projects/ProjectListTable/ProjectListTable.css',
  'src/components/features/wizard/Stepper.css',
  'src/components/common/KPICard/KPICard.css',
  'src/components/common/DocumentUpload/DocumentUpload.css'
];

const colorMap = {
  '#2563eb': 'var(--primary)',
  '#1d4ed8': 'var(--primary-hover)',
  '#F8FAFC': 'var(--bg-main)',
  '#f8fafc': 'var(--bg-main)',
  '#FFFFFF': 'var(--bg-card)',
  '#ffffff': 'var(--bg-card)',
  '#fff': 'var(--bg-card)',
  '#F1F5F9': 'var(--bg-muted)',
  '#f1f5f9': 'var(--bg-muted)',
  '#f3f4f6': 'var(--bg-muted)',
  '#0F172A': 'var(--text-main)',
  '#0f172a': 'var(--text-main)',
  '#111827': 'var(--text-main)',
  '#64748B': 'var(--text-muted)',
  '#64748b': 'var(--text-muted)',
  '#6b7280': 'var(--text-muted)',
  '#475569': 'var(--text-muted)',
  '#E2E8F0': 'var(--border-color)',
  '#e2e8f0': 'var(--border-color)',
  '#e5e7eb': 'var(--border-color)',
  '#d1d5db': 'var(--input-border)',
  '#9ca3af': 'var(--input-placeholder)',
  '#94A3B8': 'var(--input-placeholder)',
  '#059669': 'var(--success)',
  '#10b981': 'var(--success)',
  '#ECFDF5': 'var(--success-bg)',
  '#A7F3D0': 'var(--success-border)',
  '#D97706': 'var(--warning)',
  '#f59e0b': 'var(--warning)',
  '#FFFBEB': 'var(--warning-bg)',
  '#FDE68A': 'var(--warning-border)',
  '#DC2626': 'var(--danger)',
  '#ef4444': 'var(--danger)',
  '#FEF2F2': 'var(--danger-bg)',
  '#FECACA': 'var(--danger-border)',
  '#0B0B0F': 'var(--primary-sleek)'
};

function replaceColors() {
  const rootDir = path.resolve('c:/Users/soporteportal/Downloads/Proyecta-Frontend/proyecta');
  
  for (const relativePath of cssFiles) {
    const fullPath = path.join(rootDir, relativePath);
    if (!fs.existsSync(fullPath)) {
      console.log(`Skipping ${relativePath} (does not exist)`);
      continue;
    }
    
    let content = fs.readFileSync(fullPath, 'utf8');
    
    // Si es SecurityConfigPage, remover sus variables locales
    if (relativePath.includes('SecurityConfigPage.css')) {
      content = content.replace(/--bg-app:[^;]+;/g, '');
      content = content.replace(/--bg-surface:[^;]+;/g, '');
      content = content.replace(/--text-main:[^;]+;/g, '');
      content = content.replace(/--text-muted:[^;]+;/g, '');
      content = content.replace(/--border-subtle:[^;]+;/g, '');
      content = content.replace(/--shadow-premium:[^;]+;/g, '');
      content = content.replace(/--radius-xl:[^;]+;/g, '');
      content = content.replace(/--radius-md:[^;]+;/g, '');
      content = content.replace(/--primary-sleek:[^;]+;/g, '');
      // Cambiar var(--bg-app) a var(--bg-main) y var(--bg-surface) a var(--bg-card)
      content = content.replace(/var\(--bg-app\)/g, 'var(--bg-main)');
      content = content.replace(/var\(--bg-surface\)/g, 'var(--bg-card)');
    }
    
    // Reemplazar color-mix que usan #ffffff por var(--bg-card)
    content = content.replace(/color-mix\(in srgb, ([^)]+) \d+%, #ffffff\)/gi, 'color-mix(in srgb, $1 80%, transparent)');
    content = content.replace(/color-mix\(in srgb, ([^)]+) \d+%, #fff\)/gi, 'color-mix(in srgb, $1 80%, transparent)');

    // Reemplazar hex por variables
    for (const [hex, variable] of Object.entries(colorMap)) {
      // Usar un regex que verifique que el hex termine en punto y coma, coma, espacio, llave o paréntesis para evitar matching de subcadenas cortas.
      const regex = new RegExp(hex + '(?=[\\s,;\\)\\!}])', 'g');
      content = content.replace(regex, variable);
    }
    
    // Parcheos específicos de Dark Mode:
    // Algunas cosas pueden tener hex que no fueron mapeados
    // Reemplazamos box-shadow con colores hardcodeados
    content = content.replace(/rgba\(15,\s*23,\s*42,\s*[\d.]+\)/g, 'var(--border-subtle)');

    fs.writeFileSync(fullPath, content, 'utf8');
    console.log(`Updated ${relativePath}`);
  }
}

replaceColors();
