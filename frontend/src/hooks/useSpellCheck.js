import { useState, useEffect, useCallback } from 'react';

// Singleton: un solo worker compartido por toda la app
let sharedWorker = null;
let workerReady = false;
let workerFailed = false;
const pendingCallbacks = new Map(); // msgId -> resolve
let msgCounter = 0;

const CHECK_TIMEOUT_MS = 8000;

const getWorker = () => {
  if (sharedWorker) return sharedWorker;

  sharedWorker = new Worker(
    new URL('../workers/spellcheckWorker.js', import.meta.url),
    { type: 'module' }
  );

  sharedWorker.onmessage = (e) => {
    const { type, msgId, result } = e.data;

    if (type === 'INIT_SUCCESS') {
      workerReady = true;
      workerFailed = false;
      pendingCallbacks.forEach((resolve, id) => {
        if (id === '__init__') {
          resolve(true);
          pendingCallbacks.delete(id);
        }
      });
    }

    if (type === 'INIT_ERROR') {
      console.error('[SpellCheckWorker] Init error:', e.data.error);
      workerFailed = true;
      pendingCallbacks.forEach((resolve, id) => {
        if (id === '__init__') {
          resolve(false);
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
    workerFailed = true;
    pendingCallbacks.forEach((resolve, id) => {
      if (id === '__init__') {
        resolve(false);
        pendingCallbacks.delete(id);
      }
    });
  };

  sharedWorker.postMessage({ type: 'INIT' });
  return sharedWorker;
};

export const useSpellCheck = () => {
  const [isLoaded, setIsLoaded] = useState(workerReady);

  useEffect(() => {
    if (workerReady) {
      setIsLoaded(true);
      return;
    }
    if (workerFailed) {
      setIsLoaded(false);
      return;
    }

    getWorker();

    const onReady = (success) => setIsLoaded(success);
    pendingCallbacks.set('__init__', onReady);

    return () => {
      pendingCallbacks.delete('__init__');
    };
  }, []);

  const checkText = useCallback((text) => {
    return new Promise((resolve) => {
      if (!text || !text.trim()) {
        resolve([]);
        return;
      }

      if (workerFailed || !sharedWorker) {
        resolve([]);
        return;
      }

      const worker = getWorker();
      const msgId = ++msgCounter;
      const timeoutId = setTimeout(() => {
        pendingCallbacks.delete(msgId);
        resolve([]);
      }, CHECK_TIMEOUT_MS);

      pendingCallbacks.set(msgId, (result) => {
        clearTimeout(timeoutId);
        resolve(result);
      });
      worker.postMessage({ type: 'CHECK', payload: text, msgId });
    });
  }, []);

  return { isLoaded, checkText };
};
