import axios from "axios";
import { BaseInfoRepository } from "../utils/BaseInfoRepository";

class ExportRepository {
  /** Informe mensual: ventas por día y categoría + hoja de base, IGIC, coste y beneficio. */
  async getExcel(token: string | null, month?: number, year?: number) {
    const file = await axios.get(
      BaseInfoRepository.BASE_URL + "generate-report",
      {
        responseType: "blob",
        headers: {
          Authorization: `Bearer ${token}`,
        },
        params: { month, year },
      }
    );
    const blob = new Blob([file.data], {
      type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    });

    return blob;
  }

}

export default ExportRepository;
