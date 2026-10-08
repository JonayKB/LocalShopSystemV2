import React from 'react'

type Props = {
    imageSrc: string;
    title: string;
    description: string;
    orientation?: 'image-left' | 'image-right';
}

const InfoDisplay = (props: Props) => {
    return (
        <div
            style={{
                display: 'flex',
                alignItems: 'center',
                gap: '32px',
                flexDirection: props.orientation === 'image-right' ? 'row-reverse' : 'row',
                padding: '20px',
                borderRadius: 'var(--radius)',
                background: 'var(--surface-2)',
                border: '1px solid var(--border)',
                boxShadow: 'var(--shadow-sm)',
            }}
        >
            <img
                src={props.imageSrc}
                alt={props.title}
                style={{ width: '40%', objectFit: 'cover', borderRadius: 'var(--radius-sm)', aspectRatio: '5 / 4' }}
            />
            <div style={{ flex: 1 }}>
                <h2 style={{ margin: '0 0 12px', fontFamily: 'var(--font-display)' }}>{props.title}</h2>
                <p style={{ margin: 0, color: 'var(--muted)', lineHeight: 1.6 }}>{props.description}</p>
            </div>
        </div>
    )
}

export default InfoDisplay
