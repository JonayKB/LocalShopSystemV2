import axios from "axios";
import {BaseInfoRepository} from "../utils/BaseInfoRepository";

class AuthRepository {
  async login(username: string, password: string) {
    if (!username || !password) {
      return;
    }
    const response = await axios.post(
      `${BaseInfoRepository.BASE_URL}auth/login?email=${encodeURIComponent(
        username
      )}&password=${encodeURIComponent(password)}`
    );
    return response.data;
  }
}
export default AuthRepository;
