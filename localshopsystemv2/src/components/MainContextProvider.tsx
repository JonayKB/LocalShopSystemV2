import React, { Dispatch, ReactNode, SetStateAction, useState, createContext } from 'react'
import Category from '../models/Category';
import Item from '../models/Item';

type Props = {
    children: ReactNode
}

interface MainContextType {
    categories: Category[];
    setCategories: Dispatch<SetStateAction<Category[]>>;
    token: string | null;
    setToken: Dispatch<SetStateAction<string | null>>;
    basketItems: Map<Item, number>;
    setBasketItems: Dispatch<SetStateAction<Map<Item, number>>>;
    openBasket: boolean;
    setOpenBasket: Dispatch<SetStateAction<boolean>>;
    updateBasket: (item: Item, quantity: number) => void;
    openAddItemModal: Item | null;
    setOpenAddItemModal: Dispatch<SetStateAction<Item | null>>;
}
export const MainContext = createContext<MainContextType>({} as MainContextType);

const MainContextProvider = (props: Props) => {
    const [categories, setCategories] = useState<Category[]>([]);
    const [token, setToken] = useState<string | null>(null);
    const [basketItems, setBasketItems] = useState<Map<Item, number>>(new Map<Item, number>());
    const [openBasket, setOpenBasket] = useState(true);
    const [openAddItemModal, setOpenAddItemModal] = useState<Item | null>(null);
    const updateBasket = (item: Item, quantity: number) => {
        setBasketItems(prev => {
            const updated = new Map(prev);
            if (quantity <= 0) {
                updated.delete(item);
            } else {
                updated.set(item, quantity);
            }
            return updated;
        });
    };

    const contextValues = {
        categories,
        setCategories: setCategories,
        token,
        setToken: setToken,
        basketItems,
        setBasketItems: setBasketItems,
        openBasket,
        setOpenBasket: setOpenBasket,
        updateBasket: updateBasket,
        openAddItemModal,
        setOpenAddItemModal: setOpenAddItemModal,
    }

    return (
        <MainContext.Provider value={contextValues}>
            {props.children}
        </MainContext.Provider>
    );
}

export default MainContextProvider;