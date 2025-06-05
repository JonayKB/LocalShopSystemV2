import axios from "axios";
import BaseInfoRepository from "../utils/BaseInfoRepository";

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
}

export default ExportRepository;
