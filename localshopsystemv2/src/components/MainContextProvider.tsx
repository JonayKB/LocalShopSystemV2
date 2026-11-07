import React, { Dispatch, ReactNode, SetStateAction, useCallback, useEffect, useState, createContext } from 'react';
import { getAccessToken, TOKENS_CHANGED_EVENT } from '../utils/authStorage';
import Category from '../models/Category';
import Item from '../models/Item';

type Props = {
    children: ReactNode;
};

/** Una línea de la venta en curso. */
export type BasketLine = { item: Item; quantity: number };

/** Venta en curso, por código de producto (orden de inserción = orden en el carrito). */
export type Basket = Map<number, BasketLine>;

interface MainContextType {
    categories: Category[];
    setCategories: Dispatch<SetStateAction<Category[]>>;
    token: string | null;
    setToken: Dispatch<SetStateAction<string | null>>;
    basketItems: Basket;
    setBasketItems: Dispatch<SetStateAction<Basket>>;
    openBasket: boolean;
    setOpenBasket: Dispatch<SetStateAction<boolean>>;
    /** Fija la cantidad de un producto (0 o menos lo quita). */
    updateBasket: (item: Item, quantity: number) => void;
    openAddItemModal: Item | null;
    setOpenAddItemModal: Dispatch<SetStateAction<Item | null>>;
    /** Suma una unidad del producto. */
    addItemToBasket: (item: Item) => void;
}
export const MainContext = createContext<MainContextType>({} as MainContextType);

const MainContextProvider = (props: Props) => {
    const [categories, setCategories] = useState<Category[]>([]);
    const [token, setToken] = useState<string | null>(() => getAccessToken());
    // Si el interceptor renueva el token (o cierra la sesión), el contexto se entera.
    useEffect(() => {
        const sync = () => setToken(getAccessToken());
        window.addEventListener(TOKENS_CHANGED_EVENT, sync);
        return () => window.removeEventListener(TOKENS_CHANGED_EVENT, sync);
    }, []);
    const [basketItems, setBasketItems] = useState<Basket>(() => new Map());
    const [openBasket, setOpenBasket] = useState(false);
    const [openAddItemModal, setOpenAddItemModal] = useState<Item | null>(null);

    const updateBasket = useCallback((item: Item, quantity: number) => {
        setBasketItems(prev => {
            const next = new Map(prev);
            if (quantity > 0) {
                next.set(item.id, { item: prev.get(item.id)?.item ?? item, quantity });
            } else {
                next.delete(item.id);
            }
            return next;
        });
    }, []);

    const addItemToBasket = useCallback((item: Item) => {
        setBasketItems(prev => {
            const next = new Map(prev);
            const line = prev.get(item.id);
            next.set(item.id, { item: line?.item ?? item, quantity: (line?.quantity ?? 0) + 1 });
            return next;
        });
    }, []);

    const contextValues = {
        categories,
        setCategories,
        token,
        setToken,
        basketItems,
        setBasketItems,
        openBasket,
        setOpenBasket,
        updateBasket,
        openAddItemModal,
        setOpenAddItemModal,
        addItemToBasket,
    };

    return <MainContext.Provider value={contextValues}>{props.children}</MainContext.Provider>;
};

export default MainContextProvider;
