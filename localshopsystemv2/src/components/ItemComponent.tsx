import React from 'react'
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
                <img
                    src={ImageRespository.url(item.image, 320) ?? PLACEHOLDER_IMG}
                    alt={item.name}
                    loading="lazy"
                    decoding="async"
                    onError={(e) => { if (e.currentTarget.src !== PLACEHOLDER_IMG) e.currentTarget.src = PLACEHOLDER_IMG; }}
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