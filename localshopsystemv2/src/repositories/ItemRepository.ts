import axios from "axios";
import Page from "../models/Page";
import {BaseInfoRepository} from "../utils/BaseInfoRepository";
import Item from "../models/Item";
import { Page as MovementPage, PriceChange, StockMovement } from "../models/Stock";

class ItemRepository {
  async getItemsByCategory(
    categoryId: number,
    page: number,
    size: number,
    sortBy: string,
    ascending: boolean,
    name: string,
    token: string | null
  ) {
    const response = await axios.get<Page<Item>>(
      `${BaseInfoRepository.BASE_URL}items/${categoryId}/${page}/${size}`,
      {
        params: {
          sortBy,
          ascending,
          name,
        },
        headers: {
          Authorization: `Bearer ${token}`,
          Accept: "*/*",
        },
      }
    );
    return response.data;
  }

  async getItemById(itemId: string, token: string | null): Promise<Item> {
    const response = await axios.get<Item>(
      `${BaseInfoRepository.BASE_URL}items/${itemId}`,
      {
        headers: {
          Authorization: `Bearer ${token}`,
          Accept: "*/*",
        },
      }
    );
    return response.data;
  }

  async addItem(item: Item, token: string | null): Promise<Item> {
    const response = await axios.post<Item>(
      `${BaseInfoRepository.BASE_URL}items/`,
      item,
      {
        headers: {
          Authorization: `Bearer ${token}`,
          Accept: "*/*",
        },
      }
    );
    return response.data;
  }

  /** Historial de precios y costes, del más reciente al más antiguo. */
  async getPriceHistory(itemId: number, token: string | null): Promise<PriceChange[]> {
    const response = await axios.get<PriceChange[]>(`${BaseInfoRepository.BASE_URL}items/${itemId}/price-history`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    return response.data;
  }

  /** Movimientos de stock del producto, del más reciente al más antiguo. */
  async getStockMovements(itemId: number, page: number, token: string | null): Promise<MovementPage<StockMovement>> {
    const response = await axios.get<MovementPage<StockMovement>>(
      `${BaseInfoRepository.BASE_URL}items/${itemId}/stock-movements`,
      { headers: { Authorization: `Bearer ${token}` }, params: { page, size: 15 } }
    );
    return response.data;
  }

}
export default ItemRepository;
