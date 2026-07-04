import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useCart } from '../context/CartContext';
import { addressApi, type AddressResponse } from '../api/addressApi';
import { orderApi } from '../api/orderApi';
import { AlertCircle, ShoppingBag, ArrowRight, CreditCard, Clock } from 'lucide-react';
import { EditorialMedia } from '../components/EditorialMedia';

const STRIPE_CHECKOUT_ENABLED = import.meta.env.VITE_STRIPE_CHECKOUT_ENABLED === 'true';

export const Checkout: React.FC = () => {
  const { cart, isLoading: cartLoading, fetchCart, clearCart } = useCart();
  const navigate = useNavigate();
  
  const [addresses, setAddresses] = useState<AddressResponse[]>([]);
  const [selectedAddressId, setSelectedAddressId] = useState<number | null>(null);
  
  const [loadingAddresses, setLoadingAddresses] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchCart(); // Ensure cart is up-to-date
    fetchAddresses();
  }, [fetchCart]);

  const fetchAddresses = async () => {
    try {
      setLoadingAddresses(true);
      const data = await addressApi.getAddresses();
      setAddresses(data);
      
      const defaultAddr = data.find(a => a.isDefault);
      if (defaultAddr) {
        setSelectedAddressId(defaultAddr.id);
      } else if (data.length > 0) {
        setSelectedAddressId(data[0].id);
      }
    } catch (err: any) {
      console.error('Failed to load addresses:', err);
      setError('Failed to load your addresses. Please try again.');
    } finally {
      setLoadingAddresses(false);
    }
  };

  const handleCheckout = async () => {
    if (submitting) {
      return;
    }

    if (!cart || cart.items.length === 0) {
      setError('Your cart is empty.');
      return;
    }
    
    if (!selectedAddressId) {
      setError('Please select a delivery address.');
      return;
    }

    try {
      setSubmitting(true);
      setError(null);

      if (STRIPE_CHECKOUT_ENABLED) {
        const checkoutSession = await orderApi.reserveCheckoutSession(selectedAddressId);
        window.location.assign(checkoutSession.stripeCheckoutUrl);
        return;
      }

      const orderResponse = await orderApi.checkout(selectedAddressId);
      clearCart();
      navigate(`/orders/${orderResponse.id}`);
    } catch (err: any) {
      console.error('Checkout failed:', err);
      setError(err.message || 'Checkout failed. Please try again later.');
      setSubmitting(false);
    }
  };

  if (cartLoading || loadingAddresses) {
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

  if (!cart || cart.items.length === 0) {
    return (
      <div className="container animate-fade-in" style={{ padding: '80px 20px', textAlign: 'center' }}>
        <div style={{
          maxWidth: '500px',
          margin: '0 auto',
          padding: '60px 40px',
          backgroundColor: 'var(--bg-card)',
          borderRadius: 'var(--radius-lg)',
          border: '1px solid var(--border)'
        }}>
          <ShoppingBag size={48} style={{ color: 'var(--text-muted)', marginBottom: '20px' }} />
          <h2 className="title-small" style={{ marginBottom: '8px' }}>Your bag is empty</h2>
          <p style={{ color: 'var(--text-secondary)', marginBottom: '24px' }}>
            You need items in your bag to checkout.
          </p>
          <button className="btn btn-primary" onClick={() => navigate('/catalog')}>
            Continue Shopping
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="container animate-fade-in" style={{ padding: '40px 20px 80px 20px' }}>
      <div style={{ marginBottom: '30px' }}>
        <h1 className="title-medium" style={{ marginBottom: '8px' }}>Checkout</h1>
        <p style={{ color: 'var(--text-secondary)' }}>Review your details and place your order.</p>
      </div>

      {error && (
        <div className="alert alert-error animate-fade-in" style={{ marginBottom: '30px' }}>
          <AlertCircle size={20} />
          <span>{error}</span>
        </div>
      )}

      <div className="grid grid-3" style={{ gap: '60px', alignItems: 'start' }}>
        
        {/* Left Column: Address Selection & Payment */}
        <div style={{ gridColumn: 'span 2', display: 'flex', flexDirection: 'column', gap: '48px' }}>
          <div>
            <h2 style={{ fontFamily: 'var(--font-title)', fontSize: '1.8rem', fontWeight: 400, marginBottom: '32px', letterSpacing: '0.02em', display: 'flex', alignItems: 'center', gap: '12px' }}>
              <span style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', width: '32px', height: '32px', border: '1px solid var(--text-primary)', borderRadius: '50%', fontSize: '1rem' }}>1</span>
              Delivery Details
            </h2>
            
            {addresses.length === 0 ? (
              <div style={{ padding: '32px', border: '1px solid var(--border)', backgroundColor: 'var(--bg-card)' }}>
                <p style={{ color: 'var(--text-secondary)', marginBottom: '24px', fontSize: '1rem', letterSpacing: '0.02em' }}>
                  You don't have any saved addresses. Please add one to continue.
                </p>
                <button className="btn btn-primary" onClick={() => navigate('/addresses')} style={{ borderRadius: '0' }}>
                  Add New Address
                </button>
              </div>
            ) : (
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: '24px' }}>
                {addresses.map(address => {
                  const isSelected = selectedAddressId === address.id;
                  return (
                    <label
                      key={address.id}
                      style={{
                        display: 'flex',
                        alignItems: 'flex-start',
                        gap: '16px',
                        padding: '24px',
                        border: isSelected ? '1px solid var(--text-primary)' : '1px solid var(--border)',
                        backgroundColor: isSelected ? 'var(--bg-secondary)' : 'var(--bg-card)',
                        cursor: 'pointer',
                        transition: 'all var(--transition-fast)'
                      }}
                    >
                      <input 
                        type="radio" 
                        name="address" 
                        value={address.id}
                        checked={isSelected}
                        onChange={() => setSelectedAddressId(address.id)}
                        style={{ marginTop: '4px', accentColor: 'var(--text-primary)', transform: 'scale(1.2)' }}
                      />
                      <div style={{ flexGrow: 1 }}>
                        <div style={{ fontWeight: 600, fontSize: '1.05rem', marginBottom: '8px', display: 'flex', justifyContent: 'space-between' }}>
                          <span>{address.recipientName}</span>
                          {address.label && <span style={{ color: 'var(--text-secondary)', fontWeight: 400, fontSize: '0.85rem' }}>{address.label}</span>}
                        </div>
                        <div style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', lineHeight: 1.6 }}>
                          {address.addressLine1}
                          {address.addressLine2 && <><br />{address.addressLine2}</>}
                          <br />
                          {address.city}, {address.region} {address.postalCode}
                          <br />
                          {address.country}
                          <br />
                          <span style={{ marginTop: '12px', display: 'block', color: 'var(--text-muted)', fontSize: '0.85rem', letterSpacing: '0.05em' }}>{address.phoneNumber}</span>
                        </div>
                      </div>
                    </label>
                  );
                })}
              </div>
            )}
          </div>
          
          <hr style={{ border: 'none', borderTop: '1px solid var(--border)' }} />

          <div>
             <h2 style={{ fontFamily: 'var(--font-title)', fontSize: '1.8rem', fontWeight: 400, marginBottom: '32px', letterSpacing: '0.02em', display: 'flex', alignItems: 'center', gap: '12px' }}>
               <span style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', width: '32px', height: '32px', border: '1px solid var(--text-primary)', borderRadius: '50%', fontSize: '1rem' }}>2</span>
               Payment Method
             </h2>
             <div style={{ padding: '32px', border: '1px solid var(--border)', backgroundColor: 'var(--bg-card)' }}>
               {STRIPE_CHECKOUT_ENABLED ? (
                 <div style={{ display: 'flex', flexDirection: 'column', gap: '16px', color: 'var(--text-secondary)' }}>
                   <p style={{ fontSize: '1rem', lineHeight: 1.6, display: 'flex', alignItems: 'center', gap: '12px' }}>
                     <CreditCard size={24} style={{ color: 'var(--text-primary)' }} />
                     You will continue to Stripe Checkout to pay securely. Your items are reserved temporarily while payment is pending.
                   </p>
                   <p style={{ display: 'flex', alignItems: 'center', gap: '12px', fontSize: '0.9rem', color: 'var(--text-muted)', marginTop: '8px' }}>
                     <Clock size={18} />
                     Final order confirmation happens after payment is verified.
                   </p>
                 </div>
               ) : (
                 <p style={{ color: 'var(--text-secondary)', fontSize: '1rem', lineHeight: 1.6, display: 'flex', alignItems: 'center', gap: '12px' }}>
                   <CreditCard size={24} style={{ color: 'var(--text-primary)' }} />
                   Payment processing is deferred for this phase. Placing your order will secure your items and update our systems.
                 </p>
               )}
             </div>
          </div>
        </div>

        {/* Right Column: Order Summary Card */}
        <div style={{ padding: '32px', backgroundColor: 'var(--bg-secondary)', border: '1px solid var(--border)', position: 'sticky', top: '40px' }}>
          <h2 style={{ fontFamily: 'var(--font-title)', fontSize: '1.8rem', fontWeight: 400, marginBottom: '32px', letterSpacing: '0.02em' }}>Order Summary</h2>
          
          <div style={{ display: 'flex', flexDirection: 'column', gap: '24px', marginBottom: '32px', maxHeight: '400px', overflowY: 'auto', paddingRight: '8px' }}>
            {cart.items.map(item => (
              <div key={item.cartItemId} style={{ display: 'flex', gap: '20px', alignItems: 'flex-start' }}>
                <div style={{ width: '80px', flexShrink: 0 }}>
                  <EditorialMedia
                    src={item.imageUrl}
                    alt={item.productName}
                    label={item.productName}
                    style={{ height: '100px', border: 'none' }}
                  />
                </div>
                <div style={{ flex: 1 }}>
                  <div style={{ fontWeight: 400, fontFamily: 'var(--font-title)', fontSize: '1.2rem', marginBottom: '8px', letterSpacing: '0.02em' }}>{item.productName}</div>
                  <div style={{ color: 'var(--text-secondary)', fontSize: '0.8rem', marginBottom: '12px', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                    {item.color} / {item.size} / Qty: {item.quantity}
                  </div>
                  <div style={{ fontWeight: 400, fontSize: '1.1rem' }}>LKR {item.lineTotal.toFixed(2)}</div>
                </div>
              </div>
            ))}
          </div>

          <div style={{ borderTop: '1px solid var(--border)', paddingTop: '24px', marginBottom: '32px' }}>
            <div className="flex-between" style={{ marginBottom: '16px', color: 'var(--text-primary)', fontSize: '1rem' }}>
              <span>Subtotal</span>
              <span>LKR {cart.cartTotal.toFixed(2)}</span>
            </div>
            <div className="flex-between" style={{ marginBottom: '16px', color: 'var(--text-primary)', fontSize: '1rem' }}>
              <span>Shipping</span>
              <span style={{ fontStyle: 'italic', fontSize: '0.9rem', color: 'var(--text-muted)' }}>Free</span>
            </div>
            <div className="flex-between" style={{ fontWeight: 400, fontFamily: 'var(--font-title)', fontSize: '1.8rem', marginTop: '24px' }}>
              <span>Total</span>
              <span>LKR {cart.cartTotal.toFixed(2)}</span>
            </div>
          </div>

          <button 
            className="btn btn-primary" 
            style={{ width: '100%', padding: '20px', display: 'flex', justifyContent: 'center', alignItems: 'center', gap: '12px', fontSize: '1.1rem', letterSpacing: '0.1em', borderRadius: '0' }}
            onClick={handleCheckout}
            disabled={submitting || addresses.length === 0 || !selectedAddressId}
          >
            {submitting ? 'Processing...' : STRIPE_CHECKOUT_ENABLED ? 'Continue to Payment' : 'Complete Order'}
            {!submitting && <ArrowRight size={18} />}
          </button>
        </div>

      </div>
    </div>
  );
};
