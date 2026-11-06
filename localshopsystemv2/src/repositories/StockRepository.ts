import { api } from "../utils/api";
import Item from "../models/Item";
import { MovementLine, ManualMovementType, Page, StockMovement } from "../models/Stock";

class StockRepository {
  /** Registra entradas, devoluciones, mermas o ajustes. Todo o nada. */
  async registerMovements(type: ManualMovementType, lines: MovementLine[], note: string, _token?: string | null) {
    const res = await api.post<StockMovement[]>("stock/movements", { type, note: note.trim() || null, lines });
    return res.data;
  }

  /** Libro de movimientos (más recientes primero). */
  async getMovements(filters: { type?: string; itemId?: number; page?: number; size?: number }, _token?: string | null) {
    const res = await api.get<Page<StockMovement>>("stock/movements", {
      params: { page: 0, size: 25, ...filters, type: filters.type || undefined },
    });
    return res.data;
  }

  /** Devolución al proveedor: registra el movimiento y descarga el albarán en Excel. */
  async removeStock(itemsToRemove: Map<Item, number>, _token?: string | null, note?: string) {
    const body: Record<number, number> = {};
    itemsToRemove.forEach((quantity, item) => {
      body[item.id] = quantity;
    });
    const res = await api.delete("stock/", {
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
