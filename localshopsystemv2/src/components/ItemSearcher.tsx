import React, { useContext, useState } from 'react'
import ItemPagination from './ItemPagination'
import Item from '../models/Item';
import { MainContext } from './MainContextProvider';

type Props = {
    onItemClickProp?: (item: Item) => void;
}

const ItemSearcher = (props: Props) => {
    const [text, setText] = useState<string>();
    const { token, setOpenAddItemModal } = useContext(MainContext);
    const [selectedSortBy, setSelectedSortBy] = useState<string>('name')
    const [ascending, setAscending] = useState<boolean>(true);




    function onClickSortBy(sortBy: string) {
        if (selectedSortBy === sortBy) {
            setAscending(!ascending);
        } else {
            setSelectedSortBy(sortBy);
            setAscending(true);
        }
    }

    function onItemClick(item: Item) {
        if (props.onItemClickProp) {
            props.onItemClickProp(item);
            setText('');
        }
    }


    return (

        <div style={{
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            backgroundColor: '#2a2d3a',
            color: 'white',
            padding: '20px',
            fontSize: '20px',
            borderRadius: '8px'
        }}>
            <input type="text"
                placeholder="Buscar productos..."
                value={text}
                onChange={(e) => setText(e.target.value)}
                style={{ padding: '10px', borderRadius: '4px', border: '1px solid #ccc', marginBottom: '20px', backgroundColor: '#1e1e2f', color: 'white', width: '80%' }}
            />

            {text && (
                <div style={{ display: 'flex', flexDirection: 'row', alignItems: 'center', width: '100%' }}>
                    <div style={{ flex: 1, display: 'flex', justifyContent: 'start', flexDirection: 'column', alignItems: 'center', height: '65vh' }}>
                        <button
                            onClick={() => onClickSortBy('name')}
                            style={{
                                backgroundColor: selectedSortBy === 'name' ? '#3a3d4a' : '#1e1e2f',
                                color: 'white',
                                border: 'none',
                                padding: '20px',
                                width: '100%',
                                cursor: 'pointer',
                                fontWeight: selectedSortBy === 'name' ? 'bold' : 'normal'
                            }}
                        >
                            Nombre
                            {selectedSortBy === 'name' && (
                                <span style={{ marginLeft: 8 }}>
                                    {ascending ? '▲' : '▼'}
                                </span>
                            )}
                        </button>
                        <button
                            onClick={() => onClickSortBy('price')}
                            style={{
                                backgroundColor: selectedSortBy === 'price' ? '#3a3d4a' : '#1e1e2f',
                                color: 'white',
                                border: 'none',
                                padding: '20px',
                                width: '100%',
                                cursor: 'pointer',
                                fontWeight: selectedSortBy === 'price' ? 'bold' : 'normal'
                            }}
                        >
                            Precio
                            {selectedSortBy === 'price' && (
                                <span style={{ marginLeft: 8 }}>
                                    {ascending ? '▲' : '▼'}
                                </span>
                            )}
                        </button>
                        <button
                            onClick={() => onClickSortBy('category')}
                            style={{
                                backgroundColor: selectedSortBy === 'category' ? '#3a3d4a' : '#1e1e2f',
                                color: 'white',
                                border: 'none',
                                padding: '20px',
                                width: '100%',
                                cursor: 'pointer',
                                fontWeight: selectedSortBy === 'category' ? 'bold' : 'normal'
                            }}
                        >
                            Categoria
                            {selectedSortBy === 'category' && (
                                <span style={{ marginLeft: 8 }}>
                                    {ascending ? '▲' : '▼'}
                                </span>
                            )}
                        </button>
                    </div>
                    <div style={{ flex: 13 }}>
                        <ItemPagination token={token} onItemClick={onItemClick} text={text} sortBy={selectedSortBy} ascending={ascending} onItemMenu={(item) => setOpenAddItemModal(item)} />
                    </div>
                </div>
            )}

        </div>
    )
}

export default ItemSearcher