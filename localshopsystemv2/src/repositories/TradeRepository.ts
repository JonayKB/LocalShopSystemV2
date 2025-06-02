import axios from "axios";
import Item from "../models/Item";
import BaseInfoRepository from "../utils/BaseInfoRepository";

class TradeRepository {
  async createTrade(itemMap: Map<Item, number>, token: string | null) {
    let items: Item[] = [];
    if (!token) {
      console.error("Token is required to create a trade.");
      return;
    }
    if (itemMap.size <= 0) {
      console.error("Item map is empty. Cannot create trade.");
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
      }
    );
    return response.data;
  }
}
export default TradeRepository;
