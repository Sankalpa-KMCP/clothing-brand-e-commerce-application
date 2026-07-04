import React from 'react';
import { Link } from 'react-router-dom';
import { ShoppingBag, XCircle } from 'lucide-react';

export const PaymentCancel: React.FC = () => {
  return (
    <div className="flex-center animate-fade-in" style={{ minHeight: 'calc(100vh - 180px)', padding: '60px 20px' }}>
      <section className="verification-card-premium" style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '20px' }}>
        <XCircle size={44} style={{ color: 'var(--error)', margin: '0 auto' }} />
        <div>
          <h1 className="title-medium" style={{ marginBottom: '12px' }}>
            Payment cancelled
          </h1>
          <p style={{ color: 'var(--text-secondary)', lineHeight: 1.7, fontSize: '0.95rem' }}>
            Your payment was cancelled. Your bag remains available, and any temporary reservation will stay active only until its normal expiry.
          </p>
        </div>
        <Link
          to="/login?returnTo=%2Fcart"
          className="btn btn-primary"
          style={{ display: 'inline-flex', alignSelf: 'center', padding: '12px 24px' }}
        >
          <ShoppingBag size={18} />
          Sign in to return to bag
        </Link>
      </section>
    </div>
  );
};
