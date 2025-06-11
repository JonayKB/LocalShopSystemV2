import axios from "axios";
import Page from "../models/Page";
import BaseInfoRepository from "../utils/BaseInfoRepository";
import Item from "../models/Item";

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
}
export default ItemRepository;
