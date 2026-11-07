import React, { Suspense, useContext } from 'react';
import { Navigate, Outlet, useLocation } from 'react-router-dom';
import Basket from './Basket';
import Navbar from './Navbar';
import { MainContext } from './MainContextProvider';
import '../styles/admin.css';

/**
 * Marco del panel. Sin sesión, cualquier pantalla de /admin redirige al login (guardando
 * a dónde se quería ir). La seguridad real la pone el backend; esto evita pantallas vacías.
 */
// Mientras se descarga una pantalla, el menú y el tema claro siguen pintados: solo el
// contenido muestra el esqueleto (si el Suspense estuviera fuera, se vería un flash oscuro).
const ScreenLoading = () => <div className="skeleton" style={{ height: 240 }} aria-busy="true" />;

const AdminWrapper = () => {
  const { pathname } = useLocation();
  const { token } = useContext(MainContext);
  const onLogin = pathname.toLowerCase().endsWith('/login');

  if (onLogin) {
    // el login va a pantalla completa, sin menú ni carrito
    return (
      <div className="theme-light">
        <Suspense fallback={null}>
          <Outlet />
        </Suspense>
      </div>
    );
  }
  if (!token) {
    return <Navigate to="/admin/login" replace state={{ from: pathname }} />;
  }
  return (
    <div className="admin theme-light">
      <Navbar />
      <main className="admin-main">
        <div className="admin-content">
          <Suspense fallback={<ScreenLoading />}>
            <Outlet />
          </Suspense>
        </div>
      </main>
      <Basket />
    </div>
  );
};

export default AdminWrapper;
