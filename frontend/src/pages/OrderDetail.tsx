import React, { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { orderApi, type OrderDetailResponseDto } from '../api/orderApi';
import { useCart } from '../context/CartContext';
import { Package, AlertCircle, ChevronLeft, MapPin, Clock, XCircle } from 'lucide-react';

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

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'PLACED': return { bg: 'rgba(59, 130, 246, 0.1)', color: '#3b82f6' };
      case 'PROCESSING': return { bg: 'rgba(168, 85, 247, 0.1)', color: '#a855f7' };
      case 'SHIPPED': return { bg: 'rgba(234, 179, 8, 0.1)', color: '#eab308' };
      case 'DELIVERED': return { bg: 'rgba(34, 197, 94, 0.1)', color: '#22c55e' };
      case 'CANCELLED': return { bg: 'rgba(239, 68, 68, 0.1)', color: '#ef4444' };
      default: return { bg: 'rgba(107, 114, 128, 0.1)', color: '#6b7280' };
    }
  };

  if (loading && !order) {
    return (
      <div className="container" style={{ padding: '60px 0', textAlign: 'center' }}>
        <p>Loading order details...</p>
      </div>
    );
  }

  if (error && !order) {
    return (
      <div className="container" style={{ padding: '60px 0', textAlign: 'center' }}>
        <AlertCircle size={48} style={{ color: 'var(--error)', margin: '0 auto 16px' }} />
        <h2 style={{ marginBottom: '8px' }}>Oops!</h2>
        <p style={{ color: 'var(--text-secondary)', marginBottom: '24px' }}>
          {error}
        </p>
        <Link to="/orders" className="btn btn-primary">Back to Orders</Link>
      </div>
    );
  }

  if (!order) return null;

  const statusColors = getStatusColor(order.status);

  return (
    <div className="container" style={{ padding: '40px 0' }}>
      <Link to="/orders" style={{ display: 'inline-flex', alignItems: 'center', gap: '4px', color: 'var(--text-secondary)', marginBottom: '24px', textDecoration: 'none' }}
            onMouseEnter={(e) => e.currentTarget.style.color = 'var(--accent)'}
            onMouseLeave={(e) => e.currentTarget.style.color = 'var(--text-secondary)'}>
        <ChevronLeft size={16} />
        Back to Orders
      </Link>

      <div className="flex-between" style={{ alignItems: 'flex-start', marginBottom: '32px' }}>
        <div>
          <h1 style={{ fontFamily: 'var(--font-title)', display: 'flex', alignItems: 'center', gap: '12px' }}>
            Order #{order.id}
            <span style={{
              backgroundColor: statusColors.bg,
              color: statusColors.color,
              padding: '6px 12px',
              borderRadius: '20px',
              fontSize: '0.875rem',
              fontWeight: 600,
              fontFamily: 'var(--font-body)'
            }}>
              {order.status}
            </span>
          </h1>
          <p style={{ color: 'var(--text-secondary)', marginTop: '8px', display: 'flex', alignItems: 'center', gap: '6px' }}>
            <Clock size={16} />
            Placed on {new Date(order.createdAt).toLocaleString()}
          </p>
        </div>
        
        {order.status === 'PLACED' && (
          <button 
            className="btn" 
            style={{ 
              backgroundColor: 'rgba(239, 68, 68, 0.1)', 
              color: '#ef4444', 
              border: 'none', 
              display: 'flex', 
              alignItems: 'center', 
              gap: '6px',
              padding: '8px 16px',
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
        
        {/* Left Column: Items & History */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
          <div className="card" style={{ padding: '24px' }}>
            <h2 style={{ fontSize: '1.25rem', marginBottom: '20px', display: 'flex', alignItems: 'center', gap: '8px' }}>
              <Package size={20} />
              Items Ordered
            </h2>
            
            <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
              {order.items.map((item, idx) => (
                <div key={idx} style={{ display: 'flex', gap: '16px', paddingBottom: idx !== order.items.length - 1 ? '20px' : '0', borderBottom: idx !== order.items.length - 1 ? '1px solid var(--border)' : 'none' }}>
                  <img 
                    src={item.productImageUrl} 
                    alt={item.productName} 
                    style={{ width: '80px', height: '100px', objectFit: 'cover', borderRadius: '4px' }}
                  />
                  <div style={{ flex: 1 }}>
                    <div className="flex-between" style={{ alignItems: 'flex-start', marginBottom: '4px' }}>
                      <div style={{ fontWeight: 600, fontSize: '1rem' }}>{item.productName}</div>
                      <div style={{ fontWeight: 600 }}>${item.lineTotal.toFixed(2)}</div>
                    </div>
                    <div style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', marginBottom: '4px' }}>
                      {item.color} | {item.size}
                    </div>
                    <div style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
                      Qty: {item.quantity} × ${item.unitPrice.toFixed(2)}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {order.statusHistory && order.statusHistory.length > 0 && (
            <div className="card" style={{ padding: '24px' }}>
              <h2 style={{ fontSize: '1.25rem', marginBottom: '20px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                <Clock size={20} />
                Order History
              </h2>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                {order.statusHistory.map((history, idx) => (
                  <div key={idx} style={{ display: 'flex', gap: '12px' }}>
                    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
                      <div style={{ width: '10px', height: '10px', borderRadius: '50%', backgroundColor: 'var(--accent)', marginTop: '6px' }} />
                      {idx !== order.statusHistory.length - 1 && <div style={{ width: '2px', flex: 1, backgroundColor: 'var(--border)', marginTop: '4px', marginBottom: '4px' }} />}
                    </div>
                    <div style={{ paddingBottom: '16px' }}>
                      <div style={{ fontWeight: 500 }}>
                        {history.previousStatus ? `${history.previousStatus} → ` : ''}
                        {history.newStatus}
                      </div>
                      <div style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', marginTop: '4px' }}>
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
        <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
          <div className="card" style={{ padding: '24px' }}>
            <h2 style={{ fontSize: '1.125rem', marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '8px' }}>
              <MapPin size={18} />
              Delivery Address
            </h2>
            <div style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', lineHeight: 1.6 }}>
              <div style={{ fontWeight: 600, color: 'var(--text)', marginBottom: '4px' }}>
                {order.deliveryAddress.recipientName}
              </div>
              {order.deliveryAddress.addressLine1}
              {order.deliveryAddress.addressLine2 && <><br />{order.deliveryAddress.addressLine2}</>}
              <br />
              {order.deliveryAddress.city}, {order.deliveryAddress.region} {order.deliveryAddress.postalCode}
              <br />
              {order.deliveryAddress.country}
              <br />
              <span style={{ marginTop: '8px', display: 'block' }}>{order.deliveryAddress.phoneNumber}</span>
            </div>
          </div>

          <div className="card" style={{ padding: '24px' }}>
            <h2 style={{ fontSize: '1.125rem', marginBottom: '16px' }}>Summary</h2>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', fontSize: '0.875rem' }}>
              <div className="flex-between" style={{ color: 'var(--text-secondary)' }}>
                <span>Subtotal</span>
                <span>${order.subtotal.toFixed(2)}</span>
              </div>
              <div className="flex-between" style={{ color: 'var(--text-secondary)' }}>
                <span>Shipping</span>
                <span>Free</span>
              </div>
              <div style={{ borderTop: '1px solid var(--border)', paddingTop: '12px', marginTop: '4px' }}>
                <div className="flex-between" style={{ fontWeight: 700, fontSize: '1.125rem' }}>
                  <span>Total</span>
                  <span>${order.total.toFixed(2)}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

      </div>
    </div>
  );
};
