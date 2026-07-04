import React from 'react';
import { Link } from 'react-router-dom';
import { CheckCircle2, PackageSearch } from 'lucide-react';

export const PaymentSuccess: React.FC = () => {
  return (
    <div className="flex-center animate-fade-in" style={{ minHeight: 'calc(100vh - 180px)', padding: '60px 20px' }}>
      <section className="verification-card-premium" style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '20px' }}>
        <CheckCircle2 size={44} style={{ color: '#10b981', margin: '0 auto' }} />
        <div>
          <h1 className="title-medium" style={{ marginBottom: '12px' }}>
            Payment received
          </h1>
          <p style={{ color: 'var(--text-secondary)', lineHeight: 1.7, fontSize: '0.95rem' }}>
            Thanks for checking out. Payment confirmation may still be processing, and your order will update after Stripe sends verified payment confirmation.
          </p>
        </div>
        <Link
          to="/login?returnTo=%2Forders"
          className="btn btn-primary"
          style={{ display: 'inline-flex', alignSelf: 'center', padding: '12px 24px' }}
        >
          <PackageSearch size={18} />
          Sign in to view orders
        </Link>
      </section>
    </div>
  );
};
