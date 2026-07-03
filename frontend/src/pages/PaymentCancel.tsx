import React from 'react';
import { Link } from 'react-router-dom';
import { ShoppingBag, XCircle } from 'lucide-react';

export const PaymentCancel: React.FC = () => {
  return (
    <div className="container" style={{ padding: '64px 0' }}>
      <section
        className="card"
        style={{
          maxWidth: '720px',
          margin: '0 auto',
          padding: '32px',
          display: 'flex',
          flexDirection: 'column',
          gap: '20px'
        }}
      >
        <XCircle size={44} style={{ color: 'var(--error)' }} />
        <div>
          <h1 style={{ fontFamily: 'var(--font-title)', marginBottom: '12px' }}>
            Payment cancelled
          </h1>
          <p style={{ color: 'var(--text-secondary)', lineHeight: 1.7 }}>
            Your payment was cancelled. Your bag remains available, and any temporary reservation will stay active only until its normal expiry.
          </p>
        </div>
        <Link
          to="/login?returnTo=%2Fcart"
          className="btn btn-primary"
          style={{ alignSelf: 'flex-start' }}
        >
          <ShoppingBag size={18} />
          Sign in to return to bag
        </Link>
      </section>
    </div>
  );
};
