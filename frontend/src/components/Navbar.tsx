import React, { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useCart } from '../context/CartContext';
import { ShoppingBag, User, LogOut, LogIn, Menu, X } from 'lucide-react';

export const Navbar: React.FC = () => {
  const { user, logout } = useAuth();
  const { cartCount, clearCart } = useCart();
  const navigate = useNavigate();
  const location = useLocation();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const handleLogout = async () => {
    await logout();
    clearCart();
    setMobileMenuOpen(false);
    navigate('/');
  };

  const isActive = (path: string) => location.pathname === path;

  const linkStyle = (path: string) => ({
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
    fontWeight: isActive(path) ? 600 : 400,
    color: isActive(path) ? 'var(--accent)' : 'var(--text-secondary)',
    transition: 'color var(--transition-fast)',
    borderBottom: isActive(path) ? '2px solid var(--accent)' : '2px solid transparent',
    padding: '8px 0',
    fontSize: '0.875rem',
    textTransform: 'uppercase' as const,
    letterSpacing: '0.05em'
  });

  return (
    <header style={{
      borderBottom: '1px solid var(--border)',
      backgroundColor: 'var(--bg-card)',
      position: 'sticky',
      top: 0,
      zIndex: 100,
      transition: 'background-color var(--transition-normal), border-color var(--transition-normal)'
    }}>
      <div className="container flex-between" style={{ height: '80px' }}>
        {/* Brand Logo */}
        <Link to="/" style={{
          display: 'flex',
          alignItems: 'center',
          gap: '8px',
          fontFamily: 'var(--font-title)',
          fontSize: '1.25rem',
          fontWeight: 600,
          letterSpacing: '0.1em',
          color: 'var(--text-primary)',
          textTransform: 'uppercase'
        }}>
          <ShoppingBag size={20} strokeWidth={1.5} style={{ color: 'var(--accent)' }} />
          <span>THREAD & Co.</span>
        </Link>

        {/* Desktop Navigation */}
        <nav style={{ display: 'flex', alignItems: 'center', gap: '32px' }} className="desktop-nav">
          <Link to="/catalog" style={linkStyle('/catalog')}>
            <span>Catalog</span>
          </Link>

          {user && (
            <Link to="/addresses" style={linkStyle('/addresses')}>
              <span>Addresses</span>
            </Link>
          )}

          {user && (
            <Link to="/orders" style={linkStyle('/orders')}>
              <span>Orders</span>
            </Link>
          )}

          <Link to="/cart" style={{
            ...linkStyle('/cart'),
            position: 'relative'
          }}>
            <span>Bag</span>
            {cartCount > 0 && (
              <span className="flex-center" style={{
                position: 'absolute',
                top: '-4px',
                right: '-16px',
                backgroundColor: 'var(--accent)',
                color: 'white',
                fontSize: '0.65rem',
                fontWeight: 700,
                width: '16px',
                height: '16px',
                borderRadius: '50%',
                lineHeight: 1
              }}>
                {cartCount}
              </span>
            )}
          </Link>

          {user ? (
            <div style={{ display: 'flex', alignItems: 'center', gap: '24px' }}>
              <Link to="/profile" style={linkStyle('/profile')}>
                <User size={16} strokeWidth={1.5} style={{ marginRight: '-2px' }} />
                <span>{user.firstName}</span>
              </Link>
              <button onClick={handleLogout} className="btn btn-secondary" style={{
                padding: '8px 16px',
                fontSize: '0.75rem'
              }}>
                <LogOut size={12} />
                <span>Sign Out</span>
              </button>
            </div>
          ) : (
            <Link to="/login" className="btn btn-primary" style={{
              padding: '8px 16px',
              fontSize: '0.75rem'
            }}>
              <LogIn size={12} />
              <span>Sign In</span>
            </Link>
          )}
        </nav>

        {/* Mobile Nav Toggle */}
        <button
          onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
          aria-expanded={mobileMenuOpen}
          aria-label="Toggle navigation menu"
          className="mobile-nav-toggle"
          style={{
            display: 'none',
            background: 'none',
            border: 'none',
            cursor: 'pointer',
            color: 'var(--text-primary)',
            padding: '8px'
          }}
        >
          {mobileMenuOpen ? <X size={24} /> : <Menu size={24} />}
        </button>
      </div>

      {/* Mobile Drawer */}
      {mobileMenuOpen && (
        <div style={{
          position: 'absolute',
          top: '80px',
          left: 0,
          right: 0,
          backgroundColor: 'var(--bg-card)',
          borderBottom: '1px solid var(--border)',
          padding: '24px',
          display: 'flex',
          flexDirection: 'column',
          gap: '20px',
          zIndex: 99
        }} className="mobile-drawer">
          <Link to="/catalog" style={linkStyle('/catalog')} onClick={() => setMobileMenuOpen(false)}>
            <span>Catalog</span>
          </Link>
          {user && (
            <Link to="/addresses" style={linkStyle('/addresses')} onClick={() => setMobileMenuOpen(false)}>
              <span>Addresses</span>
            </Link>
          )}
          {user && (
            <Link to="/orders" style={linkStyle('/orders')} onClick={() => setMobileMenuOpen(false)}>
              <span>Orders</span>
            </Link>
          )}
          <Link to="/cart" style={linkStyle('/cart')} onClick={() => setMobileMenuOpen(false)}>
            <span>Bag ({cartCount})</span>
          </Link>
          {user ? (
            <>
              <Link to="/profile" style={linkStyle('/profile')} onClick={() => setMobileMenuOpen(false)}>
                <User size={16} strokeWidth={1.5} />
                <span>{user.firstName}</span>
              </Link>
              <button onClick={handleLogout} className="btn btn-secondary" style={{
                justifyContent: 'center',
                width: '100%'
              }}>
                <LogOut size={14} />
                <span>Sign Out</span>
              </button>
            </>
          ) : (
            <Link to="/login" className="btn btn-primary" style={{ justifyContent: 'center' }} onClick={() => setMobileMenuOpen(false)}>
              <LogIn size={14} />
              <span>Sign In</span>
            </Link>
          )}
        </div>
      )}

      {/* Responsive CSS styles injected dynamically */}
      <style>{`
        @media (max-width: 768px) {
          .desktop-nav {
            display: none !important;
          }
          .mobile-nav-toggle {
            display: block !important;
          }
        }
      `}</style>
    </header>
  );
};
