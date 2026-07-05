import React from 'react';
import { Link, Outlet, useLocation } from 'react-router-dom';
import { LayoutDashboard, Package, ShoppingCart } from 'lucide-react';

export const AdminLayout: React.FC = () => {
  const location = useLocation();

  const isActive = (path: string) => {
    if (path === '/admin' && location.pathname === '/admin') return true;
    if (path !== '/admin' && location.pathname.startsWith(path)) return true;
    return false;
  };

  const linkStyle = (path: string) => ({
    display: 'flex',
    alignItems: 'center',
    gap: '12px',
    padding: '12px 16px',
    borderRadius: '8px',
    color: isActive(path) ? 'var(--bg-primary)' : 'var(--text-secondary)',
    backgroundColor: isActive(path) ? 'var(--text-primary)' : 'transparent',
    fontWeight: isActive(path) ? 500 : 400,
    textDecoration: 'none',
    transition: 'all var(--transition-fast)',
  });

  return (
    <div style={{ display: 'flex', minHeight: 'calc(100vh - 80px)', backgroundColor: 'var(--bg-secondary)' }}>
      {/* Sidebar */}
      <aside style={{
        width: '260px',
        backgroundColor: 'var(--bg-card)',
        borderRight: '1px solid var(--border)',
        padding: '24px 16px',
        display: 'flex',
        flexDirection: 'column',
        gap: '8px'
      }}>
        <div style={{ padding: '0 16px', marginBottom: '24px' }}>
          <h2 style={{ fontSize: '1rem', textTransform: 'uppercase', letterSpacing: '0.1em', fontWeight: 600 }}>Admin Panel</h2>
        </div>

        <Link to="/admin" style={linkStyle('/admin')}>
          <LayoutDashboard size={18} />
          <span>Dashboard</span>
        </Link>
        <Link to="/admin/products" style={linkStyle('/admin/products')}>
          <Package size={18} />
          <span>Products</span>
        </Link>
        <Link to="/admin/orders" style={linkStyle('/admin/orders')}>
          <ShoppingCart size={18} />
          <span>Orders</span>
        </Link>
      </aside>

      {/* Main Content Area */}
      <main style={{ flex: 1, padding: '40px', overflowY: 'auto' }}>
        <div style={{ maxWidth: '1200px', margin: '0 auto' }}>
          <Outlet />
        </div>
      </main>
    </div>
  );
};
