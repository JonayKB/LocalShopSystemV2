import React from 'react'

type Props = {
    imageSrc: string;
    title: string;
    description: string;
    orientation?: 'image-left' | 'image-right';
}

const InfoDisplay = (props: Props) => {
    return (
        <div style={{ display: 'flex', alignItems: 'center', gap: '32px', flexDirection: (props.orientation === 'image-right' ? 'row-reverse' : 'row'), textAlign: (props.orientation === 'image-left' ? 'left' : 'right') }}>
            <img
                src={props.imageSrc}
                alt={props.title}
                style={{ width: '420px', height: '300px', objectFit: 'cover', borderRadius: '12px', boxShadow: '0 2px 8px #0006' }}
            />
            <div style={{ flex: 1 }}>
                <h2 style={{ marginBottom: '12px' }}>{props.title}</h2>
                <p>{props.description}</p>
            </div>
        </div>
    )
}

export default InfoDisplay