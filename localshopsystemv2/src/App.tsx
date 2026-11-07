import React, { lazy, Suspense } from 'react';
import { BrowserRouter, Route, Routes } from 'react-router-dom';
import MainContextProvider from './components/MainContextProvider';
import './styles/App.css';
import HomeScreen from './screens/HomeScreen';

// El panel se descarga solo al entrar en /admin: la web pública (clientes) no carga
// las pantallas de gestión ni la librería de gráficas.
const AdminWrapper = lazy(() => import('./components/AdminWrapper'));
const LoginScreen = lazy(() => import('./screens/LoginScreen'));
const AdminHomeScreen = lazy(() => import('./screens/AdminHomeScreen'));
const ItemsScreen = lazy(() => import('./screens/ItemsScreen'));
const TradesScreen = lazy(() => import('./screens/TradesScreen'));
const ExportScreen = lazy(() => import('./screens/ExportScreen'));
const StockScreen = lazy(() => import('./screens/StockScreen'));
const GraphScreen = lazy(() => import('./screens/GraphScreen'));
const TodayCash = lazy(() => import('./screens/TodayCash'));

function App() {
  return (
    <BrowserRouter>
      <MainContextProvider>
        <div className="app-container">
          <div className="main-content">
            {/* Solo cubre la descarga del propio AdminWrapper: sin fallback visible para no pintar
                un fondo oscuro fuera del tema claro. Las pantallas tienen su Suspense dentro del marco. */}
            <Suspense fallback={null}>
              <Routes>
                <Route path="/" element={<HomeScreen />} />
                <Route path="admin" element={<AdminWrapper />}>
                  <Route path="login" element={<LoginScreen />} />
                  <Route path="home" element={<AdminHomeScreen />} />
                  <Route path="items" element={<ItemsScreen />} />
                  <Route path="trades" element={<TradesScreen />} />
                  <Route path="export" element={<ExportScreen />} />
                  <Route path="stock" element={<StockScreen />} />
                  <Route path="graphs" element={<GraphScreen />} />
                  <Route path="today-cash" element={<TodayCash />} />
                </Route>
              </Routes>
            </Suspense>
          </div>
        </div>
      </MainContextProvider>
    </BrowserRouter>
  );
}

export default App;
