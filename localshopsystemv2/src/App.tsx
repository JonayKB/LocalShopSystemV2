import React, { useContext } from 'react';

import { BrowserRouter, Route, Routes } from 'react-router-dom';
import Navbar from './components/Navbar';
import MainContextProvider, { MainContext } from './components/MainContextProvider';
import BarcodeListener from './components/BarcodeListener';
import './styles/App.css';
import HomeScreen from './screens/HomeScreen';
import LoginScreen from './screens/LoginScreen';
import ItemsScreen from './screens/ItemsScreen';
import TradesScreen from './screens/TradesScreen';
import Basket from './components/Basket';
import ItemRepository from './repositories/ItemRepository';
import ExportScreen from './screens/ExportScreen';

function App() {
  const { updateBasket, setOpenBasket, token } = useContext(MainContext);
  const itemRepository = new ItemRepository();
  const handleBarcodeScan = async (code: string) => {
    const item = await itemRepository.getItemById(code, token);
    if (item) {
      updateBasket(item, 1);
      setOpenBasket(true);
    } 


  };

  return (
    <BrowserRouter>
      <MainContextProvider>
        <div className="app-container">
          <Basket />
          <Navbar />
          <div className="main-content">
            <BarcodeListener onScan={handleBarcodeScan} />
            <Routes>
              <Route path="/" element={<HomeScreen />} />
              <Route path='/login' element={<LoginScreen />} />
              <Route path="/items" element={<ItemsScreen />} />
              <Route path="/trades" element={<TradesScreen />} />
              <Route path="/export" element={<ExportScreen />} />
            </Routes>
          </div>
        </div>
      </MainContextProvider>
    </BrowserRouter>
  );
}




export default App;
