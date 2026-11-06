import { api } from "../utils/api";
import Page from "../models/Page";
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
    _token?: string | null
  ) {
    const response = await api.get<Page<Item>>(`items/${categoryId}/${page}/${size}`, {
      params: { sortBy, ascending, name },
    });
    return response.data;
  }

  async getItemById(itemId: string, _token?: string | null): Promise<Item> {
    const response = await api.get<Item>(`items/${itemId}`);
    return response.data;
  }

  /** Alta o edición (si el código ya existe se edita conservando su historial). */
  async addItem(item: Item, _token?: string | null): Promise<Item> {
    const response = await api.post<Item>("items/", item);
    return response.data;
  }

  /** Historial de precios y costes, del más reciente al más antiguo. */
  async getPriceHistory(itemId: number, _token?: string | null): Promise<PriceChange[]> {
    const response = await api.get<PriceChange[]>(`items/${itemId}/price-history`);
    return response.data;
  }

  /** Movimientos de stock del producto, del más reciente al más antiguo. */
  async getStockMovements(itemId: number, page: number, _token?: string | null): Promise<MovementPage<StockMovement>> {
    const response = await api.get<MovementPage<StockMovement>>(`items/${itemId}/stock-movements`, {
      params: { page, size: 15 },
    });
    return response.data;
  }
}
export default ItemRepository;
