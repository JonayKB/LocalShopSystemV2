import React, { useContext, useEffect, useState } from 'react'
import { Boxes, Minus, Plus, Trash2 } from 'lucide-react';
import { MainContext } from '../components/MainContextProvider';
import { useNavigate } from 'react-router-dom';
import ItemSearcher from '../components/ItemSearcher';
import Item from '../models/Item';
import BarcodeListener from '../components/BarcodeListener';
import ItemRepository from '../repositories/ItemRepository';
import StockRepository from '../repositories/StockRepository';
import PageHeader from '../components/PageHeader';

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

    function setQuantity(item: Item, quantity: number) {
        setItemsToProcess(prev => {
            const newMap = new Map(prev);
            if (quantity > 0) {
                newMap.set(item, quantity);
            } else {
                newMap.delete(item);
            }
            return newMap;
        });
    }

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
        const toSend = itemsToProcess;
        setItemsToProcess(new Map());
        const data = stockRepository.addStock(toSend, token);
        data.then((res) => {
            if (res) {
                alert('Stock añadido correctamente');
            } else {
                alert('Error al añadir el stock');
            }
        });
    };

    async function removeStock() {
        const toSend = itemsToProcess;
        setItemsToProcess(new Map());
        const blob = await stockRepository.removeStock(toSend, token);
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

    const entries = Array.from(itemsToProcess.entries());

    return (
        <>
            <PageHeader
                eyebrow="Gestión"
                title="Stock"
                sub="Añade los productos que entran o que devuelves al proveedor y confirma el movimiento."
            />
            <BarcodeListener handleBarcodeScan={handleBarcodeScan} />
            <ItemSearcher onItemClickProp={onItemClick} />

            <section className="card card-pad" style={{ marginTop: 28 }}>
                <h2 className="section-title">Movimiento actual</h2>
                {entries.length === 0 ? (
                    <div className="empty">
                        <Boxes size={34} strokeWidth={1.4} />
                        <strong>Aún no has añadido productos</strong>
                        <span>Escanea o busca un producto para empezar.</span>
                    </div>
                ) : (
                    <>
                        <div className="table-wrap">
                            <table className="table">
                                <thead>
                                    <tr>
                                        <th>Producto</th>
                                        <th className="num">Cantidad</th>
                                        <th />
                                    </tr>
                                </thead>
                                <tbody>
                                    {entries.map(([item, cantidad]) => (
                                        <tr key={item.id}>
                                            <td className="name">{item.name}</td>
                                            <td className="num">
                                                <div className="qty">
                                                    <button onClick={() => setQuantity(item, cantidad - 1)} aria-label="Quitar uno"><Minus size={14} /></button>
                                                    <input
                                                        type="number"
                                                        min={1}
                                                        value={cantidad}
                                                        aria-label={`Cantidad de ${item.name}`}
                                                        onChange={e => setQuantity(item, parseInt(e.target.value) || 0)}
                                                    />
                                                    <button onClick={() => setQuantity(item, cantidad + 1)} aria-label="Añadir uno"><Plus size={14} /></button>
                                                </div>
                                            </td>
                                            <td className="num">
                                                <button className="btn-icon" onClick={() => setQuantity(item, 0)} aria-label={`Quitar ${item.name}`}>
                                                    <Trash2 size={17} />
                                                </button>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                        <div className="action-bar">
                            <button className="btn btn-danger" onClick={removeStock} type="button">
                                Devolver
                            </button>
                            <button className="btn btn-primary" onClick={addStock} type="button">
                                Añadir al stock
                            </button>
                        </div>
                    </>
                )}
            </section>
        </>
    );
};

export default StockScreen;
