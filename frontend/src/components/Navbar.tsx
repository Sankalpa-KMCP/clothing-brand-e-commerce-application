import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useCart } from '../context/CartContext';
import { ShoppingBag, User, LogOut, LogIn, Compass, MapPin, Package } from 'lucide-react';

export const Navbar: React.FC = () => {
  const { user, logout } = useAuth();
  const { cartCount, clearCart } = useCart();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    clearCart();
    navigate('/');
  };

  return (
    <header style={{
      borderBottom: '1px solid var(--border)',
      backgroundColor: 'var(--bg-card)',
      position: 'sticky',
      top: 0,
      zIndex: 100,
      transition: 'background-color var(--transition-normal), border-color var(--transition-normal)'
    }}>
      <div className="container flex-between" style={{ height: '70px' }}>
        {/* Brand Logo */}
        <Link to="/" style={{
          display: 'flex',
          alignItems: 'center',
          gap: '8px',
          fontFamily: 'var(--font-title)',
          fontSize: '1.5rem',
          fontWeight: 800,
          color: 'var(--accent)'
        }}>
          <ShoppingBag size={24} />
          <span>THREAD & Co.</span>
        </Link>

        {/* Navigation Items */}
        <nav style={{ display: 'flex', alignItems: 'center', gap: '24px' }}>
          <Link to="/catalog" className="flex-center" style={{
            gap: '6px',
            fontWeight: 500,
            color: 'var(--text-secondary)',
            transition: 'color var(--transition-fast)'
          }} onMouseEnter={(e) => (e.currentTarget.style.color = 'var(--accent)')}
             onMouseLeave={(e) => (e.currentTarget.style.color = 'var(--text-secondary)')}>
            <Compass size={18} />
            <span>Catalog</span>
          </Link>

          {user && (
            <Link to="/addresses" className="flex-center" style={{
              gap: '6px',
              fontWeight: 500,
              color: 'var(--text-secondary)',
              transition: 'color var(--transition-fast)'
            }} onMouseEnter={(e) => (e.currentTarget.style.color = 'var(--accent)')}
               onMouseLeave={(e) => (e.currentTarget.style.color = 'var(--text-secondary)')}>
              <MapPin size={18} />
              <span>Addresses</span>
            </Link>
          )}

          {user && (
            <Link to="/orders" className="flex-center" style={{
              gap: '6px',
              fontWeight: 500,
              color: 'var(--text-secondary)',
              transition: 'color var(--transition-fast)'
            }} onMouseEnter={(e) => (e.currentTarget.style.color = 'var(--accent)')}
               onMouseLeave={(e) => (e.currentTarget.style.color = 'var(--text-secondary)')}>
              <Package size={18} />
              <span>Orders</span>
            </Link>
          )}

          <Link to="/cart" className="flex-center" style={{
            gap: '6px',
            fontWeight: 500,
            color: 'var(--text-secondary)',
            transition: 'color var(--transition-fast)',
            position: 'relative'
          }} onMouseEnter={(e) => (e.currentTarget.style.color = 'var(--accent)')}
             onMouseLeave={(e) => (e.currentTarget.style.color = 'var(--text-secondary)')}>
            <ShoppingBag size={18} />
            <span>Bag</span>
            {cartCount > 0 && (
              <span className="flex-center" style={{
                position: 'absolute',
                top: '-8px',
                right: '-12px',
                backgroundColor: 'var(--accent)',
                color: 'white',
                fontSize: '0.7rem',
                fontWeight: 700,
                width: '18px',
                height: '18px',
                borderRadius: '50%',
                lineHeight: 1
              }}>
                {cartCount}
              </span>
            )}
          </Link>

          {user ? (
            <div style={{ display: 'flex', alignItems: 'center', gap: '20px' }}>
              <Link to="/profile" className="flex-center" style={{
                gap: '6px',
                fontWeight: 500,
                color: 'var(--text-secondary)',
                transition: 'color var(--transition-fast)'
              }} onMouseEnter={(e) => (e.currentTarget.style.color = 'var(--accent)')}
                 onMouseLeave={(e) => (e.currentTarget.style.color = 'var(--text-secondary)')}>
                <User size={18} />
                <span>{user.firstName}</span>
              </Link>
              <button onClick={handleLogout} className="btn btn-secondary flex-center" style={{
                padding: '6px 12px',
                fontSize: '0.875rem',
                gap: '6px'
              }}>
                <LogOut size={14} />
                <span>Sign Out</span>
              </button>
            </div>
          ) : (
            <Link to="/login" className="btn btn-primary flex-center" style={{
              padding: '8px 16px',
              fontSize: '0.875rem',
              gap: '6px'
            }}>
              <LogIn size={14} />
              <span>Sign In</span>
            </Link>
          )}
        </nav>
      </div>
    </header>
  );
};
