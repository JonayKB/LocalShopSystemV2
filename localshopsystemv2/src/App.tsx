import React from 'react';

import { BrowserRouter, Route, Routes } from 'react-router-dom';
import Navbar from './components/Navbar';
import MainContextProvider from './components/MainContextProvider';
import BarcodeListener from './components/BarcodeListener';
import './styles/App.css';
import HomeScreen from './screens/HomeScreen';
import LoginScreen from './screens/LoginScreen';
import ItemsScreen from './screens/ItemsScreen';
import TradesScreen from './screens/TradesScreen';
import Basket from './components/Basket';

function App() {
  const handleBarcodeScan = (code: string) => {
    console.log('Código escaneado:', code);
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
            </Routes>
          </div>
        </div>
      </MainContextProvider>
    </BrowserRouter>
  );
}




export default App;
