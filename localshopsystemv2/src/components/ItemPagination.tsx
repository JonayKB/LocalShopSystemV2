import React, { Dispatch, useEffect, useState } from 'react';
import ItemComponent from './ItemComponent';
import Item from '../models/Item';
import Page from '../models/Page';
import axios from 'axios';
import BaseInfoRepository from '../utils/BaseInfoRepository';

type Props = {
    categoryId?: number;
    token: string | null,
    onItemClick?: (item: Item) => void;
    
}

const ItemPagination = (props: Props) => {
    const { categoryId, token } = props;
    const [pageData, setPageData] = useState<Page<Item> | null>(null);
    const [page, setPage] = useState(0);
    const pageSize = 12;
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        const fetchItems = async () => {
            setLoading(true);
            let url = `items/${categoryId}/${page}/${pageSize}`;
            if (categoryId === undefined) {
                url = `items/${page}/${pageSize}`;
            }
            const res = await axios.get(BaseInfoRepository.BASE_URL + url, {
                params: {
                    sortBy: 'name',
                    ascending: true,
                    name: '',
                },
                headers: {
                    Authorization: `Bearer ${token}`,
                },
            });
            setPageData(res.data);

            setLoading(false);
        };
        fetchItems();
    }, [page, categoryId, token]);

    return (
        <div>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '20px', padding: '20px', backgroundColor: '#1e1e2f', color: 'white', borderRadius: 8, height: '65vh', overflowY: 'scroll' }}>
                {(() => {
                    if (loading) {
                        return <div style={{ color: 'white', fontSize: 18 }}>Cargando...</div>;
                    } else if (!pageData || pageData.content.length === 0) {
                        return <div style={{ color: 'white', fontSize: 18 }}>No hay productos.</div>;
                    } else {
                        return pageData.content.map(item => (
                            <ItemComponent key={item.id} item={item} token={token} onClick={props.onItemClick} />
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

export default ItemPagination