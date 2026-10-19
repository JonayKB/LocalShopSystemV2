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

/** Texto y tono del estado de envío a la AEAT de una venta. */
export const verifactuInfo = (status?: string | null): { label: string; tone: 'ok' | 'warn' | 'danger'; hint: string } | null => {
    switch (status) {
        case 'ACCEPTED':
            return { label: 'AEAT ✓', tone: 'ok', hint: 'Registro aceptado por la Agencia Tributaria' };
        case 'PENDING':
            return { label: 'AEAT ⏳', tone: 'warn', hint: 'Pendiente de enviar a la Agencia Tributaria (se reintenta solo)' };
        case 'ACCEPTED_WITH_ERRORS':
            return { label: 'AEAT !', tone: 'warn', hint: 'Aceptado con errores: revisa el registro' };
        case 'REJECTED':
            return { label: 'AEAT ✕', tone: 'danger', hint: 'Rechazado por la Agencia Tributaria: hay que corregirlo' };
        default:
            return null;
    }
};
