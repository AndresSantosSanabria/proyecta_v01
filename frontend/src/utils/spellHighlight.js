/**
 * Generación segura del HTML de resaltado ortográfico (CWE-79 / OWASP A03).
 *
 * Estrategia: se divide el texto ORIGINAL en segmentos (palabras vs. resto) y se
 * escapa cada segmento aisladamente con escapeHTML(). Así:
 *  1. Ningún carácter del valor crudo llega sin escapar al dangerouslySetInnerHTML.
 *  2. Las entidades generadas (&lt;, &amp;, ...) nunca se vuelven a procesar ni se
 *     rompen envolviendo letras dentro de ellas.
 *  3. Solo las palabras realmente marcadas por el corrector se envuelven en <mark>.
 */

export function escapeHTML(str) {
  if (str == null) return '';
  return String(str)
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;');
}

const WORD_SEGMENT_RE = /([\p{L}\p{M}]+)/u;
const WORD_ONLY_RE = /^[\p{L}\p{M}]+$/u;

function highlightSegment(segment, errors) {
  if (segment && WORD_ONLY_RE.test(segment) && errors?.has(segment.toLowerCase())) {
    const safeWord = escapeHTML(segment);
    return `<mark class="spell-error" data-word="${safeWord}">${safeWord}</mark>`;
  }
  return escapeHTML(segment);
}

/**
 * @param {string} value texto crudo (puede contener HTML malicioso)
 * @param {Set<string>} errors palabras marcadas como erróneas (minúsculas)
 * @param {{ multiline?: boolean }} [options] separa por líneas para <textarea>
 * @returns {string} HTML seguro para innerHTML
 */
export function buildSpellHighlightHtml(value, errors, options = {}) {
  const safeValue = value || '';
  if (!safeValue) return '';

  const renderLine = (line) =>
    line
      .split(WORD_SEGMENT_RE)
      .map((segment) => highlightSegment(segment, errors))
      .join('');

  if (options.multiline) {
    return safeValue
      .split('\n')
      .map((line) => (line ? renderLine(line) : '&nbsp;'))
      .join('\n');
  }
  return renderLine(safeValue);
}
