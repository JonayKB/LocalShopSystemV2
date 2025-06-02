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
    try {
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
      console.log(`Response received:`, response.data);
      return response.data;
    } catch (error) {
      console.error(`Error fetching items for category ${categoryId}:`, error);
      throw error; // Re-throw the error to be handled by the caller
    }
  }
}
export default ItemRepository;
