import React, { forwardRef, useEffect, useImperativeHandle, useState } from 'react';
import ItemComponent from './ItemComponent';
import Item from '../models/Item';
import Page from '../models/Page';
import axios from 'axios';
import {BaseInfoRepository} from '../utils/BaseInfoRepository';

type Props = {
    categoryId?: number;
    token: string | null,
    onItemClick?: (item: Item) => void;
    text?: string;
    sortBy?: string;
    ascending?: boolean;
    onItemMenu?: (item: Item) => void;
}

const ItemPagination = forwardRef((props: Props, ref) => {
    const { categoryId, token } = props;
    const [pageData, setPageData] = useState<Page<Item> | null>(null);
    const [page, setPage] = useState(0);
    const pageSize = 12;
    const [loading, setLoading] = useState(false);
    const fetchItems = async () => {
        setLoading(true);
        let url = `items/${categoryId}/${page}/${pageSize}`;
        if (categoryId === undefined) {
            url = `items/${page}/${pageSize}`;
        }
        if (/^\d+$/.test(props.text ?? '')) {
            url = `items/${props.text}`;
        }
        const res = await axios.get(BaseInfoRepository.BASE_URL + url, {
            params: {
                sortBy: props.sortBy ?? 'name',
                ascending: props.ascending ?? true,
                name: props.text ?? '',
            },
            headers: {
                Authorization: `Bearer ${token}`,
            },
        });
        if (!res.data || typeof res.data !== 'object' || !('content' in res.data && 'totalPages' in res.data && 'number' in res.data)) {
            console.warn('Response is not of type Page<Item>', res.data);
            if (!res.data || typeof res.data !== 'object') {
                setPageData(null);
                setLoading(false);
                return;
            }
            res.data = {
                content: [res.data] as Item[],
                totalPages: 0,
                number: page,
                first: true,
                last: true,
                totalElements: 0,
                pageable: {
                    paged: true,
                    pageNumber: page,
                    pageSize: pageSize,
                    offset: 0,
                    sort: {
                        sorted: false,
                        empty: true,
                        unsorted: true,
                    },
                    unpaged: false,
                },
                size: pageSize,
                sort: {
                    sorted: false,
                    empty: true,
                    unsorted: true,
                },
                empty: true,
                numberOfElements: 1,
            };
        }
        setPageData(res.data);

        setLoading(false);
    };

    useImperativeHandle(ref, () => ({
        fetchItems: fetchItems
    }));

    useEffect(() => {

        fetchItems();
    }, [page, categoryId, token, props.text, props.sortBy, props.ascending]);

    return (
        <div>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(15%, 1fr))', gap: '20px', padding: '20px', backgroundColor: '#1e1e2f', color: 'white', borderRadius: 8, height: '65vh', overflowY: 'scroll' }}>
                {(() => {
                    if (loading) {
                        return <div style={{ color: 'white', fontSize: 18 }}>Cargando...</div>;
                    } else if (!pageData || pageData.content.length === 0) {
                        return <div style={{ color: 'white', fontSize: 18 }}>No hay productos.</div>;
                    } else {
                        return pageData.content.map(item => (
                            <ItemComponent key={item.id} item={item} token={token} onClick={props.onItemClick} onContextMenu={props.onItemMenu} />
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
})
export default ItemPagination