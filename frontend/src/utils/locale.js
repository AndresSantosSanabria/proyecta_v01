export const APP_LOCALE = 'es-CO';
export const APP_TIME_ZONE = 'America/Bogota';

export const dateFormatOptions = {
  timeZone: APP_TIME_ZONE,
};

export const dateTimeFormatOptions = {
  timeZone: APP_TIME_ZONE,
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
  second: '2-digit',
  hour12: false,
};

export const formatDate = (value, options = {}) => {
  if (!value) return '';
  const date = value instanceof Date ? value : new Date(value);
  if (Number.isNaN(date.getTime())) return '';
  return new Intl.DateTimeFormat(APP_LOCALE, { ...dateFormatOptions, ...options }).format(date);
};

export const formatDateTime = (value, options = {}) => {
  if (!value) return '';
  const date = value instanceof Date ? value : new Date(value);
  if (Number.isNaN(date.getTime())) return '';
  return new Intl.DateTimeFormat(APP_LOCALE, { ...dateTimeFormatOptions, ...options }).format(date);
};

export const formatNumber = (value, options = {}) => {
  const number = Number(value);
  if (!Number.isFinite(number)) return '';
  return new Intl.NumberFormat(APP_LOCALE, options).format(number);
};
