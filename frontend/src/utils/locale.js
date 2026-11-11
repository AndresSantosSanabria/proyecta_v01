export const APP_LOCALE = 'es-CO';
export const APP_TIME_ZONE = 'America/Bogota';

export const dateFormatOptions = {
  timeZone: APP_TIME_ZONE,
  day: 'numeric',
  month: 'long',
  year: 'numeric',
};

export const dateTimeFormatOptions = {
  timeZone: APP_TIME_ZONE,
  year: 'numeric',
  month: 'long',
  day: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
  second: '2-digit',
  hour12: false,
};

// Las fechas de la API llegan como "AAAA-MM-DD" sin zona horaria: new Date() las
// interpreta como medianoche UTC y al formatear en America/Bogota (UTC-5) muestran
// el dia anterior. Se tratan como fecha de calendario pura (medianoche UTC + UTC).
const DATE_ONLY_PATTERN = /^\d{4}-\d{2}-\d{2}$/;

const parseDate = (value) => {
  if (value instanceof Date) return { date: value, timeZone: APP_TIME_ZONE };
  const raw = String(value).trim();
  if (DATE_ONLY_PATTERN.test(raw)) {
    const [year, month, day] = raw.split('-').map(Number);
    return { date: new Date(Date.UTC(year, month - 1, day)), timeZone: 'UTC' };
  }
  return { date: new Date(raw), timeZone: APP_TIME_ZONE };
};

export const formatDate = (value, options = {}) => {
  if (!value) return '';
  const { date, timeZone } = parseDate(value);
  if (Number.isNaN(date.getTime())) return '';
  return new Intl.DateTimeFormat(APP_LOCALE, { ...dateFormatOptions, timeZone, ...options }).format(date);
};

export const formatDateTime = (value, options = {}) => {
  if (!value) return '';
  const { date, timeZone } = parseDate(value);
  if (Number.isNaN(date.getTime())) return '';
  return new Intl.DateTimeFormat(APP_LOCALE, { ...dateTimeFormatOptions, timeZone, ...options }).format(date);
};
