import React from 'react';

import { BrowserRouter, Route, Routes } from 'react-router-dom';
import Home from './screens/Home';
import Navbar from './components/Navbar';
import MainContextProvider from './components/MainContextProvider';
import BarcodeListener from './components/BarcodeListener';
import './styles/App.css';

function App() {
  const handleBarcodeScan = (code: string) => {
    console.log('Código escaneado:', code);
  };

  return (
    <BrowserRouter>
      <MainContextProvider>
        <div className="app-container">
          <Navbar />
          <div className="main-content">
            <BarcodeListener onScan={handleBarcodeScan} />
            <Routes>
              <Route path="/" element={<Home />} />
            </Routes>
          </div>
        </div>
      </MainContextProvider>
    </BrowserRouter>
  );
}




export default App;
