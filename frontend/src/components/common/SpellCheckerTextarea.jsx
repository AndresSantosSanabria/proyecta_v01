import { useState, useRef, useEffect, useCallback } from 'react';
import useSpellChecker from '../../hooks/useSpellChecker';
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
    suggest,
    addToUserDict,
  } = useSpellChecker({ enabled: spellCheck });

  const wrapperRef = useRef(null);
  const textareaRef = useRef(null);
  const highlightRef = useRef(null);
  const [contextMenu, setContextMenu] = useState(null);

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

  const handleContextMenu = useCallback((e) => {
    if (disabled || readOnly || !ready) return;

    const ta = textareaRef.current;
    if (!ta) return;

    const sel = window.getSelection();
    const selectedText = sel?.toString() || '';

    if (selectedText && errors.has(selectedText.toLowerCase())) {
      e.preventDefault();
      const suggestions = suggest(selectedText);
      setContextMenu({
        x: e.clientX,
        y: e.clientY,
        word: selectedText,
        suggestions: suggestions.slice(0, 6),
      });
    } else {
      setContextMenu(null);
    }
  }, [disabled, readOnly, ready, errors, suggest]);

  useEffect(() => {
    const close = () => setContextMenu(null);
    document.addEventListener('click', close);
    return () => document.removeEventListener('click', close);
  }, []);

  const handleReplaceWord = useCallback((original, replacement) => {
    if (!onChange) return;
    const newValue = value.replace(original, replacement);
    onChange({ target: { value: newValue } });
    setContextMenu(null);
  }, [value, onChange]);

  const handleAddToDict = useCallback((word) => {
    addToUserDict(word);
    setContextMenu(null);
  }, [addToUserDict]);

  const buildHighlightedHTML = useCallback(() => {
    if (!ready || errors.size === 0 || !value) {
      return escapeHTML(value || '');
    }

    const lines = value.split('\n');
    return lines.map((line) => {
      if (!line) return '&nbsp;';
      return line.replace(/[\p{L}\p{M}]+/gu, (word) => {
        if (errors.has(word.toLowerCase())) {
          return `<mark class="spell-error" data-word="${escapeAttr(word)}">${escapeHTML(word)}</mark>`;
        }
        return escapeHTML(word);
      });
    }).join('\n');
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

      {contextMenu && (
        <div
          className="spell-context-menu"
          style={{ top: contextMenu.y, left: contextMenu.x }}
          onClick={(e) => e.stopPropagation()}
        >
          {contextMenu.suggestions.length > 0 ? (
            contextMenu.suggestions.map((s) => (
              <button
                key={s}
                type="button"
                className="spell-suggestion"
                onClick={() => handleReplaceWord(contextMenu.word, s)}
              >
                {s}
              </button>
            ))
          ) : (
            <span className="spell-no-suggestions">Sin sugerencias</span>
          )}
          <div className="spell-menu-divider" />
          <button
            type="button"
            className="spell-add-dict"
            onClick={() => handleAddToDict(contextMenu.word)}
          >
            Agregar al diccionario
          </button>
        </div>
      )}
    </div>
  );
}

function escapeHTML(str) {
  return str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;');
}

function escapeAttr(str) {
  return str.replace(/"/g, '&quot;');
}
