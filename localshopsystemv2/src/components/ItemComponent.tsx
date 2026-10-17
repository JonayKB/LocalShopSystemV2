import React, { useEffect, useState } from 'react'
import Item from '../models/Item'
import ImageRespository from '../repositories/ImageRepository'
import { eur } from '../utils/format'
import { PLACEHOLDER_IMG } from '../utils/placeholder'

type Props = {
    item: Item,
    token: string | null,
    onClick?: (item: Item) => void,
    onContextMenu?: (item: Item) => void,
}

const ItemComponent = (props: Props) => {
    const { item } = props;
    const imageRepository = new ImageRespository();
    const [imageUrl, setImageUrl] = useState<string>(PLACEHOLDER_IMG);

    useEffect(() => {
        let cancelled = false;
        imageRepository.getImageById(item.image, props.token)
            .then(url => { if (!cancelled) setImageUrl(url); })
            .catch(() => { if (!cancelled) setImageUrl(PLACEHOLDER_IMG); });
        return () => { cancelled = true; };
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [item.image, props.token]);

    const out = !item.ignoreStock && item.stock <= 0;
    const low = !item.ignoreStock && !out && item.stock < item.bareMinimun;

    return (
        <div
            className={`item-card ${props.onClick ? 'is-clickable' : ''} ${out ? 'is-out' : ''} ${low ? 'is-low' : ''}`}
            onClick={props.onClick ? () => props.onClick!(item) : undefined}
            onContextMenu={(e) => {
                e.preventDefault();
                props.onContextMenu?.(item);
            }}
        >
            <div className="item-img">
                <img src={imageUrl} alt={item.name} onError={() => setImageUrl(PLACEHOLDER_IMG)} />
            </div>
            <div className="item-name">{item.name}</div>
            <div className="item-foot">
                <span className="price">{eur(item.price)}</span>
                {item.ignoreStock ? (
                    <span className="badge badge-muted">Sin control</span>
                ) : out ? (
                    <span className="badge badge-danger">Agotado</span>
                ) : low ? (
                    <span className="badge badge-warn">Quedan {item.stock}</span>
                ) : (
                    <span className="badge badge-ok">{item.stock} uds.</span>
                )}
            </div>
        </div>
    )
}

export default ItemComponent
