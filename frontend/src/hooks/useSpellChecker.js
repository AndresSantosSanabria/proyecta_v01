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

function stripAccents(str) {
  return str.normalize('NFD').replace(/[\u0300-\u036f]/g, '');
}

const ACCENT_MAP = { 'a':'á','e':'é','i':'í','o':'ó','u':'ú','n':'ñ' };

function findAccentedMatch(word, spellInstance) {
  const chars = [...word];
  for (let i = 0; i < chars.length; i++) {
    const ch = chars[i];
    const accented = ACCENT_MAP[ch];
    if (!accented) continue;
    const candidate = chars.slice(0, i).join('') + accented + chars.slice(i + 1).join('');
    if (spellInstance.correct(candidate)) return candidate;
  }
  return null;
}

const ACCENT_SUGGESTIONS = {
  'salio':'salió','llego':'llegó','compro':'compró','entro':'entró',
  'empezo':'empezó','termino':'terminó','inicio':'inició',
  'habia':'había','dia':'día','nino':'niño','nina':'niña',
  'pagina':'página','numero':'número','titulo':'título',
  'rapido':'rápido','unico':'único','basico':'básico',
  'minimo':'mínimo','maximo':'máximo','ultimo':'último',
  'tambien':'también','ademas':'además','aun':'aún',
  'solucion':'solución','informacion':'información',
  'educacion':'educación','tecnologia':'tecnología',
  'publico':'público','privado':'privado',
  'proximo':'próximo','mayoria':'mayoría',
  'razon':'razón','accion':'acción','funcion':'función',
  'condicion':'condición','situacion':'situación',
  'direccion':'dirección','gestion':'gestión',
  'decision':'decisión','division':'división',
  'conclusion':'conclusión','session':'sesión',
  'institucion':'institución','organizacion':'organización',
  'comunicacion':'comunicación','capacitacion':'capacitación',
  'formacion':'formación','investigacion':'investigación',
  'innovacion':'innovación','cooperacion':'cooperación',
  'integracion':'integración','participacion':'participación',
  'contribucion':'contribución','interaccion':'interacción',
  'operacion':'operación','construccion':'construcción',
  'produccion':'producción','reduccion':'reducción',
  'expansion':'expansión','dimension':'dimensión',
  'posicion':'posición','precision':'precisión',
  'energia':'energía','area':'área',
  'velocidad':'velocidad','densidad':'densidad',
  'capacidad':'capacidad','intensidad':'intensidad',
  'frecuencia':'frecuencia','temperatura':'temperatura',
  'resistencia':'resistencia','friccion':'fricción',
  'aceleracion':'aceleración','vibracion':'vibración',
  'reflexion':'reflexión','transmision':'transmisión',
  'emision':'emisión','radiacion':'radiación',
  'iluminacion':'iluminación','distorsion':'distorsión',
  'aberracion':'aberración','perfeccion':'perfección',
  'eficiencia':'eficiencia','eficacia':'eficacia',
  'productividad':'productividad','sostenibilidad':'sostenibilidad',
  'resiliencia':'resiliencia','flexibilidad':'flexibilidad',
  'accesibilidad':'accesibilidad','disponibilidad':'disponibilidad',
  'confiabilidad':'confiabilidad','integridad':'integridad',
  'autenticidad':'autenticidad','transparencia':'transparencia',
  'responsabilidad':'responsabilidad','inclusion':'inclusión',
  'diversidad':'diversidad','igualdad':'igualdad',
  'armonia':'armonía','continuidad':'continuidad',
  'conciencia':'conciencia','conocimiento':'conocimiento',
  'sabiduria':'sabiduría','inteligencia':'inteligencia',
  'creatividad':'creatividad','imaginacion':'imaginación',
  'originalidad':'originalidad','modernidad':'modernidad',
  'antiguedad':'antigüedad','enfermedad':'enfermedad',
  'discapacidad':'discapacidad','autonomia':'autonomía',
  'independencia':'independencia','libertad':'libertad',
  'democracia':'democracia','monarquia':'monarquía',
  'burocracia':'burocracia','anarquia':'anarquía',
  'nacionalismo':'nacionalismo','capitalismo':'capitalismo',
  'socialismo':'socialismo','comunismo':'comunismo',
  'intolerancia':'intolerancia','discriminacion':'discriminación',
  'globalizacion':'globalización','politico':'político',
  'economico':'económico','tecnico':'técnico',
  'practico':'práctico','teorico':'teórico',
  'historico':'histórico','juridico':'jurídico',
  'clasico':'clásico','contemporaneo':'contemporáneo',
  'agricola':'agrícola','oceanico':'oceánico',
  'aereo':'aéreo','atomico':'atómico',
  'cuantico':'cuántico',
  'oportunidad':'oportunidad','necesidad':'necesidad',
  'dependencia':'dependencia','estandares':'estándares',
  'parametros':'parámetros','indicadores':'indicadores',
  'estrategias':'estrategias','actividades':'actividades',
  'resultados':'resultados','beneficios':'beneficios',
  'proveedores':'proveedores','organizaciones':'organizaciones',
  'instituciones':'instituciones','infraestructura':'infraestructura',
  'comunidades':'comunidades','publicaciones':'publicaciones',
  'certificados':'certificados','regulaciones':'regulaciones',
  'procedimientos':'procedimientos','protocolos':'protocolos',
  'requisitos':'requisitos','herramientas':'herramientas',
  'tecnologias':'tecnologías','soluciones':'soluciones',
};

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
      const lower = w.toLowerCase();
      if (spell.correct(lower)) return;

      const stripped = stripAccents(lower);
      if (stripped !== lower && spell.correct(stripped)) {
        errSet.add(lower);
        return;
      }

      if (ACCENT_SUGGESTIONS[lower] && ACCENT_SUGGESTIONS[lower] !== lower) {
        errSet.add(lower);
        return;
      }

      if (findAccentedMatch(lower, spell)) {
        errSet.add(lower);
        return;
      }

      const suggestions = spell.suggest(lower);
      if (suggestions.length > 0) {
        errSet.add(lower);
        return;
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
    const lower = word.toLowerCase();
    const accentSug = ACCENT_SUGGESTIONS[lower];
    const nspellSuggestions = spell.suggest(lower);

    if (accentSug && accentSug !== lower) {
      const stripped = stripAccents(lower);
      if (stripped !== lower && spell.correct(stripped)) {
        return [accentSug, stripped, ...nspellSuggestions.filter(s => s !== accentSug && s !== stripped)].slice(0, 6);
      }
      return [accentSug, ...nspellSuggestions.filter(s => s !== accentSug)].slice(0, 6);
    }

    const stripped = stripAccents(lower);
    if (stripped !== lower && spell.correct(stripped)) {
      return [stripped, ...nspellSuggestions.filter(s => s !== stripped)].slice(0, 6);
    }

    const accented = findAccentedMatch(lower, spell);
    if (accented) {
      return [accented, ...nspellSuggestions.filter(s => s !== accented)].slice(0, 6);
    }

    return nspellSuggestions.slice(0, 6);
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
