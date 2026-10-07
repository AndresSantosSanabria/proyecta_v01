import { useState, useRef, useEffect, useCallback } from 'react';
import useSpellChecker from '../../hooks/useSpellChecker';
import { buildSpellHighlightHtml } from '../../utils/spellHighlight';
import './SpellCheckerTextarea.css';

export default function SpellCheckerTextarea({
  value = '',
  onChange,
  disabled = false,
  readOnly = false,
  className = '',
  placeholder = '',
  maxLength,
  rows = 4,
  spellCheck = true,
  ...rest
}) {
  const {
    ready,
    loading,
    errors,
    checkDebounced,
  } = useSpellChecker({ enabled: spellCheck });

  const wrapperRef = useRef(null);
  const textareaRef = useRef(null);
  const highlightRef = useRef(null);
  const [hlStyle, setHlStyle] = useState({});

  useEffect(() => {
    const ta = textareaRef.current;
    if (ta) {
      const cs = window.getComputedStyle(ta);
      setHlStyle({
        padding: cs.padding,
        fontFamily: cs.fontFamily,
        fontSize: cs.fontSize,
        lineHeight: cs.lineHeight,
        letterSpacing: cs.letterSpacing,
        wordSpacing: cs.wordSpacing,
      });
    }
  }, []);

  useEffect(() => {
    if (ready && value) {
      checkDebounced(value);
    }
  }, [value, ready, checkDebounced]);

  const syncScroll = useCallback(() => {
    if (highlightRef.current && textareaRef.current) {
      highlightRef.current.scrollTop = textareaRef.current.scrollTop;
      highlightRef.current.scrollLeft = textareaRef.current.scrollLeft;
    }
  }, []);

  useEffect(() => {
    const ta = textareaRef.current;
    if (ta) {
      ta.addEventListener('scroll', syncScroll, { passive: true });
      return () => ta.removeEventListener('scroll', syncScroll);
    }
  }, [syncScroll]);

  // CWE-79: el HTML de resaltado se construye dividiendo el texto original en
  // segmentos y escapando cada uno aisladamente (ver utils/spellHighlight.js).
  const buildHighlightedHTML = useCallback(() => {
    if (!ready || errors.size === 0 || !value) {
      return buildSpellHighlightHtml(value || '', null, { multiline: true });
    }
    return buildSpellHighlightHtml(value, errors, { multiline: true });
  }, [value, ready, errors]);

  const textareaClasses = [
    'spell-textarea',
    loading ? 'spell-loading' : '',
    className,
  ].filter(Boolean).join(' ');

  return (
    <div className="spell-wrapper" ref={wrapperRef}>
      <div
        className="spell-highlight"
        ref={highlightRef}
        aria-hidden="true"
        style={hlStyle}
        dangerouslySetInnerHTML={{ __html: buildHighlightedHTML() }}
      />
      <textarea
        ref={textareaRef}
        className={textareaClasses}
        value={value}
        onChange={onChange}
        disabled={disabled}
        readOnly={readOnly}
        placeholder={placeholder}
        maxLength={maxLength}
        rows={rows}
        spellCheck={false}
        {...rest}
      />
      {loading && <span className="spell-loading-badge">Cargando diccionario...</span>}
    </div>
  );
}

