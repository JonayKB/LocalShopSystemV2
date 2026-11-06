import { clearTokens, getAccessToken, getRefreshToken, setTokens, TOKENS_CHANGED_EVENT } from './authStorage';

describe('authStorage', () => {
  beforeEach(() => localStorage.clear());

  test('guarda, lee y borra ambos tokens avisando del cambio', () => {
    const listener = jest.fn();
    window.addEventListener(TOKENS_CHANGED_EVENT, listener);

    setTokens({ accessToken: 'a', refreshToken: 'r' });
    expect(getAccessToken()).toBe('a');
    expect(getRefreshToken()).toBe('r');

    clearTokens();
    expect(getAccessToken()).toBeNull();
    expect(getRefreshToken()).toBeNull();

    expect(listener).toHaveBeenCalledTimes(2);
    window.removeEventListener(TOKENS_CHANGED_EVENT, listener);
  });

  test('sin localStorage disponible no rompe', () => {
    const spy = jest.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
      throw new Error('bloqueado');
    });
    expect(getAccessToken()).toBeNull();
    spy.mockRestore();
  });
});
