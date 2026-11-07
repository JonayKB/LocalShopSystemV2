import React, { useCallback, useContext, useEffect, useState } from 'react';
import { Boxes, Minus, Plus, Trash2 } from 'lucide-react';
import { MainContext } from '../components/MainContextProvider';
import { useNavigate } from 'react-router-dom';
import ItemSearcher from '../components/ItemSearcher';
import Item from '../models/Item';
import BarcodeListener from '../components/BarcodeListener';
import ItemRepository from '../repositories/ItemRepository';
import StockRepository from '../repositories/StockRepository';
import PageHeader from '../components/PageHeader';
import { ManualMovementType, MovementLine, Page, StockMovement } from '../models/Stock';
import { eur, formatDateTime } from '../utils/format';
import { Seg } from '../components/reports/ReportParts';

const TYPES: { value: ManualMovementType; label: string; hint: string; action: string }[] = [
  { value: 'RESTOCK', label: 'Entrada', hint: 'Mercancía que llega del proveedor. Si indicas el coste del albarán, se actualiza el coste del producto.', action: 'Registrar entrada' },
  { value: 'SUPPLIER_RETURN', label: 'Devolución', hint: 'Productos que devuelves al proveedor. Se descarga el albarán de devolución en Excel.', action: 'Registrar devolución' },
  { value: 'LOSS', label: 'Merma', hint: 'Roturas, caducados, robos o consumo propio: salen del stock sin venderse.', action: 'Registrar merma' },
  { value: 'ADJUSTMENT', label: 'Recuento', hint: 'Escribe las unidades que hay de verdad en la tienda: se ajusta la diferencia.', action: 'Guardar recuento' },
];

type Row = { item: Item; quantity: number; counted: number; unitCost: string };

const FILTERS = [
  { value: '', label: 'Todos' },
  { value: 'SALE', label: 'Ventas' },
  { value: 'RESTOCK', label: 'Entradas' },
  { value: 'SUPPLIER_RETURN', label: 'Devoluciones' },
  { value: 'LOSS', label: 'Mermas' },
  { value: 'ADJUSTMENT', label: 'Ajustes' },
];

const StockScreen = () => {
  const { token } = useContext(MainContext);
  const navigate = useNavigate();
  const itemRepository = new ItemRepository();
  const stockRepository = new StockRepository();

  const [type, setType] = useState<ManualMovementType>('RESTOCK');
  const [rows, setRows] = useState<Row[]>([]);
  const [note, setNote] = useState('');
  const [busy, setBusy] = useState(false);
  const [msg, setMsg] = useState<{ ok: boolean; text: string } | null>(null);

  const [filter, setFilter] = useState('');
  const [page, setPage] = useState(0);
  const [ledger, setLedger] = useState<Page<StockMovement> | null>(null);

  useEffect(() => {
    if (!token) navigate('/admin/login');
  }, [token, navigate]);

  const loadLedger = useCallback(() => {
    if (!token) return;
    new StockRepository()
      .getMovements({ type: filter, page, size: 20 }, token)
      .then(setLedger)
      .catch(() => setLedger(null));
  }, [token, filter, page]);

  useEffect(loadLedger, [loadLedger]);

  const current = TYPES.find(t => t.value === type)!;

  function addItem(item: Item) {
    setMsg(null);
    setRows(prev => {
      const existing = prev.find(r => r.item.id === item.id);
      if (existing) {
        return prev.map(r => (r.item.id === item.id ? { ...r, quantity: r.quantity + 1 } : r));
      }
      return [...prev, { item, quantity: 1, counted: Math.max(0, item.stock ?? 0), unitCost: '' }];
    });
  }

  const update = (id: number, patch: Partial<Row>) =>
    setRows(prev => prev.map(r => (r.item.id === id ? { ...r, ...patch } : r)));
  const remove = (id: number) => setRows(prev => prev.filter(r => r.item.id !== id));

  const handleBarcodeScan = async (code: string) => {
    if (!token) return;
    try {
      const item = await itemRepository.getItemById(code, token);
      if (item) addItem(item);
      else setMsg({ ok: false, text: `El código ${code} no está registrado. Dalo de alta antes.` });
    } catch {
      setMsg({ ok: false, text: `El código ${code} no está registrado. Dalo de alta antes.` });
    }
  };

  async function submit() {
    if (rows.length === 0) return;
    setBusy(true);
    setMsg(null);
    try {
      if (type === 'SUPPLIER_RETURN') {
        const map = new Map<Item, number>(rows.map(r => [r.item, r.quantity]));
        const blob = await stockRepository.removeStock(map, token, note);
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `devolucion-${new Date().toISOString().slice(0, 10)}.xlsx`;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        URL.revokeObjectURL(url);
      } else {
        const lines: MovementLine[] = rows.map(r => {
          if (type === 'ADJUSTMENT') return { itemId: r.item.id, countedStock: r.counted };
          const cost = r.unitCost.trim() === '' ? undefined : Number(r.unitCost.replace(',', '.'));
          return { itemId: r.item.id, quantity: r.quantity, unitCost: type === 'RESTOCK' ? cost : undefined };
        });
        const done = await stockRepository.registerMovements(type, lines, note, token);
        if (type === 'ADJUSTMENT' && done.length === 0) {
          setMsg({ ok: true, text: 'El recuento coincide con el stock: no había nada que ajustar.' });
        }
      }
      setMsg(m => m ?? { ok: true, text: `${current.label} registrada correctamente.` });
      setRows([]);
      setNote('');
      setPage(0);
      loadLedger();
    } catch (e: any) {
      let text = 'No se pudo registrar el movimiento.';
      const data = e?.response?.data;
      if (typeof data === 'string' && data) text = data;
      else if (data instanceof Blob) text = (await data.text()) || text;
      setMsg({ ok: false, text });
    } finally {
      setBusy(false);
    }
  }

  return (
    <>
      <PageHeader
        eyebrow="Gestión"
        title="Stock"
        sub="Cada entrada, devolución, merma o recuento queda registrada con fecha, usuario y motivo."
      />
      <BarcodeListener handleBarcodeScan={handleBarcodeScan} />

      <div className="toolbar">
        <Seg label="Tipo de movimiento" value={type} options={TYPES} onChange={t => { setType(t); setMsg(null); }} />
      </div>
      <p className="report-sub" style={{ marginTop: -8, marginBottom: 16 }}>{current.hint}</p>

      <ItemSearcher onItemClickProp={addItem} />

      <section className="card card-pad" style={{ marginTop: 28 }}>
        <h2 className="section-title">Movimiento actual</h2>
        {msg && (
          <div className={msg.ok ? 'print-msg is-ok' : 'cart-error'} role={msg.ok ? 'status' : 'alert'}>
            {msg.text}
          </div>
        )}
        {rows.length === 0 ? (
          <div className="empty">
            <Boxes size={34} strokeWidth={1.4} />
            <strong>Aún no has añadido productos</strong>
            <span>Escanea o busca un producto para empezar.</span>
          </div>
        ) : (
          <>
            <div className="table-wrap">
              <table className="table">
                <thead>
                  <tr>
                    <th>Producto</th>
                    <th className="num">Stock actual</th>
                    <th className="num">{type === 'ADJUSTMENT' ? 'Hay de verdad' : 'Cantidad'}</th>
                    {type === 'ADJUSTMENT' && <th className="num">Diferencia</th>}
                    {type === 'RESTOCK' && <th className="num">Coste unitario</th>}
                    <th />
                  </tr>
                </thead>
                <tbody>
                  {rows.map(r => {
                    const diff = r.counted - (r.item.stock ?? 0);
                    return (
                      <tr key={r.item.id}>
                        <td className="name">{r.item.name}</td>
                        <td className="num">{r.item.ignoreStock ? '—' : r.item.stock}</td>
                        <td className="num">
                          {type === 'ADJUSTMENT' ? (
                            <input
                              className="input input-num"
                              type="number"
                              min={0}
                              value={r.counted}
                              aria-label={`Unidades reales de ${r.item.name}`}
                              onChange={e => update(r.item.id, { counted: Math.max(0, parseInt(e.target.value) || 0) })}
                            />
                          ) : (
                            <div className="qty">
                              <button onClick={() => (r.quantity > 1 ? update(r.item.id, { quantity: r.quantity - 1 }) : remove(r.item.id))} aria-label="Quitar uno"><Minus size={14} /></button>
                              <input
                                type="number"
                                min={1}
                                value={r.quantity}
                                aria-label={`Cantidad de ${r.item.name}`}
                                onChange={e => update(r.item.id, { quantity: Math.max(1, parseInt(e.target.value) || 1) })}
                              />
                              <button onClick={() => update(r.item.id, { quantity: r.quantity + 1 })} aria-label="Añadir uno"><Plus size={14} /></button>
                            </div>
                          )}
                        </td>
                        {type === 'ADJUSTMENT' && (
                          <td className={`num ${diff < 0 ? 'neg' : diff > 0 ? 'pos' : 'muted'}`}>{diff > 0 ? `+${diff}` : diff}</td>
                        )}
                        {type === 'RESTOCK' && (
                          <td className="num">
                            <input
                              className="input input-num"
                              type="number"
                              min={0}
                              step="0.01"
                              placeholder={r.item.net ? String(r.item.net) : 'opcional'}
                              value={r.unitCost}
                              aria-label={`Coste unitario de ${r.item.name}`}
                              onChange={e => update(r.item.id, { unitCost: e.target.value })}
                            />
                          </td>
                        )}
                        <td className="num">
                          <button className="btn-icon" onClick={() => remove(r.item.id)} aria-label={`Quitar ${r.item.name}`}>
                            <Trash2 size={17} />
                          </button>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
            <div className="field" style={{ marginTop: 16 }}>
              <label htmlFor="stock-note">Nota (opcional)</label>
              <input
                id="stock-note"
                className="input"
                maxLength={255}
                placeholder={type === 'RESTOCK' ? 'Nº de albarán, proveedor…' : type === 'LOSS' ? 'Motivo: rotura, caducado…' : 'Motivo'}
                value={note}
                onChange={e => setNote(e.target.value)}
              />
            </div>
            <div className="action-bar">
              <button className="btn" type="button" onClick={() => setRows([])} disabled={busy}>
                Vaciar
              </button>
              <button className={`btn ${type === 'RESTOCK' || type === 'ADJUSTMENT' ? 'btn-primary' : 'btn-danger'}`} onClick={submit} type="button" disabled={busy}>
                {busy ? 'Guardando…' : current.action}
              </button>
            </div>
          </>
        )}
      </section>

      <section className="card card-pad" style={{ marginTop: 28 }}>
        <div className="report-card-head">
          <h2 className="section-title" style={{ margin: 0 }}>Últimos movimientos</h2>
          <select className="select select-sm" aria-label="Filtrar movimientos" value={filter} onChange={e => { setFilter(e.target.value); setPage(0); }}>
            {FILTERS.map(f => <option key={f.value} value={f.value}>{f.label}</option>)}
          </select>
        </div>
        <MovementsTable page={ledger} showItem />
        {ledger && ledger.totalPages > 1 && (
          <div className="action-bar">
            <button className="btn btn-sm" disabled={page === 0} onClick={() => setPage(p => p - 1)}>Anteriores</button>
            <span className="muted">Página {page + 1} de {ledger.totalPages}</span>
            <button className="btn btn-sm" disabled={page + 1 >= ledger.totalPages} onClick={() => setPage(p => p + 1)}>Siguientes</button>
          </div>
        )}
      </section>
    </>
  );
};

/** Tabla de movimientos (también la usa la ficha de producto). */
export const MovementsTable = ({ page, showItem = false }: { page: Page<StockMovement> | null; showItem?: boolean }) => {
  if (!page) return <div className="skeleton" style={{ height: 160 }} />;
  if (page.content.length === 0) return <div className="empty" style={{ border: 'none' }}><span>Sin movimientos</span></div>;
  return (
    <div className="table-wrap">
      <table className="table report-table">
        <thead>
          <tr>
            <th>Fecha</th>
            {showItem && <th>Producto</th>}
            <th>Tipo</th>
            <th className="num">Cantidad</th>
            <th className="num">Stock después</th>
            <th className="num">Coste ud.</th>
            <th>Nota</th>
          </tr>
        </thead>
        <tbody>
          {page.content.map(m => (
            <tr key={m.id}>
              <td className="nowrap">{formatDateTime(m.createdAt)}</td>
              {showItem && <td className="name">{m.itemName ?? m.itemId}</td>}
              <td>{m.typeLabel}</td>
              <td className={`num ${m.quantity < 0 ? 'neg' : 'pos'}`}>{m.quantity > 0 ? `+${m.quantity}` : m.quantity}</td>
              <td className="num">{m.stockAfter}</td>
              <td className="num">{m.unitCost != null ? eur(m.unitCost) : '—'}</td>
              <td className="muted">
                {m.note ?? ''}
                {m.createdBy && <span className="sub">{m.createdBy}</span>}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

export default StockScreen;
