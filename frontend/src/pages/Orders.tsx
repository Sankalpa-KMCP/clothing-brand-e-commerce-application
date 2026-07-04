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

  if (loading && !data) {
    return (
      <div className="container" style={{ padding: '60px 0', textAlign: 'center' }}>
        <p>Loading your orders...</p>
      </div>
    );
  }

  return (
    <div className="container" style={{ padding: '40px 0' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '32px' }}>
        <Package size={28} />
        <h1 style={{ fontFamily: 'var(--font-title)' }}>My Orders</h1>
      </div>

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

      {!loading && data && data.content.length === 0 ? (
        <div className="card" style={{ padding: '60px 20px', textAlign: 'center' }}>
          <Package size={48} style={{ color: 'var(--text-muted)', margin: '0 auto 16px' }} />
          <h2 style={{ marginBottom: '8px' }}>No orders yet</h2>
          <p style={{ color: 'var(--text-secondary)', marginBottom: '24px' }}>
            When you place orders, they will appear here.
          </p>
          <Link to="/catalog" className="btn btn-primary">Start Shopping</Link>
        </div>
      ) : data ? (
        <div className="card" style={{ overflow: 'hidden' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
            <thead>
              <tr style={{ backgroundColor: 'rgba(0,0,0,0.02)', borderBottom: '1px solid var(--border)' }}>
                <th style={{ padding: '16px', fontWeight: 600, color: 'var(--text-secondary)' }}>Order ID</th>
                <th style={{ padding: '16px', fontWeight: 600, color: 'var(--text-secondary)' }}>Date</th>
                <th style={{ padding: '16px', fontWeight: 600, color: 'var(--text-secondary)' }}>Total</th>
                <th style={{ padding: '16px', fontWeight: 600, color: 'var(--text-secondary)' }}>Status</th>
                <th style={{ padding: '16px', fontWeight: 600, color: 'var(--text-secondary)', textAlign: 'right' }}>Action</th>
              </tr>
            </thead>
            <tbody>
              {data.content.map((order: OrderSummaryResponseDto) => {
                const statusColors = getStatusColor(order.status);
                return (
                  <tr key={order.id} style={{ borderBottom: '1px solid var(--border)', transition: 'background-color var(--transition-fast)' }}
                      onMouseEnter={(e) => e.currentTarget.style.backgroundColor = 'rgba(0,0,0,0.01)'}
                      onMouseLeave={(e) => e.currentTarget.style.backgroundColor = 'transparent'}>
                    <td style={{ padding: '16px', fontWeight: 500 }}>#{order.id}</td>
                    <td style={{ padding: '16px' }}>{new Date(order.createdAt).toLocaleDateString()}</td>
                    <td style={{ padding: '16px', fontWeight: 500 }}>${order.total.toFixed(2)}</td>
                    <td style={{ padding: '16px' }}>
                      <span style={{
                        backgroundColor: statusColors.bg,
                        color: statusColors.color,
                        padding: '4px 10px',
                        borderRadius: '16px',
                        fontSize: '0.75rem',
                        fontWeight: 600,
                        display: 'inline-block'
                      }}>
                        {order.status}
                      </span>
                    </td>
                    <td style={{ padding: '16px', textAlign: 'right' }}>
                      <Link to={`/orders/${order.id}`} style={{ color: 'var(--accent)', fontWeight: 500, fontSize: '0.875rem' }}>
                        View Details
                      </Link>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>

          {data.totalPages > 1 && (
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '16px', borderTop: '1px solid var(--border)' }}>
              <span style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
                Showing page {data.page + 1} of {data.totalPages}
              </span>
              <div style={{ display: 'flex', gap: '8px' }}>
                <button 
                  className="btn btn-secondary flex-center" 
                  style={{ padding: '6px 12px' }}
                  onClick={() => setPage(p => Math.max(0, p - 1))}
                  disabled={data.first || loading}
                >
                  <ChevronLeft size={16} /> Prev
                </button>
                <button 
                  className="btn btn-secondary flex-center" 
                  style={{ padding: '6px 12px' }}
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
