import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { Mail, Send } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export const ForgotPassword: React.FC = () => {
  const { forgotPassword } = useAuth();
  const [email, setEmail] = useState('');
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    setMessage(null);
    setError(null);

    if (!/\S+@\S+\.\S+/.test(email.trim())) {
      setError('Enter the email address for your account.');
      return;
    }

    setIsSubmitting(true);
    try {
      const responseMessage = await forgotPassword(email.trim());
      setMessage(responseMessage);
    } catch (err: any) {
      setError(err.message || 'We could not process that request right now.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="flex-center animate-fade-in" style={{ minHeight: 'calc(100vh - 180px)', padding: '40px 20px' }}>
      <div className="card" style={{ width: '100%', maxWidth: '480px', padding: '40px 32px' }}>
        <div style={{ textAlign: 'center', marginBottom: '28px' }}>
          <Mail size={40} style={{ color: 'var(--accent)', marginBottom: '16px' }} />
          <h1 className="title-medium" style={{ marginBottom: '8px' }}>Reset your password</h1>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.925rem', lineHeight: 1.6 }}>
            Enter your email address and we will send reset instructions if the account can receive them.
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

        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
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
            {isSubmitting ? 'Sending...' : 'Send reset instructions'}
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
