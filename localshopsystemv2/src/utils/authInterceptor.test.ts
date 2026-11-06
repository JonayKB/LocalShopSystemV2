import axios, { AxiosAdapter, AxiosError, AxiosResponse, InternalAxiosRequestConfig } from 'axios';
import AuthRepository from '../repositories/AuthRepository';
import { registerAuthInterceptor } from './authInterceptor';
import { getAccessToken, getRefreshToken, setTokens } from './authStorage';

/** Servidor falso: responde 401 si el token no es el "bueno" en ese momento. */
function fakeServer(validToken: () => string) {
  const calls: string[] = [];
  const adapter: AxiosAdapter = async (config: InternalAxiosRequestConfig) => {
    const auth = String(config.headers?.Authorization ?? '');
    calls.push(auth);
    const response: AxiosResponse = {
      data: auth === `Bearer ${validToken()}` ? 'ok' : { error: 'unauthorized' },
      status: auth === `Bearer ${validToken()}` ? 200 : 401,
      statusText: '',
      headers: {},
      config,
    };
    if (response.status === 401) {
      throw new AxiosError('401', 'ERR_BAD_REQUEST', config, null, response);
    }
    return response;
  };
  return { adapter, calls };
}

describe('authInterceptor', () => {
  const replace = jest.fn();
  let refreshSpy: jest.SpyInstance;

  beforeAll(() => {
    Object.defineProperty(window, 'location', {
      configurable: true,
      value: { ...window.location, pathname: '/admin/items', replace },
    });
  });

  beforeEach(() => {
    localStorage.clear();
    replace.mockReset();
    refreshSpy = jest.spyOn(AuthRepository.prototype, 'refresh');
  });

  afterEach(() => refreshSpy.mockRestore());

  test('renueva el token caducado y repite la petición', async () => {
    setTokens({ accessToken: 'viejo', refreshToken: 'r1' });
    let valid = 'nuevo';
    const { adapter, calls } = fakeServer(() => valid);
    const api = axios.create({ adapter });
    registerAuthInterceptor(api);
    refreshSpy.mockResolvedValue({ accessToken: 'nuevo', refreshToken: 'r2', expiresIn: 900 });

    const res = await api.get('/items', { headers: { Authorization: 'Bearer viejo' } });

    expect(res.data).toBe('ok');
    expect(refreshSpy).toHaveBeenCalledWith('r1');
    expect(calls).toEqual(['Bearer viejo', 'Bearer nuevo']);
    expect(getAccessToken()).toBe('nuevo');
    expect(getRefreshToken()).toBe('r2');
    expect(replace).not.toHaveBeenCalled();
    valid = '';
  });

  test('usa siempre el token más reciente aunque el componente pase uno viejo', async () => {
    setTokens({ accessToken: 'actual', refreshToken: 'r1' });
    const { adapter, calls } = fakeServer(() => 'actual');
    const api = axios.create({ adapter });
    registerAuthInterceptor(api);

    await api.get('/items', { headers: { Authorization: 'Bearer del-contexto-viejo' } });

    expect(calls).toEqual(['Bearer actual']);
    expect(refreshSpy).not.toHaveBeenCalled();
  });

  test('varias peticiones a la vez comparten un único refresco', async () => {
    setTokens({ accessToken: 'viejo', refreshToken: 'r1' });
    const { adapter } = fakeServer(() => 'nuevo');
    const api = axios.create({ adapter });
    registerAuthInterceptor(api);
    // El refresco tarda: así las tres peticiones fallan antes de que termine.
    refreshSpy.mockImplementation(
      () =>
        new Promise((resolve) =>
          setTimeout(() => resolve({ accessToken: 'nuevo', refreshToken: 'r2', expiresIn: 900 }), 20)
        )
    );
    // El interceptor de petición lee el token guardado: hay que "congelar" el viejo para las tres.
    const results = await Promise.all(
      [1, 2, 3].map(() => api.get('/x', { headers: { Authorization: 'Bearer viejo' } }))
    );

    expect(results.map((r) => r.data)).toEqual(['ok', 'ok', 'ok']);
    expect(refreshSpy).toHaveBeenCalledTimes(1);
  });

  test('si el refresco falla, cierra la sesión y manda al login', async () => {
    setTokens({ accessToken: 'viejo', refreshToken: 'revocado' });
    const { adapter } = fakeServer(() => 'nunca');
    const api = axios.create({ adapter });
    registerAuthInterceptor(api);
    refreshSpy.mockRejectedValue(new Error('401'));

    await expect(api.get('/x', { headers: { Authorization: 'Bearer viejo' } })).rejects.toBeTruthy();

    expect(getAccessToken()).toBeNull();
    expect(getRefreshToken()).toBeNull();
    expect(replace).toHaveBeenCalledWith('/admin/login');
  });

  test('sin refresh token no intenta refrescar y manda al login', async () => {
    localStorage.setItem('token', 'viejo');
    const { adapter } = fakeServer(() => 'nunca');
    const api = axios.create({ adapter });
    registerAuthInterceptor(api);

    await expect(api.get('/x', { headers: { Authorization: 'Bearer viejo' } })).rejects.toBeTruthy();

    expect(refreshSpy).not.toHaveBeenCalled();
    expect(replace).toHaveBeenCalledWith('/admin/login');
  });

  test('un 401 de una petición pública (sin token) no toca la sesión', async () => {
    setTokens({ accessToken: 'a', refreshToken: 'r' });
    const { adapter } = fakeServer(() => 'nunca');
    const api = axios.create({ adapter });
    registerAuthInterceptor(api);

    await expect(api.get('/publica')).rejects.toBeTruthy();

    expect(refreshSpy).not.toHaveBeenCalled();
    expect(getAccessToken()).toBe('a');
    expect(replace).not.toHaveBeenCalled();
  });

  test('un 401 del login (credenciales malas) no intenta refrescar', async () => {
    const { adapter } = fakeServer(() => 'nunca');
    const api = axios.create({ adapter });
    registerAuthInterceptor(api);

    await expect(
      api.post('/kiosco/auth/login', {}, { headers: { Authorization: 'Bearer x' } })
    ).rejects.toBeTruthy();

    expect(refreshSpy).not.toHaveBeenCalled();
    expect(replace).not.toHaveBeenCalled();
  });
});
