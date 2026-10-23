import React, { useState } from 'react'
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

    const realSrc = ImageRespository.url(item.image, 320) || null;

    // Si no hay imagen real, ya se considera "cargada" (se muestra el placeholder directamente)
    const [loaded, setLoaded] = useState(!realSrc);
    const [failed, setFailed] = useState(false);

    const out = !item.ignoreStock && item.stock <= 0;
    const low = !item.ignoreStock && !out && item.stock < item.bareMinimun;

    const src = failed || !realSrc ? PLACEHOLDER_IMG : realSrc;

    return (
        <div
            className={`item-card ${props.onClick ? 'is-clickable' : ''} ${out ? 'is-out' : ''} ${low ? 'is-low' : ''}`}
            onClick={props.onClick ? () => props.onClick!(item) : undefined}
            onContextMenu={(e) => {
                e.preventDefault();
                props.onContextMenu?.(item);
            }}
        >
            <div
                className="item-img"
                style={{ backgroundImage: `url("${PLACEHOLDER_IMG}")` }}
            >
                <img
                    key={src}
                    src={src}
                    alt={item.name}
                    loading="lazy"
                    decoding="async"
                    className={loaded ? 'is-loaded' : ''}
                    ref={(el) => {
                        if (el && el.complete && el.naturalWidth > 0) setLoaded(true);
                    }}
                    onLoad={() => setLoaded(true)}
                    onError={() => {
                        setFailed(true);
                        setLoaded(true);
                    }}
                />
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