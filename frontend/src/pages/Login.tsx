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
    <div className="animate-fade-in auth-split-layout" style={{ backgroundColor: 'var(--bg-primary)' }}>
      {/* Image Panel */}
      <div className="auth-image-panel">
        <img src="/assets/auth_bg.jpg" alt="VÉLURE Lifestyle" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
        <div style={{ position: 'absolute', inset: 0, backgroundColor: 'rgba(0,0,0,0.1)' }} />
        <div style={{ position: 'absolute', bottom: '60px', left: '60px', color: 'white' }}>
          <h2 style={{ fontFamily: 'var(--font-title)', fontSize: '3rem', letterSpacing: '0.05em', marginBottom: '8px' }}>VÉLURE</h2>
          <p style={{ fontSize: '1rem', letterSpacing: '0.1em', textTransform: 'uppercase' }}>Join the Inner Circle</p>
        </div>
      </div>

      {/* Form Panel */}
      <div className="auth-form-panel">
        <div style={{ width: '100%', maxWidth: '420px' }}>
          {/* Header */}
          <div style={{ textAlign: 'center', marginBottom: '40px' }}>
            <h1 className="title-medium" style={{ marginBottom: '12px' }}>Welcome Back</h1>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.95rem' }}>
              Sign in to access your curated profile.
            </p>
          </div>

          {/* Error Callouts */}
          {validationError && (
            <div className="alert alert-error" style={{ fontSize: '0.875rem', padding: '12px', marginBottom: '24px' }}>
              {validationError}
            </div>
          )}
          {apiError && (
            <div className="alert alert-error" style={{ fontSize: '0.875rem', padding: '12px', marginBottom: '24px' }}>
              {apiError}
            </div>
          )}

          {/* Login Form */}
          <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
            {/* Email field */}
            <div className="form-group-premium">
              <label className="form-label-premium">Email Address</label>
              <div className="premium-input-container">
                <Mail size={16} style={{ position: 'absolute', left: '16px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
                <input
                  type="email"
                  placeholder="you@example.com"
                  className="input-field"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  style={{ paddingLeft: '48px', height: '48px' }}
                  disabled={isSubmitting}
                />
              </div>
            </div>

            {/* Password field */}
            <div className="form-group-premium">
              <label className="form-label-premium">Password</label>
              <div className="premium-input-container">
                <Lock size={16} style={{ position: 'absolute', left: '16px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
                <input
                  type="password"
                  placeholder="Enter your password"
                  className="input-field"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  style={{ paddingLeft: '48px', height: '48px' }}
                  disabled={isSubmitting}
                />
              </div>
            </div>

            {/* Submit button */}
            <button
              type="submit"
              className="btn btn-primary flex-center"
              style={{ width: '100%', padding: '16px', marginTop: '16px' }}
              disabled={isSubmitting}
            >
              {isSubmitting ? (
                <span>Signing In...</span>
              ) : (
                <span className="flex-center" style={{ gap: '8px' }}>
                  <span>Sign In</span>
                  <ArrowRight size={16} />
                </span>
              )}
            </button>
          </form>

          <div style={{ textAlign: 'center', marginTop: '24px', fontSize: '0.875rem' }}>
            <Link to="/forgot-password" style={{ color: 'var(--text-secondary)', textDecoration: 'underline' }}>
              Forgot your password?
            </Link>
          </div>

          <div style={{ borderTop: '1px solid var(--border)', marginTop: '40px', paddingTop: '32px', textAlign: 'center', fontSize: '0.9rem', color: 'var(--text-secondary)' }}>
            New to VÉLURE?{' '}
            <Link to="/register" style={{ color: 'var(--text-primary)', fontWeight: 600, borderBottom: '1px solid var(--text-primary)', paddingBottom: '2px' }}>
              Create an account
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
};
