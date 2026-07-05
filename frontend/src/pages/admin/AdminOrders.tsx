import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { adminClient } from '../../api/adminClient';
import { Eye } from 'lucide-react';

export const AdminOrders: React.FC = () => {
  const [orders, setOrders] = useState<any[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    adminClient.getOrders()
      .then(res => {
        // The endpoint typically returns a paginated list { content: [...] }
        setOrders(res.content || res || []);
      })
      .catch(console.error)
      .finally(() => setIsLoading(false));
  }, []);

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'PENDING': return { bg: 'rgba(234, 179, 8, 0.1)', text: 'var(--warning)' };
      case 'PAID': return { bg: 'rgba(59, 130, 246, 0.1)', text: '#2563eb' };
      case 'SHIPPED': return { bg: 'rgba(168, 85, 247, 0.1)', text: '#9333ea' };
      case 'DELIVERED': return { bg: 'rgba(34, 197, 94, 0.1)', text: 'var(--success)' };
      case 'CANCELLED': return { bg: 'rgba(239, 68, 68, 0.1)', text: 'var(--error)' };
      default: return { bg: 'rgba(100, 116, 139, 0.1)', text: '#475569' };
    }
  };

  return (
    <div className="animate-fade-in">
      <div style={{ marginBottom: '32px' }}>
        <h1 style={{ fontSize: '2rem', fontFamily: 'var(--font-title)', marginBottom: '8px' }}>Orders</h1>
        <p style={{ color: 'var(--text-secondary)' }}>View and manage customer orders.</p>
      </div>

      <div style={{ backgroundColor: 'var(--bg-card)', borderRadius: '12px', border: '1px solid var(--border)', overflow: 'hidden' }}>
        {isLoading ? (
          <div className="flex-center" style={{ padding: '64px' }}>
            <div className="loader-spinner"></div>
          </div>
        ) : orders.length === 0 ? (
          <div style={{ padding: '64px', textAlign: 'center', color: 'var(--text-secondary)' }}>
            No orders found.
          </div>
        ) : (
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
            <thead>
              <tr style={{ borderBottom: '1px solid var(--border)', backgroundColor: 'var(--bg-secondary)' }}>
                <th style={{ padding: '16px 24px', fontWeight: 600, fontSize: '0.75rem', textTransform: 'uppercase', letterSpacing: '0.05em', color: 'var(--text-secondary)' }}>Order ID</th>
                <th style={{ padding: '16px 24px', fontWeight: 600, fontSize: '0.75rem', textTransform: 'uppercase', letterSpacing: '0.05em', color: 'var(--text-secondary)' }}>Date</th>
                <th style={{ padding: '16px 24px', fontWeight: 600, fontSize: '0.75rem', textTransform: 'uppercase', letterSpacing: '0.05em', color: 'var(--text-secondary)' }}>Total</th>
                <th style={{ padding: '16px 24px', fontWeight: 600, fontSize: '0.75rem', textTransform: 'uppercase', letterSpacing: '0.05em', color: 'var(--text-secondary)' }}>Status</th>
                <th style={{ padding: '16px 24px', fontWeight: 600, fontSize: '0.75rem', textTransform: 'uppercase', letterSpacing: '0.05em', color: 'var(--text-secondary)', textAlign: 'right' }}>Action</th>
              </tr>
            </thead>
            <tbody>
              {orders.map(order => {
                const statusStyle = getStatusColor(order.status);
                return (
                  <tr key={order.id} style={{ borderBottom: '1px solid var(--border)' }}>
                    <td style={{ padding: '16px 24px', fontWeight: 500 }}>#{order.id}</td>
                    <td style={{ padding: '16px 24px', color: 'var(--text-secondary)' }}>
                      {new Date(order.createdAt).toLocaleDateString()}
                    </td>
                    <td style={{ padding: '16px 24px', fontWeight: 500 }}>LKR {order.totalAmount.toLocaleString(undefined, { minimumFractionDigits: 2 })}</td>
                    <td style={{ padding: '16px 24px' }}>
                      <span style={{ 
                        padding: '4px 8px', 
                        borderRadius: '4px', 
                        fontSize: '0.75rem', 
                        fontWeight: 600,
                        backgroundColor: statusStyle.bg,
                        color: statusStyle.text
                      }}>
                        {order.status}
                      </span>
                    </td>
                    <td style={{ padding: '16px 24px', textAlign: 'right' }}>
                      <Link to={`/admin/orders/${order.id}`} style={{ color: 'var(--text-secondary)', transition: 'color var(--transition-fast)' }} onMouseEnter={e => e.currentTarget.style.color = 'var(--accent)'} onMouseLeave={e => e.currentTarget.style.color = 'var(--text-secondary)'}>
                        <Eye size={18} />
                      </Link>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
};
