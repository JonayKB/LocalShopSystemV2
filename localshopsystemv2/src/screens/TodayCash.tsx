import React, { useContext, useEffect, useState } from 'react'
import { MainContext } from '../components/MainContextProvider';
import { useNavigate } from 'react-router-dom';
import GraphRepository from '../repositories/GraphRepository';
import { CategoryShare, Summary } from '../models/Analytics';
import { today } from '../utils/dates';
import TradeRepository from '../repositories/TradeRepository';
import { Printer, LayoutList } from 'lucide-react';
import PageHeader from '../components/PageHeader';
import { eur } from '../utils/format';

type Props = {}

const TodayCash = (props: Props) => {
    const { token } = useContext(MainContext);
    const navitigation = useNavigate();
    const [summary, setSummary] = useState<Summary>();
    const [categories, setCategories] = useState<CategoryShare[]>();
    const [error, setError] = useState<string | null>(null);
    const [printing, setPrinting] = useState<'products' | 'categories' | null>(null);
    const [printMsg, setPrintMsg] = useState<{ ok: boolean; text: string } | null>(null);

    async function printDaily(mode: 'products' | 'categories') {
        setPrinting(mode);
        setPrintMsg(null);
        try {
            await new TradeRepository().printDaily(mode, token);
            setPrintMsg({ ok: true, text: 'Enviado a la impresora.' });
        } catch (e: any) {
            const detail = typeof e?.response?.data === 'string' ? e.response.data : '';
            setPrintMsg({ ok: false, text: detail || 'No se pudo imprimir. Revisa que la impresora esté encendida y conectada.' });
        } finally {
            setPrinting(null);
        }
    }

    useEffect(() => {
        if (!token) {
            navitigation('/admin/login');
            return;
        }
        // Mismos cálculos que los informes: líneas congeladas de las ventas emitidas hoy.
        const range = { from: today(), to: today() };
        const graphs = new GraphRepository();
        Promise.all([graphs.summary(range, token), graphs.byCategory(range, token)])
            .then(([s, c]) => {
                setSummary(s);
                setCategories(c);
            })
            .catch(() => setError('No se pudo cargar la caja del día.'));
    }, [token, navitigation]);

    const t = summary?.current;

    return (
        <>
            <PageHeader
                eyebrow="Día a día"
                title="Caja del día"
                sub="Ingresos de hoy por forma de pago y por categoría. Las ventas anuladas no se incluyen."
                actions={
                    <>
                        <button type="button" className="btn" disabled={printing !== null}
                            onClick={() => printDaily('products')}>
                            <Printer size={16} aria-hidden="true" />
                            {printing === 'products' ? 'Imprimiendo…' : 'Imprimir productos'}
                        </button>
                        <button type="button" className="btn" disabled={printing !== null}
                            onClick={() => printDaily('categories')}>
                            <LayoutList size={16} aria-hidden="true" />
                            {printing === 'categories' ? 'Imprimiendo…' : 'Imprimir por categoría'}
                        </button>
                    </>
                }
            />
            {printMsg && (
                <div className={printMsg.ok ? 'print-msg is-ok' : 'cart-error'} role={printMsg.ok ? 'status' : 'alert'}>
                    {printMsg.text}
                </div>
            )}
            {error ? (
                <div className="cart-error" role="alert">{error}</div>
            ) : !t || !categories ? (
                <div className="skeleton" style={{ height: 200 }} aria-busy="true" />
            ) : (
                <>
                <div className="stat-grid">
                    <div className="stat is-total">
                        <small>Total de hoy · {t.trades} {t.trades === 1 ? 'venta' : 'ventas'}</small>
                        <strong>{eur(t.revenue)}</strong>
                    </div>
                    <div className="stat">
                        <small>Efectivo</small>
                        <strong>{eur(t.cashRevenue)}</strong>
                    </div>
                    <div className="stat">
                        <small>Tarjeta</small>
                        <strong>{eur(t.cardRevenue)}</strong>
                    </div>
                    <div className="stat">
                        <small>IGIC repercutido</small>
                        <strong>{eur(t.tax)}</strong>
                    </div>
                    {t.cancelledTrades > 0 && (
                        <div className="stat">
                            <small>Anuladas ({t.cancelledTrades})</small>
                            <strong>{eur(t.cancelledAmount)}</strong>
                        </div>
                    )}
                </div>
                <section className="cash-categories" aria-labelledby="cash-categories-title">
                    <h2 className="section-title" id="cash-categories-title">Por categoría</h2>
                    {categories.length === 0 ? (
                        <div className="empty">
                            <strong>Todavía no hay ventas hoy</strong>
                        </div>
                    ) : (
                        <div className="stat-grid">
                            {categories.map(c => (
                                <div className="stat" key={c.category}>
                                    <small>{c.category}</small>
                                    <strong>{eur(c.revenue)}</strong>
                                </div>
                            ))}
                        </div>
                    )}
                </section>
                </>
            )}
        </>
    );
}

export default TodayCash