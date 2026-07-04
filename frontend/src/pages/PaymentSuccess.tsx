import React from 'react';
import { Link } from 'react-router-dom';
import { CheckCircle2, PackageSearch } from 'lucide-react';

export const PaymentSuccess: React.FC = () => {
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
        <CheckCircle2 size={44} style={{ color: 'var(--success)' }} />
        <div>
          <h1 style={{ fontFamily: 'var(--font-title)', marginBottom: '12px' }}>
            Payment received
          </h1>
          <p style={{ color: 'var(--text-secondary)', lineHeight: 1.7 }}>
            Thanks for checking out. Payment confirmation may still be processing, and your order will update after Stripe sends verified payment confirmation.
          </p>
        </div>
        <Link
          to="/login?returnTo=%2Forders"
          className="btn btn-primary"
          style={{ alignSelf: 'flex-start' }}
        >
          <PackageSearch size={18} />
          Sign in to view orders
        </Link>
      </section>
    </div>
  );
};
