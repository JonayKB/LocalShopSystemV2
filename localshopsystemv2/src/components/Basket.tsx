import React, { useContext, useState } from 'react';
import { MainContext } from './MainContextProvider';
import TradeRepository from '../repositories/TradeRepository';
import { Checkbox } from '@mui/material';

const Basket: React.FC = () => {
    const { basketItems, openBasket, setOpenBasket, updateBasket, token, setBasketItems } = useContext(MainContext);
    const tradeRepository = new TradeRepository();
    const [printTicket, setPrintTicket] = useState(false);

    const handleCreateTrade = async () => {
        console.log('Creating trade with items:', basketItems);
        const result = await tradeRepository.createTrade(basketItems, token, printTicket);
        if (result) {
            playSound('success_sound.mp3');
            setBasketItems(new Map());
            setPrintTicket(false);
            document.activeElement instanceof HTMLElement && document.activeElement.blur();
        }
    };

    const playSound = (src: string) => {
        const audio = new Audio(src);
        audio.play();
    };

    const toggleSidebar = () => {
        setOpenBasket(prev => !prev);
    };



    return (
        <>
            {/* Botón hamburguesa */}
            <button
                onClick={toggleSidebar}
                style={{
                    position: 'fixed',
                    top: 20,
                    right: openBasket ? -300 : 20,
                    transition: 'right 0.3s ease-in-out',
                    zIndex: 1001,
                    background: 'transparent',
                    border: 'none',
                    fontSize: '28px',
                    color: 'white',
                    cursor: 'pointer',
                }}
            >
                🛒
            </button>

            {/* Sidebar */}
            <div
                style={{
                    position: 'fixed',
                    top: 0,
                    right: openBasket ? 0 : -600,
                    width: 500,
                    height: '93.7vh',
                    backgroundColor: '#1e1e2f',
                    color: 'white',
                    transition: 'right 0.3s ease-in-out',
                    padding: '60px 20px 20px',
                    zIndex: 1000,
                    boxShadow: '2px 0 10px rgba(0, 0, 0, 0.3)',
                    borderRadius: '0 0 0 10px',
                    overflowY: 'auto',
                    borderLeft: '1px solid #555',
                    display: 'flex',
                    flexDirection: 'column',
                    justifyContent: 'flex-end'
                }}
            >
                <button
                    onClick={toggleSidebar}
                    style={{
                        position: 'absolute',
                        top: 20,
                        left: 20,
                        background: 'transparent',
                        border: 'none',
                        fontSize: '28px',
                        color: 'white',
                        cursor: 'pointer',
                    }}
                >
                    ✖
                </button>
                <div style={{ marginBottom: '20px', fontSize: '20px', fontWeight: 'bold', overflowY: 'scroll', height: '75vh', backgroundColor: '#2a2d3a', padding: '20px', borderRadius: '8px', display: 'flex', flexDirection: 'column', gap: '10px' }}>
                    {Array.from(basketItems.entries()).map(([item, quantity]) => (
                        <div key={item.id} style={{ marginBottom: '10px' }}>
                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                <span style={{ textTransform: 'capitalize' }}>{item.name}</span>
                                <button
                                    onClick={() => {
                                        updateBasket(item, quantity - 1);

                                    }}
                                    style={{
                                        background: 'none',
                                        border: 'none',
                                        color: '#ff6b6b',
                                        cursor: 'pointer',
                                        fontSize: '14px',
                                    }}
                                >
                                    -
                                </button>

                                <input
                                    type="number"
                                    min={1}
                                    value={quantity}
                                    onChange={e => {
                                        updateBasket(item, parseInt(e.target.value));
                                    }}
                                    style={{
                                        width: '60px',
                                        fontSize: '12px',
                                        color: '#aaa',
                                        background: 'transparent',
                                        border: '1px solid #555',
                                        borderRadius: '4px',
                                        padding: '2px 6px',
                                        margin: '0 8px',
                                        textAlign: 'center',
                                        MozAppearance: 'textfield',
                                    }}
                                />

                                <button
                                    onClick={() => {
                                        updateBasket(item, quantity + 1);

                                    }}
                                    style={{
                                        background: 'none',
                                        border: 'none',
                                        color: '#6bcf6b',
                                        cursor: 'pointer',
                                        fontSize: '14px',
                                    }}
                                >
                                    +
                                </button>
                                <span>{(item.price * quantity).toFixed(2)}€</span>
                            </div>


                        </div>
                    ))}
                    <span style={{ flex: 1, fontWeight: 'bold', display: 'block', marginTop: '20px', textAlign: 'right', justifyContent: 'flex-end', alignContent: 'flex-end' }}>
                        Total: {Array.from(basketItems.entries()).reduce((total, [item, quantity]) => total + item.price * quantity, 0).toFixed(2)}€
                    </span>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', marginBottom: '16px' }}>
                    <Checkbox
                        checked={printTicket}
                        onChange={(e) => setPrintTicket(e.target.checked)}
                        color="primary"
                        inputProps={{ 'aria-label': 'Print Ticket' }}
                        style={{ color: 'white' }}
                    />
                    <span style={{ marginLeft: '8px' }}>Print Ticket</span>
                </div>
                <button
                    onClick={handleCreateTrade}
                    onMouseEnter={(e) => {
                        e.currentTarget.style.transform = 'scale(1.05)';
                    }
                    }
                    onMouseLeave={(e) => {
                        e.currentTarget.style.transform = 'scale(1)';
                    }
                    }
                    tabIndex={0}
                    aria-label="Checkout"
                    style={{
                        width: '100%',
                        padding: '10px',
                        backgroundColor: '#6bcf6b',
                        color: 'white',
                        border: 'none',
                        borderRadius: '8px',
                        cursor: 'pointer',
                        fontSize: '16px',
                        transition: 'all 0.3s ease',
                        boxShadow: '0 2px 4px rgba(0, 0, 0, 0.1)',
                        fontWeight: 'bold',
                        marginBottom: '20px',

                    }}
                >
                    Checkout
                </button>
            </div>
        </>
    );
};

export default Basket;
