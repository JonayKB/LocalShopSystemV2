import React, { Dispatch, ReactNode, SetStateAction, useState, createContext } from 'react'
import Category from '../models/Category';

type Props = {
    children: ReactNode
}

interface MainContextType {
    categories?: Category[];
}
export const MainContext = createContext<MainContextType>({} as MainContextType);

const MainContextProvider = (props: Props) => {
    const [categories, setCategories] = useState<Category[]>([]);

    const contextValues = {
        categories,
        setCategories: setCategories as Dispatch<SetStateAction<Category[]>>,
    }

    return (
        <MainContext.Provider value={contextValues}>
            {props.children}
        </MainContext.Provider>
    );
}

export default MainContextProvider;