/**
 * Descarga un Blob como archivo local (sin abrirlo en el navegador).
 *
 * Centraliza un patron repetido en multiples componentes y evita dejar
 * ObjectURLs huerfanas en memoria.
 */
export function downloadBlob(blob, fileName) {
  if (!blob) return false;
  const objectUrl = URL.createObjectURL(blob);
  try {
    const link = document.createElement('a');
    link.href = objectUrl;
    link.setAttribute('download', fileName || 'archivo.pdf');
    document.body.appendChild(link);
    link.click();
    link.remove();
    return true;
  } finally {
    URL.revokeObjectURL(objectUrl);
  }
}

export default downloadBlob;
