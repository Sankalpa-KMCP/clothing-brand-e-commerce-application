import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { orderApi, type OrderHistoryPageResponseDto, type OrderSummaryResponseDto } from '../api/orderApi';
import { Package, AlertCircle, ChevronLeft, ChevronRight } from 'lucide-react';

export const Orders: React.FC = () => {
  const [data, setData] = useState<OrderHistoryPageResponseDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const size = 10;

  useEffect(() => {
    fetchOrders(page);
  }, [page]);

  const fetchOrders = async (pageIndex: number) => {
    try {
      setLoading(true);
      setError(null);
      const response = await orderApi.getMyOrders(pageIndex, size);
      setData(response);
    } catch (err) {
      console.error('Failed to load orders:', err);
      setError('Failed to load your order history.');
    } finally {
      setLoading(false);
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

  if (loading && !data) {
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

  return (
    <div className="container animate-fade-in" style={{ padding: '40px 20px 80px 20px' }}>
      <div style={{ marginBottom: '30px' }}>
        <span className="atelier-kicker">Purchasing Records</span>
        <h1 className="title-medium" style={{ marginTop: '6px', display: 'flex', alignItems: 'center', gap: '12px' }}>
          <Package size={28} strokeWidth={1.5} style={{ color: 'var(--accent)' }} />
          <span>My Orders</span>
        </h1>
      </div>

      {error && (
        <div className="alert alert-error animate-fade-in" style={{ marginBottom: '30px' }}>
          <AlertCircle size={20} />
          <span>{error}</span>
        </div>
      )}

      {!loading && data && data.content.length === 0 ? (
        <div style={{
          textAlign: 'center',
          padding: '80px 20px',
          backgroundColor: 'var(--bg-card)',
          border: '1px solid var(--border)',
          maxWidth: '500px',
          margin: '0 auto'
        }}>
          <Package size={48} style={{ color: 'var(--text-muted)', marginBottom: '16px' }} />
          <h2 style={{ fontFamily: 'var(--font-title)', fontSize: '1.5rem', marginBottom: '8px', fontWeight: 400 }}>No orders yet</h2>
          <p style={{ color: 'var(--text-secondary)', marginBottom: '32px' }}>
            When you place orders, they will appear here.
          </p>
          <Link to="/catalog" className="btn btn-primary" style={{ borderRadius: 0, padding: '16px 32px' }}>Start Shopping</Link>
        </div>
      ) : data ? (
        <div style={{ border: '1px solid var(--border)', backgroundColor: 'var(--bg-card)' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
            <thead>
              <tr style={{ backgroundColor: 'var(--bg-secondary)', borderBottom: '1px solid var(--text-primary)' }}>
                <th style={{ padding: '24px', fontWeight: 600, fontSize: '0.75rem', textTransform: 'uppercase', letterSpacing: '0.1em', color: 'var(--text-primary)' }}>Order ID</th>
                <th style={{ padding: '24px', fontWeight: 600, fontSize: '0.75rem', textTransform: 'uppercase', letterSpacing: '0.1em', color: 'var(--text-primary)' }}>Date</th>
                <th style={{ padding: '24px', fontWeight: 600, fontSize: '0.75rem', textTransform: 'uppercase', letterSpacing: '0.1em', color: 'var(--text-primary)' }}>Total</th>
                <th style={{ padding: '24px', fontWeight: 600, fontSize: '0.75rem', textTransform: 'uppercase', letterSpacing: '0.1em', color: 'var(--text-primary)' }}>Status</th>
                <th style={{ padding: '24px', fontWeight: 600, fontSize: '0.75rem', textTransform: 'uppercase', letterSpacing: '0.1em', color: 'var(--text-primary)', textAlign: 'right' }}>Action</th>
              </tr>
            </thead>
            <tbody>
              {data.content.map((order: OrderSummaryResponseDto) => {
                return (
                  <tr key={order.id} style={{ borderBottom: '1px solid var(--border)', transition: 'background-color var(--transition-fast)' }}
                      onMouseEnter={(e) => e.currentTarget.style.backgroundColor = 'var(--bg-secondary)'}
                      onMouseLeave={(e) => e.currentTarget.style.backgroundColor = 'transparent'}>
                    <td style={{ padding: '24px', fontWeight: 400, fontFamily: 'var(--font-title)', fontSize: '1.1rem' }}>#{order.id}</td>
                    <td style={{ padding: '24px', fontSize: '0.9rem', color: 'var(--text-secondary)' }}>{new Date(order.createdAt).toLocaleDateString()}</td>
                    <td style={{ padding: '24px', fontWeight: 400, fontFamily: 'var(--font-title)', fontSize: '1.1rem' }}>LKR {order.total.toFixed(2)}</td>
                    <td style={{ padding: '24px' }}>
                      <span className={getStatusClass(order.status)} style={{ borderRadius: 0, padding: '6px 12px', fontSize: '0.7rem', letterSpacing: '0.1em', textTransform: 'uppercase', fontWeight: 600 }}>
                        {order.status}
                      </span>
                    </td>
                    <td style={{ padding: '24px', textAlign: 'right' }}>
                      <Link to={`/orders/${order.id}`} className="editorial-text-link" style={{ fontSize: '0.75rem', letterSpacing: '0.1em' }}>
                        View Details
                      </Link>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>

          {data.totalPages > 1 && (
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '24px', borderTop: '1px solid var(--border)', backgroundColor: 'var(--bg-primary)' }}>
              <span style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                Showing page {data.page + 1} of {data.totalPages}
              </span>
              <div style={{ display: 'flex', gap: '8px' }}>
                <button 
                  className="btn btn-secondary flex-center" 
                  style={{ padding: '12px 20px', fontSize: '0.8rem', borderRadius: 0 }}
                  onClick={() => setPage(p => Math.max(0, p - 1))}
                  disabled={data.first || loading}
                >
                  <ChevronLeft size={16} /> Prev
                </button>
                <button 
                  className="btn btn-secondary flex-center" 
                  style={{ padding: '12px 20px', fontSize: '0.8rem', borderRadius: 0 }}
                  onClick={() => setPage(p => p + 1)}
                  disabled={data.last || loading}
                >
                  Next <ChevronRight size={16} />
                </button>
              </div>
            </div>
          )}
        </div>
      ) : null}
    </div>
  );
};
