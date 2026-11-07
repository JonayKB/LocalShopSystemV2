import React, { useEffect, useState } from 'react';
import ItemRepository from '../repositories/ItemRepository';
import { Page, PriceChange, StockMovement } from '../models/Stock';
import { eur, formatDateTime } from '../utils/format';
import { MovementsTable } from '../screens/StockScreen';
import { fmtPct, Seg } from './reports/ReportParts';

type Props = { itemId: number; token: string | null };

/** Historial de precios y de movimientos de stock de un producto. */
const ItemHistory = ({ itemId, token }: Props) => {
  const [tab, setTab] = useState<'prices' | 'stock'>('prices');
  const [prices, setPrices] = useState<PriceChange[] | null>(null);
  const [moves, setMoves] = useState<Page<StockMovement> | null>(null);
  const [page, setPage] = useState(0);
  const [error, setError] = useState(false);

  useEffect(() => {
    const repo = new ItemRepository();
    setError(false);
    repo.getPriceHistory(itemId, token).then(setPrices).catch(() => setError(true));
  }, [itemId, token]);

  useEffect(() => {
    new ItemRepository().getStockMovements(itemId, page, token).then(setMoves).catch(() => setError(true));
  }, [itemId, page, token]);

  return (
    <section className="item-history">
      <div className="report-card-head">
        <h3 className="section-title" style={{ margin: 0 }}>Historial</h3>
        <Seg
          label="Historial"
          value={tab}
          onChange={setTab}
          options={[
            { value: 'prices', label: 'Precios' },
            { value: 'stock', label: 'Stock' },
          ]}
        />
      </div>
      {error && <div className="cart-error" role="alert">No se pudo cargar el historial.</div>}
      {tab === 'prices' ? (
        !prices ? (
          <div className="skeleton" style={{ height: 120 }} />
        ) : (
          <div className="table-wrap">
            <table className="table report-table">
              <thead>
                <tr>
                  <th>Desde</th>
                  <th className="num">PVP</th>
                  <th className="num">Coste</th>
                  <th className="num">Margen</th>
                  <th>Motivo</th>
                </tr>
              </thead>
              <tbody>
                {prices.map((p, i) => (
                  <tr key={p.id} className={i === 0 ? 'is-current' : ''}>
                    <td className="nowrap">{formatDateTime(p.validFrom)}</td>
                    <td className="num">{eur(p.price)}</td>
                    <td className="num">{p.cost ? eur(p.cost) : '—'}</td>
                    <td className="num">{fmtPct(p.margin)}</td>
                    <td className="muted">
                      {p.reason ?? ''}
                      {p.changedBy && <span className="sub">{p.changedBy}</span>}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )
      ) : (
        <>
          <MovementsTable page={moves} />
          {moves && moves.totalPages > 1 && (
            <div className="action-bar">
              <button type="button" className="btn btn-sm" disabled={page === 0} onClick={() => setPage(p => p - 1)}>
                Anteriores
              </button>
              <span className="muted">
                {page + 1} / {moves.totalPages}
              </span>
              <button type="button" className="btn btn-sm" disabled={page + 1 >= moves.totalPages} onClick={() => setPage(p => p + 1)}>
                Siguientes
              </button>
            </div>
          )}
        </>
      )}
    </section>
  );
};

export default ItemHistory;
