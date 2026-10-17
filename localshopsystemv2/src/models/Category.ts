type Category = {
  id: number;
  name: string;
  image: string;
  /** Tipo de IGIC (%) de la categoría. null = tipo por defecto del servidor. */
  igicRate?: number | null;
};
export default Category;
