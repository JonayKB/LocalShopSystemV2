import React, { useContext, useEffect, useState } from 'react'
import { MainContext } from './MainContextProvider';
import Category from '../models/Category';
import Item from '../models/Item';
import ItemRepository from '../repositories/ItemRepository';

type Props = {
}

const AddItemComponent = (props: Props) => {
    const { openAddItemModal, categories, setOpenAddItemModal, token } = useContext(MainContext);
    const [item, setItem] = useState<Item | null>(openAddItemModal);
    const itemRepository = new ItemRepository();
    useEffect(() => {
        setItem(openAddItemModal);
    }, [openAddItemModal]);

    const onSubmit = (e: React.FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        const formData = new FormData(e.currentTarget);
        let code = formData.get('code') as string;
        if (item?.id !== undefined) {
            code = item?.id.toString() ?? code;
        }
        const category = formData.get('category') as string;
        const price = parseFloat(formData.get('price') as string);
        const name = formData.get('name') as string;
        const newItem: Item = {
            id: Number(code),
            name: name,
            price: price,
            categoryId: Number(category),
            image: item?.image ?? code + '.png',
        };
        if (newItem.id < 0 || newItem.name === '' || newItem.price <= 0 || newItem.categoryId < 0) {
            alert('Rellena los datos correctamente');
            return;

        }
        console.log('Submitting item:', newItem);
        itemRepository.addItem(newItem, token)
            .then(() => {
                alert('Producto agregado correctamente');
                setOpenAddItemModal(null);
            })
            .catch((error) => {
                console.error('Error adding item:', error);
                alert('Error al agregar el producto. Por favor, inténtalo de nuevo.');
            });


    }
    return (
        <div
            style={{
                position: 'fixed',
                top: 0,
                left: 0,
                width: '100vw',
                height: '100vh',
                background: 'rgba(42,45,58,0.85)',
                zIndex: 1000,
                display: item !== null ? 'flex' : 'none',
                alignItems: 'center',
                justifyContent: 'center',
            }}
            className={`modal ${item !== null ? 'open' : ''}`}
        >

            <div
                style={{
                    background: '#23263a',
                    color: 'white',
                    padding: 32,
                    borderRadius: 12,
                    width: 420,
                    boxShadow: '0 8px 32px rgba(0,0,0,0.25)',
                    position: 'relative',
                    fontSize: 18,
                }}
            >
                <button
                    onClick={() => { setOpenAddItemModal(null) }}
                    style={{
                        position: 'absolute',
                        top: 16,
                        right: 16,
                        width: 32,
                        height: 32,
                        border: 'none',
                        borderRadius: '50%',
                        background: '#444',
                        color: '#fff',
                        fontSize: 20,
                        cursor: 'pointer',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        transition: 'background 0.2s',
                    }}
                    aria-label="Cerrar"
                    onMouseOver={e => (e.currentTarget.style.background = '#666')}
                    onMouseOut={e => (e.currentTarget.style.background = '#444')}
                >
                    ✖
                </button>
                <form onSubmit={onSubmit}>
                    <div style={{ marginBottom: 20 }}>
                        <label style={{ display: 'block', marginBottom: 6 }}>
                            Código:
                        </label>
                        <input
                            type="text"
                            value={
                                item?.id === -1
                                    ? ''
                                    : item?.id.toString()
                            }
                            disabled={!!item?.id && item.id !== -1}
                            placeholder="Introduce el código del producto"
                            style={{
                                width: '94%',
                                padding: '8px 12px',
                                borderRadius: 6,
                                border: '1px solid #444',
                                background: !!item?.id && item.id !== -1 ? '#444' : '#181a24',
                                color: '#fff',
                                fontSize: 16,
                            }}
                            name="code"
                            id="code"
                            required
                        />
                    </div>
                    <div style={{ marginBottom: 20 }}>
                        <label style={{ display: 'block', marginBottom: 6 }}>
                            Nombre:
                        </label>
                        <input
                            type="text"
                            value={item?.name ?? ''}
                            placeholder="Introduce el nombre del producto"
                            onChange={(e) => setItem(prev => prev ? { ...prev, name: e.target.value } : null)}
                            style={{
                                width: '94%',
                                padding: '8px 12px',
                                borderRadius: 6,
                                border: '1px solid #444',
                                background: '#181a24',
                                color: '#fff',
                                fontSize: 16,
                            }}
                            name="name"
                            id="name"
                            required
                        />
                    </div>
                    <div style={{ marginBottom: 20 }}>
                        <label style={{ display: 'block', marginBottom: 6 }}>
                            Categoría:
                        </label>
                        <select
                            style={{
                                width: '100%',
                                padding: '8px 12px',
                                borderRadius: 6,
                                border: '1px solid #444',
                                background: '#181a24',
                                color: '#fff',
                                fontSize: 16,
                                textTransform: 'capitalize',
                            }}
                            name="category"
                            id="category"
                        >
                            {Array.isArray(categories) &&
                                categories.map((cat: Category) => (
                                    <option
                                        key={cat.id}
                                        value={cat.id}
                                        selected={item?.categoryId === cat.id}
                                    >
                                        {cat.name.toUpperCase()}
                                    </option>
                                ))}
                        </select>
                    </div>
                    <div style={{ marginBottom: 24 }}>
                        <label style={{ display: 'block', marginBottom: 6 }}>
                            Precio:
                        </label>
                        <input
                            type="number"
                            min="0"
                            step="0.01"
                            style={{
                                width: '94%',
                                padding: '8px 12px',
                                borderRadius: 6,
                                border: '1px solid #444',
                                background: '#181a24',
                                color: '#fff',
                                fontSize: 16,
                            }}
                            value={item?.price ?? 0}
                            onChange={(e) => {
                                const value = parseFloat(e.target.value);
                                if (!isNaN(value) && value >= 0) {
                                    setItem(prev => prev ? { ...prev, price: value } : null);
                                } else {
                                    console.error('Invalid price input:', e.target.value);
                                    alert('Please enter a valid price.');
                                    e.target.value = '0';
                                    setItem(prev => prev ? { ...prev, price: 0 } : null);
                                }
                            }
                            }
                            name="price"
                            id="price"
                            required
                        />
                    </div>
                    <button
                        type="submit"
                        style={{
                            width: '100%',
                            padding: '12px 0',
                            borderRadius: 6,
                            border: 'none',
                            background: '#4f8cff',
                            color: '#fff',
                            fontWeight: 600,
                            fontSize: 18,
                            cursor: 'pointer',
                            transition: 'background 0.2s',
                        }}
                    >
                        Guardar
                    </button>
                </form>
            </div>
        </div>
    )
}

export default AddItemComponent