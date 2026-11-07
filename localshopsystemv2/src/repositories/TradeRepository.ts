import { api } from "../utils/api";
import Trade, { PaymentMethod } from "../models/Trade";
import { BasketLine } from "../components/MainContextProvider";

class TradeRepository {
  /** Cobra la venta: una línea por producto con su cantidad. */
  async createTrade(lines: BasketLine[], token: string | null, print: boolean, paymentMethod: PaymentMethod = 'CASH') {
    if (!token || lines.length === 0) {
      return;
    }
    const response = await api.post<Trade>("trade/sale", {
      lines: lines.map(l => ({ itemId: l.item.id, quantity: l.quantity })),
      paymentMethod,
      print,
    });
    return response.data;
  }

  /**
   * Anula una venta (no la borra): conserva el registro, la marca como
   * anulada y el servidor devuelve el stock.
   */
  async cancelTrade(tradeId: number, reason: string, token: string | null) {
    if (!token || !tradeId) {
      return;
    }
    const response = await api.post<Trade>(`trade/${tradeId}/cancel`, null, {
      params: reason.trim() ? { reason: reason.trim() } : undefined,
    });
    return response.data;
  }

  /**
   * Imprime en la impresora de tickets el resumen de hoy: todos los productos
   * ('products') o agrupados por categoría ('categories').
   */
  async printDaily(mode: 'products' | 'categories', token: string | null) {
    if (!token) {
      return;
    }
    await api.post('trade/print-daily', null, { params: { mode } });
  }

  async getPage(page: number, size: number) {
    const response = await api.get(`trade/${page}/${size}`);
    return response.data;
  }
}

export default TradeRepository;
