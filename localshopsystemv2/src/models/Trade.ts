import Item from "./Item";

export type TradeLine = {
  itemId: number | null;
  description: string;
  quantity: number;
  unitPrice: number;
  taxRate: number;
  taxBase: number;
  taxAmount: number;
  lineTotal: number;
};

type Trade = {
  id: number;
  items: Item[];
  date: string;
  /** Serie y número correlativo (null en ventas anteriores a Verifactu). */
  series?: string | null;
  number?: number | null;
  /** Número legible, p. ej. "T-000123". */
  invoiceNumber?: string | null;
  status?: 'ISSUED' | 'CANCELLED';
  cancelledAt?: string | null;
  cancellationReason?: string | null;
  lines?: TradeLine[];
  totalBase?: number | null;
  totalTax?: number | null;
  total?: number | null;
};
export default Trade;
