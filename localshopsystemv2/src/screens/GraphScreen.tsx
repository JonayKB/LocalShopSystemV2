import React, { useContext, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { AlertTriangle, PackageX } from 'lucide-react';
import { BarChart, LineChart } from '@mui/x-charts';
import { MainContext } from '../components/MainContextProvider';
import PageHeader from '../components/PageHeader';
import GraphRepository, { DateRange } from '../repositories/GraphRepository';
import {
  Bucket,
  CategoryShare,
  Granularity,
  Inventory,
  ItemRank,
  PaymentShare,
  RankMetric,
  Summary,
  TimePoint,
} from '../models/Analytics';
import { eur } from '../utils/format';
import { daysBetween, isoDay, longDay, monthLabel, shortDay } from '../utils/dates';
import { DataBar, Delta, Empty, eurAxis, fmtInt, fmtPct, Kpi, Section, Seg, SERIES } from '../components/reports/ReportParts';

type Preset = 'today' | '7d' | '30d' | 'month' | 'lastMonth' | 'year' | 'custom';

const PRESETS: { value: Preset; label: string }[] = [
  { value: 'today', label: 'Hoy' },
  { value: '7d', label: '7 días' },
  { value: '30d', label: '30 días' },
  { value: 'month', label: 'Este mes' },
  { value: 'lastMonth', label: 'Mes pasado' },
  { value: 'year', label: 'Este año' },
  { value: 'custom', label: 'Fechas…' },
];

function rangeOf(p: Preset, custom: DateRange): DateRange {
  const now = new Date();
  const y = now.getFullYear();
  const m = now.getMonth();
  const back = (days: number) => isoDay(new Date(y, m, now.getDate() - days));
  switch (p) {
    case 'today':
      return { from: isoDay(now), to: isoDay(now) };
    case '7d':
      return { from: back(6), to: isoDay(now) };
    case '30d':
      return { from: back(29), to: isoDay(now) };
    case 'month':
      return { from: isoDay(new Date(y, m, 1)), to: isoDay(now) };
    case 'lastMonth':
      return { from: isoDay(new Date(y, m - 1, 1)), to: isoDay(new Date(y, m, 0)) };
    case 'year':
      return { from: isoDay(new Date(y, 0, 1)), to: isoDay(now) };
    default:
      return custom;
  }
}

/** Agrupación automática: días hasta ~6 semanas, semanas hasta ~13 meses, si no meses. */
const autoGranularity = (r: DateRange): Granularity => {
  const d = daysBetween(r.from, r.to);
  return d <= 45 ? 'day' : d <= 400 ? 'week' : 'month';
};

const periodLabel = (s: string, g: Granularity) =>
  g === 'month' ? monthLabel(s) : g === 'week' ? `sem. ${shortDay(s)}` : shortDay(s);

const chartSx = {
  '& .MuiChartsAxis-line, & .MuiChartsAxis-tick': { stroke: '#cbc7bb' },
  '& .MuiChartsAxis-tickLabel': { fill: '#5b665f' },
  '& .MuiChartsGrid-line': { stroke: '#efede6' },
};

type Data = {
  summary: Summary;
  series: TimePoint[];
  categories: CategoryShare[];
  payments: PaymentShare[];
  hours: Bucket[];
  weekdays: Bucket[];
};

const GraphScreen = () => {
  const { token } = useContext(MainContext);
  const navigate = useNavigate();
  const repo = useMemo(() => new GraphRepository(), []);

  const [preset, setPreset] = useState<Preset>('30d');
  const [custom, setCustom] = useState<DateRange>(() => rangeOf('30d', { from: '', to: '' }));
  const range = useMemo(() => rangeOf(preset, custom), [preset, custom]);
  const [granularity, setGranularity] = useState<Granularity>(() => autoGranularity(range));
  const [metric, setMetric] = useState<RankMetric>('revenue');
  const [limit, setLimit] = useState(10);

  const [data, setData] = useState<Data | null>(null);
  const [ranking, setRanking] = useState<ItemRank[] | null>(null);
  const [inventory, setInventory] = useState<Inventory | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!token) navigate('/admin/login');
  }, [token, navigate]);

  useEffect(() => setGranularity(autoGranularity(range)), [range]);

  const validRange = range.from && range.to && range.from <= range.to;

  useEffect(() => {
    if (!token || !validRange) return;
    let alive = true;
    setLoading(true);
    setError(null);
    Promise.all([
      repo.summary(range, token),
      repo.salesOverTime(range, granularity, token),
      repo.byCategory(range, token),
      repo.byPaymentMethod(range, token),
      repo.byHour(range, token),
      repo.byWeekday(range, token),
    ])
      .then(([summary, series, categories, payments, hours, weekdays]) => {
        if (alive) setData({ summary, series, categories, payments, hours, weekdays });
      })
      .catch(e => alive && setError(typeof e?.response?.data === 'string' ? e.response.data : 'No se pudieron cargar los informes.'))
      .finally(() => alive && setLoading(false));
    return () => {
      alive = false;
    };
  }, [token, range, granularity, validRange, repo]);

  useEffect(() => {
    if (!token || !validRange) return;
    let alive = true;
    repo.topItems(range, metric, limit, token).then(r => alive && setRanking(r)).catch(() => alive && setRanking([]));
    return () => {
      alive = false;
    };
  }, [token, range, metric, limit, validRange, repo]);

  useEffect(() => {
    if (!token) return;
    repo.inventory(token).then(setInventory).catch(() => setInventory(null));
  }, [token, repo]);

  const cur = data?.summary.current;
  const prev = data?.summary.previous;
  const hasSales = !!cur && cur.trades > 0;

  return (
    <>
      <PageHeader
        eyebrow="Análisis"
        title="Informes"
        sub="Ventas, beneficio e inventario. Se calculan con el precio, el IGIC y el coste de cada venta en su momento; las anuladas no cuentan."
      />

      <div className="toolbar report-toolbar">
        <Seg label="Periodo" value={preset} options={PRESETS} onChange={setPreset} />
        {preset === 'custom' && (
          <div className="range-inputs">
            <label>
              Desde
              <input
                className="input"
                type="date"
                value={custom.from}
                max={custom.to || undefined}
                onChange={e => setCustom(c => ({ ...c, from: e.target.value }))}
              />
            </label>
            <label>
              Hasta
              <input
                className="input"
                type="date"
                value={custom.to}
                min={custom.from || undefined}
                onChange={e => setCustom(c => ({ ...c, to: e.target.value }))}
              />
            </label>
          </div>
        )}
        {validRange && (
          <span className="range-label" aria-live="polite">
            {range.from === range.to ? longDay(range.from) : `${longDay(range.from)} – ${longDay(range.to)}`}
          </span>
        )}
      </div>

      {!validRange && <div className="cart-error" role="alert">Elige una fecha de inicio anterior a la de fin.</div>}
      {error && <div className="cart-error" role="alert">{error}</div>}

      {/* ------------------------------------------------ KPIs */}
      {!cur || !prev ? (
        <div className="skeleton" style={{ height: 140 }} aria-busy="true" />
      ) : (
        <div className={`stat-grid kpi-grid ${loading ? 'is-loading' : ''}`}>
          <Kpi label="Ventas (IGIC incl.)" value={eur(cur.revenue)}>
            <Delta now={cur.revenue} before={prev.revenue} />
          </Kpi>
          <Kpi
            label="Beneficio bruto"
            value={eur(cur.profit)}
            hint={cur.marginPct != null ? `Margen ${fmtPct(cur.marginPct)} sobre base` : 'Sin costes registrados'}
          >
            <Delta now={cur.profit} before={prev.profit} />
          </Kpi>
          <Kpi label="Tickets" value={fmtInt(cur.trades)} hint={`${fmtInt(cur.units)} unidades · ${cur.avgUnitsPerTrade} por ticket`}>
            <Delta now={cur.trades} before={prev.trades} />
          </Kpi>
          <Kpi label="Ticket medio" value={eur(cur.avgTicket)}>
            <Delta now={cur.avgTicket} before={prev.avgTicket} />
          </Kpi>
          <Kpi label="Base imponible" value={eur(cur.taxBase)} hint={`IGIC repercutido ${eur(cur.tax)}`} />
          <Kpi
            label="Anuladas"
            value={fmtInt(cur.cancelledTrades)}
            hint={cur.cancelledTrades ? `${eur(cur.cancelledAmount)} no cobrados` : 'Ninguna en el periodo'}
          />
        </div>
      )}
      {hasSales && cur!.costCoverage < 100 && (
        <p className="report-note" role="note">
          <AlertTriangle size={15} aria-hidden="true" /> El beneficio solo incluye el {fmtPct(cur!.costCoverage)} de las
          ventas: hay productos sin coste. Rellena el coste en su ficha (o en la entrada de mercancía) para que cuenten.
        </p>
      )}

      {/* ------------------------------------------------ Evolución */}
      <Section
        title="Evolución de ingresos y beneficio"
        sub="Mismo eje en euros para ambas series."
        actions={
          <Seg
            label="Agrupar por"
            value={granularity}
            onChange={setGranularity}
            options={[
              { value: 'day', label: 'Días' },
              { value: 'week', label: 'Semanas' },
              { value: 'month', label: 'Meses' },
            ]}
          />
        }
      >
        {!data ? (
          <div className="skeleton" style={{ height: 300 }} />
        ) : !hasSales ? (
          <Empty />
        ) : (
          <LineChart
            height={320}
            margin={{ left: 8, right: 16, top: 16, bottom: 8 }}
            grid={{ horizontal: true }}
            xAxis={[
              {
                scaleType: 'point',
                data: data.series.map(p => periodLabel(p.period, granularity)),
                tickLabelStyle: { fontSize: 12 },
              },
            ]}
            yAxis={[{ valueFormatter: eurAxis, width: 64, tickLabelStyle: { fontSize: 12 } }]}
            series={[
              {
                label: 'Ingresos',
                data: data.series.map(p => p.revenue),
                color: SERIES.revenue,
                showMark: data.series.length <= 31,
                valueFormatter: v => eur(v),
              },
              {
                label: 'Beneficio bruto',
                data: data.series.map(p => p.profit),
                color: SERIES.profit,
                showMark: data.series.length <= 31,
                valueFormatter: v => eur(v),
              },
            ]}
            sx={{ ...chartSx, '& .MuiLineElement-root': { strokeWidth: 2 } }}
          />
        )}
      </Section>

      <div className="report-grid">
        {/* ------------------------------------------------ Productos */}
        <Section
          className="span-2"
          title="Productos"
          sub="Beneficio = base imponible − coste. «—» = producto sin coste registrado."
          actions={
            <div className="report-actions">
              <Seg
                label="Ordenar por"
                value={metric}
                onChange={setMetric}
                options={[
                  { value: 'revenue', label: 'Ingresos' },
                  { value: 'units', label: 'Unidades' },
                  { value: 'profit', label: 'Beneficio' },
                ]}
              />
              <select
                className="select select-sm"
                aria-label="Número de productos"
                value={limit}
                onChange={e => setLimit(Number(e.target.value))}
              >
                {[10, 25, 50, 100].map(n => (
                  <option key={n} value={n}>
                    Top {n}
                  </option>
                ))}
              </select>
            </div>
          }
        >
          {!ranking ? (
            <div className="skeleton" style={{ height: 240 }} />
          ) : ranking.length === 0 ? (
            <Empty />
          ) : (
            <RankingTable rows={ranking} metric={metric} />
          )}
        </Section>

        {/* ------------------------------------------------ Categorías */}
        <Section title="Por categoría" sub="Categoría del producto en el momento de la venta.">
          {!data ? (
            <div className="skeleton" style={{ height: 200 }} />
          ) : data.categories.length === 0 ? (
            <Empty />
          ) : (
            <CategoryTable rows={data.categories} />
          )}
        </Section>

        {/* ------------------------------------------------ Forma de pago */}
        <Section title="Forma de pago">
          {!data ? <div className="skeleton" style={{ height: 120 }} /> : !hasSales ? <Empty /> : <PaymentSplit rows={data.payments} />}
        </Section>

        {/* ------------------------------------------------ Hora y día */}
        <Section title="Ingresos por hora" sub="Hora de Canarias.">
          {!data ? <div className="skeleton" style={{ height: 240 }} /> : !hasSales ? <Empty /> : <BucketChart rows={trimHours(data.hours)} />}
        </Section>
        <Section title="Ingresos por día de la semana">
          {!data ? (
            <div className="skeleton" style={{ height: 240 }} />
          ) : !hasSales ? (
            <Empty />
          ) : (
            <BucketChart rows={data.weekdays.map(w => ({ ...w, label: w.label.slice(0, 3) }))} />
          )}
        </Section>
      </div>

      {/* ------------------------------------------------ Inventario */}
      <h2 className="section-title report-section-title">Inventario actual</h2>
      {!inventory ? (
        <div className="skeleton" style={{ height: 140 }} />
      ) : (
        <InventoryBlock inv={inventory} />
      )}
    </>
  );
};

/* ================================================================== partes */

const RankingTable = ({ rows, metric }: { rows: ItemRank[]; metric: RankMetric }) => {
  const valueOf = (r: ItemRank) => (metric === 'units' ? r.units : metric === 'profit' ? r.profit ?? 0 : r.revenue);
  const max = Math.max(...rows.map(valueOf), 0);
  return (
    <div className="table-wrap">
      <table className="table report-table">
        <thead>
          <tr>
            <th className="num">#</th>
            <th>Producto</th>
            <th className="num">Uds.</th>
            <th className="num">Ingresos</th>
            <th className="num">Beneficio</th>
            <th className="num">Margen</th>
            <th className="bar-col" aria-hidden="true" />
          </tr>
        </thead>
        <tbody>
          {rows.map((r, i) => (
            <tr key={`${r.itemId ?? r.name}`}>
              <td className="num muted">{i + 1}</td>
              <td className="name">
                {r.name}
                <span className="sub">{r.category}</span>
              </td>
              <td className="num">{fmtInt(r.units)}</td>
              <td className="num">{eur(r.revenue)}</td>
              <td className="num">{r.profit == null ? '—' : eur(r.profit)}</td>
              <td className="num">{fmtPct(r.marginPct)}</td>
              <td className="bar-col">
                <DataBar value={valueOf(r)} max={max} color={metric === 'profit' ? SERIES.profit : SERIES.revenue} />
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

const CategoryTable = ({ rows }: { rows: CategoryShare[] }) => {
  const max = Math.max(...rows.map(r => r.revenue), 0);
  return (
    <div className="table-wrap">
      <table className="table report-table">
        <thead>
          <tr>
            <th>Categoría</th>
            <th className="num">Ingresos</th>
            <th className="num">%</th>
            <th className="num">Beneficio</th>
            <th className="bar-col" aria-hidden="true" />
          </tr>
        </thead>
        <tbody>
          {rows.map(r => (
            <tr key={r.category}>
              <td className="name">
                {r.category}
                <span className="sub">{fmtInt(r.units)} uds.</span>
              </td>
              <td className="num">{eur(r.revenue)}</td>
              <td className="num">{fmtPct(r.sharePct)}</td>
              <td className="num">{r.profit == null ? '—' : eur(r.profit)}</td>
              <td className="bar-col">
                <DataBar value={r.revenue} max={max} />
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

/** Barra partida efectivo / tarjeta con etiquetas visibles (el color no va solo). */
const PaymentSplit = ({ rows }: { rows: PaymentShare[] }) => (
  <div className="split">
    <div className="split-bar" role="img" aria-label={rows.map(r => `${r.label} ${fmtPct(r.sharePct)}`).join(', ')}>
      {rows
        .filter(r => r.revenue > 0)
        .map(r => (
          <span
            key={r.method}
            style={{ flexGrow: r.revenue, background: r.method === 'CARD' ? SERIES.card : SERIES.cash }}
            title={`${r.label}: ${eur(r.revenue)}`}
          />
        ))}
    </div>
    <ul className="split-legend">
      {rows.map(r => (
        <li key={r.method}>
          <span className="swatch" style={{ background: r.method === 'CARD' ? SERIES.card : SERIES.cash }} aria-hidden="true" />
          <strong>{r.label}</strong>
          <span>{eur(r.revenue)}</span>
          <span className="muted">
            {fmtPct(r.sharePct)} · {fmtInt(r.trades)} {r.trades === 1 ? 'ticket' : 'tickets'}
          </span>
        </li>
      ))}
    </ul>
  </div>
);

/** Solo las horas entre la primera y la última con ventas (no 24 barras vacías). */
const trimHours = (hours: Bucket[]): Bucket[] => {
  const used = hours.filter(h => h.trades > 0).map(h => h.bucket);
  if (used.length === 0) return hours;
  const lo = Math.min(...used);
  const hi = Math.max(...used);
  return hours.filter(h => h.bucket >= lo && h.bucket <= hi).map(h => ({ ...h, label: h.label.slice(0, 2) + 'h' }));
};

const BucketChart = ({ rows }: { rows: Bucket[] }) => (
  <BarChart
    height={240}
    margin={{ left: 8, right: 8, top: 8, bottom: 4 }}
    borderRadius={4}
    grid={{ horizontal: true }}
    hideLegend
    xAxis={[{ scaleType: 'band', data: rows.map(r => r.label), tickLabelStyle: { fontSize: 12 }, categoryGapRatio: 0.35 }]}
    yAxis={[{ valueFormatter: eurAxis, width: 56, tickLabelStyle: { fontSize: 12 } }]}
    series={[
      {
        label: 'Ingresos',
        data: rows.map(r => r.revenue),
        color: SERIES.revenue,
        valueFormatter: (v, { dataIndex }) =>
          `${eur(v)} · ${fmtInt(rows[dataIndex].trades)} tickets · medio ${eur(rows[dataIndex].avgTicket)}`,
      },
    ]}
    sx={chartSx}
  />
);

const InventoryBlock = ({ inv }: { inv: Inventory }) => {
  const maxCat = Math.max(...inv.byCategory.map(c => c.valueAtCost), 0);
  return (
    <>
      <div className="stat-grid kpi-grid">
        <Kpi label="Valor a coste" value={eur(inv.valueAtCost)} hint={`${fmtInt(inv.units)} unidades en ${fmtInt(inv.items - inv.ignoringStock)} productos`} />
        <Kpi label="Valor a precio de venta" value={eur(inv.valueAtPrice)} hint="IGIC incluido" />
        <Kpi label="Beneficio potencial" value={eur(inv.potentialProfit)} hint="Si se vende todo al precio actual" />
        <Kpi label="Agotados" value={fmtInt(inv.outOfStock)} hint={`${fmtInt(inv.belowMinimum)} bajo el mínimo`} />
        <Kpi
          label="Sin coste"
          value={fmtInt(inv.itemsWithoutCost)}
          hint={inv.itemsWithoutCost ? 'Su beneficio no se puede calcular' : 'Todos tienen coste'}
        />
      </div>

      <div className="report-grid">
        <Section title="Valor por categoría" sub="Stock × coste actual (stock negativo cuenta como 0).">
          {inv.byCategory.length === 0 ? (
            <Empty text="No hay productos con stock" />
          ) : (
            <div className="table-wrap">
              <table className="table report-table">
                <thead>
                  <tr>
                    <th>Categoría</th>
                    <th className="num">Uds.</th>
                    <th className="num">A coste</th>
                    <th className="num">A PVP</th>
                    <th className="bar-col" aria-hidden="true" />
                  </tr>
                </thead>
                <tbody>
                  {inv.byCategory.map(c => (
                    <tr key={c.category}>
                      <td className="name">
                        {c.category}
                        <span className="sub">{fmtInt(c.items)} productos</span>
                      </td>
                      <td className="num">{fmtInt(c.units)}</td>
                      <td className="num">{eur(c.valueAtCost)}</td>
                      <td className="num">{eur(c.valueAtPrice)}</td>
                      <td className="bar-col">
                        <DataBar value={c.valueAtCost} max={maxCat} />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </Section>

        <Section title="Reponer" sub="Agotados primero, después los que están bajo el mínimo.">
          {inv.alerts.length === 0 ? (
            <Empty text="Todo el stock está por encima del mínimo" />
          ) : (
            <div className="table-wrap">
              <table className="table report-table">
                <thead>
                  <tr>
                    <th>Producto</th>
                    <th className="num">Stock</th>
                    <th className="num">Mínimo</th>
                  </tr>
                </thead>
                <tbody>
                  {inv.alerts.map(a => (
                    <tr key={a.itemId}>
                      <td className="name">
                        {a.stock <= 0 ? (
                          <span className="badge badge-danger">
                            <PackageX size={12} aria-hidden="true" /> Agotado
                          </span>
                        ) : (
                          <span className="badge badge-warn">Bajo mínimo</span>
                        )}{' '}
                        {a.name}
                        <span className="sub">{a.category}</span>
                      </td>
                      <td className="num">{fmtInt(a.stock)}</td>
                      <td className="num">{fmtInt(a.bareMinimun)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </Section>
      </div>
    </>
  );
};

export default GraphScreen;
