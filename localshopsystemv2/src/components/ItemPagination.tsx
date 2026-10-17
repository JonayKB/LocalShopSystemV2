import React, { forwardRef, useEffect, useImperativeHandle, useState } from 'react';
import { PackageOpen } from 'lucide-react';
import ItemComponent from './ItemComponent';
import Pager from './Pager';
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
            {(() => {
                if (loading && !pageData) {
                    return (
                        <div className="item-grid" aria-busy="true">
                            {Array.from({ length: 8 }).map((_, i) => (
                                <div key={i} className="skeleton" style={{ height: 250 }} />
                            ))}
                        </div>
                    );
                } else if (!pageData || pageData.content.length === 0) {
                    return (
                        <div className="empty">
                            <PackageOpen size={34} strokeWidth={1.4} />
                            <strong>No hay productos</strong>
                            <span>Prueba con otra búsqueda o categoría.</span>
                        </div>
                    );
                } else {
                    return (
                        <div className="item-grid" style={{ opacity: loading ? 0.6 : 1, transition: 'opacity 0.2s' }}>
                            {pageData.content.map(item => (
                                <ItemComponent key={item.id} item={item} token={token} onClick={props.onItemClick} onContextMenu={props.onItemMenu} />
                            ))}
                        </div>
                    );
                }
            })()}
            <Pager
                page={pageData}
                onPrev={() => setPage(p => Math.max(0, p - 1))}
                onNext={() => setPage(p => pageData ? Math.min(pageData.totalPages - 1, p + 1) : p)}
            />
        </div>
    );
})
export default ItemPagination
