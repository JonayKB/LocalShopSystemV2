import React, { useState } from 'react';
import { Link } from 'react-router-dom';

const Navbar: React.FC = () => {
  const [open, setOpen] = useState(false);

  const toggleSidebar = () => {
    setOpen(prev => !prev);
  };


  return (
    <>
      {/* Botón hamburguesa */}
      <button
        onClick={toggleSidebar}
        style={{
          position: 'fixed',
          top: 20,
          left: open ? -300 : 20,
          transition: 'left 0.3s ease-in-out',
          zIndex: 1001,
          background: 'transparent',
          border: 'none',
          fontSize: '28px',
          color: 'white',
          cursor: 'pointer',
        }}
      >
        ☰
      </button>

      {/* Fondo semitransparente detrás del sidebar */}
      {open && (
        <div
          onClick={toggleSidebar}
          style={{
            position: 'fixed',
            top: 0,
            left: 0,
            width: '100vw',
            height: '100vh',
            backgroundColor: 'rgba(0,0,0,0.5)',
            zIndex: 999,
          }}
        />
      )}

      {/* Sidebar */}
      <div
        style={{
          position: 'fixed',
          top: 0,
          left: open ? 0 : -300,
          width: 240,
          height: '100vh',
          backgroundColor: '#1e1e2f', // dark navy
          color: 'white',
          transition: 'left 0.3s ease-in-out',
          padding: '60px 20px 20px',
          zIndex: 1000,
          boxShadow: '2px 0 10px rgba(0, 0, 0, 0.3)',
        }}
      >
        <ul style={{ listStyle: 'none', padding: 0, margin: 0 }}>
          <button
            onClick={toggleSidebar}
            style={{
              position: 'absolute',
              top: 20,
              right: 20,
              background: 'transparent',
              border: 'none',
              fontSize: '28px',
              color: 'white',
              cursor: 'pointer',
            }}
          >
            ✖
          </button>
          <li style={{ margin: '20px 0' }}>
            <Link
              to="/"
              style={{
                color: 'white',
                fontSize: '18px',
                textDecoration: 'none',
                display: 'block',
                padding: '8px 12px',
                borderRadius: '4px',
              }}
              onMouseOver={e => (e.currentTarget.style.background = '#333')}
              onMouseOut={e => (e.currentTarget.style.background = 'transparent')}
            >
              Inicio
            </Link>
          </li>
          <li style={{ margin: '20px 0' }}>
            <Link
              to="/items"
              style={{
                color: 'white',
                fontSize: '18px',
                textDecoration: 'none',
                display: 'block',
                padding: '8px 12px',
                borderRadius: '4px',
              }}
              onMouseOver={e => (e.currentTarget.style.background = '#333')}
              onMouseOut={e => (e.currentTarget.style.background = 'transparent')}
            >
              Productos
            </Link>
          </li>
          <li style={{ margin: '20px 0' }}>
            <Link
              to="/trades"
              style={{
                color: 'white',
                fontSize: '18px',
                textDecoration: 'none',
                display: 'block',
                padding: '8px 12px',
                borderRadius: '4px',
              }}
              onMouseOver={e => (e.currentTarget.style.background = '#333')}
              onMouseOut={e => (e.currentTarget.style.background = 'transparent')}
            >
              Ventas
            </Link>
          </li>

          <li style={{ margin: '20px 0' }}>
            <Link
              to="/export"
              style={{
                color: 'white',
                fontSize: '18px',
                textDecoration: 'none',
                display: 'block',
                padding: '8px 12px',
                borderRadius: '4px',
              }}
              onMouseOver={e => (e.currentTarget.style.background = '#333')}
              onMouseOut={e => (e.currentTarget.style.background = 'transparent')}
            >
              Exportar
            </Link>

          </li>
          <li style={{ margin: '20px 0' }}>
            <Link
              to="/stock"
              style={{
                color: 'white',
                fontSize: '18px',
                textDecoration: 'none',
                display: 'block',
                padding: '8px 12px',
                borderRadius: '4px',
              }}
              onMouseOver={e => (e.currentTarget.style.background = '#333')}
              onMouseOut={e => (e.currentTarget.style.background = 'transparent')}
            >
              Stock
            </Link>

          </li>
        </ul>
      </div>
    </>
  );
};

export default Navbar;
