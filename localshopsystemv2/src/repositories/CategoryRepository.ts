import axios from "axios";
import {BaseInfoRepository} from "../utils/BaseInfoRepository";
import Category from "../models/Category";

class CategoryRepository {
  async getCategories(token: string|null) {
    const response = await axios.get(
      BaseInfoRepository.BASE_URL + "categories/",
      {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      }
    );
    return response.data as Category[];
  }
}
export default CategoryRepository;
