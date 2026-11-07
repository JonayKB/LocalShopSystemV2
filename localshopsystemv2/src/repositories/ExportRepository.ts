import { api } from "../utils/api";

class ExportRepository {
  /** Informe mensual: ventas por día y categoría + hoja de base, IGIC, coste y beneficio. */
  async getExcel(token: string | null, month?: number, year?: number) {
    const file = await api.get("generate-report",
      {
        responseType: "blob",
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
