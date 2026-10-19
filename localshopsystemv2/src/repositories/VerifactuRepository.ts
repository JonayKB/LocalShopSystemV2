import axios from "axios";
import { BaseInfoRepository } from "../utils/BaseInfoRepository";

export type VerifactuSummary = {
  enabled: boolean;
  environment: string;
  counts: Record<string, number>;
  lastError?: string | null;
  lastSuccess?: string | null;
};

class VerifactuRepository {
  async getStatus(token: string | null): Promise<VerifactuSummary | undefined> {
    if (!token) {
      return;
    }
    const response = await axios.get(BaseInfoRepository.BASE_URL + "verifactu/status", {
      headers: { Authorization: `Bearer ${token}` },
    });
    return response.data;
  }

  /** Lanza un ciclo de envío a la AEAT ahora mismo. */
  async sendNow(token: string | null) {
    if (!token) {
      return;
    }
    const response = await axios.post(BaseInfoRepository.BASE_URL + "verifactu/send-now", null, {
      headers: { Authorization: `Bearer ${token}` },
    });
    return response.data;
  }
}

export default VerifactuRepository;
