import React, { useState, useEffect, useRef, useCallback } from 'react';
import { useSpellCheck } from '../../../hooks/useSpellCheck';
import './SpellCheckInput.css';

export const SpellCheckInput = ({ 
  value, 
  onChange, 
  as: Component = 'input', 
  onErrorChange,
  className: externalClassName = '',
  ...props 
}) => {
  const { isLoaded, checkText } = useSpellCheck();
  const [errors, setErrors] = useState([]);
  const [dismissedWords, setDismissedWords] = useState(new Set());
  const mountedRef = useRef(true);

  useEffect(() => {
    mountedRef.current = true;
    return () => { mountedRef.current = false; };
  }, []);

  useEffect(() => {
    if (!isLoaded) return;

    const timeoutId = setTimeout(async () => {
      try {
        const misspelled = await checkText(value);
        if (!mountedRef.current) return;
        setErrors(misspelled);
        setDismissedWords((prev) => {
          if (prev.size === 0) return prev;
          const currentWords = new Set(misspelled.map((e) => e.word));
          const next = new Set();
          prev.forEach((w) => { if (currentWords.has(w)) next.add(w); });
          return next;
        });
        if (onErrorChange) {
          onErrorChange(misspelled.length > 0);
        }
      } catch {
        if (!mountedRef.current) return;
        setErrors([]);
        if (onErrorChange) onErrorChange(false);
      }
    }, 500);

    return () => clearTimeout(timeoutId);
  }, [value, isLoaded, checkText, onErrorChange]);

  const visibleErrors = errors.filter((e) => !dismissedWords.has(e.word));
  const hasErrors = visibleErrors.length > 0;

  const mergedClassName = [
    'spellcheck-field',
    hasErrors ? 'has-spelling-errors' : '',
    externalClassName,
  ].filter(Boolean).join(' ');

  const handleReplace = useCallback((originalWord, suggestedWord) => {
    if (!onChange) return;
    const newValue = value.replace(new RegExp(`\\b${originalWord}\\b`, 'i'), suggestedWord);
    onChange({ target: { value: newValue } });
  }, [value, onChange]);

  const handleDismiss = useCallback(() => {
    const currentWords = new Set(errors.map((e) => e.word));
    setDismissedWords(currentWords);
    if (onErrorChange) onErrorChange(false);
  }, [errors, onErrorChange]);

  return (
    <div className="spellcheck-container">
      <Component
        value={value}
        onChange={onChange}
        spellCheck="true"
        lang="es"
        className={mergedClassName}
        {...props}
      />
      {hasErrors && (
        <div className="spellcheck-warning-box">
          <div className="spellcheck-warning">
            <span className="spellcheck-warning-label">
              Posibles correcciones ortográficas
            </span>
            <button
              type="button"
              className="spellcheck-omit-btn"
              onClick={handleDismiss}
            >
              Omitir
            </button>
          </div>
          <ul className="spellcheck-suggestions-list">
            {visibleErrors.map((err, idx) => (
              <li key={`${err.word}-${idx}`}>
                <strong>&quot;{err.word}&quot;</strong>
                {err.suggestions && err.suggestions.length > 0 ? (
                  <>
                    <span> &mdash; ¿Quisiste decir: </span>
                    {err.suggestions.map((sug, si) => (
                      <React.Fragment key={sug}>
                        <button 
                          type="button" 
                          className="spellcheck-suggestion-btn"
                          onClick={() => handleReplace(err.word, sug)}
                        >
                          {sug}
                        </button>
                        {si < err.suggestions.length - 1 && <span>, </span>}
                      </React.Fragment>
                    ))}
                    <span>?</span>
                  </>
                ) : (
                  <span> <em>(sin sugerencias)</em></span>
                )}
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
};
