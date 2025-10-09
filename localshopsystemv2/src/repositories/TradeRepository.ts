import axios from "axios";
import Item from "../models/Item";
import { BaseInfoRepository } from "../utils/BaseInfoRepository";
import { start } from "repl";



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
  async deleteTrade(tradeId: number, token: string | null) {
    if (!token) {
      return;
    }
    if (!tradeId) {
      return;
    }
    const response = await axios.delete(
      BaseInfoRepository.BASE_URL + `trade/${tradeId}`,
      {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      }
    );
    return response.data;
  }
  async getTodayTrades(token: string | null, startDate?: Date, endDate?: Date) {
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
