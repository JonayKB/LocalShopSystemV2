import axios from "axios";
import Item from "../models/Item";
import { BaseInfoRepository } from "../utils/BaseInfoRepository";



class TradeRepository {

  async createTrade(itemMap: Map<Item, number>, token: string | null, print: boolean) {
    let items: Item[] = [];
    if (!token) {
      return;
    }
    if (itemMap.size <= 0) {
      return;
    }
    items = [];
    itemMap.forEach((quantity, item) => {
      for (let i = 0; i < quantity; i++) {
        items.push({
          id: item.id,
          name: item.name,
          price: item.price,
          categoryId: item.categoryId,
          image: item.image,
          stock: item.stock,
          bareMinimun: item.bareMinimun,
          net: item.net,
          ignoreStock: item.ignoreStock,
        });
      }
    });

    const response = await axios.post(
      BaseInfoRepository.BASE_URL + "trade/newTrade",
      items,
      {
        headers: {
          Authorization: `Bearer ${token}`,
        },
        params: { print },
      }
    );

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
    const response = await axios.post(
      BaseInfoRepository.BASE_URL + `trade/${tradeId}/cancel`,
      null,
      {
        headers: {
          Authorization: `Bearer ${token}`,
        },
        params: reason.trim() ? { reason: reason.trim() } : undefined,
      }
    );
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
    await axios.post(BaseInfoRepository.BASE_URL + 'trade/print-daily', null, {
      headers: { Authorization: `Bearer ${token}` },
      params: { mode },
    });
  }

  async getTradesByDate(token: string | null, startDate?: Date, endDate?: Date) {
    if (!token) {
      return;
    }

    const today = new Date();
    const startOfDay = new Date(today.getFullYear(), today.getMonth(), today.getDate(), 0, 0, 0, 0);
    const endOfDay = new Date(today.getFullYear(), today.getMonth(), today.getDate(), 23, 59, 59, 999);

    const response = await axios.get(
      BaseInfoRepository.BASE_URL + `trade/range`,
      {
        headers: {
          Authorization: `Bearer ${token}`,
        },
        params: { startDate: startDate?.toISOString() ?? startOfDay.toISOString(), endDate: endDate?.toISOString() ?? endOfDay.toISOString() },
      }
    );
    return response.data;
  }

}

export default TradeRepository;