import React, { useState, useEffect, useRef } from 'react';
import { useSpellCheck } from '../../../hooks/useSpellCheck';
import './SpellCheckInput.css';

export const SpellCheckInput = ({ 
  value, 
  onChange, 
  as: Component = 'input', 
  onErrorChange,
  ...props 
}) => {
  const { isLoaded, checkText } = useSpellCheck();
  const [errors, setErrors] = useState([]);
  const mountedRef = useRef(true);

  useEffect(() => {
    mountedRef.current = true;
    return () => { mountedRef.current = false; };
  }, []);

  useEffect(() => {
    if (!isLoaded) return;

    // Esperar 3 segundos tras la última pulsación de tecla
    const timeoutId = setTimeout(async () => {
      const misspelled = await checkText(value);
      if (!mountedRef.current) return; // componente desmontado, no actualizar state
      setErrors(misspelled);
      if (onErrorChange) {
        onErrorChange(misspelled.length > 0);
      }
    }, 3000);

    return () => clearTimeout(timeoutId);
  }, [value, isLoaded, checkText, onErrorChange]);

  const hasErrors = errors.length > 0;

  const handleReplace = (originalWord, suggestedWord) => {
    if (!onChange) return;
    // Replace the first occurrence of the original word (this is simple, for robust replacement you'd want to find word boundaries)
    const newValue = value.replace(new RegExp(`\\b${originalWord}\\b`, 'i'), suggestedWord);
    onChange({ target: { value: newValue } });
  };

  return (
    <div className="spellcheck-container">
      <Component
        value={value}
        onChange={onChange}
        spellCheck="true"
        lang="es"
        className={`spellcheck-field ${hasErrors ? 'has-spelling-errors' : ''} ${props.className || ''}`}
        {...props}
      />
      {hasErrors && (
        <div className="spellcheck-warning-box">
          <span className="spellcheck-warning">
            Posibles errores ortográficos detectados:
          </span>
          <ul className="spellcheck-suggestions-list">
            {errors.map((err, idx) => (
              <li key={`${err.word}-${idx}`}>
                <strong>"{err.word}"</strong> 
                {err.suggestions && err.suggestions.length > 0 ? (
                  <>
                    <span> ¿Quisiste decir:</span>
                    {err.suggestions.map((sug) => (
                      <button 
                        key={sug} 
                        type="button" 
                        className="spellcheck-suggestion-btn"
                        onClick={() => handleReplace(err.word, sug)}
                      >
                        {sug}
                      </button>
                    ))}
                    <span>?</span>
                  </>
                ) : (
                  <span> (Sin sugerencias)</span>
                )}
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
};
