/** Fecha local en formato yyyy-mm-dd (la que entiende la API de informes). */
export const isoDay = (d: Date): string => {
  const p = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`;
};

export const today = (): string => isoDay(new Date());

/** "2025-06-02" -> objeto Date a medianoche local (sin desfases de zona). */
export const parseDay = (s: string): Date => {
  const [y, m, d] = s.split('-').map(Number);
  return new Date(y, m - 1, d);
};

export const daysBetween = (from: string, to: string): number =>
  Math.round((parseDay(to).getTime() - parseDay(from).getTime()) / 86400000) + 1;

const fmtShort = new Intl.DateTimeFormat('es-ES', { day: 'numeric', month: 'short' });
const fmtLong = new Intl.DateTimeFormat('es-ES', { day: 'numeric', month: 'short', year: 'numeric' });
const fmtMonth = new Intl.DateTimeFormat('es-ES', { month: 'short', year: '2-digit' });

/** Etiqueta corta de un día: "2 jun". */
export const shortDay = (s: string): string => fmtShort.format(parseDay(s)).replace('.', '');
export const longDay = (s: string): string => fmtLong.format(parseDay(s)).replace('.', '');
export const monthLabel = (s: string): string => fmtMonth.format(parseDay(s)).replace('.', '');
