import { api } from "../utils/api";
import { BaseInfoRepository } from "../utils/BaseInfoRepository";
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

  /** Sube la imagen (PNG, JPG, GIF o WEBP, máx. 10 MB) y devuelve su nombre en el servidor. */
  async uploadImage(file: File, _token?: string | null): Promise<string> {
    const formData = new FormData();
    formData.append("file", file);
    const response = await api.post<string>("images/", formData);
    return response.data;
  }
}
export default ImageRespository;
