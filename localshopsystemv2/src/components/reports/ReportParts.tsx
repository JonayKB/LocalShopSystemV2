import React from 'react';
import { ArrowDownRight, ArrowUpRight, Minus } from 'lucide-react';

/**
 * Piezas comunes de los informes. Colores por función (validados con el validador de
 * paleta sobre la superficie blanca del panel):
 *  - ingresos = azul, beneficio = naranja (siempre los mismos en todas las gráficas)
 *  - efectivo = aqua, tarjeta = violeta (el aqua lleva siempre etiqueta visible)
 */
export const SERIES = {
  revenue: '#2a78d6',
  profit: '#eb6834',
  cash: '#1baf7a',
  card: '#6250d6',
} as const;

const pct = new Intl.NumberFormat('es-ES', { maximumFractionDigits: 1, minimumFractionDigits: 0 });
export const fmtPct = (n: number | null | undefined): string => (n == null ? '—' : `${pct.format(n)} %`);
const int = new Intl.NumberFormat('es-ES');
export const fmtInt = (n: number | null | undefined): string => int.format(n ?? 0);
const compact = new Intl.NumberFormat('es-ES', { notation: 'compact', maximumFractionDigits: 1 });
/** Eje de euros sin decimales de más: "1,2 mil €". */
export const eurAxis = (n: number | null): string => (n == null ? '' : `${compact.format(n)} €`);

/** Variación respecto al periodo anterior, con icono y texto (nunca solo color). */
export const Delta = ({ now, before, inverse = false }: { now: number; before: number; inverse?: boolean }) => {
  if (!before) {
    return <span className="kpi-delta is-flat">{now ? 'sin datos del periodo anterior' : '—'}</span>;
  }
  const change = ((now - before) / Math.abs(before)) * 100;
  if (Math.abs(change) < 0.05) {
    return (
      <span className="kpi-delta is-flat">
        <Minus size={14} aria-hidden="true" /> igual que el periodo anterior
      </span>
    );
  }
  const up = change > 0;
  const good = inverse ? !up : up;
  return (
    <span className={`kpi-delta ${good ? 'is-good' : 'is-bad'}`}>
      {up ? <ArrowUpRight size={14} aria-hidden="true" /> : <ArrowDownRight size={14} aria-hidden="true" />}
      {up ? '+' : '−'}
      {pct.format(Math.abs(change))} % vs. periodo anterior
    </span>
  );
};

export const Kpi = ({
  label,
  value,
  hint,
  children,
}: {
  label: string;
  value: string;
  hint?: string;
  children?: React.ReactNode;
}) => (
  <div className="stat kpi">
    <small>{label}</small>
    <strong>{value}</strong>
    {children}
    {hint && <span className="kpi-hint">{hint}</span>}
  </div>
);

/** Barra de datos dentro de una celda de tabla (magnitud relativa al máximo). */
export const DataBar = ({ value, max, color = SERIES.revenue }: { value: number; max: number; color?: string }) => (
  <span className="databar" aria-hidden="true">
    <span style={{ width: `${max > 0 ? Math.max(0, (value / max) * 100) : 0}%`, background: color }} />
  </span>
);

export const Section = ({
  title,
  sub,
  actions,
  children,
  className = '',
}: {
  title: string;
  sub?: string;
  actions?: React.ReactNode;
  children: React.ReactNode;
  className?: string;
}) => (
  <section className={`card report-card ${className}`}>
    <header className="report-card-head">
      <div>
        <h2 className="chart-title">{title}</h2>
        {sub && <p className="report-sub">{sub}</p>}
      </div>
      {actions}
    </header>
    {children}
  </section>
);

export const Empty = ({ text = 'No hay ventas en este periodo' }: { text?: string }) => (
  <div className="empty" style={{ border: 'none', minHeight: 160 }}>
    <span>{text}</span>
  </div>
);

/** Control segmentado accesible (role=tablist). */
export function Seg<T extends string>({
  value,
  options,
  onChange,
  label,
}: {
  value: T;
  options: { value: T; label: string }[];
  onChange: (v: T) => void;
  label: string;
}) {
  return (
    <div className="seg seg-sm" role="tablist" aria-label={label}>
      {options.map(o => (
        <button
          key={o.value}
          type="button"
          role="tab"
          aria-selected={value === o.value}
          className={value === o.value ? 'is-active' : ''}
          onClick={() => onChange(o.value)}
        >
          {o.label}
        </button>
      ))}
    </div>
  );
}
