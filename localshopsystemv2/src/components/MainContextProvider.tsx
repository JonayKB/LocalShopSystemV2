import React, { Dispatch, ReactNode, SetStateAction, useState, createContext } from 'react'

type Props = {
    children: ReactNode
}

interface MainContextType {
    token: string;
    setToken: Dispatch<SetStateAction<string>>;
}
export const MainContext = createContext<MainContextType>({} as MainContextType);

const MainContextProvider = (props: Props) => {
    const [token, setToken] = useState<string>(localStorage.getItem('token') ?? '');

    const contextValues = {
        token,
        setToken
    }

    return (
        <MainContext.Provider value={contextValues}>
            {props.children}
        </MainContext.Provider>
    );
}

export default MainContextProvider;