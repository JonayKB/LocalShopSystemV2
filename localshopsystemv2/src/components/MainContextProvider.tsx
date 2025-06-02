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
    basketItems: Item[];
    setBasketItems: Dispatch<SetStateAction<Item[]>>;
}
export const MainContext = createContext<MainContextType>({} as MainContextType);

const MainContextProvider = (props: Props) => {
    const [categories, setCategories] = useState<Category[]>([]);
    const [token, setToken] = useState<string | null>(null);
    const [basketItems, setBasketItems] = useState<Item[]>([]);

    const contextValues = {
        categories,
        setCategories: setCategories as Dispatch<SetStateAction<Category[]>>,
        token,
        setToken: setToken as Dispatch<SetStateAction<string | null>>,
        basketItems,
        setBasketItems: setBasketItems as Dispatch<SetStateAction<Item[]>>,
    }

    return (
        <MainContext.Provider value={contextValues}>
            {props.children}
        </MainContext.Provider>
    );
}

export default MainContextProvider;