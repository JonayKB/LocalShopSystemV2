import { api } from "../utils/api";
import Category from "../models/Category";

class CategoryRepository {
  async getCategories(_token?: string | null) {
    const response = await api.get<Category[]>("categories/");
    return response.data;
  }

  async saveCategory(category: Category, _token?: string | null) {
    const response = await api.post<Category>("categories/", category);
    return response.data;
  }
}
export default CategoryRepository;
