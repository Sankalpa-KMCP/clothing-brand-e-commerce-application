import React, { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { adminClient } from '../../api/adminClient';
import type { OrderDetailResponseDto } from '../../api/orderApi';
import { ArrowLeft, Save } from 'lucide-react';

export const AdminOrderDetail: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const [order, setOrder] = useState<OrderDetailResponseDto | null>(null);
  const [status, setStatus] = useState<string>('');
  const [isSaving, setIsSaving] = useState(false);

  useEffect(() => {
    if (id) {
      adminClient.getOrderById(parseInt(id, 10))
        .then(res => {
          setOrder(res);
          setStatus(res.status);
        })
        .catch(console.error);
    }
  }, [id]);

  const handleStatusUpdate = async () => {
    if (!id || !status) return;
    try {
      setIsSaving(true);
      await adminClient.updateOrderStatus(parseInt(id, 10), status);
      alert('Order status updated successfully');
      // optionally refresh order
      const updated = await adminClient.getOrderById(parseInt(id, 10));
      setOrder(updated);
    } catch (err) {
      alert('Failed to update status');
    } finally {
      setIsSaving(false);
    }
  };

  if (!order) return <div className="flex-center" style={{ minHeight: '50vh' }}><div className="loader-spinner"></div></div>;

  return (
    <div className="animate-fade-in" style={{ maxWidth: '900px', margin: '0 auto' }}>
      <Link to="/admin/orders" style={{ display: 'inline-flex', alignItems: 'center', gap: '8px', color: 'var(--text-secondary)', textDecoration: 'none', marginBottom: '24px', fontSize: '0.875rem' }}>
        <ArrowLeft size={16} />
        <span>Back to Orders</span>
      </Link>

      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', marginBottom: '32px' }}>
        <div>
          <h1 style={{ fontSize: '2rem', fontFamily: 'var(--font-title)', marginBottom: '8px' }}>Order #{order.id}</h1>
          <p style={{ color: 'var(--text-secondary)' }}>Placed on {new Date(order.createdAt).toLocaleString()}</p>
        </div>
      </div>

      <div className="grid grid-2" style={{ gap: '24px', alignItems: 'start' }}>
        {/* Status Update Panel */}
        <div style={{ backgroundColor: 'var(--bg-card)', padding: '32px', borderRadius: '12px', border: '1px solid var(--border)', display: 'flex', flexDirection: 'column', gap: '24px' }}>
          <h2 style={{ fontSize: '1.25rem', fontWeight: 600, borderBottom: '1px solid var(--border)', paddingBottom: '16px' }}>Order Status</h2>
          
          <div className="form-group">
            <label className="form-label">Current Status</label>
            <select className="form-input" value={status} onChange={e => setStatus(e.target.value)}>
              <option value="PENDING">PENDING</option>
              <option value="PAID">PAID</option>
              <option value="SHIPPED">SHIPPED</option>
              <option value="DELIVERED">DELIVERED</option>
              <option value="CANCELLED">CANCELLED</option>
            </select>
          </div>
          
          <button onClick={handleStatusUpdate} disabled={isSaving || status === order.status} className="btn btn-primary" style={{ width: '100%', justifyContent: 'center' }}>
            <Save size={16} />
            <span>{isSaving ? 'Saving...' : 'Update Status'}</span>
          </button>
        </div>

        {/* Customer Info */}
        <div style={{ backgroundColor: 'var(--bg-card)', padding: '32px', borderRadius: '12px', border: '1px solid var(--border)', display: 'flex', flexDirection: 'column', gap: '24px' }}>
          <h2 style={{ fontSize: '1.25rem', fontWeight: 600, borderBottom: '1px solid var(--border)', paddingBottom: '16px' }}>Customer & Delivery</h2>
          
          <div>
            <div style={{ fontSize: '0.75rem', textTransform: 'uppercase', color: 'var(--text-secondary)', marginBottom: '4px' }}>Customer</div>
            <div style={{ fontWeight: 500 }}>{order.deliveryAddress?.recipientName}</div>
          </div>
          
          <div>
            <div style={{ fontSize: '0.75rem', textTransform: 'uppercase', color: 'var(--text-secondary)', marginBottom: '4px' }}>Delivery Address</div>
            <div style={{ lineHeight: 1.5, color: 'var(--text-secondary)' }}>
              {order.deliveryAddress?.addressLine1}<br />
              {order.deliveryAddress?.addressLine2 && <>{order.deliveryAddress.addressLine2}<br /></>}
              {order.deliveryAddress?.city}, {order.deliveryAddress?.region} {order.deliveryAddress?.postalCode}<br />
              {order.deliveryAddress?.country}
            </div>
          </div>
        </div>
      </div>

      {/* Order Items */}
      <div style={{ marginTop: '32px', backgroundColor: 'var(--bg-card)', padding: '32px', borderRadius: '12px', border: '1px solid var(--border)' }}>
        <h2 style={{ fontSize: '1.25rem', fontWeight: 600, borderBottom: '1px solid var(--border)', paddingBottom: '16px', marginBottom: '24px' }}>Items</h2>
        
        <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
          {order.items.map((item, idx) => (
            <div key={idx} style={{ display: 'flex', gap: '24px', alignItems: 'center' }}>
              <div style={{ width: '80px', height: '100px', backgroundColor: 'var(--bg-secondary)', borderRadius: '8px', overflow: 'hidden' }}>
                 {/* Product image would ideally be included in the API response or fetched, placeholder for now */}
              </div>
              <div style={{ flex: 1 }}>
                <h4 style={{ fontWeight: 500, fontSize: '1rem', marginBottom: '4px' }}>{item.productName}</h4>
                <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>Size: {item.size} • Color: {item.color}</p>
              </div>
              <div style={{ textAlign: 'right' }}>
                <div style={{ fontWeight: 500 }}>LKR {item.unitPrice.toLocaleString(undefined, { minimumFractionDigits: 2 })}</div>
                <div style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>Qty: {item.quantity}</div>
              </div>
            </div>
          ))}
        </div>
        
        <div style={{ marginTop: '32px', paddingTop: '24px', borderTop: '1px solid var(--border)', display: 'flex', justifyContent: 'flex-end' }}>
          <div style={{ width: '300px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '16px' }}>
              <span style={{ color: 'var(--text-secondary)' }}>Subtotal</span>
              <span>LKR {order.subtotal.toLocaleString(undefined, { minimumFractionDigits: 2 })}</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', fontWeight: 600, fontSize: '1.25rem' }}>
              <span>Total</span>
              <span>LKR {order.total.toLocaleString(undefined, { minimumFractionDigits: 2 })}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
