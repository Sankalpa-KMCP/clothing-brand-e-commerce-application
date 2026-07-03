import React, { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Lock, Mail, ArrowRight } from 'lucide-react';
import { sanitizeReturnTo } from '../utils/returnTo';

export const Login: React.FC = () => {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const safeReturnTo = sanitizeReturnTo(searchParams.get('returnTo'));

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  
  const [validationError, setValidationError] = useState<string | null>(null);
  const [apiError, setApiError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setValidationError(null);
    setApiError(null);

    // Simple client-side validations
    if (!email.trim() || !password.trim()) {
      setValidationError('Please fill in all fields.');
      return;
    }

    if (!/\S+@\S+\.\S+/.test(email)) {
      setValidationError('Please enter a valid email address.');
      return;
    }

    setIsSubmitting(true);
    try {
      await login(email.trim(), password);
      navigate(safeReturnTo ?? '/profile', { replace: true });
    } catch (err: any) {
      setApiError(err.message || 'Invalid credentials or login failed.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="flex-center animate-fade-in" style={{ minHeight: 'calc(100vh - 180px)', padding: '40px 20px' }}>
      <div className="card" style={{
        width: '100%',
        maxWidth: '450px',
        padding: '40px 32px'
      }}>
        {/* Header */}
        <div style={{ textAlign: 'center', marginBottom: '32px' }}>
          <h1 className="title-medium" style={{ marginBottom: '8px' }}>Welcome Back</h1>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.925rem' }}>
            Sign in to access your profile and personalized shop.
          </p>
        </div>

        {/* Error Callouts */}
        {validationError && (
          <div className="alert alert-error" style={{ fontSize: '0.875rem', padding: '12px' }}>
            {validationError}
          </div>
        )}
        {apiError && (
          <div className="alert alert-error" style={{ fontSize: '0.875rem', padding: '12px' }}>
            {apiError}
          </div>
        )}

        {/* Login Form */}
        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
          {/* Email field */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
            <label style={{ fontSize: '0.875rem', fontWeight: 600, color: 'var(--text-secondary)' }}>Email Address</label>
            <div style={{ position: 'relative' }}>
              <Mail size={16} style={{
                position: 'absolute',
                left: '16px',
                top: '50%',
                transform: 'translateY(-50%)',
                color: 'var(--text-muted)'
              }} />
              <input
                type="email"
                placeholder="you@example.com"
                className="input-field"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                style={{ paddingLeft: '48px' }}
                disabled={isSubmitting}
              />
            </div>
          </div>

          {/* Password field */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
            <label style={{ fontSize: '0.875rem', fontWeight: 600, color: 'var(--text-secondary)' }}>Password</label>
            <div style={{ position: 'relative' }}>
              <Lock size={16} style={{
                position: 'absolute',
                left: '16px',
                top: '50%',
                transform: 'translateY(-50%)',
                color: 'var(--text-muted)'
              }} />
              <input
                type="password"
                placeholder="Enter your password"
                className="input-field"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                style={{ paddingLeft: '48px' }}
                disabled={isSubmitting}
              />
            </div>
          </div>

          {/* Submit button */}
          <button
            type="submit"
            className="btn btn-primary flex-center"
            style={{ width: '100%', padding: '12px', marginTop: '10px' }}
            disabled={isSubmitting}
          >
            {isSubmitting ? (
              <span>Signing In...</span>
            ) : (
              <span className="flex-center" style={{ gap: '6px' }}>
                <span>Sign In</span>
                <ArrowRight size={16} />
              </span>
            )}
          </button>
        </form>

        <div style={{ textAlign: 'right', marginTop: '14px', fontSize: '0.875rem' }}>
          <Link to="/forgot-password" style={{
            color: 'var(--accent)',
            fontWeight: 600
          }}>
            Forgot password?
          </Link>
        </div>

        {/* Footer Navigation link */}
        <div style={{
          textAlign: 'center',
          marginTop: '32px',
          fontSize: '0.875rem',
          color: 'var(--text-secondary)'
        }}>
          Don't have an account?{' '}
          <Link to="/register" style={{
            color: 'var(--accent)',
            fontWeight: 600
          }}>
            Register Now
          </Link>
        </div>
      </div>
    </div>
  );
};
