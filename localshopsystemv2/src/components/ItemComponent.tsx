import React, { useEffect, useState } from 'react'
import Item from '../models/Item'
import ImageRespository from '../repositories/ImageRepository'

type Props = {
    item: Item,
    token: string | null,
    onClick?: (item: Item) => void,
    onContextMenu?: (item: Item) => void,

}

const ItemComponent = (props: Props) => {
    const imageRepository = new ImageRespository();
    const [imageUrl, setImageUrl] = useState<string>();
    useEffect(() => {
        const fetchImage = async () => {
            try {
                const url = await imageRepository.getImageById(props.item.image, props.token);
                setImageUrl(url);
            } catch (error) {
                setImageUrl('https://static.vecteezy.com/system/resources/previews/008/695/917/non_2x/no-image-available-icon-simple-two-colors-template-for-no-image-or-picture-coming-soon-and-placeholder-illustration-isolated-on-white-background-vector.jpg');
            }
        };

        fetchImage();
    }
        , []);
    return (
        <div
            style={{
            textAlign: 'center',
            color: 'white',
            background: props.item.ignoreStock ? '#b0b0b0' : '#3a3d4a',
            padding: '10px',
            borderRadius: '8px',
            boxShadow: '0 2px 4px rgba(0, 0, 0, 0.1)',
            height: '35vh',
            cursor: props.onClick ? 'pointer' : 'default',
            boxSizing: 'border-box',
            border: props.item.ignoreStock
                ? undefined
                : props.item.stock <= 0
                ? '4px solid red'
                : props.item.stock < props.item.bareMinimun
                ? '4px solid orange'
                : undefined,
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'space-between',
            }}
            onClick={props.onClick ? () => props.onClick!(props.item) : undefined}
            onContextMenu={(e) => {
            e.preventDefault();
            if (props.onContextMenu) {
                props.onContextMenu(props.item);
            }
            }}
        >
            <img
            src={imageUrl}
            alt={props.item.name}
            style={{
                width: '100%',
                maxWidth: '20rem',
                height: '60%',
                objectFit: 'contain',
                borderRadius: '8px',
            }}
            />
            <h3 style={{ textTransform: 'capitalize', margin: '8px 0 4px 0' }}>{props.item.name}</h3>
            <p style={{ margin: 0 }}>{props.item.price.toFixed(2)} €</p>
        </div>
    )
}

export default ItemComponent