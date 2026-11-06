import axios from 'axios';
import AuthRepository from './AuthRepository';
import { BaseInfoRepository } from '../utils/BaseInfoRepository';

describe('AuthRepository', () => {
  let post: jest.SpyInstance;
  const repo = new AuthRepository();

  beforeEach(() => {
    post = jest.spyOn(axios, 'post').mockResolvedValue({
      data: { accessToken: 'a', refreshToken: 'r', expiresIn: 900 },
    });
  });
  afterEach(() => post.mockRestore());

  test('login envía las credenciales en el cuerpo, no en la URL', async () => {
    const tokens = await repo.login('admin@test', 'clave&rara?=');

    const [url, body] = post.mock.calls[0];
    expect(url).toBe(`${BaseInfoRepository.BASE_URL}auth/login`);
    expect(url).not.toContain('password');
    expect(url).not.toContain('?');
    expect(body).toEqual({ email: 'admin@test', password: 'clave&rara?=' });
    expect(tokens).toEqual({ accessToken: 'a', refreshToken: 'r', expiresIn: 900 });
  });

  test('login sin datos no llama al servidor', async () => {
    expect(await repo.login('', '')).toBeUndefined();
    expect(post).not.toHaveBeenCalled();
  });

  test('refresh y logout mandan el refresh token', async () => {
    await repo.refresh('r1');
    await repo.logout('r2');
    await repo.logout(null);

    expect(post).toHaveBeenNthCalledWith(1, `${BaseInfoRepository.BASE_URL}auth/refresh`, { refreshToken: 'r1' });
    expect(post).toHaveBeenNthCalledWith(2, `${BaseInfoRepository.BASE_URL}auth/logout`, { refreshToken: 'r2' });
    expect(post).toHaveBeenCalledTimes(2);
  });
});
