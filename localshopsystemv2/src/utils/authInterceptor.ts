import { AxiosError, AxiosInstance, InternalAxiosRequestConfig } from 'axios';
import AuthRepository from '../repositories/AuthRepository';
import { api } from './api';
import { AuthTokens, clearTokens, getAccessToken, getRefreshToken, setTokens } from './authStorage';

type RetriableConfig = InternalAxiosRequestConfig & { _retried?: boolean };

const AUTH_PATHS = ['auth/login', 'auth/refresh', 'auth/logout'];

function isAuthCall(url: string | undefined): boolean {
  return Boolean(url && AUTH_PATHS.some((p) => url.includes(p)));
}

function hasAuthHeader(config: InternalAxiosRequestConfig | undefined): boolean {
  return Boolean(config?.headers?.Authorization);
}

function goToLogin(): void {
  clearTokens();
  if (!window.location.pathname.toLowerCase().endsWith('/admin/login')) {
    // Recarga completa: también limpia el estado de React.
    window.location.replace('/admin/login');
  }
}

const registered = new WeakSet<AxiosInstance>();
/** Un único refresco en vuelo: si fallan varias peticiones a la vez, todas esperan al mismo. */
let refreshing: Promise<string> | null = null;

export function refreshAccessToken(authRepository: AuthRepository = new AuthRepository()): Promise<string> {
  if (!refreshing) {
    const refreshToken = getRefreshToken();
    refreshing = (refreshToken
      ? authRepository.refresh(refreshToken)
      : Promise.reject(new Error('Sin refresh token'))
    )
      .then((tokens: AuthTokens) => {
        setTokens(tokens);
        return tokens.accessToken;
      })
      .finally(() => {
        refreshing = null;
      });
  }
  return refreshing;
}

/**
 * - Cada petición (salvo login/refresh/logout) lleva el token de acceso más reciente si hay
 *   sesión. Los repositorios no tocan cabeceras.
 * - Respuesta 401 de una petición con token: se renueva con el refresh token y se repite
 *   una vez. Si el refresco falla (caducado, revocado...), se cierra la sesión.
 */
export function registerAuthInterceptor(instance: AxiosInstance = api): void {
  if (registered.has(instance)) {
    return;
  }
  registered.add(instance);

  instance.interceptors.request.use((config) => {
    if (!isAuthCall(config.url)) {
      const latest = getAccessToken();
      if (latest) {
        config.headers.Authorization = `Bearer ${latest}`;
      }
    }
    return config;
  });

  instance.interceptors.response.use(
    (response) => response,
    async (error: AxiosError) => {
      const config = error.config as RetriableConfig | undefined;
      const status = error.response?.status;

      if (status !== 401 || !config || !hasAuthHeader(config) || isAuthCall(config.url)) {
        return Promise.reject(error);
      }
      if (config._retried) {
        goToLogin();
        return Promise.reject(error);
      }
      try {
        const accessToken = await refreshAccessToken();
        config._retried = true;
        config.headers.Authorization = `Bearer ${accessToken}`;
        return instance.request(config);
      } catch {
        goToLogin();
        return Promise.reject(error);
      }
    }
  );
}
