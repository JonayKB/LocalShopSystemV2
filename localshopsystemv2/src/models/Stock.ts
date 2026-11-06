export type StockMovementType =
  | 'OPENING_BALANCE'
  | 'SALE'
  | 'SALE_CANCEL'
  | 'RESTOCK'
  | 'SUPPLIER_RETURN'
  | 'LOSS'
  | 'ADJUSTMENT';

/** Tipos que se registran a mano desde la pantalla de stock. */
export type ManualMovementType = 'RESTOCK' | 'SUPPLIER_RETURN' | 'LOSS' | 'ADJUSTMENT';

export type StockMovement = {
  id: number;
  itemId: number;
  itemName: string | null;
  type: StockMovementType;
  typeLabel: string;
  /** Con signo: + entra, - sale. */
  quantity: number;
  stockAfter: number;
  unitCost: number | null;
  tradeId: number | null;
  note: string | null;
  createdBy: string | null;
  createdAt: string;
};

export type PriceChange = {
  id: number;
  price: number;
  cost: number;
  /** Margen (%) sobre el precio sin IGIC; null si no hay coste. */
  margin: number | null;
  validFrom: string;
  changedBy: string | null;
  reason: string | null;
};

export type MovementLine = {
  itemId: number;
  quantity?: number;
  /** Solo en ajustes: el recuento real. */
  countedStock?: number;
  /** Solo en entradas: coste unitario del albarán (actualiza el coste del producto). */
  unitCost?: number;
};

export type Page<T> = { content: T[]; totalElements: number; totalPages: number; number: number; size: number };
