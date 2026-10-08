import React, { useContext, useRef, useState } from 'react'
import { ChevronDown, ChevronUp } from 'lucide-react'
import ItemPagination from './ItemPagination'
import Item from '../models/Item';
import { MainContext } from './MainContextProvider';
import AddItemComponent from './AddItemComponent';

type Props = {
    onItemClickProp?: (item: Item) => void;
    onItemMenu?: (item: Item) => void;
}

const ItemSearcher = (props: Props) => {
    const [text, setText] = useState<string>();
    const { token, setOpenAddItemModal } = useContext(MainContext);
    const [selectedSortBy, setSelectedSortBy] = useState<string>('name')
    const [ascending, setAscending] = useState<boolean>(true);
    const itemPaginationRef = useRef<{ fetchItems: () => void }>(null);




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
            backgroundColor: 'var(--bg)',
            color: 'white',
            padding: '20px',
            fontSize: '20px',
            borderRadius: '8px'
        }}>
            <AddItemComponent onComplete={() => itemPaginationRef.current?.fetchItems()} />

            <input type="text"
                placeholder="Buscar productos..."
                value={text}
                onChange={(e) => setText(e.target.value)}
                style={{ padding: '10px', borderRadius: '4px', border: '1px solid var(--muted)', marginBottom: '20px', backgroundColor: 'var(--surface)', color: 'white', width: '80%' }}
            />

            {text && (
                <div style={{ display: 'flex', flexDirection: 'row', alignItems: 'center', width: '100%' }}>
                    <div style={{ flex: 1, display: 'flex', justifyContent: 'start', flexDirection: 'column', alignItems: 'center', height: '65vh' }}>
                        <button
                            onClick={() => onClickSortBy('name')}
                            style={{
                                backgroundColor: selectedSortBy === 'name' ? 'var(--surface-3)' : 'var(--surface)',
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
                                <span style={{ marginLeft: 8, display: 'inline-flex', verticalAlign: 'middle' }}>
                                    {ascending ? <ChevronUp size={16} /> : <ChevronDown size={16} />}
                                </span>
                            )}
                        </button>
                        <button
                            onClick={() => onClickSortBy('category')}
                            style={{
                                backgroundColor: selectedSortBy === 'category' ? 'var(--surface-3)' : 'var(--surface)',
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
                                <span style={{ marginLeft: 8, display: 'inline-flex', verticalAlign: 'middle' }}>
                                    {ascending ? <ChevronUp size={16} /> : <ChevronDown size={16} />}
                                </span>
                            )}
                        </button>
                    </div>
                    <div style={{ flex: 13 }}>
                        <ItemPagination token={token} onItemClick={onItemClick} text={text} sortBy={selectedSortBy} ascending={ascending} onItemMenu={props.onItemMenu} ref={itemPaginationRef} />
                    </div>
                </div>
            )}

        </div>
    )
}

export default ItemSearcher