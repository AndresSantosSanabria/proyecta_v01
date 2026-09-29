import { useState, useRef, useEffect, useCallback } from 'react';
import useSpellChecker from '../../hooks/useSpellChecker';
import { buildSpellHighlightHtml } from '../../utils/spellHighlight';
import './SpellCheckerTextarea.css';

export default function SpellCheckerInput({
  value = '',
  onChange,
  disabled = false,
  readOnly = false,
  className = '',
  placeholder = '',
  maxLength,
  type = 'text',
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
  const inputRef = useRef(null);
  const highlightRef = useRef(null);
  const [hlStyle, setHlStyle] = useState({});

  useEffect(() => {
    const inp = inputRef.current;
    if (inp) {
      const cs = window.getComputedStyle(inp);
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
    if (highlightRef.current && inputRef.current) {
      highlightRef.current.scrollLeft = inputRef.current.scrollLeft;
    }
  }, []);

  useEffect(() => {
    const inp = inputRef.current;
    if (inp) {
      inp.addEventListener('scroll', syncScroll, { passive: true });
      return () => inp.removeEventListener('scroll', syncScroll);
    }
  }, [syncScroll]);

  // CWE-79: el HTML de resaltado se construye dividiendo el texto original en
  // segmentos y escapando cada uno aisladamente (ver utils/spellHighlight.js).
  const buildHighlightedHTML = useCallback(() => {
    if (!ready || errors.size === 0 || !value) {
      return buildSpellHighlightHtml(value || '', null);
    }
    return buildSpellHighlightHtml(value, errors);
  }, [value, ready, errors]);

  const inputClasses = [
    'spell-input',
    loading ? 'spell-loading' : '',
    className,
  ].filter(Boolean).join(' ');

  return (
    <div className="spell-wrapper spell-wrapper-input" ref={wrapperRef}>
      <div
        className="spell-highlight spell-highlight-input"
        ref={highlightRef}
        aria-hidden="true"
        style={hlStyle}
        dangerouslySetInnerHTML={{ __html: buildHighlightedHTML() }}
      />
      <input
        ref={inputRef}
        type={type}
        className={inputClasses}
        value={value}
        onChange={onChange}
        disabled={disabled}
        readOnly={readOnly}
        placeholder={placeholder}
        maxLength={maxLength}
        spellCheck={false}
        {...rest}
      />
      {loading && <span className="spell-loading-badge spell-loading-badge-input">...</span>}
    </div>
  );
}

