import React, { useContext, useEffect, useState } from 'react';
import { MainContext } from '../components/MainContextProvider';
import { useNavigate } from 'react-router-dom';
import ItemSearcher from '../components/ItemSearcher';
import Item from '../models/Item';
import BarcodeListener from '../components/BarcodeListener';
import ItemRepository from '../repositories/ItemRepository';
import StockRepository from '../repositories/StockRepository';

type Props = {}

const StockScreen = (props: Props) => {
    const { token } = useContext(MainContext);
    const navitigation = useNavigate();
    const [itemsToProcess, setItemsToProcess] = useState<Map<Item, number>>(new Map());
    const itemRepository = new ItemRepository();
    const stockRepository = new StockRepository();

    useEffect(() => {
        if (!token) {
            navitigation('/admin/login');
        }
    }, [token, navitigation]);

    function onItemClick(item: Item) {
        setItemsToProcess(prev => {
            const newMap = new Map(prev);
            const existingEntry = Array.from(newMap.keys()).find(i => i.id === item.id);
            if (existingEntry) {
                newMap.set(existingEntry, (newMap.get(existingEntry) || 0) + 1);
            } else {
                newMap.set(item, 1);
            }
            return newMap;
        });
    }

    function addStock() {
        setItemsToProcess(new Map());
        const data = stockRepository.addStock(itemsToProcess, token);
        data.then((res) => {
            if (res) {
                alert('Stock añadido correctamente');
            } else {
                alert('Error al añadir el stock');
            }
        });

        
    };
    async function removeStock() {

        setItemsToProcess(new Map());
        const blob = await stockRepository.removeStock(itemsToProcess, token);
        if (blob) {
            const url = URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = url;
            link.download = 'DEVOLUCIÓN_' + new Date().toISOString() + '.xlsx';
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            URL.revokeObjectURL(url);
        } else {
            alert('Error al eliminar el stock');
        }

    }

    const handleBarcodeScan = async (code: string) => {
        if (!token) {
            return;
        }
        const item = await itemRepository.getItemById(code, token);
        if (item) {
            onItemClick(item);
        } else {
            alert('Este item no esta registrado en el sistema. Por favor, registralo antes.');
        }
    };

    return (
        <div
            style={{
                flex: 1,
                background: '#2a2d3a',
                width: '100%',
                color: 'white',
                padding: '20px',
                fontSize: '20px',
            }}
        >
            <BarcodeListener handleBarcodeScan={handleBarcodeScan} />
            <ItemSearcher onItemClickProp={onItemClick} />

            <div style={{ maxHeight: '50%', overflowY: 'auto', marginBottom: '20px' }}>
                <table style={{ width: '80%', borderCollapse: 'collapse', marginLeft: 'auto', marginRight: 'auto' }}>
                    <thead>
                        <tr>
                            <th style={{ borderBottom: '1px solid #444', padding: '10px', textAlign: 'left' }}>Nombre</th>
                            <th style={{ borderBottom: '1px solid #444', padding: '10px', textAlign: 'left' }}>Cantidad</th>
                        </tr>
                    </thead>
                    <tbody>
                        {Array.from(itemsToProcess.entries()).map(([item, cantidad]) => (
                            <tr key={item.id}>
                                <td style={{ borderBottom: '1px solid #444', padding: '10px' }}>{item.name}</td>
                                <td style={{ borderBottom: '1px solid #444', padding: '10px' }}>{cantidad}</td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>

            {Array.from(itemsToProcess.entries()).length > 0 && (
                <div style={{ textAlign: 'center' }}>
                    <button
                        style={{
                            textAlign: 'center',
                            width: 'fit-content',
                            background: '#3a2323ff',
                            border: '1px solid #444',
                            padding: '12px 28px',
                            cursor: 'pointer',
                            color: '#fff',
                            borderRadius: '6px',
                            boxShadow: '0 2px 8px rgba(0,0,0,0.12)',
                            transition: 'background 0.2s, transform 0.2s',
                            fontWeight: 500,
                            fontSize: '1rem',
                        }}
                        onClick={removeStock}
                        onMouseEnter={(e) => {
                            e.currentTarget.style.background = '#5a3535ff';
                            e.currentTarget.style.transform = 'scale(1.06)';
                        }}
                        onMouseLeave={(e) => {
                            e.currentTarget.style.background = '#3a2323ff';
                            e.currentTarget.style.transform = 'scale(1)';
                        }}
                        tabIndex={0}
                        aria-label="Process Stock Update"
                        type="button"
                    >
                        Devolver
                    </button>
                    <button
                        style={{
                            textAlign: 'center',
                            width: 'fit-content',
                            background: '#233a30ff',
                            border: '1px solid #444',
                            padding: '12px 28px',
                            cursor: 'pointer',
                            color: '#fff',
                            borderRadius: '6px',
                            boxShadow: '0 2px 8px rgba(0,0,0,0.12)',
                            transition: 'background 0.2s, transform 0.2s',
                            fontWeight: 500,
                            fontSize: '1rem',
                            marginLeft: '20px'
                        }}
                        onClick={addStock}
                        onMouseEnter={(e) => {
                            e.currentTarget.style.background = '#355a40ff';
                            e.currentTarget.style.transform = 'scale(1.06)';
                        }}
                        onMouseLeave={(e) => {
                            e.currentTarget.style.background = '#233a30ff';
                            e.currentTarget.style.transform = 'scale(1)';
                        }}
                        tabIndex={0}
                        aria-label="Clear Stock Update"
                        type="button"
                    >
                        Añadir
                    </button>
                </div>
            )}



        </div>
    );
};

export default StockScreen;
