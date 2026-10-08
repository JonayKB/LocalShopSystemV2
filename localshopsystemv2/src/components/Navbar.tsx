import React, { useState } from 'react';
import { NavLink } from 'react-router-dom';
import { Boxes, FileDown, Globe, House, LineChart, Menu, Package, Receipt, Wallet, X } from 'lucide-react';
import '../styles/navbar.css';

const LINKS = [
  { to: 'home', label: 'Inicio', icon: House },
  { to: 'items', label: 'Productos', icon: Package },
  { to: 'trades', label: 'Ventas', icon: Receipt },
  { to: 'today-cash', label: 'Caja', icon: Wallet },
  { to: 'export', label: 'Exportar', icon: FileDown },
  { to: 'stock', label: 'Stock', icon: Boxes },
  { to: 'graphs', label: 'Gráficos', icon: LineChart },
];

const Navbar: React.FC = () => {
  const [open, setOpen] = useState(false);

  const toggleSidebar = () => setOpen(prev => !prev);

  return (
    <>
      {/* Botón hamburguesa */}
      <button
        onClick={toggleSidebar}
        className={`nav-toggle ${open ? 'is-hidden' : ''}`}
        aria-label="Abrir menú"
      >
        <Menu size={20} strokeWidth={1.9} />
      </button>

      {/* Fondo difuminado */}
      <div className={`nav-overlay ${open ? 'is-open' : ''}`} onClick={toggleSidebar} />

      {/* Sidebar */}
      <aside className={`nav-sidebar ${open ? 'is-open' : ''}`}>
        <div className="nav-head">
          <strong>Kiosco Botanico</strong>
          <button className="nav-close" onClick={toggleSidebar} aria-label="Cerrar menú">
            <X size={18} strokeWidth={1.9} />
          </button>
        </div>

        {LINKS.map(link => (
          <NavLink
            key={link.to}
            to={link.to}
            className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
            onClick={() => setOpen(false)}
          >
            <link.icon size={19} strokeWidth={1.8} aria-hidden="true" />
            {link.label}
          </NavLink>
        ))}

        <div className="nav-spacer" />
        <NavLink to="/" className="nav-link nav-exit" onClick={() => setOpen(false)}>
          <Globe size={19} strokeWidth={1.8} aria-hidden="true" />
          Ver web pública
        </NavLink>
      </aside>
    </>
  );
};

export default Navbar;
