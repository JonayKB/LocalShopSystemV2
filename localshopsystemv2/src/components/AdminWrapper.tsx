import React from 'react';
import { Outlet, useLocation } from 'react-router-dom';
import Basket from './Basket';
import Navbar from './Navbar';
import '../styles/admin.css';

const AdminWrapper = () => {
  const { pathname } = useLocation();
  // El login va a pantalla completa, sin menú ni carrito
  if (pathname.toLowerCase().endsWith('/login')) {
    return <Outlet />;
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
