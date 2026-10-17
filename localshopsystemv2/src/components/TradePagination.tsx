import React, { useCallback, useEffect, useState } from 'react'
import { Receipt } from 'lucide-react';
import Page from '../models/Page';
import Trade from '../models/Trade';
import axios from 'axios';
import { BaseInfoRepository } from '../utils/BaseInfoRepository';
import TradeComponnet from './TradeComponent';
import TradeRepository from '../repositories/TradeRepository';
import CancelTradeModal from './CancelTradeModal';
import Pager from './Pager';

type Props = {
    token: string | null,
}

const TradePagination = (props: Props) => {
    const [pageData, setPageData] = useState<Page<Trade> | null>(null);
    const [page, setPage] = useState(0);
    const pageSize = 12;
    const [loading, setLoading] = useState(false);
    const [showCancelled, setShowCancelled] = useState(true);
    const [toCancel, setToCancel] = useState<Trade | null>(null);
    const [error, setError] = useState<string | null>(null);

    const fetchItems = useCallback(async () => {
        setLoading(true);
        setError(null);
        try {
            const res = await axios.get(BaseInfoRepository.BASE_URL + `trade/${page}/${pageSize}`, {
                headers: { Authorization: `Bearer ${props.token}` },
            });
            setPageData(res.data);
        } catch {
            setError('No se pudieron cargar las ventas.');
        } finally {
            setLoading(false);
        }
    }, [page, props.token]);

    useEffect(() => {
        fetchItems();
    }, [fetchItems]);

    const confirmCancel = async (reason: string) => {
        if (!toCancel) return;
        await new TradeRepository().cancelTrade(toCancel.id, reason, props.token);
        setToCancel(null);
        await fetchItems();
    };

    const trades = (pageData?.content ?? []).filter(t => showCancelled || t.status !== 'CANCELLED');

    return (
        <div>
            <div className="toolbar">
                <label className="check">
                    <input type="checkbox" checked={showCancelled} onChange={e => setShowCancelled(e.target.checked)} />
                    Mostrar ventas anuladas
                </label>
                {pageData && (
                    <span style={{ marginLeft: 'auto', color: 'var(--muted)', fontSize: '0.88rem' }}>
                        {pageData.totalElements} ventas en total
                    </span>
                )}
            </div>

            {error ? (
                <div className="cart-error" role="alert">{error}</div>
            ) : loading && !pageData ? (
                <div className="skeleton" style={{ height: 320 }} aria-busy="true" />
            ) : trades.length === 0 ? (
                <div className="empty">
                    <Receipt size={34} strokeWidth={1.4} />
                    <strong>No hay ventas que mostrar</strong>
                    <span>Las ventas que cobres aparecerán aquí.</span>
                </div>
            ) : (
                <div style={{ opacity: loading ? 0.6 : 1, transition: 'opacity 0.2s' }}>
                    {trades.map(trade => (
                        <TradeComponnet key={trade.id} trade={trade} onCancel={setToCancel} />
                    ))}
                </div>
            )}

            <Pager
                page={pageData}
                onPrev={() => setPage(p => Math.max(0, p - 1))}
                onNext={() => setPage(p => pageData ? Math.min(pageData.totalPages - 1, p + 1) : p)}
            />

            {toCancel && (
                <CancelTradeModal trade={toCancel} onClose={() => setToCancel(null)} onConfirm={confirmCancel} />
            )}
        </div>
    );
}

export default TradePagination
