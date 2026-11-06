import axios from "axios";
import { BaseInfoRepository } from "../utils/BaseInfoRepository";
import { AuthTokens } from "../utils/authStorage";

class AuthRepository {
  /** Las credenciales van en el cuerpo (JSON), nunca en la URL: así no acaban en los logs. */
  async login(username: string, password: string): Promise<AuthTokens | undefined> {
    if (!username || !password) {
      return;
    }
    const response = await axios.post<AuthTokens>(`${BaseInfoRepository.BASE_URL}auth/login`, {
      email: username,
      password,
    });
    return response.data;
  }

  /** Cambia el refresh token por un par nuevo (el usado queda revocado). */
  async refresh(refreshToken: string): Promise<AuthTokens> {
    const response = await axios.post<AuthTokens>(`${BaseInfoRepository.BASE_URL}auth/refresh`, {
      refreshToken,
    });
    return response.data;
  }

  /** Revoca el refresh token en el servidor. Si falla, la sesión se cierra igualmente en local. */
  async logout(refreshToken: string | null): Promise<void> {
    if (!refreshToken) {
      return;
    }
    await axios.post(`${BaseInfoRepository.BASE_URL}auth/logout`, { refreshToken });
  }
}
export default AuthRepository;
