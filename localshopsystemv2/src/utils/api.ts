import axios from 'axios';
import { BaseInfoRepository } from './BaseInfoRepository';

/**
 * Cliente HTTP único de la API: URL base común y, mediante los interceptores de
 * authInterceptor.ts, el token de acceso (y su renovación) en cada petición.
 * Los repositorios no gestionan cabeceras.
 */
export const api = axios.create({ baseURL: BaseInfoRepository.BASE_URL });

/** Mensaje de error legible: el servidor responde con texto plano. */
export function errorMessage(e: unknown, fallback: string): string {
  const data = (e as { response?: { data?: unknown } })?.response?.data;
  return typeof data === 'string' && data.trim() ? data : fallback;
}
