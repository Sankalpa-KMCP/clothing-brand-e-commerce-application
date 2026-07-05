import React from 'react';
import { Link } from 'react-router-dom';
import { Package, ShoppingCart, Users, TrendingUp } from 'lucide-react';

export const AdminDashboard: React.FC = () => {
  return (
    <div className="animate-fade-in">
      <div style={{ marginBottom: '40px' }}>
        <h1 style={{ fontSize: '2rem', fontFamily: 'var(--font-title)', marginBottom: '8px' }}>Dashboard Overview</h1>
        <p style={{ color: 'var(--text-secondary)' }}>Welcome to the VÉLURE Admin Control Center.</p>
      </div>

      <div className="grid grid-4" style={{ gap: '24px', marginBottom: '40px' }}>
        <div style={{ backgroundColor: 'var(--bg-card)', padding: '24px', borderRadius: '12px', border: '1px solid var(--border)', display: 'flex', flexDirection: 'column', gap: '16px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <h3 style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', fontWeight: 500, textTransform: 'uppercase' }}>Total Revenue</h3>
            <TrendingUp size={20} color="var(--accent)" />
          </div>
          <div style={{ fontSize: '2rem', fontWeight: 700 }}>LKR 0</div>
          <div style={{ fontSize: '0.75rem', color: 'var(--success)' }}>+0% from last month</div>
        </div>

        <div style={{ backgroundColor: 'var(--bg-card)', padding: '24px', borderRadius: '12px', border: '1px solid var(--border)', display: 'flex', flexDirection: 'column', gap: '16px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <h3 style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', fontWeight: 500, textTransform: 'uppercase' }}>Orders</h3>
            <ShoppingCart size={20} color="var(--accent)" />
          </div>
          <div style={{ fontSize: '2rem', fontWeight: 700 }}>0</div>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Pending fulfillment</div>
        </div>

        <div style={{ backgroundColor: 'var(--bg-card)', padding: '24px', borderRadius: '12px', border: '1px solid var(--border)', display: 'flex', flexDirection: 'column', gap: '16px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <h3 style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', fontWeight: 500, textTransform: 'uppercase' }}>Products</h3>
            <Package size={20} color="var(--accent)" />
          </div>
          <div style={{ fontSize: '2rem', fontWeight: 700 }}>0</div>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Active in catalog</div>
        </div>
        
        <div style={{ backgroundColor: 'var(--bg-card)', padding: '24px', borderRadius: '12px', border: '1px solid var(--border)', display: 'flex', flexDirection: 'column', gap: '16px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <h3 style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', fontWeight: 500, textTransform: 'uppercase' }}>Customers</h3>
            <Users size={20} color="var(--accent)" />
          </div>
          <div style={{ fontSize: '2rem', fontWeight: 700 }}>0</div>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Registered accounts</div>
        </div>
      </div>

      <div className="grid grid-2" style={{ gap: '24px' }}>
        <div style={{ backgroundColor: 'var(--bg-card)', padding: '32px', borderRadius: '12px', border: '1px solid var(--border)' }}>
          <h2 style={{ fontSize: '1.25rem', marginBottom: '16px', fontWeight: 600 }}>Quick Actions</h2>
          <div style={{ display: 'flex', gap: '16px', flexWrap: 'wrap' }}>
            <Link to="/admin/products/new" className="btn btn-primary">Add New Product</Link>
            <Link to="/admin/orders" className="btn btn-secondary">View Pending Orders</Link>
          </div>
        </div>
      </div>
    </div>
  );
};
