import React, { useEffect, useState } from 'react'
import Page from '../models/Page';
import Trade from '../models/Trade';
import axios from 'axios';
import {BaseInfoRepository} from '../utils/BaseInfoRepository';
import TradeComponnet from './TradeComponent';
import TradeRepository from '../repositories/TradeRepository';

type Props = {
    token: string | null,
}

const TradePagination = (props: Props) => {
    const [pageData, setPageData] = useState<Page<Trade> | null>(null);
    const [page, setPage] = useState(0);
    const pageSize = 12;
    const [loading, setLoading] = useState(false);
    const tradeRepository = new TradeRepository();
    const fetchItems = async () => {
        setLoading(true);
        let url = `trade/${page}/${pageSize}`;
        const res = await axios.get(BaseInfoRepository.BASE_URL + url, {
            headers: {
                Authorization: `Bearer ${props.token}`,
            },
        });
        setPageData(res.data);

        setLoading(false);
    };

    useEffect(() => {
        fetchItems();
    }, [page, props.token]);

    const handleDelete = async (tradeId: number) => {
        if (!window.confirm('¿Estás seguro de que deseas eliminar esta venta? Esta acción no se puede deshacer.')) {
            return;
        }
        await tradeRepository.deleteTrade(tradeId, props.token);
        fetchItems();
    };

    return (
        <div>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(100%, 1fr))', gap: '20px', padding: '20px', backgroundColor: '#1e1e2f', color: 'white', borderRadius: 8, height: '85vh', overflowY: 'scroll', margin: 10 }}>
                <div style={{ flex: 1, display: 'flex', justifyContent: 'space-around', alignItems: 'center', marginBottom: 10 }}>
                    <h2 style={{ textAlign: 'center' }}>Identificador</h2>
                    <h2 style={{ textAlign: 'center' }}>Fecha</h2>
                    <h2 style={{ textAlign: 'center' }}>Total</h2>
                    <h2 style={{ textAlign: 'center' }}>Acciones</h2>
                </div>
                {(() => {
                    if (loading) {
                        return <div style={{ color: 'white', fontSize: 18 }}>Cargando...</div>;
                    } else if (!pageData || pageData.content.length === 0) {
                        return <div style={{ color: 'white', fontSize: 18 }}>No hay productos.</div>;
                    } else {
                        return pageData.content.map(item => (
                            <TradeComponnet key={item.id} trade={item} onDelete={handleDelete} />
                        ));
                    }
                })()}
            </div>
            <div style={{ display: 'flex', justifyContent: 'center', marginTop: 24 }}>
                <button
                    onClick={() => setPage(p => Math.max(0, p - 1))}
                    disabled={!pageData || pageData.first}
                    style={{
                        background: '#1e1e2f',
                        color: 'white',
                        border: 'none',
                        borderRadius: 4,
                        padding: '8px 16px',
                        marginRight: 8,
                        cursor: !pageData || pageData.first ? 'not-allowed' : 'pointer',
                        opacity: !pageData || pageData.first ? 0.5 : 1,
                    }}
                >
                    Anterior
                </button>
                <span style={{ color: 'white', fontSize: 16, margin: '0 12px' }}>
                    Página {pageData ? pageData.number + 1 : 0} de {pageData ? pageData.totalPages : 0}
                </span>
                <button
                    onClick={() => setPage(p => pageData ? Math.min(pageData.totalPages - 1, p + 1) : p)}
                    disabled={!pageData || pageData.last || pageData.totalPages === 0}
                    style={{
                        background: '#1e1e2f',
                        color: 'white',
                        border: 'none',
                        borderRadius: 4,
                        padding: '8px 16px',
                        marginLeft: 8,
                        cursor: !pageData || pageData.last || pageData.totalPages === 0 ? 'not-allowed' : 'pointer',
                        opacity: !pageData || pageData.last || pageData.totalPages === 0 ? 0.5 : 1,
                    }}
                >
                    Siguiente
                </button>
            </div>
        </div>
    );
}

export default TradePagination