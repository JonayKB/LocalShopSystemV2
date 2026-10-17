import Trade from '../models/Trade';

const euro = new Intl.NumberFormat('es-ES', { style: 'currency', currency: 'EUR' });

/** 12.5 -> "12,50 €" */
export const eur = (n: number | null | undefined): string => euro.format(n ?? 0);

/** Total de una venta: el congelado en servidor o, en ventas antiguas, la suma de precios. */
export const tradeTotal = (t: Trade): number =>
  t.total != null ? t.total : t.items.reduce((sum, item) => sum + item.price, 0);

/** "T-000123" o, en ventas antiguas, "#45". */
export const tradeLabel = (t: Trade): string => t.invoiceNumber ?? `#${t.id}`;

export const isCancelled = (t: Trade): boolean => t.status === 'CANCELLED';

export const formatDateTime = (iso: string | null | undefined): string =>
  iso
    ? new Date(iso).toLocaleString('es-ES', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      })
    : '';
