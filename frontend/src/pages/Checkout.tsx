import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useCart } from '../context/CartContext';
import { addressApi, type AddressResponse } from '../api/addressApi';
import { orderApi } from '../api/orderApi';
import { MapPin, AlertCircle, ShoppingBag, ArrowRight, CreditCard, Clock } from 'lucide-react';

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
      <div className="container" style={{ padding: '40px 0', textAlign: 'center' }}>
        <p>Loading checkout...</p>
      </div>
    );
  }

  if (!cart || cart.items.length === 0) {
    return (
      <div className="container" style={{ padding: '60px 0', textAlign: 'center' }}>
        <ShoppingBag size={48} style={{ color: 'var(--text-muted)', margin: '0 auto 16px' }} />
        <h2>Your bag is empty</h2>
        <p style={{ color: 'var(--text-secondary)', marginBottom: '24px' }}>
          You need items in your bag to checkout.
        </p>
        <button className="btn btn-primary" onClick={() => navigate('/catalog')}>
          Continue Shopping
        </button>
      </div>
    );
  }

  return (
    <div className="container" style={{ padding: '40px 0' }}>
      <h1 style={{ marginBottom: '32px', fontFamily: 'var(--font-title)' }}>Checkout</h1>

      {error && (
        <div style={{
          backgroundColor: 'rgba(239, 68, 68, 0.1)',
          color: 'var(--error)',
          padding: '16px',
          borderRadius: '8px',
          marginBottom: '24px',
          display: 'flex',
          alignItems: 'center',
          gap: '12px'
        }}>
          <AlertCircle size={20} />
          <span>{error}</span>
        </div>
      )}

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 380px', gap: '32px', alignItems: 'start' }}>
        
        {/* Left Column: Address Selection */}
        <div>
          <div className="card" style={{ padding: '24px', marginBottom: '24px' }}>
            <h2 style={{ fontSize: '1.25rem', marginBottom: '20px', display: 'flex', alignItems: 'center', gap: '8px' }}>
              <MapPin size={20} />
              Delivery Address
            </h2>
            
            {addresses.length === 0 ? (
              <div>
                <p style={{ color: 'var(--text-secondary)', marginBottom: '16px' }}>
                  You don't have any saved addresses. Please add one to continue.
                </p>
                <button className="btn btn-secondary" onClick={() => navigate('/addresses')}>
                  Add New Address
                </button>
              </div>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                {addresses.map(address => (
                  <label key={address.id} style={{
                    display: 'flex',
                    alignItems: 'flex-start',
                    gap: '12px',
                    padding: '16px',
                    border: `1px solid ${selectedAddressId === address.id ? 'var(--accent)' : 'var(--border)'}`,
                    borderRadius: '8px',
                    cursor: 'pointer',
                    transition: 'border-color var(--transition-fast)'
                  }}>
                    <input 
                      type="radio" 
                      name="address" 
                      value={address.id}
                      checked={selectedAddressId === address.id}
                      onChange={() => setSelectedAddressId(address.id)}
                      style={{ marginTop: '4px' }}
                    />
                    <div>
                      <div style={{ fontWeight: 600, marginBottom: '4px' }}>
                        {address.recipientName} {address.label && <span style={{ color: 'var(--text-secondary)', fontWeight: 400, marginLeft: '8px' }}>({address.label})</span>}
                        {address.isDefault && (
                          <span style={{ 
                            marginLeft: '8px', 
                            fontSize: '0.75rem', 
                            backgroundColor: 'rgba(59, 130, 246, 0.1)', 
                            color: 'var(--accent)', 
                            padding: '2px 8px', 
                            borderRadius: '12px' 
                          }}>Default</span>
                        )}
                      </div>
                      <div style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', lineHeight: 1.5 }}>
                        {address.addressLine1}
                        {address.addressLine2 && <><br />{address.addressLine2}</>}
                        <br />
                        {address.city}, {address.region} {address.postalCode}
                        <br />
                        {address.country}
                        <br />
                        <span style={{ marginTop: '4px', display: 'block' }}>{address.phoneNumber}</span>
                      </div>
                    </div>
                  </label>
                ))}
              </div>
            )}
          </div>
          
          <div className="card" style={{ padding: '24px' }}>
             <h2 style={{ fontSize: '1.25rem', marginBottom: '20px', display: 'flex', alignItems: 'center', gap: '8px' }}>
               <CreditCard size={20} />
               Payment
             </h2>
             {STRIPE_CHECKOUT_ENABLED ? (
               <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', color: 'var(--text-secondary)' }}>
                 <p>
                   You will continue to Stripe Checkout to pay securely. Your items are reserved temporarily while payment is pending.
                 </p>
                 <p style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '0.9rem' }}>
                   <Clock size={16} />
                   Final order confirmation happens after payment is verified.
                 </p>
               </div>
             ) : (
               <p style={{ color: 'var(--text-secondary)' }}>
                 Payment processing is deferred for this phase. Placing your order will secure your items and update our systems.
               </p>
             )}
          </div>
        </div>

        {/* Right Column: Order Summary */}
        <div className="card" style={{ padding: '24px', position: 'sticky', top: '90px' }}>
          <h2 style={{ fontSize: '1.25rem', marginBottom: '20px' }}>Order Summary</h2>
          
          <div style={{ display: 'flex', flexDirection: 'column', gap: '16px', marginBottom: '24px', maxHeight: '400px', overflowY: 'auto' }}>
            {cart.items.map(item => (
              <div key={item.cartItemId} style={{ display: 'flex', gap: '12px' }}>
                <img 
                  src={item.imageUrl} 
                  alt={item.productName} 
                  style={{ width: '60px', height: '80px', objectFit: 'cover', borderRadius: '4px' }}
                />
                <div style={{ flex: 1 }}>
                  <div style={{ fontWeight: 500, fontSize: '0.875rem', marginBottom: '4px' }}>{item.productName}</div>
                  <div style={{ color: 'var(--text-secondary)', fontSize: '0.75rem', marginBottom: '4px' }}>
                    {item.color} | {item.size} | Qty: {item.quantity}
                  </div>
                  <div style={{ fontWeight: 600, fontSize: '0.875rem' }}>${item.lineTotal.toFixed(2)}</div>
                </div>
              </div>
            ))}
          </div>

          <div style={{ borderTop: '1px solid var(--border)', paddingTop: '16px', marginBottom: '24px' }}>
            <div className="flex-between" style={{ marginBottom: '12px', color: 'var(--text-secondary)' }}>
              <span>Subtotal</span>
              <span>${cart.cartTotal.toFixed(2)}</span>
            </div>
            <div className="flex-between" style={{ marginBottom: '12px', color: 'var(--text-secondary)' }}>
              <span>Shipping</span>
              <span>Free</span>
            </div>
            <div className="flex-between" style={{ fontWeight: 700, fontSize: '1.125rem', marginTop: '16px' }}>
              <span>Total</span>
              <span>${cart.cartTotal.toFixed(2)}</span>
            </div>
          </div>

          <button 
            className="btn btn-primary" 
            style={{ width: '100%', display: 'flex', justifyContent: 'center', alignItems: 'center', gap: '8px' }}
            onClick={handleCheckout}
            disabled={submitting || addresses.length === 0 || !selectedAddressId}
          >
            {submitting ? 'Processing...' : STRIPE_CHECKOUT_ENABLED ? 'Continue to Payment' : 'Place Order'}
            {!submitting && <ArrowRight size={18} />}
          </button>
        </div>

      </div>
    </div>
  );
};
