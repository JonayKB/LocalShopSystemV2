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
        responseType: "blob",
      }
    );

    const blob = response.data;
    return URL.createObjectURL(blob);
  }

  async uploadImage(file: File, token: string | null) {
    const formData = new FormData();
    formData.append("file", file); 

    const response = await axios.post(
      BaseInfoRepository.BASE_URL + "images/",
      formData,
      {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      }
    );

    return response.data;
  }
}
export default ImageRespository;
