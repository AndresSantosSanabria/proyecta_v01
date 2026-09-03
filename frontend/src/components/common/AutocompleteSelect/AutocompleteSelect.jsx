import { useState, useRef, useEffect, useMemo, useCallback } from 'react';
import { createPortal } from 'react-dom';
import './AutocompleteSelect.css';

export default function AutocompleteSelect({
  value = '',
  onChange,
  options = [],
  placeholder = 'Buscar...',
  allLabel = null,
  allValue = 'ALL',
  disabled = false,
  className = '',
  id,
  getOptionLabel = (opt) => (typeof opt === 'string' ? opt : opt.label ?? opt.nombre ?? opt.codigo ?? ''),
  getOptionValue = (opt) => (typeof opt === 'string' ? opt : opt.value ?? opt.codigo ?? opt.id ?? getOptionLabel(opt)),
  getOptionKey = (opt, i) => (typeof opt === 'string' ? opt : opt.value ?? opt.codigo ?? opt.id ?? i),
  sortAlphabetically = true,
  showSearchThreshold = 5,
}) {
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState('');
  const [dropdownStyle, setDropdownStyle] = useState({});
  const inputRef = useRef(null);
  const containerRef = useRef(null);
  const listRef = useRef(null);

  const calculateDropdownPosition = useCallback(() => {
    if (!containerRef.current) return;
    const rect = containerRef.current.getBoundingClientRect();
    const viewportHeight = window.innerHeight;
    const maxDropdownHeight = 324;
    const spaceBelow = viewportHeight - rect.bottom;
    const spaceAbove = rect.top;
    let top;
    let effectiveMaxHeight;
    if (spaceBelow >= 180 || spaceBelow >= spaceAbove) {
      top = rect.bottom + window.scrollY + 4;
      effectiveMaxHeight = Math.min(maxDropdownHeight, spaceBelow - 8);
    } else {
      effectiveMaxHeight = Math.min(maxDropdownHeight, spaceAbove - 8);
      top = rect.top + window.scrollY - effectiveMaxHeight - 4;
    }
    setDropdownStyle({
      position: 'absolute',
      top,
      left: rect.left + window.scrollX,
      width: rect.width,
      maxHeight: effectiveMaxHeight,
      zIndex: 99999,
    });
  }, []);

  const sortedOptions = useMemo(() => {
    if (!sortAlphabetically) return options;
    return [...options].sort((a, b) => {
      const la = getOptionLabel(a).toLowerCase();
      const lb = getOptionLabel(b).toLowerCase();
      return la.localeCompare(lb, 'es');
    });
  }, [options, sortAlphabetically, getOptionLabel]);

  const filtered = useMemo(() => {
    if (!query.trim()) return sortedOptions;
    const q = query.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '');
    return sortedOptions.filter((opt) => {
      const label = getOptionLabel(opt).toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '');
      return label.includes(q);
    });
  }, [sortedOptions, query, getOptionLabel]);

  const selectedLabel = useMemo(() => {
    if (!value || value === allValue) return allLabel ?? '';
    const found = sortedOptions.find((opt) => String(getOptionValue(opt)) === String(value));
    return found ? getOptionLabel(found) : value;
  }, [value, sortedOptions, allLabel, allValue, getOptionValue, getOptionLabel]);

  const showSearch = sortedOptions.length >= showSearchThreshold;

  const selectOption = useCallback((opt) => {
    if (opt === null) {
      onChange(allValue);
    } else {
      onChange(getOptionValue(opt));
    }
    setOpen(false);
    setQuery('');
  }, [onChange, allValue, getOptionValue]);

  useEffect(() => {
    if (!open) return;
    calculateDropdownPosition();
    const handleUpdate = () => calculateDropdownPosition();
    window.addEventListener('scroll', handleUpdate, true);
    window.addEventListener('resize', handleUpdate);
    return () => {
      window.removeEventListener('scroll', handleUpdate, true);
      window.removeEventListener('resize', handleUpdate);
    };
  }, [open, calculateDropdownPosition]);

  useEffect(() => {
    if (!open) return;
    const handleClickOutside = (e) => {
      const portal = document.getElementById('acs-portal-dropdown');
      if (
        containerRef.current && !containerRef.current.contains(e.target) &&
        (!portal || !portal.contains(e.target))
      ) {
        setOpen(false);
        setQuery('');
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [open]);

  useEffect(() => {
    if (open && inputRef.current) {
      inputRef.current.focus();
    }
  }, [open]);

  const handleKeyDown = (e) => {
    if (e.key === 'Escape') {
      setOpen(false);
      setQuery('');
    } else if (e.key === 'ArrowDown') {
      e.preventDefault();
      const list = listRef.current;
      if (list) {
        const first = list.querySelector('[role="option"]');
        if (first) first.focus();
      }
    }
  };

  const handleListKeyDown = (e, opt, index) => {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      selectOption(opt);
    } else if (e.key === 'Escape') {
      setOpen(false);
      setQuery('');
    } else if (e.key === 'ArrowDown') {
      e.preventDefault();
      const items = listRef.current?.querySelectorAll('[role="option"]');
      if (items && items[index + 1]) items[index + 1].focus();
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      if (index === 0 && showSearch && inputRef.current) {
        inputRef.current.focus();
      } else {
        const items = listRef.current?.querySelectorAll('[role="option"]');
        if (items && items[index - 1]) items[index - 1].focus();
      }
    }
  };

  const highlightMatch = (label) => {
    if (!query.trim()) return label;
    const q = query;
    const idx = label.toLowerCase().indexOf(q.toLowerCase());
    if (idx === -1) return label;
    return (
      <>
        {label.slice(0, idx)}
        <mark className="acs-highlight">{label.slice(idx, idx + q.length)}</mark>
        {label.slice(idx + q.length)}
      </>
    );
  };

  const hasAllOption = allLabel !== null;
  const displayValue = open && showSearch ? query : selectedLabel;

  return (
    <div className={`acs-container ${className}`} ref={containerRef}>
      <button
        type="button"
        id={id}
        className={`acs-trigger ${disabled ? 'acs-disabled' : ''} ${open ? 'acs-open' : ''}`}
        onClick={() => !disabled && setOpen(!open)}
        onKeyDown={handleKeyDown}
        disabled={disabled}
        aria-haspopup="listbox"
        aria-expanded={open}
      >
        <span className={`acs-value ${!selectedLabel ? 'acs-placeholder' : ''}`}>
          {selectedLabel || placeholder}
        </span>
        <span className={`acs-arrow ${open ? 'acs-arrow-up' : ''}`}>&#9662;</span>
      </button>

      {open && createPortal(
        <div
          id="acs-portal-dropdown"
          className="acs-dropdown"
          role="listbox"
          aria-label={placeholder}
          style={dropdownStyle}
        >
          {showSearch && (
            <div className="acs-search-wrapper">
              <input
                ref={inputRef}
                type="text"
                className="acs-search"
                placeholder="Escriba para buscar..."
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                onKeyDown={handleKeyDown}
              />
            </div>
          )}

          <div className="acs-options" ref={listRef}>
            {hasAllOption && (
              <div
                role="option"
                tabIndex={0}
                className={`acs-option ${value === allValue ? 'acs-selected' : ''}`}
                onClick={() => selectOption(null)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault();
                    selectOption(null);
                  }
                }}
              >
                {allLabel}
              </div>
            )}

            {filtered.length === 0 && (
              <div className="acs-no-results">No se encontraron resultados</div>
            )}

            {filtered.map((opt, i) => (
              <div
                key={getOptionKey(opt, i)}
                role="option"
                tabIndex={0}
                className={`acs-option ${String(getOptionValue(opt)) === String(value) ? 'acs-selected' : ''}`}
                onClick={() => selectOption(opt)}
                onKeyDown={(e) => handleListKeyDown(e, opt, i)}
              >
                {highlightMatch(getOptionLabel(opt))}
              </div>
            ))}
          </div>
        </div>,
        document.body
      )}
    </div>
  );
}
