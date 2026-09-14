import Typo from 'typo-js';

let dictionary = null;
let isLoaded = false;

const ACCENT_MAP = {
  'salio':'salió','llego':'llegó','compro':'compró','entro':'entró',
  'vio':'vio','dijo':'dijo','hizo':'hizo','puso':'puso','trajo':'trajo',
  'empezo':'empezó','termino':'terminó','inicio':'inició',
  'dia':'día','nino':'niño','nina':'niña','año':'año',
  'pagina':'página','numero':'número','titulo':'título',
  'rapido':'rápido','unico':'único','basico':'básico','minimo':'mínimo',
  'maximo':'máximo','ultimo':'último',
  'tambien':'también','ademas':'además','aun':'aún',
  'solucion':'solución','informacion':'información',
  'educacion':'educación','tecnologia':'tecnología',
  'publico':'público','privado':'privado',
  'proximo':'próximo','mayoria':'mayoría','minoria':'minoría',
  'fácil':'fácil','difícil':'difícil','útil':'útil',
  'después':'después','segun':'según',
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
  'operacion':'operación','fabricacion':'fabricación',
  'conservacion':'conservación','contaminacion':'contaminación',
  'poblacion':'población','edificacion':'edificación',
  'construccion':'construcción','destruccion':'destrucción',
  'produccion':'producción','reduccion':'reducción',
  'expansion':'expansión',
  'dimension':'dimensión','posicion':'posición',
  'extension':'extensión','precision':'precisión',
  'energia':'energía','area':'área',
  'velocidad':'velocidad','densidad':'densidad',
  'capacidad':'capacidad','intensidad':'intensidad',
  'frecuencia':'frecuencia','temperatura':'temperatura',
  'resistencia':'resistencia','friccion':'fricción',
  'aceleracion':'aceleración','vibracion':'vibración',
  'reflexion':'reflexión','dispersion':'dispersión',
  'absorcion':'absorción','transmision':'transmisión',
  'emision':'emisión','radiacion':'radiación',
  'iluminacion':'iluminación','distorsion':'distorsión',
  'aberracion':'aberración','perfeccion':'perfección',
  'eficiencia':'eficiencia','eficacia':'eficacia',
  'productividad':'productividad','rentabilidad':'rentabilidad',
  'viabilidad':'viabilidad','sostenibilidad':'sostenibilidad',
  'durabilidad':'durabilidad','resiliencia':'resiliencia',
  'adaptabilidad':'adaptabilidad','flexibilidad':'flexibilidad',
  'escalabilidad':'escalabilidad','usabilidad':'usabilidad',
  'accesibilidad':'accesibilidad','disponibilidad':'disponibilidad',
  'confiabilidad':'confiabilidad','integridad':'integridad',
  'autenticidad':'autenticidad','legalidad':'legalidad',
  'legitimidad':'legitimidad','transparencia':'transparencia',
  'responsabilidad':'responsabilidad','rendicion':'rendición',
  'gobernabilidad':'gobernabilidad','equidad':'equidad',
  'inclusion':'inclusión','diversidad':'diversidad',
  'igualdad':'igualdad','armonia':'armonía',
  'continuidad':'continuidad','perseverancia':'perseverancia',
  'conciencia':'conciencia','conocimiento':'conocimiento',
  'sabiduria':'sabiduría','inteligencia':'inteligencia',
  'creatividad':'creatividad','imaginacion':'imaginación',
  'originalidad':'originalidad','modernidad':'modernidad',
  'antiguedad':'antigüedad','juventud':'juventud',
  'natalidad':'natalidad','mortalidad':'mortalidad',
  'longevidad':'longevidad','enfermedad':'enfermedad',
  'discapacidad':'discapacidad','autonomia':'autonomía',
  'independencia':'independencia','libertad':'libertad',
  'democracia':'democracia','monarquia':'monarquía',
  'oligarquia':'oligarquía','burocracia':'burocracia',
  'anarquia':'anarquía','totalitarismo':'totalitarismo',
  'autoritarismo':'autoritarismo','nacionalismo':'nacionalismo',
  'imperialismo':'imperialismo','colonialismo':'colonialismo',
  'capitalismo':'capitalismo','socialismo':'socialismo',
  'comunismo':'comunismo','fascismo':'fascismo',
  'homofobia':'homofobia','xenofobia':'xenofobia',
  'intolerancia':'intolerancia','discriminacion':'discriminación',
  'globalizacion':'globalización','regionalizacion':'regionalización',
  'descentralizacion':'descentralización','privatizacion':'privatización',
  'nacionalizacion':'nacionalización','expropiacion':'expropiación',
  'occidentalizacion':'occidentalización',
  'politico':'político','economico':'económico',
  'tecnico':'técnico','practico':'práctico',
  'teorico':'teórico','logico':'lógico',
  'artistico':'artístico','pictorico':'pictórico',
  'historico':'histórico','juridico':'jurídico',
  'clasico':'clásico','contemporaneo':'contemporáneo',
  'agricola':'agrícola','oceanico':'oceánico',
  'aereo':'aéreo','atomico':'atómico',
  'cuantico':'cuántico','relativistico':'relativístico',
  'homofobico':'homofóbico','xenofobico':'xenofóbico',
  'oportunidad':'oportunidad','necesidad':'necesidad',
  'calidad':'calidad','actividad':'actividad',
  'realidad':'realidad','ciudad':'ciudad',
  'general':'general','especial':'especial',
  'diferente':'diferente','actual':'actual',
  'total':'total','principal':'principal',
  'siguiente':'siguiente','propio':'propio',
  'mayor':'mayor','menor':'menor',
  'completo':'completo','grave':'grave',
  'más':'más','aunque':'aunque','porque':'porque',
  'durante':'durante','mediante':'mediante',
  'siempre':'siempre','nunca':'nunca',
  'entonces':'entonces','acerca':'acerca',
  'dentro':'dentro','fuera':'fuera',
  'hacia':'hacia','junto':'junto',
  'mientras':'mientras','sobre':'sobre',
  'proyecto':'proyecto','sistema':'sistema',
  'proceso':'proceso','equipo':'equipo',
  'modelo':'modelo','grupo':'grupo',
  'manera':'manera','momento':'momento',
  'forma':'forma','lugar':'lugar',
  'caso':'caso','tiempo':'tiempo',
  'pais':'país','imagen':'imagen',
  'universidad':'universidad',
  'infraestructura':'infraestructura',
  'dependencia':'dependencia','estandares':'estándares',
  'parametros':'parámetros','indicadores':'indicadores',
  'estrategias':'estrategias','actividades':'actividades',
  'resultados':'resultados','beneficios':'beneficios',
  'oportunidades':'oportunidades','amenazas':'amenazas',
  'fortalezas':'fortalezas','debilidades':'debilidades',
  'necesidades':'necesidades','expectativas':'expectativas',
  'proveedores':'proveedores','colaboradores':'colaboradores',
  'organizaciones':'organizaciones','instituciones':'instituciones',
  'direcciones':'direcciones','comunidades':'comunidades',
  'simposios':'simposios','coloquios':'coloquios',
  'conferencias':'conferencias','congresos':'congresos',
  'publicaciones':'publicaciones','especializaciones':'especializaciones',
  'maestrias':'maestrías','doctorados':'doctorados',
  'diplomas':'diplomas','certificados':'certificados',
  'comprobantes':'comprobantes','facturas':'facturas',
  'contribuciones':'contribuciones','obligaciones':'obligaciones',
  'regulaciones':'regulaciones','normativas':'normativas',
  'lineamientos':'lineamientos','procedimientos':'procedimientos',
  'protocolos':'protocolos','requisitos':'requisitos',
  'restricciones':'restricciones','limitaciones':'limitaciones',
  'contenedores':'contenedores','repositorios':'repositorios',
  'aplicaciones':'aplicaciones','herramientas':'herramientas',
  'plataformas':'plataformas','soluciones':'soluciones',
  'tecnologias':'tecnologías',
};

function stripAccents(str) {
  return str.normalize('NFD').replace(/[\u0300-\u036f]/g, '');
}

const VOWEL_ACCENT_MAP = { 'a':'á','e':'é','i':'í','o':'ó','u':'ú','n':'ñ' };

function findAccentedMatch(word) {
  if (!dictionary) return null;
  const chars = [...word];
  for (let i = 0; i < chars.length; i++) {
    const ch = chars[i];
    const accented = VOWEL_ACCENT_MAP[ch];
    if (!accented) continue;
    const candidate = chars.slice(0, i).join('') + accented + chars.slice(i + 1).join('');
    if (dictionary.check(candidate)) return candidate;
  }
  return null;
}

function checkAccent(word) {
  const lower = word.toLowerCase();
  if (ACCENT_MAP[lower] && ACCENT_MAP[lower] !== lower) {
    return ACCENT_MAP[lower];
  }
  const stripped = stripAccents(lower);
  if (stripped !== lower && dictionary && dictionary.check(stripped)) {
    return stripped;
  }
  return findAccentedMatch(lower);
}

self.onmessage = async (e) => {
  const { type, payload, msgId } = e.data;

  if (type === 'INIT') {
    try {
      const [affRes, dicRes] = await Promise.all([
        fetch('/dictionaries/es_CO.aff'),
        fetch('/dictionaries/es_CO.dic'),
      ]);

      if (!affRes.ok || !dicRes.ok) {
        throw new Error(`Dictionary fetch failed: aff=${affRes.status} dic=${dicRes.status}`);
      }

      const affData = await affRes.text();
      const dicData = await dicRes.text();
      dictionary = new Typo("es_CO", affData, dicData);

      isLoaded = true;
      self.postMessage({ type: 'INIT_SUCCESS' });
    } catch (error) {
      console.error("[SpellCheckWorker] Init error:", error);
      self.postMessage({ type: 'INIT_ERROR', error: error.message });
    }
  }

  if (type === 'CHECK') {
    if (!isLoaded || !dictionary) {
      self.postMessage({ type: 'RESULT', msgId, result: [] });
      return;
    }

    const text = payload || "";
    const words = text.replace(/[.,\/#!$%\^&*;:{}=\-_`~()¿¡]/g,"").split(/\s+/);
    const misspelledWords = [];

    words.forEach(word => {
      if (!word.trim() || !isNaN(word) || word.length <= 1) return;
      const lower = word.toLowerCase();
      if (misspelledWords.find(w => w.word === lower)) return;

      const accentSuggestion = checkAccent(lower);
      if (accentSuggestion) {
        if (!misspelledWords.find(w => w.word === lower)) {
          misspelledWords.push({ word: lower, suggestions: [accentSuggestion] });
        }
        return;
      }

      const isCorrect = dictionary.check(lower);
      if (!isCorrect) {
        const rawSuggestions = dictionary.suggest(lower);
        const filtered = rawSuggestions.filter(s => {
          if (s.length < 2) return false;
          const slArr = s.toLowerCase().split('');
          const wArr = lower.split('');
          if (Math.abs(slArr.length - wArr.length) > 3) return false;
          const maxLen = Math.max(slArr.length, wArr.length);
          let common = 0;
          const slCopy = [...slArr];
          for (const c of wArr) {
            const idx = slCopy.indexOf(c);
            if (idx !== -1) { common++; slCopy.splice(idx, 1); }
          }
          if (common / maxLen < 0.3) return false;
          return true;
        }).slice(0, 6);

        misspelledWords.push({ word: lower, suggestions: filtered });
      }
    });

    self.postMessage({ type: 'RESULT', msgId, result: misspelledWords });
  }
};
