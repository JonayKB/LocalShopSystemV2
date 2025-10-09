import React from 'react';

import { BrowserRouter, Route, Routes, useLocation } from 'react-router-dom';
import Navbar from './components/Navbar';
import MainContextProvider from './components/MainContextProvider';
import './styles/App.css';
import AdminHomeScreen from './screens/AdminHomeScreen';
import LoginScreen from './screens/LoginScreen';
import ItemsScreen from './screens/ItemsScreen';
import TradesScreen from './screens/TradesScreen';
import Basket from './components/Basket';
import ExportScreen from './screens/ExportScreen';
import StockScreen from './screens/StockScreen';
import GraphScreen from './screens/GraphScreen';
import HomeScreen from './screens/HomeScreen';
import AdminWrapper from './components/AdminWrapper';
import TodayCash from './screens/TodayCash';

function App() {


  return (
    <BrowserRouter>
      <MainContextProvider>
        <div className="app-container">



          <div className="main-content">
            <Routes>
              <Route path="/" element={<HomeScreen />} />

              <Route path="admin" element={
                <AdminWrapper />}>

                <Route path='login' element={<LoginScreen />} />
                <Route path='home' element={<AdminHomeScreen />} />
                <Route path="items" element={<ItemsScreen />} />
                <Route path="trades" element={<TradesScreen />} />
                <Route path="export" element={<ExportScreen />} />
                <Route path="stock" element={<StockScreen />} />
                <Route path='graphs' element={<GraphScreen />} />
                <Route path='today-cash' element={<TodayCash />} />

              </Route>

            </Routes>
          </div>
        </div>
      </MainContextProvider>
    </BrowserRouter>
  );
}




export default App;
