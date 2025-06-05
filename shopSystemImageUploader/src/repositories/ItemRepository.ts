import axios from 'axios';
import Page from '../models/Page';
import Item from '../models/Item';
import BaseInfoRepository from '../utils/BaseInfoRepository';

class ItemRepository {
  async getItemsByCategory(
    categoryId: number,
    page: number,
    size: number,
    sortBy: string,
    ascending: boolean,
    name: string,
    token: string | null,
    ip: string | undefined,
  ) {
    const response = await axios.get<Page<Item>>(
      `${BaseInfoRepository.getBaseUrl(ip)}items/${categoryId}/${page}/${size}`,
      {
        params: {
          sortBy,
          ascending,
          name,
        },
        headers: {
          Authorization: `Bearer ${token}`,
          Accept: '*/*',
        },
      },
    );
    return response.data;
  }

  async getItemById(
    itemId: string,
    token: string | null,
    ip: string | undefined,
  ): Promise<Item> {
    const response = await axios.get<Item>(
      `${BaseInfoRepository.getBaseUrl(ip)}items/${itemId}`,
      {
        headers: {
          Authorization: `Bearer ${token}`,
          Accept: '*/*',
        },
      },
    );
    return response.data;
  }
}
export default ItemRepository;
