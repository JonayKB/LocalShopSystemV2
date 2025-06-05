import axios from 'axios';
import BaseInfoRepository from '../utils/BaseInfoRepository';

class AuthRepository {
  async login(username: string, password: string, ip: string | undefined) {
    const response = await axios.post(
      `${BaseInfoRepository.getBaseUrl(
        ip,
      )}auth/login?email=${encodeURIComponent(
        username,
      )}&password=${encodeURIComponent(password)}`,
    );
    return response.data;
  }
}
export default AuthRepository;
