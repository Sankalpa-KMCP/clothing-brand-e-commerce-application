import React, { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { orderApi, type OrderDetailResponseDto } from '../api/orderApi';
import { useCart } from '../context/CartContext';
import { Package, AlertCircle, ChevronLeft, MapPin, Clock, XCircle } from 'lucide-react';
import { EditorialMedia } from '../components/EditorialMedia';

export const OrderDetail: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const { fetchCart } = useCart();
  
  const [order, setOrder] = useState<OrderDetailResponseDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [cancelling, setCancelling] = useState(false);

  useEffect(() => {
    if (id) {
      fetchOrder(Number(id));
    }
  }, [id]);

  const fetchOrder = async (orderId: number) => {
    try {
      setLoading(true);
      setError(null);
      const data = await orderApi.getMyOrder(orderId);
      setOrder(data);
    } catch (err: any) {
      console.error('Failed to load order:', err);
      if (err.response && err.response.status === 404) {
        setError('Order not found.');
      } else {
        setError('Failed to load order details.');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleCancel = async () => {
    if (!order || order.status !== 'PLACED') return;
    
    if (!window.confirm('Are you sure you want to cancel this order? This action cannot be undone.')) {
      return;
    }

    try {
      setCancelling(true);
      setError(null);
      const updatedOrder = await orderApi.cancelMyOrder(order.id);
      setOrder(updatedOrder);
      // Refresh cart state as requested since cancellation restores stock 
      // and could theoretically affect cart availability
      fetchCart();
    } catch (err: any) {
      console.error('Failed to cancel order:', err);
      setError('Failed to cancel order. It may have already been processed.');
    } finally {
      setCancelling(false);
    }
  };

  const getStatusClass = (status: string) => {
    switch (status) {
      case 'DELIVERED':
      case 'SHIPPED':
        return 'status-badge-premium status-badge-completed';
      case 'CANCELLED':
        return 'status-badge-premium status-badge-cancelled';
      default:
        return 'status-badge-premium status-badge-pending';
    }
  };

  if (loading && !order) {
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

  if (error && !order) {
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
          <AlertCircle size={48} style={{ color: 'var(--error)', marginBottom: '20px' }} />
          <h2 className="title-small" style={{ marginBottom: '8px' }}>Oops!</h2>
          <p style={{ color: 'var(--text-secondary)', marginBottom: '24px' }}>
            {error}
          </p>
          <Link to="/orders" className="btn btn-primary">Back to Orders</Link>
        </div>
      </div>
    );
  }

  if (!order) return null;

  return (
    <div className="container animate-fade-in" style={{ padding: '40px 20px 80px 20px' }}>
      <Link to="/orders" style={{ display: 'inline-flex', alignItems: 'center', gap: '4px', color: 'var(--text-secondary)', marginBottom: '24px', textDecoration: 'none', fontSize: '0.875rem' }}
            className="editorial-text-link">
        <ChevronLeft size={16} />
        Back to Orders
      </Link>

      <div className="flex-between" style={{ alignItems: 'flex-start', marginBottom: '32px', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <h1 className="title-medium" style={{ display: 'flex', alignItems: 'center', gap: '16px', flexWrap: 'wrap' }}>
            <span>Order #{order.id}</span>
            <span className={getStatusClass(order.status)}>
              {order.status}
            </span>
          </h1>
          <p style={{ color: 'var(--text-secondary)', marginTop: '8px', display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.875rem' }}>
            <Clock size={16} />
            Placed on {new Date(order.createdAt).toLocaleString()}
          </p>
        </div>
        
        {order.status === 'PLACED' && (
          <button 
            className="btn" 
            style={{ 
              backgroundColor: 'var(--error-bg)', 
              color: 'var(--error)', 
              border: '1px solid var(--error)', 
              display: 'flex', 
              alignItems: 'center', 
              gap: '6px',
              padding: '10px 20px',
              fontWeight: 600
            }}
            onClick={handleCancel}
            disabled={cancelling}
          >
            <XCircle size={18} />
            {cancelling ? 'Cancelling...' : 'Cancel Order'}
          </button>
        )}
      </div>

      {error && order && (
        <div className="alert alert-error animate-fade-in" style={{ marginBottom: '30px' }}>
          <AlertCircle size={20} />
          <span>{error}</span>
        </div>
      )}

      <div className="grid grid-3" style={{ gap: '60px', alignItems: 'start' }}>
        
        {/* Left Column: Items & History */}
        <div style={{ gridColumn: 'span 2', display: 'flex', flexDirection: 'column', gap: '48px' }}>
          <div>
            <h2 style={{ fontFamily: 'var(--font-title)', fontSize: '1.8rem', fontWeight: 400, marginBottom: '32px', letterSpacing: '0.02em', display: 'flex', alignItems: 'center', gap: '12px' }}>
              <Package size={24} style={{ color: 'var(--text-primary)' }} />
              Items Ordered
            </h2>
            
            <div style={{ display: 'flex', flexDirection: 'column', gap: '32px' }}>
              {order.items.map((item, idx) => (
                <div key={idx} style={{ display: 'flex', gap: '24px', paddingBottom: '32px', borderBottom: idx !== order.items.length - 1 ? '1px solid var(--border)' : 'none', alignItems: 'flex-start' }}>
                  <div style={{ width: '100px', flexShrink: 0 }}>
                    <EditorialMedia
                      src={item.productImageUrl}
                      alt={item.productName}
                      label={item.productName}
                      style={{ height: '140px', border: 'none' }}
                    />
                  </div>
                  <div style={{ flex: 1, paddingTop: '4px' }}>
                    <div className="flex-between" style={{ alignItems: 'flex-start', marginBottom: '8px' }}>
                      <div style={{ fontWeight: 400, fontFamily: 'var(--font-title)', fontSize: '1.2rem', letterSpacing: '0.02em' }}>{item.productName}</div>
                      <div style={{ fontWeight: 400, fontSize: '1.2rem' }}>LKR {item.lineTotal.toFixed(2)}</div>
                    </div>
                    <div style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', marginBottom: '16px', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                      {item.color} / {item.size}
                    </div>
                    <div style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>
                      Qty: {item.quantity} &times; LKR {item.unitPrice.toFixed(2)}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>

          <hr style={{ border: 'none', borderTop: '1px solid var(--border)' }} />

          {order.statusHistory && order.statusHistory.length > 0 && (
            <div>
              <h2 style={{ fontFamily: 'var(--font-title)', fontSize: '1.8rem', fontWeight: 400, marginBottom: '32px', letterSpacing: '0.02em', display: 'flex', alignItems: 'center', gap: '12px' }}>
                <Clock size={24} style={{ color: 'var(--text-primary)' }} />
                Order History
              </h2>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
                {order.statusHistory.map((history, idx) => (
                  <div key={idx} style={{ display: 'flex', gap: '20px' }}>
                    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
                      <div style={{ width: '12px', height: '12px', borderRadius: '50%', backgroundColor: 'var(--text-primary)', marginTop: '4px' }} />
                      {idx !== order.statusHistory.length - 1 && <div style={{ width: '1px', flex: 1, backgroundColor: 'var(--border)', marginTop: '8px', marginBottom: '4px' }} />}
                    </div>
                    <div style={{ paddingBottom: '16px' }}>
                      <div style={{ fontWeight: 600, fontSize: '1rem', textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: '4px' }}>
                        {history.previousStatus ? `${history.previousStatus} → ` : ''}
                        {history.newStatus}
                      </div>
                      <div style={{ color: 'var(--text-secondary)', fontSize: '0.85rem' }}>
                        {new Date(history.createdAt).toLocaleString()} by {history.actorType}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>

        {/* Right Column: Address & Summary */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '32px', position: 'sticky', top: '40px' }}>
          <div style={{ padding: '32px', border: '1px solid var(--border)', backgroundColor: 'var(--bg-secondary)' }}>
            <h2 style={{ fontFamily: 'var(--font-title)', fontSize: '1.5rem', fontWeight: 400, marginBottom: '24px', letterSpacing: '0.02em', display: 'flex', alignItems: 'center', gap: '12px' }}>
              <MapPin size={20} style={{ color: 'var(--text-primary)' }} />
              Delivery Address
            </h2>
            <div style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', lineHeight: 1.6 }}>
              <div style={{ fontWeight: 600, color: 'var(--text-primary)', marginBottom: '8px', fontSize: '1rem' }}>
                {order.deliveryAddress.recipientName}
              </div>
              {order.deliveryAddress.addressLine1}
              {order.deliveryAddress.addressLine2 && <><br />{order.deliveryAddress.addressLine2}</>}
              <br />
              {order.deliveryAddress.city}, {order.deliveryAddress.region} {order.deliveryAddress.postalCode}
              <br />
              {order.deliveryAddress.country}
              <br />
              <span style={{ marginTop: '16px', display: 'block', color: 'var(--text-muted)', fontSize: '0.85rem', letterSpacing: '0.05em' }}>{order.deliveryAddress.phoneNumber}</span>
            </div>
          </div>

          <div style={{ padding: '32px', border: '1px solid var(--border)', backgroundColor: 'var(--bg-card)' }}>
            <h2 style={{ fontFamily: 'var(--font-title)', fontSize: '1.5rem', fontWeight: 400, marginBottom: '32px', letterSpacing: '0.02em' }}>Summary</h2>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '16px', fontSize: '1rem' }}>
              <div className="flex-between" style={{ color: 'var(--text-secondary)' }}>
                <span>Subtotal</span>
                <span>LKR {order.subtotal.toFixed(2)}</span>
              </div>
              <div className="flex-between" style={{ color: 'var(--text-secondary)' }}>
                <span>Shipping</span>
                <span style={{ fontStyle: 'italic', fontSize: '0.9rem' }}>Free</span>
              </div>
              <div style={{ borderTop: '1px solid var(--border)', paddingTop: '24px', marginTop: '8px' }}>
                <div className="flex-between" style={{ fontWeight: 400, fontFamily: 'var(--font-title)', fontSize: '1.5rem' }}>
                  <span>Total</span>
                  <span>LKR {order.total.toFixed(2)}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

      </div>
    </div>
  );
};
