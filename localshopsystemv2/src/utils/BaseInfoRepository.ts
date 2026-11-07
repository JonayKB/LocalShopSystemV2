/**
 * URL base de la API. Por defecto relativa (/kiosco/): en desarrollo la sirve el proxy de
 * Vite (vite.config.ts) y en producción se fija en .env.production (VITE_API_URL).
 */
const base = import.meta.env.VITE_API_URL || '/kiosco/';

export const BaseInfoRepository = {
  BASE_URL: base.endsWith('/') ? base : `${base}/`,
} as const;
