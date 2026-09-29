/**
 * Crea ObjectURLs seguras para previsualizar blobs en <iframe> (CWE-79 / CWE-829).
 *
 * Un blob: hereda el origen del documento que lo crea: si el servidor devolviera
 * HTML/SVG con scripts, ejecutaría en la SPA con acceso a los tokens. Por eso:
 *  1. Se validan los magic bytes reales (%PDF) del contenido.
 *  2. El blob se fuerza explícitamente a type application/pdf (sin sniffing).
 * Si la validación falla devuelve null y el caller degrada a "descargar".
 */

const PDF_MAGIC = [0x25, 0x50, 0x44, 0x46]; // %PDF

export async function createSafePdfObjectUrl(blob) {
  if (!blob || typeof blob.slice !== 'function') {
    return null;
  }
  try {
    const header = new Uint8Array(await blob.slice(0, 4).arrayBuffer());
    const isPdf =
      header.length === 4 &&
      header.every((byte, index) => byte === PDF_MAGIC[index]);
    if (!isPdf) {
      return null;
    }
    const safeBlob = new Blob([blob], { type: 'application/pdf' });
    return URL.createObjectURL(safeBlob);
  } catch {
    return null;
  }
}
