import React, { useState } from 'react'
import { Ban, ChevronRight, Info } from 'lucide-react'
import Trade from '../models/Trade'
import { eur, formatDateTime, isCancelled, tradeLabel, tradeTotal } from '../utils/format'

type Props = {
    trade: Trade,
    onCancel?: (trade: Trade) => void;
}

type Row = { key: string; name: string; qty: number; unit: number; rate: number | null; total: number };

/** Líneas a mostrar: las congeladas de la venta o, en ventas antiguas, las agrupadas por producto. */
const rowsOf = (trade: Trade): Row[] => {
    if (trade.lines && trade.lines.length > 0) {
        return trade.lines.map((l, i) => ({
            key: `${l.itemId ?? 'x'}-${i}`,
            name: l.description,
            qty: l.quantity,
            unit: l.unitPrice,
            rate: l.taxRate,
            total: l.lineTotal,
        }));
    }
    const grouped: Record<string, Row> = {};
    trade.items.forEach(item => {
        const k = String(item.id);
        if (grouped[k]) {
            grouped[k].qty += 1;
            grouped[k].total += item.price;
        } else {
            grouped[k] = { key: k, name: item.name, qty: 1, unit: item.price, rate: null, total: item.price };
        }
    });
    return Object.values(grouped);
};

const TradeComponnet = (props: Props) => {
    const { trade } = props;
    const [open, setOpen] = useState(false);
    const cancelled = isCancelled(trade);
    const rows = rowsOf(trade);

    return (
        <div className={`trade ${open ? 'is-open' : ''} ${cancelled ? 'is-cancelled' : ''}`}>
            <button className="trade-head" onClick={() => setOpen(o => !o)} aria-expanded={open}>
                <ChevronRight className="chev" size={18} />
                <span className="trade-no">{tradeLabel(trade)}</span>
                <span className="trade-date">{formatDateTime(trade.date)}</span>
                <span className="trade-status">
                    {cancelled ? <span className="badge badge-danger">Anulada</span> : <span className="badge badge-ok">Emitida</span>}
                </span>
                <span className="trade-total">{eur(tradeTotal(trade))}</span>
                <span className="trade-act">
                    {!cancelled && (
                        <span
                            role="button"
                            tabIndex={0}
                            className="btn btn-danger btn-sm"
                            aria-label={`Anular venta ${tradeLabel(trade)}`}
                            onClick={(e) => { e.stopPropagation(); props.onCancel?.(trade); }}
                            onKeyDown={(e) => {
                                if (e.key === 'Enter' || e.key === ' ') {
                                    e.preventDefault();
                                    e.stopPropagation();
                                    props.onCancel?.(trade);
                                }
                            }}
                        >
                            <Ban size={14} /> Anular
                        </span>
                    )}
                </span>
            </button>

            <div className="trade-body">
                <div>
                    <div className="trade-detail">
                        {cancelled && (
                            <div className="trade-note">
                                <Info size={16} />
                                <span>
                                    Anulada{trade.cancelledAt ? ` el ${formatDateTime(trade.cancelledAt)}` : ''}
                                    {trade.cancellationReason ? ` — ${trade.cancellationReason}` : ''}. El stock se devolvió y no cuenta en caja ni en informes.
                                </span>
                            </div>
                        )}
                        <div className="table-wrap">
                            <table className="table">
                                <thead>
                                    <tr>
                                        <th>Producto</th>
                                        <th className="num">Cant.</th>
                                        <th className="num">Precio</th>
                                        <th className="num">IGIC</th>
                                        <th className="num">Importe</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {rows.map(r => (
                                        <tr key={r.key}>
                                            <td className="name">{r.name}</td>
                                            <td className="num">{r.qty}</td>
                                            <td className="num">{eur(r.unit)}</td>
                                            <td className="num">{r.rate != null ? `${r.rate}%` : '—'}</td>
                                            <td className="num">{eur(r.total)}</td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                        {trade.total != null && (
                            <div className="trade-sums">
                                <span>Base <strong>{eur(trade.totalBase)}</strong></span>
                                <span>IGIC <strong>{eur(trade.totalTax)}</strong></span>
                                <span>Total <strong>{eur(trade.total)}</strong></span>
                            </div>
                        )}
                        {!cancelled && (
                            <div className="trade-actions">
                                <button className="btn btn-danger btn-sm" onClick={() => props.onCancel?.(trade)}>
                                    <Ban size={14} /> Anular venta
                                </button>
                            </div>
                        )}
                    </div>
                </div>
            </div>
        </div>
    )
}

export default TradeComponnet
