type Item = {
  id: number;
  name: string;
  price: number;
  categoryId: number;
  image: string;
  stock:number;
  bareMinimun:number;
  net:number;
  ignoreStock:boolean;
  /** Al editar: stock que mostraba el formulario al abrirse (solo se aplica la diferencia). */
  originalStock?: number;
  /** Al editar: motivo del cambio de stock. */
  stockNote?: string;
};
export default Item;
