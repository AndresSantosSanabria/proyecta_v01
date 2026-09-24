import { useState, useRef, useEffect, useMemo, useCallback } from 'react';
import { createPortal } from 'react-dom';
import './AutocompleteSelect.css';

const OPEN_EVENT = 'acs:open';

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
  const openRef = useRef(false);

  const closeDropdown = useCallback(() => {
    setOpen(false);
    setQuery('');
  }, []);

  const openDropdown = useCallback(() => {
    window.dispatchEvent(new CustomEvent(OPEN_EVENT));
    setOpen(true);
  }, []);

  useEffect(() => {
    openRef.current = open;
  }, [open]);

  useEffect(() => {
    const handleOtherOpen = () => {
      if (openRef.current) {
        setOpen(false);
        setQuery('');
      }
    };
    window.addEventListener(OPEN_EVENT, handleOtherOpen);
    return () => window.removeEventListener(OPEN_EVENT, handleOtherOpen);
  }, []);

  const sortedOptions = useMemo(() => {
    const seen = new Set();
    const unique = options.filter((opt) => {
      const key = String(getOptionValue(opt) ?? '').trim().toLowerCase();
      if (!key || seen.has(key)) return false;
      seen.add(key);
      return true;
    });
    if (!sortAlphabetically) return unique;
    return [...unique].sort((a, b) => {
      const la = getOptionLabel(a).toLowerCase();
      const lb = getOptionLabel(b).toLowerCase();
      return la.localeCompare(lb, 'es');
    });
  }, [options, sortAlphabetically, getOptionLabel, getOptionValue]);

  const filtered = useMemo(() => {
    if (!query.trim()) return sortedOptions;
    const q = query.toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '');
    return sortedOptions.filter((opt) => {
      const label = getOptionLabel(opt).toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '');
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
    if (!open) return undefined;

    const syncPosition = () => {
      const el = containerRef.current;
      if (!el) return;
      const rect = el.getBoundingClientRect();
      const width = Math.max(rect.width, 220);
      const maxDropdown = 320;
      const spaceBelow = window.innerHeight - rect.bottom - 12;
      const spaceAbove = rect.top - 12;
      const openUp = spaceBelow < 160 && spaceAbove > spaceBelow;
      const maxH = Math.min(maxDropdown, openUp ? spaceAbove : spaceBelow);
      const left = Math.min(rect.left, window.innerWidth - width - 12);
      setDropdownStyle({
        position: 'fixed',
        top: openUp ? undefined : rect.bottom + 4,
        bottom: openUp ? window.innerHeight - rect.top + 4 : undefined,
        left: Math.max(12, left),
        width,
        maxWidth: `calc(100vw - 24px)`,
        maxHeight: Math.max(140, maxH),
      });
    };

    syncPosition();
    window.addEventListener('scroll', syncPosition, true);
    window.addEventListener('resize', syncPosition);
    return () => {
      window.removeEventListener('scroll', syncPosition, true);
      window.removeEventListener('resize', syncPosition);
    };
  }, [open, query]);

  useEffect(() => {
    const handleClickOutside = (e) => {
      const target = e.target;
      const inContainer = containerRef.current && containerRef.current.contains(target);
      const inDropdown = target.closest && target.closest('.acs-dropdown');
      if (!inContainer && !inDropdown) {
        setOpen(false);
        setQuery('');
      }
    };
    document.addEventListener('mousedown', handleClickOutside, true);
    document.addEventListener('click', handleClickOutside, true);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside, true);
      document.removeEventListener('click', handleClickOutside, true);
    };
  }, []);

  useEffect(() => {
    if (open && showSearch && inputRef.current) {
      inputRef.current.focus();
    }
  }, [open, showSearch]);

  const handleKeyDown = (e) => {
    if (e.key === 'Escape') {
      closeDropdown();
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
      closeDropdown();
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

  const toggleOpen = () => {
    if (disabled) return;
    if (open) {
      closeDropdown();
    } else {
      openDropdown();
    }
  };

  const optionProps = (opt) => ({
    role: 'option',
    tabIndex: 0,
    className: `acs-option ${String(getOptionValue(opt)) === String(value) ? 'acs-selected' : ''}`,
    onMouseDown: (e) => {
      e.preventDefault();
      selectOption(opt);
    },
    onClick: (e) => {
      e.preventDefault();
      selectOption(opt);
    },
    onKeyDown: (e) => handleListKeyDown(e, opt, 0),
  });

  return (
    <div className={`acs-container ${className}`} ref={containerRef}>
      <button
        type="button"
        id={id}
        className={`acs-trigger ${disabled ? 'acs-disabled' : ''} ${open ? 'acs-open' : ''}`}
        onClick={toggleOpen}
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
        <div className="acs-dropdown" role="listbox" aria-label={placeholder} style={dropdownStyle}>
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
                onMouseDown={(e) => {
                  e.preventDefault();
                  selectOption(null);
                }}
                onClick={(e) => {
                  e.preventDefault();
                  selectOption(null);
                }}
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

            {filtered.map((opt, i) => {
              const props = optionProps(opt);
              return (
                <div
                  key={getOptionKey(opt, i)}
                  {...props}
                  onKeyDown={(e) => handleListKeyDown(e, opt, i)}
                >
                  {highlightMatch(getOptionLabel(opt))}
                </div>
              );
            })}
          </div>
        </div>,
        document.body
      )}
    </div>
  );
}
