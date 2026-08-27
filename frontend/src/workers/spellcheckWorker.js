import Typo from 'typo-js';

let dictionary = null;
let isLoaded = false;

self.onmessage = async (e) => {
  const { type, payload, msgId } = e.data;

  if (type === 'INIT') {
    try {
      const affData = await fetch('/dictionaries/es/es_ANY.aff').then(res => res.text());
      const dicData = await fetch('/dictionaries/es/es_ANY.dic').then(res => res.text());
      
      dictionary = new Typo("es_ANY", affData, dicData);
      isLoaded = true;
      self.postMessage({ type: 'INIT_SUCCESS' });
    } catch (error) {
      console.error("Worker error initializing dictionary:", error);
      self.postMessage({ type: 'INIT_ERROR', error: error.message });
    }
  }

  if (type === 'CHECK') {
    if (!isLoaded || !dictionary) {
      self.postMessage({ type: 'RESULT', msgId, result: [] });
      return;
    }

    const text = payload || "";
    // Normalize and split text by spaces and punctuation
    const words = text.replace(/[.,\/#!$%\^&\*;:{}=\-_`~()]/g,"").split(/\s+/);
    const misspelledWords = [];

    words.forEach(word => {
      // Typo-js may fail for numbers or empty strings, so check if word is valid
      if (word.trim() && isNaN(word) && !dictionary.check(word)) {
        // Prevent duplicate entries for the same word
        if (!misspelledWords.find(w => w.word === word)) {
           misspelledWords.push({
             word,
             suggestions: dictionary.suggest(word).slice(0, 4) // Top 4 suggestions
           });
        }
      }
    });

    self.postMessage({ type: 'RESULT', msgId, result: misspelledWords });
  }
};
