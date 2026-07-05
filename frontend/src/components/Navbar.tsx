import React, { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useCart } from '../context/CartContext';
import { User, LogOut, LogIn, Menu, X } from 'lucide-react';

export const Navbar: React.FC = () => {
  const { user, logout, isAdmin } = useAuth();
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

  const linkClassName = (path: string) => 
    `nav-link-premium ${isActive(path) ? 'is-active' : ''}`;

  const linkStyle = (path: string) => ({
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
    fontWeight: isActive(path) ? 500 : 400,
    color: isActive(path) ? 'var(--text-primary)' : 'var(--text-secondary)',
    transition: 'color var(--transition-fast)',
    padding: '8px 0',
    fontSize: '0.8125rem',
    textTransform: 'uppercase' as const,
    letterSpacing: '0.08em'
  });

  return (
    <header className="premium-header">
      {/* Top Announcement Bar */}
      <div style={{ backgroundColor: 'var(--text-primary)', color: 'white', padding: '8px 24px', textAlign: 'center', fontSize: '0.75rem', fontWeight: 500, letterSpacing: '0.05em', textTransform: 'uppercase' }}>
        Complimentary Global Shipping on Orders Over LKR 100,000
      </div>
      <div className="container flex-between" style={{ height: '80px' }}>
        {/* Brand Logo */}
        <Link to="/" style={{
          display: 'flex',
          alignItems: 'center',
          gap: '12px',
          fontFamily: 'var(--font-title)',
          fontSize: '1.4rem',
          fontWeight: 700,
          letterSpacing: '0.15em',
          color: 'var(--text-primary)',
          textTransform: 'uppercase'
        }} aria-label="VÉLURE Home">
          <img src="/assets/velure_logo.jpg" alt="VÉLURE Logo" style={{ width: '32px', height: '32px', objectFit: 'cover', borderRadius: '4px' }} />
          <span>VÉLURE</span>
        </Link>

        {/* Desktop Navigation */}
        <nav style={{ display: 'flex', alignItems: 'center', gap: '32px' }} className="desktop-nav" aria-label="Primary Navigation">
          <Link to="/catalog" style={linkStyle('/catalog')} className={linkClassName('/catalog')}>
            <span>Catalog</span>
          </Link>

          {user && (
            <Link to="/addresses" style={linkStyle('/addresses')} className={linkClassName('/addresses')}>
              <span>Addresses</span>
            </Link>
          )}

          {user && (
            <Link to="/orders" style={linkStyle('/orders')} className={linkClassName('/orders')}>
              <span>Orders</span>
            </Link>
          )}

          <Link to="/cart" style={{
            ...linkStyle('/cart'),
            position: 'relative'
          }} className={linkClassName('/cart')}>
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
              }} aria-label={`${cartCount} items in bag`}>
                {cartCount}
              </span>
            )}
          </Link>

          {user ? (
            <div style={{ display: 'flex', alignItems: 'center', gap: '24px' }}>
              {isAdmin && (
                <Link to="/admin" style={{...linkStyle('/admin'), color: 'var(--accent)', fontWeight: 600}} className={linkClassName('/admin')}>
                  <span>Admin</span>
                </Link>
              )}
              <Link to="/profile" style={linkStyle('/profile')} className={linkClassName('/profile')}>
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
          <Link to="/catalog" style={linkStyle('/catalog')} className={linkClassName('/catalog')} onClick={() => setMobileMenuOpen(false)}>
            <span>Catalog</span>
          </Link>
          {user && (
            <Link to="/addresses" style={linkStyle('/addresses')} className={linkClassName('/addresses')} onClick={() => setMobileMenuOpen(false)}>
              <span>Addresses</span>
            </Link>
          )}
          {user && (
            <Link to="/orders" style={linkStyle('/orders')} className={linkClassName('/orders')} onClick={() => setMobileMenuOpen(false)}>
              <span>Orders</span>
            </Link>
          )}
          <Link to="/cart" style={linkStyle('/cart')} className={linkClassName('/cart')} onClick={() => setMobileMenuOpen(false)}>
            <span>Bag ({cartCount})</span>
          </Link>
          {user ? (
            <>
              {isAdmin && (
                <Link to="/admin" style={{...linkStyle('/admin'), color: 'var(--accent)', fontWeight: 600}} className={linkClassName('/admin')} onClick={() => setMobileMenuOpen(false)}>
                  <span>Admin Panel</span>
                </Link>
              )}
              <Link to="/profile" style={linkStyle('/profile')} className={linkClassName('/profile')} onClick={() => setMobileMenuOpen(false)}>
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

