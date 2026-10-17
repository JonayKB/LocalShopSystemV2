import React, { useContext, useState } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import {
  Boxes,
  FileDown,
  Globe,
  LineChart,
  LogOut,
  Menu,
  Package,
  Receipt,
  ShoppingBag,
  Wallet,
} from 'lucide-react';
import { MainContext } from './MainContextProvider';

const GROUPS = [
  {
    label: 'Día a día',
    links: [
      { to: 'home', label: 'Vender', icon: ShoppingBag },
      { to: 'trades', label: 'Ventas', icon: Receipt },
      { to: 'today-cash', label: 'Caja del día', icon: Wallet },
    ],
  },
  {
    label: 'Gestión',
    links: [
      { to: 'items', label: 'Productos', icon: Package },
      { to: 'stock', label: 'Stock', icon: Boxes },
    ],
  },
  {
    label: 'Análisis',
    links: [
      { to: 'graphs', label: 'Gráficos', icon: LineChart },
      { to: 'export', label: 'Exportar', icon: FileDown },
    ],
  },
];

const Navbar: React.FC = () => {
  const [open, setOpen] = useState(false);
  const { setToken } = useContext(MainContext);
  const navigate = useNavigate();
  const close = () => setOpen(false);

  const logout = () => {
    try {
      localStorage.removeItem('token');
    } catch {
      /* sin almacenamiento disponible */
    }
    setToken(null);
    close();
    navigate('/admin/login');
  };

  return (
    <>
      <div className="admin-topbar">
        <button className="btn-icon" onClick={() => setOpen(true)} aria-label="Abrir menú">
          <Menu size={20} strokeWidth={1.9} />
        </button>
        <strong>Kiosco Botanico</strong>
      </div>

      <div className={`admin-scrim ${open ? 'is-open' : ''}`} onClick={close} />

      <aside className={`admin-sidebar ${open ? 'is-open' : ''}`}>
        <div className="admin-brand">
          <img src="/logo192.png" alt="" />
          <div>
            <strong>Kiosco Botanico</strong>
            <small>Panel</small>
          </div>
        </div>

        {GROUPS.map(group => (
          <React.Fragment key={group.label}>
            <div className="admin-nav-label">{group.label}</div>
            {group.links.map(link => (
              <NavLink
                key={link.to}
                to={link.to}
                className={({ isActive }) => `admin-link ${isActive ? 'active' : ''}`}
                onClick={close}
              >
                <link.icon size={19} strokeWidth={1.8} aria-hidden="true" />
                {link.label}
              </NavLink>
            ))}
          </React.Fragment>
        ))}

        <div className="admin-sidebar-foot">
          <NavLink to="/" className="admin-link" onClick={close}>
            <Globe size={19} strokeWidth={1.8} aria-hidden="true" />
            Ver web pública
          </NavLink>
          <button className="admin-link" onClick={logout}>
            <LogOut size={19} strokeWidth={1.8} aria-hidden="true" />
            Cerrar sesión
          </button>
        </div>
      </aside>
    </>
  );
};

export default Navbar;
