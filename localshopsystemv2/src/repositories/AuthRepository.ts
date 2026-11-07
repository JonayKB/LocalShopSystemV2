import { api } from "../utils/api";
import { AuthTokens } from "../utils/authStorage";

class AuthRepository {
  /** Las credenciales van en el cuerpo (JSON), nunca en la URL: así no acaban en los logs. */
  async login(username: string, password: string): Promise<AuthTokens | undefined> {
    if (!username || !password) {
      return;
    }
    const response = await api.post<AuthTokens>(`auth/login`, {
      email: username,
      password,
    });
    return response.data;
  }

  /** Cambia el refresh token por un par nuevo (el usado queda revocado). */
  async refresh(refreshToken: string): Promise<AuthTokens> {
    const response = await api.post<AuthTokens>(`auth/refresh`, {
      refreshToken,
    });
    return response.data;
  }

  /** Revoca el refresh token en el servidor. Si falla, la sesión se cierra igualmente en local. */
  async logout(refreshToken: string | null): Promise<void> {
    if (!refreshToken) {
      return;
    }
    await api.post(`auth/logout`, { refreshToken });
  }
}
export default AuthRepository;
