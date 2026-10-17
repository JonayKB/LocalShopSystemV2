import React, { useContext, useRef, useState } from 'react'
import { ArrowDown, ArrowUp, Search } from 'lucide-react'
import ItemPagination from './ItemPagination'
import Item from '../models/Item';
import { MainContext } from './MainContextProvider';
import AddItemComponent from './AddItemComponent';

type Props = {
    onItemClickProp?: (item: Item) => void;
    onItemMenu?: (item: Item) => void;
    /** Muestra todos los productos aunque no haya texto (pantalla de Productos). */
    showAll?: boolean;
    /** Se muestra solo cuando no hay búsqueda (p. ej. las categorías). */
    children?: React.ReactNode;
}

const SORTS = [
    { key: 'name', label: 'Nombre' },
    { key: 'category', label: 'Categoría' },
];

const ItemSearcher = (props: Props) => {
    const [text, setText] = useState<string>('');
    const { token } = useContext(MainContext);
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

    const searching = text.trim().length > 0;

    return (
        <div>
            <AddItemComponent onComplete={() => itemPaginationRef.current?.fetchItems()} />

            <div className="search" style={{ marginBottom: 20 }}>
                <Search size={18} />
                <input
                    type="text"
                    className="input"
                    placeholder="Buscar por nombre o código…"
                    value={text}
                    onChange={(e) => setText(e.target.value)}
                    aria-label="Buscar productos"
                />
            </div>

            {(searching || props.showAll) ? (
                <>
                    <div className="toolbar" style={{ marginBottom: 16 }}>
                        <span className="label">Ordenar por</span>
                        <div className="chips">
                            {SORTS.map(s => (
                                <button
                                    key={s.key}
                                    className={`chip ${selectedSortBy === s.key ? 'is-active' : ''}`}
                                    onClick={() => onClickSortBy(s.key)}
                                    type="button"
                                >
                                    {s.label}
                                    {selectedSortBy === s.key && (ascending ? <ArrowUp size={14} /> : <ArrowDown size={14} />)}
                                </button>
                            ))}
                        </div>
                    </div>
                    <ItemPagination token={token} onItemClick={props.onItemClickProp ? onItemClick : undefined} text={text} sortBy={selectedSortBy} ascending={ascending} onItemMenu={props.onItemMenu} ref={itemPaginationRef} />
                </>
            ) : (
                props.children
            )}
        </div>
    )
}

export default ItemSearcher
