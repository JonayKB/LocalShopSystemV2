import React from 'react';

import './App.css';
import { BrowserRouter, Route, Routes } from 'react-router-dom';
import Home from './screens/Home';
import Navbar from './components/Navbar';
import MainContextProvider from './components/MainContextProvider';

function App() {
  return (
    <BrowserRouter>
    <MainContextProvider>
      <Navbar/>
      <Routes>
        <Route path="/" element={<Home />} />
      </Routes>

    </MainContextProvider>
    </BrowserRouter>
  );
}

export default App;
