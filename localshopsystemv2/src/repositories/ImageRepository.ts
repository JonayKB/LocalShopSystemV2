import axios from "axios";
import BaseInfoRepository from "../utils/BaseInfoRepository";

class ImageRespository {
  async getImageById(id: string, token: string | null): Promise<string> {
    const response = await axios.get(
      BaseInfoRepository.BASE_URL + `images/${id}`,
      {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      }
    );

    const data = response.data;
    return data;
  }
}
export default ImageRespository;
