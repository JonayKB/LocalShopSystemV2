import { api } from "../utils/api";

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
    const response = await api.get<VerifactuSummary>("verifactu/status");
    return response.data;
  }

  /** Lanza un ciclo de envío a la AEAT ahora mismo. */
  async sendNow(token: string | null) {
    if (!token) {
      return;
    }
    const response = await api.post("verifactu/send-now");
    return response.data;
  }
}

export default VerifactuRepository;
