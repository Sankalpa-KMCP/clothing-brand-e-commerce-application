import React, { useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { MailCheck, Send } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export const VerificationSent: React.FC = () => {
  const { resendVerification } = useAuth();
  const location = useLocation();
  const initialEmail = typeof location.state === 'object' && location.state && 'email' in location.state
    ? String(location.state.email)
    : '';

  const [email, setEmail] = useState(initialEmail);
  const [message, setMessage] = useState<string | null>(
    initialEmail ? 'Check your email for a verification link before signing in.' : null
  );
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleResend = async (event: React.FormEvent) => {
    event.preventDefault();
    setError(null);
    setMessage(null);

    if (!/\S+@\S+\.\S+/.test(email.trim())) {
      setError('Enter the email address used for registration.');
      return;
    }

    setIsSubmitting(true);
    try {
      const responseMessage = await resendVerification(email.trim());
      setMessage(responseMessage);
    } catch (err: any) {
      setError(err.message || 'We could not process that request right now.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="flex-center animate-fade-in" style={{ minHeight: 'calc(100vh - 180px)', padding: '40px 20px' }}>
      <div className="verification-card-premium">
        <div style={{ textAlign: 'center', marginBottom: '28px' }}>
          <MailCheck size={42} style={{ color: 'var(--accent)', marginBottom: '16px' }} />
          <h1 className="title-medium" style={{ marginBottom: '8px' }}>Verify your email</h1>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.925rem', lineHeight: 1.6 }}>
            Your account is created. Verify your email address before signing in.
          </p>
        </div>

        {message && (
          <div className="alert alert-success" style={{ fontSize: '0.875rem', padding: '12px' }}>
            {message}
          </div>
        )}
        {error && (
          <div className="alert alert-error" style={{ fontSize: '0.875rem', padding: '12px' }}>
            {error}
          </div>
        )}

        <form onSubmit={handleResend} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          <div className="form-group-premium">
            <label className="form-label-premium">Email Address</label>
            <input
              type="email"
              className="input-field"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              placeholder="you@example.com"
              disabled={isSubmitting}
            />
          </div>
          <button type="submit" className="btn btn-primary flex-center" style={{ padding: '14px' }} disabled={isSubmitting}>
            <Send size={16} />
            {isSubmitting ? 'Sending...' : 'Resend verification'}
          </button>
        </form>

        <div style={{ textAlign: 'center', marginTop: '28px', fontSize: '0.875rem' }}>
          <Link to="/login" style={{ color: 'var(--accent)', fontWeight: 600 }}>
            Back to sign in
          </Link>
        </div>
      </div>
    </div>
  );
};
