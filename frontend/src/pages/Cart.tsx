import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { useCart } from '../context/CartContext';
import { Trash2, Plus, Minus, ShoppingBag, AlertCircle, ArrowRight, Compass } from 'lucide-react';

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
        <div className="grid grid-3" style={{ gap: '40px', alignItems: 'start' }}>
          {/* Left Column: Items List */}
          <div style={{ gridColumn: 'span 2', display: 'flex', flexDirection: 'column', gap: '20px' }}>
            {cart.items.map((item) => (
              <div
                key={item.cartItemId}
                style={{
                  display: 'flex',
                  gap: '20px',
                  backgroundColor: 'var(--bg-card)',
                  padding: '20px',
                  borderRadius: 'var(--radius-lg)',
                  border: item.available ? '1px solid var(--border)' : '1px solid var(--error)',
                  position: 'relative'
                }}
              >
                {/* Product Thumbnail */}
                <div style={{
                  width: '100px',
                  height: '120px',
                  backgroundColor: 'var(--bg-secondary)',
                  backgroundImage: item.imageUrl ? `url(${item.imageUrl})` : 'none',
                  backgroundSize: 'cover',
                  backgroundPosition: 'center',
                  borderRadius: 'var(--radius-md)',
                  flexShrink: 0,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  color: 'var(--text-muted)'
                }}>
                  {!item.imageUrl && <ShoppingBag size={24} />}
                </div>

                {/* Details info */}
                <div style={{ display: 'flex', flexDirection: 'column', justifyContent: 'space-between', flexGrow: 1 }}>
                  <div>
                    <div className="flex-between">
                      <Link to={`/products/${item.productId}`} style={{ fontWeight: 600, fontSize: '1.05rem' }}>
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
                          padding: '4px'
                        }}
                        onMouseEnter={(e) => (e.currentTarget.style.color = 'var(--error)')}
                        onMouseLeave={(e) => (e.currentTarget.style.color = 'var(--text-muted)')}
                      >
                        <Trash2 size={18} />
                      </button>
                    </div>

                    <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)', display: 'block', marginTop: '4px' }}>
                      Size: {item.size} | Color: {item.color}
                    </span>

                    {/* Stock Warning Badge */}
                    {!item.available && (
                      <span style={{
                        display: 'inline-flex',
                        fontSize: '0.75rem',
                        fontWeight: 600,
                        backgroundColor: 'var(--error-bg)',
                        color: 'var(--error)',
                        padding: '2px 8px',
                        borderRadius: 'var(--radius-sm)',
                        marginTop: '8px'
                      }}>
                        Unavailable / Out of Stock
                      </span>
                    )}
                  </div>

                  {/* Quantity and Price section */}
                  <div className="flex-between" style={{ marginTop: '16px' }}>
                    {/* Quantity selectors */}
                    <div className="flex-center" style={{
                      border: '1px solid var(--border)',
                      borderRadius: 'var(--radius-md)',
                      backgroundColor: 'var(--bg-primary)',
                      padding: '2px'
                    }}>
                      <button
                        onClick={() => handleQuantityChange(item.cartItemId, item.quantity, -1)}
                        disabled={item.quantity <= 1 || updatingItemId === item.cartItemId}
                        className="btn btn-secondary"
                        style={{
                          padding: '4px 8px',
                          border: 'none',
                          borderRadius: 'var(--radius-sm)'
                        }}
                      >
                        <Minus size={14} />
                      </button>
                      <span style={{ minWidth: '32px', textAlign: 'center', fontWeight: 600, fontSize: '0.925rem' }}>
                        {item.quantity}
                      </span>
                      <button
                        onClick={() => handleQuantityChange(item.cartItemId, item.quantity, 1)}
                        disabled={updatingItemId === item.cartItemId || !item.available}
                        className="btn btn-secondary"
                        style={{
                          padding: '4px 8px',
                          border: 'none',
                          borderRadius: 'var(--radius-sm)'
                        }}
                      >
                        <Plus size={14} />
                      </button>
                    </div>

                    {/* Pricing */}
                    <div style={{ textAlign: 'right' }}>
                      <span style={{ fontSize: '0.875rem', color: 'var(--text-muted)', marginRight: '8px' }}>
                        ${item.unitPrice.toFixed(2)} ea
                      </span>
                      <span style={{ fontWeight: 700, fontSize: '1.125rem' }}>
                        ${item.lineTotal.toFixed(2)}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            ))}
          </div>

          {/* Right Column: Order Summary Card */}
          <div className="card" style={{ padding: '24px', display: 'flex', flexDirection: 'column', gap: '20px' }}>
            <h2 className="title-small" style={{ fontWeight: 700 }}>Summary</h2>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
              <div className="flex-between" style={{ fontSize: '0.925rem', color: 'var(--text-secondary)' }}>
                <span>Subtotal ({cart.totalQuantity} items)</span>
                <span>${cart.cartTotal.toFixed(2)}</span>
              </div>
              <div className="flex-between" style={{ fontSize: '0.925rem', color: 'var(--text-secondary)' }}>
                <span>Shipping</span>
                <span style={{ fontStyle: 'italic', fontSize: '0.85rem' }}>Computed at checkout</span>
              </div>
            </div>

            <hr style={{ border: 'none', borderTop: '1px solid var(--border)' }} />

            <div className="flex-between" style={{ fontWeight: 700, fontSize: '1.125rem' }}>
              <span>Total</span>
              <span style={{ color: 'var(--accent)' }}>${cart.cartTotal.toFixed(2)}</span>
            </div>

            {hasUnavailableItems && (
              <div className="alert alert-error" style={{ fontSize: '0.8rem', padding: '8px 12px', marginBottom: 0 }}>
                Please remove unavailable items to check out.
              </div>
            )}

            {/* Proceed to checkout Link */}
            <Link
              to="/checkout"
              className="btn btn-primary flex-center"
              style={{
                width: '100%',
                padding: '14px',
                fontSize: '1rem',
                fontWeight: 600,
                textDecoration: 'none',
                justifyContent: 'center',
                gap: '8px'
              }}
            >
              <span>Proceed to Checkout</span>
              <ArrowRight size={16} />
            </Link>
            <p style={{
              fontSize: '0.75rem',
              color: 'var(--text-muted)',
              textAlign: 'center',
              lineHeight: 1.4
            }}>
              Checkout integration is deferred for the next development phase.
            </p>
          </div>
        </div>
      )}
    </div>
  );
};
