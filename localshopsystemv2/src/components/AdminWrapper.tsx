import React, { useContext } from 'react';
import { Navigate, Outlet, useLocation } from 'react-router-dom';
import Basket from './Basket';
import Navbar from './Navbar';
import { MainContext } from './MainContextProvider';
import '../styles/admin.css';

/**
 * Marco del panel. Sin sesión, cualquier pantalla de /admin redirige al login (guardando
 * a dónde se quería ir). La seguridad real la pone el backend; esto evita pantallas vacías.
 */
const AdminWrapper = () => {
  const { pathname } = useLocation();
  const { token } = useContext(MainContext);
  const onLogin = pathname.toLowerCase().endsWith('/login');

  if (onLogin) {
    return <Outlet />; // el login va a pantalla completa, sin menú ni carrito
  }
  if (!token) {
    return <Navigate to="/admin/login" replace state={{ from: pathname }} />;
  }
  return (
    <div className="admin theme-light">
      <Navbar />
      <main className="admin-main">
        <div className="admin-content">
          <Outlet />
        </div>
      </main>
      <Basket />
    </div>
  );
};

export default AdminWrapper;
