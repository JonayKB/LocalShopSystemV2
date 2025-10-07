import React, { useState } from 'react'
import Trade from '../models/Trade'

type Props = {
    trade: Trade,
    onDelete?: (tradeId: number) => void;
}

const TradeComponnet = (props: Props) => {
    const { trade } = props;
    const [open, setOpen] = useState(false);

    return (
        <div style={{ textAlign: 'center', color: 'white', background: '#3a3d4a', padding: '10px', borderRadius: '8px', boxShadow: '0 2px 4px rgba(0, 0, 0, 0.1)', marginBottom: '10px' }}>
            <div
                style={{ cursor: 'pointer', display: 'flex', flexDirection: 'row', justifyContent: 'space-evenly', alignItems: 'center' }}
                onClick={() => setOpen(o => !o)}
            >
                <h3 style={{ textTransform: 'capitalize', padding: 20 }}>{trade.id}</h3>
                <h3 style={{ textTransform: 'capitalize', padding: 20 }}>
                    {new Date(trade.date).toLocaleString('es-ES', {
                        year: 'numeric',
                        month: 'numeric',
                        day: 'numeric',
                        hour: 'numeric',
                        minute: 'numeric',
                    })}</h3>
                <h3 style={{ textTransform: 'capitalize', padding: 20 }}>
                    {trade.items.reduce((sum, item) => sum + item.price, 0).toFixed(2)}€
                </h3>
                <div style={{ display: 'flex', flexDirection: 'row', alignItems: 'center', gap: 10 }}>
                    <button style={{ color: 'red', border: 'none', cursor: 'pointer', backgroundColor: 'transparent', fontSize: 38, padding: 20 }} onClick={() => props?.onDelete?.(trade.id)}>✖</button>
                </div>
            </div>
            <div
                style={{
                    maxHeight: open ? 200 : 0,
                    overflow: 'hidden',
                    transition: 'max-height 0.3s ease',
                    opacity: open ? 1 : 0,
                    transitionProperty: 'max-height, opacity',
                    transitionDuration: '0.3s',
                    display: 'flex',
                    flexDirection: 'row',
                    justifyContent: 'center',
                    alignItems: 'center',
                    gap: '10px',
                    marginTop: open ? 10 : 0,

                }}
            >
                {Object.values(
                    trade.items.reduce<Record<string, { name: string; price: number; amount: number }>>((acc, item) => {
                        if (acc[item.id]) {
                            acc[item.id].amount += 1;
                        } else {
                            acc[item.id] = { name: item.name, price: item.price, amount: 1 };
                        }
                        return acc;
                    }, {})
                ).map((item, index) => (
                    <div key={index} style={{ textAlign: 'center', padding: '10px', borderRadius: '8px', backgroundColor: '#2a2d3a' }}>
                        <h4 style={{ textTransform: 'capitalize' }}>
                            {item.name} {item.amount > 1 && <span style={{ color: '#aaa' }}>x{item.amount}</span>}
                        </h4>
                        <p>{item.price.toFixed(2)} €</p>
                    </div>
                ))}
            </div>
        </div>
    )
}

export default TradeComponnet