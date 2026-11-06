import axios from "axios";
import Item from "../models/Item";
import { MovementLine, ManualMovementType, Page, StockMovement } from "../models/Stock";
import { BaseInfoRepository } from "../utils/BaseInfoRepository";

class StockRepository {
  /** Registra entradas, devoluciones, mermas o ajustes. Todo o nada. */
  async registerMovements(type: ManualMovementType, lines: MovementLine[], note: string, token: string | null) {
    const res = await axios.post<StockMovement[]>(
      BaseInfoRepository.BASE_URL + "stock/movements",
      { type, note: note.trim() || null, lines },
      { headers: { Authorization: `Bearer ${token}` } }
    );
    return res.data;
  }

  /** Libro de movimientos (más recientes primero). */
  async getMovements(
    filters: { type?: string; itemId?: number; page?: number; size?: number },
    token: string | null
  ) {
    const res = await axios.get<Page<StockMovement>>(BaseInfoRepository.BASE_URL + "stock/movements", {
      headers: { Authorization: `Bearer ${token}` },
      params: { page: 0, size: 25, ...filters, type: filters.type || undefined },
    });
    return res.data;
  }

  /** Devolución al proveedor: registra el movimiento y descarga el albarán en Excel. */
  async removeStock(itemsToRemove: Map<Item, number>, token: string | null, note?: string) {
    const body: Record<number, number> = {};
    itemsToRemove.forEach((quantity, item) => {
      body[item.id] = quantity;
    });

    const res = await axios.delete(BaseInfoRepository.BASE_URL + "stock/", {
      headers: { Authorization: `Bearer ${token}` },
      params: { note: note?.trim() || undefined },
      data: body,
      responseType: "blob",
    });

    return new Blob([res.data], {
      type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    });
  }
}

export default StockRepository;
