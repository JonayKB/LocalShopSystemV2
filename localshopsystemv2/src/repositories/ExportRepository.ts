import axios from "axios";
import { BaseInfoRepository } from "../utils/BaseInfoRepository";

class ExportRepository {
  async getExcel(token: string | null) {
    const file = await axios.get(
      BaseInfoRepository.BASE_URL + "generate-report",
      {
        responseType: "blob",
        headers: {
          Authorization: `Bearer ${token}`,
        },
      }
    );
    const blob = new Blob([file.data], {
      type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    });

    return blob;
  }

  async getIncomeByCategory(token: string | null, startDate?: Date, endDate?: Date) {
    if (!token) {
      return;
    }
    if (!startDate || !endDate) {
      const today = new Date();
      startDate = new Date(today.getFullYear(), today.getMonth(), today.getDate(), 1, 0, 0, 0);
      endDate = new Date(today.getFullYear(), today.getMonth(), today.getDate(), 24, 59, 59, 999);
    }

    const formattedStart = startDate?.toISOString().slice(0, -1);
    const formattedEnd = endDate?.toISOString().slice(0, -1);

    const response = await axios.get(
      BaseInfoRepository.BASE_URL + `total-income-by-category`,
      {
        headers: {
          Authorization: `Bearer ${token}`,
        },
        params: { startDate: formattedStart, endDate: formattedEnd },
      }
    );
    return response.data;
  }
}

export default ExportRepository;
