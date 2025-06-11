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
        <div style={{ textAlign: 'center', color: 'white', background: '#3a3d4a', padding: '10px', borderRadius: '8px', boxShadow: '0 2px 4px rgba(0, 0, 0, 0.1)', height: '35vh', cursor: props.onClick ? 'pointer' : 'default' }}
            onClick={props.onClick ? () => props.onClick!(props.item) : undefined}
            onContextMenu={(e) => {
                e.preventDefault();
                if (props.onContextMenu) {
                    props.onContextMenu(props.item);
                }
            }
            }
        >
            <img
                src={imageUrl}
                alt={props.item.name}
                style={{ width: '100%', height: 'auto', borderRadius: '8px', maxWidth: '20rem' }}
            />
            <h3 style={{ textTransform: 'capitalize' }}>{props.item.name}</h3>
            <p>{props.item.price.toFixed(2)} €</p>
        </div>
    )
}

export default ItemComponent