import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { useCart } from '../context/CartContext';
import { Plus, Minus, ShoppingBag, AlertCircle, ArrowRight, Compass } from 'lucide-react';
import { EditorialMedia } from '../components/EditorialMedia';

export const Cart: React.FC = () => {
  const { cart, isLoading, error, updateQty, removeItem } = useCart();
  const [updatingItemId, setUpdatingItemId] = useState<number | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  const handleQuantityChange = async (cartItemId: number, currentQty: number, delta: number) => {
    const newQty = currentQty + delta;
    if (newQty < 1) return;

    setUpdatingItemId(cartItemId);
    setActionError(null);
    try {
      await updateQty(cartItemId, newQty);
    } catch (err: any) {
      setActionError(err.message || 'Failed to update quantity. Stock may be insufficient.');
    } finally {
      setUpdatingItemId(null);
    }
  };

  const handleRemoveItem = async (cartItemId: number) => {
    setUpdatingItemId(cartItemId);
    setActionError(null);
    try {
      await removeItem(cartItemId);
    } catch (err: any) {
      setActionError(err.message || 'Failed to remove item.');
    } finally {
      setUpdatingItemId(null);
    }
  };

  if (isLoading && !cart) {
    return (
      <div className="flex-center" style={{ height: '400px' }}>
        <div style={{
          border: '4px solid var(--border)',
          borderTopColor: 'var(--accent)',
          borderRadius: 'var(--radius-full)',
          width: '40px',
          height: '40px',
          animation: 'spin 1s linear infinite'
        }} />
        <style>{`
          @keyframes spin {
            0% { transform: rotate(0deg); }
            100% { transform: rotate(360deg); }
          }
        `}</style>
      </div>
    );
  }

  const hasUnavailableItems = cart?.items.some(item => !item.available) || false;

  return (
    <div className="container animate-fade-in" style={{ padding: '40px 20px 80px 20px' }}>
      <div style={{ marginBottom: '30px' }}>
        <h1 className="title-medium" style={{ marginBottom: '8px' }}>Shopping Bag</h1>
        <p style={{ color: 'var(--text-secondary)' }}>Review your items before checking out.</p>
      </div>

      {/* Errors display */}
      {(error || actionError) && (
        <div className="alert alert-error animate-fade-in" style={{ marginBottom: '30px' }}>
          <AlertCircle size={18} />
          <span>{actionError || error}</span>
        </div>
      )}

      {!cart || cart.items.length === 0 ? (
        /* Empty State */
        <div style={{
          textAlign: 'center',
          padding: '80px 20px',
          backgroundColor: 'var(--bg-card)',
          borderRadius: 'var(--radius-lg)',
          border: '1px solid var(--border)'
        }}>
          <ShoppingBag size={48} style={{ color: 'var(--text-muted)', marginBottom: '16px' }} />
          <h3 className="title-small" style={{ marginBottom: '8px' }}>Your bag is empty</h3>
          <p style={{ color: 'var(--text-secondary)', marginBottom: '24px' }}>
            Looks like you haven't added anything to your bag yet.
          </p>
          <Link to="/catalog" className="btn btn-primary flex-center" style={{ display: 'inline-flex' }}>
            <Compass size={16} />
            <span>Go to Shop</span>
          </Link>
        </div>
      ) : (
        /* Cart Grid Layout */
        <div className="grid grid-3" style={{ gap: '60px', alignItems: 'start' }}>
          {/* Left Column: Items List */}
          <div style={{ gridColumn: 'span 2', display: 'flex', flexDirection: 'column', gap: '32px' }}>
            {cart.items.map((item) => (
              <div
                key={item.cartItemId}
                style={{
                  display: 'flex',
                  gap: '24px',
                  paddingBottom: '32px',
                  borderBottom: '1px solid var(--border)',
                  position: 'relative'
                }}
              >
                {/* Product Thumbnail */}
                <div style={{ width: '120px', flexShrink: 0 }}>
                  <EditorialMedia
                    src={item.imageUrl}
                    alt={item.productName}
                    label={item.productName}
                    style={{ height: '160px', border: 'none' }}
                  />
                </div>

                {/* Details info */}
                <div style={{ display: 'flex', flexDirection: 'column', justifyContent: 'space-between', flexGrow: 1, paddingTop: '8px' }}>
                  <div>
                    <div className="flex-between" style={{ alignItems: 'flex-start' }}>
                      <Link to={`/products/${item.productId}`} style={{ fontWeight: 400, fontFamily: 'var(--font-title)', fontSize: '1.5rem', letterSpacing: '0.02em' }}>
                        {item.productName}
                      </Link>
                      <button
                        onClick={() => handleRemoveItem(item.cartItemId)}
                        disabled={updatingItemId === item.cartItemId}
                        style={{
                          background: 'none',
                          border: 'none',
                          color: 'var(--text-muted)',
                          cursor: 'pointer',
                          padding: '4px',
                          display: 'flex',
                          alignItems: 'center',
                          gap: '6px',
                          textTransform: 'uppercase',
                          fontSize: '0.7rem',
                          letterSpacing: '0.1em',
                          fontWeight: 600
                        }}
                        onMouseEnter={(e) => (e.currentTarget.style.color = 'var(--text-primary)')}
                        onMouseLeave={(e) => (e.currentTarget.style.color = 'var(--text-muted)')}
                      >
                        Remove
                      </button>
                    </div>

                    <div style={{ display: 'flex', gap: '16px', marginTop: '12px' }}>
                      <span style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                        Color: <span style={{ color: 'var(--text-primary)' }}>{item.color}</span>
                      </span>
                      <span style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                        Size: <span style={{ color: 'var(--text-primary)' }}>{item.size}</span>
                      </span>
                    </div>

                    {/* Stock Warning Badge */}
                    {!item.available && (
                      <span style={{
                        display: 'inline-flex',
                        fontSize: '0.75rem',
                        fontWeight: 600,
                        backgroundColor: 'var(--error-bg)',
                        color: 'var(--error)',
                        padding: '4px 12px',
                        marginTop: '16px',
                        textTransform: 'uppercase',
                        letterSpacing: '0.1em'
                      }}>
                        Out of Stock
                      </span>
                    )}
                  </div>

                  {/* Quantity and Price section */}
                  <div className="flex-between" style={{ marginTop: '32px', alignItems: 'flex-end' }}>
                    {/* Quantity selectors */}
                    <div className="flex-center" style={{ gap: '12px' }}>
                      <span style={{ fontSize: '0.75rem', textTransform: 'uppercase', letterSpacing: '0.1em', color: 'var(--text-muted)' }}>QTY</span>
                      <div className="flex-center" style={{ borderBottom: '1px solid var(--text-primary)', paddingBottom: '4px', gap: '16px' }}>
                        <button
                          onClick={() => handleQuantityChange(item.cartItemId, item.quantity, -1)}
                          disabled={item.quantity <= 1 || updatingItemId === item.cartItemId}
                          style={{ background: 'none', border: 'none', cursor: 'pointer', padding: '0 4px', color: 'var(--text-primary)' }}
                        >
                          <Minus size={14} />
                        </button>
                        <span style={{ minWidth: '24px', textAlign: 'center', fontWeight: 400, fontSize: '1.1rem' }}>
                          {item.quantity}
                        </span>
                        <button
                          onClick={() => handleQuantityChange(item.cartItemId, item.quantity, 1)}
                          disabled={updatingItemId === item.cartItemId || !item.available}
                          style={{ background: 'none', border: 'none', cursor: 'pointer', padding: '0 4px', color: 'var(--text-primary)' }}
                        >
                          <Plus size={14} />
                        </button>
                      </div>
                    </div>

                    {/* Pricing */}
                    <div style={{ textAlign: 'right' }}>
                      <div style={{ fontSize: '0.85rem', color: 'var(--text-muted)', marginBottom: '4px' }}>
                        LKR {item.unitPrice.toFixed(2)} ea
                      </div>
                      <div style={{ fontWeight: 400, fontFamily: 'var(--font-title)', fontSize: '1.5rem' }}>
                        LKR {item.lineTotal.toFixed(2)}
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            ))}
          </div>

          {/* Right Column: Order Summary Card */}
          <div style={{ padding: '32px', backgroundColor: 'var(--bg-secondary)', border: '1px solid var(--border)' }}>
            <h2 style={{ fontFamily: 'var(--font-title)', fontSize: '1.8rem', fontWeight: 400, marginBottom: '32px', letterSpacing: '0.02em' }}>Order Summary</h2>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
              <div className="flex-between" style={{ fontSize: '1rem', color: 'var(--text-primary)' }}>
                <span>Subtotal <span style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>({cart.totalQuantity} items)</span></span>
                <span>LKR {cart.cartTotal.toFixed(2)}</span>
              </div>
              <div className="flex-between" style={{ fontSize: '1rem', color: 'var(--text-primary)' }}>
                <span>Shipping</span>
                <span style={{ fontStyle: 'italic', fontSize: '0.9rem', color: 'var(--text-muted)' }}>Calculated at checkout</span>
              </div>
            </div>

            <hr style={{ border: 'none', borderTop: '1px solid var(--border)', margin: '24px 0' }} />

            <div className="flex-between" style={{ fontWeight: 400, fontFamily: 'var(--font-title)', fontSize: '1.8rem', marginBottom: '32px' }}>
              <span>Total</span>
              <span>LKR {cart.cartTotal.toFixed(2)}</span>
            </div>

            {hasUnavailableItems && (
              <div className="alert alert-error" style={{ fontSize: '0.8rem', padding: '12px', marginBottom: '24px', borderRadius: '0' }}>
                Please remove unavailable items to check out.
              </div>
            )}

            {/* Proceed to checkout Link */}
            <Link
              to="/checkout"
              className="btn btn-primary flex-center"
              style={{
                width: '100%',
                padding: '20px',
                fontSize: '1.1rem',
                letterSpacing: '0.1em',
                textDecoration: 'none',
                justifyContent: 'center',
                gap: '12px',
                borderRadius: '0'
              }}
            >
              <span>Checkout Securely</span>
              <ArrowRight size={18} />
            </Link>
          </div>
        </div>
      )}
    </div>
  );
};
