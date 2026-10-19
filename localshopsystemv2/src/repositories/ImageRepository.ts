import axios from "axios";
import {BaseInfoRepository} from "../utils/BaseInfoRepository";
import { PLACEHOLDER_IMG } from "../utils/placeholder";

class ImageRespository {
  /**
   * URL directa de la imagen. Va como <img src>: así la cachea el navegador (el servidor
   * la marca como cacheable un año) y se pide solo cuando la imagen está a punto de verse.
   * width pide una miniatura (160, 320, 640 o 1024).
   */
  static url(name: string | null | undefined, width?: number): string {
    if (!name) {
      return PLACEHOLDER_IMG;
    }
    const w = width ? `?w=${width}` : "";
    return BaseInfoRepository.BASE_URL + `images/${encodeURIComponent(name)}${w}`;
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