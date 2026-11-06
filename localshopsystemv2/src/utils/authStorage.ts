/**
 * Guarda el token de acceso (vida corta) y el refresh token (rota en cada uso).
 * Avisa con un evento cuando cambian, para que el contexto de React se actualice
 * también cuando el interceptor renueva el token en segundo plano.
 */
const ACCESS_KEY = 'token';
const REFRESH_KEY = 'refreshToken';
export const TOKENS_CHANGED_EVENT = 'auth-tokens-changed';

export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
  expiresIn?: number;
}

function read(key: string): string | null {
  try {
    return localStorage.getItem(key);
  } catch {
    return null;
  }
}

function notify(): void {
  window.dispatchEvent(new Event(TOKENS_CHANGED_EVENT));
}

export function getAccessToken(): string | null {
  return read(ACCESS_KEY);
}

export function getRefreshToken(): string | null {
  return read(REFRESH_KEY);
}

export function setTokens(tokens: AuthTokens): void {
  try {
    localStorage.setItem(ACCESS_KEY, tokens.accessToken);
    localStorage.setItem(REFRESH_KEY, tokens.refreshToken);
  } catch {
    /* sin almacenamiento disponible */
  }
  notify();
}

export function clearTokens(): void {
  try {
    localStorage.removeItem(ACCESS_KEY);
    localStorage.removeItem(REFRESH_KEY);
  } catch {
    /* sin almacenamiento disponible */
  }
  notify();
}
