import React, { useContext, useState } from 'react';
import { MainContext } from './MainContextProvider';

const Basket: React.FC = () => {
    const [open, setOpen] = useState(true);
    const { basketItems } = useContext(MainContext);


    const toggleSidebar = () => {
        setOpen(prev => !prev);
    };

    return (
        <>
            {/* Botón hamburguesa */}
            <button
                onClick={toggleSidebar}
                style={{
                    position: 'fixed',
                    top: 20,
                    right: open ? -300 : 20,
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
                    right: open ? 0 : -600,
                    width: 500,
                    height: '100vh',
                    backgroundColor: '#1e1e2f',
                    color: 'white',
                    transition: 'right 0.3s ease-in-out',
                    padding: '60px 20px 20px',
                    zIndex: 1000,
                    boxShadow: '2px 0 10px rgba(0, 0, 0, 0.3)',
                    borderRadius: '0 0 0 10px',
                    overflowY: 'auto',
                    borderLeft: '1px solid #555',
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
                <ul style={{ listStyle: 'none', padding: 0, margin: 0 }}>
                    <li style={{ margin: '20px 0' }}>
                        {basketItems?.map((item, index) => (
                            <div key={index} style={{ marginBottom: '10px' }}>
                                {/* <strong>{item.name}</strong> - ${item.price.toFixed(2)} */}

                            </div>
                        ))}

                    </li>
                </ul>
            </div>
        </>
    );
};

export default Basket;
