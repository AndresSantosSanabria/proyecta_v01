import { useState, useEffect, useRef, useCallback } from 'react';
import nspell from 'nspell';

const DICT_BASE = `${import.meta.env?.BASE_URL || '/'}dictionaries`;
const USER_DICT_KEY = 'spellchecker_user_dict';

let sharedInstance = null;
let loadingPromise = null;

function loadDictionary() {
  if (sharedInstance) return Promise.resolve(sharedInstance);
  if (loadingPromise) return loadingPromise;

  loadingPromise = (async () => {
    const [affRes, dicRes] = await Promise.all([
      fetch(`${DICT_BASE}/es_CO.aff`),
      fetch(`${DICT_BASE}/es_CO.dic`),
    ]);

    if (!affRes.ok || !dicRes.ok) {
      throw new Error('No se pudo cargar el diccionario ortografico.');
    }

    const aff = new Uint8Array(await affRes.arrayBuffer());
    const dic = new Uint8Array(await dicRes.arrayBuffer());

    const spell = nspell(aff, dic);

    try {
      const saved = JSON.parse(localStorage.getItem(USER_DICT_KEY) || '[]');
      saved.forEach((w) => spell.add(w));
    } catch { /* ignore */ }

    sharedInstance = spell;
    return spell;
  })();

  return loadingPromise;
}

function tokenize(text) {
  if (!text) return [];
  const matches = text.match(/[\p{L}\p{M}]+/gu);
  return matches || [];
}

export default function useSpellChecker({ enabled = true } = {}) {
  const [spell, setSpell] = useState(null);
  const [loading, setLoading] = useState(true);
  const [errors, setErrors] = useState(new Set());
  const debounceRef = useRef(null);

  useEffect(() => {
    if (!enabled) { setLoading(false); return; }
    loadDictionary()
      .then((s) => { setSpell(s); setLoading(false); })
      .catch(() => setLoading(false));
  }, [enabled]);

  const check = useCallback((text) => {
    if (!spell || !enabled) {
      setErrors(new Set());
      return;
    }
    const words = tokenize(text);
    const errSet = new Set();
    words.forEach((w) => {
      if (!spell.correct(w)) {
        errSet.add(w.toLowerCase());
      }
    });
    setErrors(errSet);
  }, [spell, enabled]);

  const checkDebounced = useCallback((text, delay = 300) => {
    clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => check(text), delay);
  }, [check]);

  const suggest = useCallback((word) => {
    if (!spell) return [];
    return spell.suggest(word);
  }, [spell]);

  const addToUserDict = useCallback((word) => {
    if (!spell || !word) return;
    spell.add(word);
    try {
      const saved = JSON.parse(localStorage.getItem(USER_DICT_KEY) || '[]');
      if (!saved.includes(word)) {
        saved.push(word);
        localStorage.setItem(USER_DICT_KEY, JSON.stringify(saved));
      }
    } catch { /* ignore */ }
    setErrors((prev) => {
      const next = new Set(prev);
      next.delete(word.toLowerCase());
      return next;
    });
  }, [spell]);

  const removeFromUserDict = useCallback((word) => {
    if (!spell || !word) return;
    spell.remove(word);
    try {
      const saved = JSON.parse(localStorage.getItem(USER_DICT_KEY) || '[]');
      localStorage.setItem(
        USER_DICT_KEY,
        JSON.stringify(saved.filter((w) => w !== word))
      );
    } catch { /* ignore */ }
  }, [spell]);

  return {
    ready: !!spell,
    loading,
    errors,
    check,
    checkDebounced,
    suggest,
    addToUserDict,
    removeFromUserDict,
  };
}
