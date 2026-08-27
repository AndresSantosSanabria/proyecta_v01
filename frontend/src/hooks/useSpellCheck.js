import { useState, useEffect, useCallback, useRef } from 'react';

// Singleton: un solo worker compartido por toda la app
let sharedWorker = null;
let workerReady = false;
const pendingCallbacks = new Map(); // msgId -> resolve
let msgCounter = 0;

const getWorker = () => {
  if (!sharedWorker) {
    sharedWorker = new Worker(
      new URL('../workers/spellcheckWorker.js', import.meta.url),
      { type: 'module' }
    );

    sharedWorker.onmessage = (e) => {
      const { type, msgId, result } = e.data;

      if (type === 'INIT_SUCCESS') {
        workerReady = true;
        // Resolver callbacks pendientes que estaban esperando por la inicialización
        pendingCallbacks.forEach((resolve, id) => {
          if (id === '__init__') {
            resolve(true);
            pendingCallbacks.delete(id);
          }
        });
      }

      if (type === 'RESULT' && pendingCallbacks.has(msgId)) {
        pendingCallbacks.get(msgId)(result);
        pendingCallbacks.delete(msgId);
      }
    };

    sharedWorker.onerror = (err) => {
      console.error('[SpellCheckWorker] Error:', err);
    };

    // Inicializar el worker (carga los diccionarios una sola vez)
    sharedWorker.postMessage({ type: 'INIT' });
  }

  return sharedWorker;
};

export const useSpellCheck = () => {
  const [isLoaded, setIsLoaded] = useState(workerReady);

  useEffect(() => {
    if (workerReady) {
      setIsLoaded(true);
      return;
    }

    const worker = getWorker();

    // Registrar un callback para cuando el worker termine de inicializar
    const onReady = () => setIsLoaded(true);
    pendingCallbacks.set('__init__', onReady);

    return () => {
      // Si el componente se desmonta antes de que el worker esté listo,
      // evitar el setState en un componente desmontado
      pendingCallbacks.delete('__init__');
    };
  }, []);

  const checkText = useCallback((text) => {
    return new Promise((resolve) => {
      if (!text || !text.trim()) {
        resolve([]);
        return;
      }

      const worker = getWorker();
      const msgId = ++msgCounter;
      pendingCallbacks.set(msgId, resolve);
      worker.postMessage({ type: 'CHECK', payload: text, msgId });
    });
  }, []);

  return { isLoaded, checkText };
};
