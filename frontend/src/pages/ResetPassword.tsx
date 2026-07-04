import React, { useEffect, useRef, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { KeyRound, Lock } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export const ResetPassword: React.FC = () => {
  const { resetPassword } = useAuth();
  const [searchParams] = useSearchParams();
  const tokenRef = useRef<string | null>(searchParams.get('token'));
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(
    tokenRef.current ? null : 'This reset link is invalid or expired.'
  );
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (tokenRef.current && window.location.search.includes('token=')) {
      window.history.replaceState(null, '', '/reset-password');
    }
  }, []);

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    setMessage(null);
    setError(null);

    if (!tokenRef.current) {
      setError('This reset link is invalid or expired.');
      return;
    }
    if (password.length < 8) {
      setError('Password must be at least 8 characters long.');
      return;
    }
    if (password !== confirmPassword) {
      setError('Passwords do not match.');
      return;
    }

    setIsSubmitting(true);
    try {
      const responseMessage = await resetPassword(tokenRef.current, password);
      tokenRef.current = null;
      setPassword('');
      setConfirmPassword('');
      setMessage(responseMessage);
    } catch (err: any) {
      setError(err.message || 'This reset link is invalid or expired.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="flex-center animate-fade-in" style={{ minHeight: 'calc(100vh - 180px)', padding: '40px 20px' }}>
      <div className="card" style={{ width: '100%', maxWidth: '480px', padding: '40px 32px' }}>
        <div style={{ textAlign: 'center', marginBottom: '28px' }}>
          <KeyRound size={40} style={{ color: 'var(--accent)', marginBottom: '16px' }} />
          <h1 className="title-medium" style={{ marginBottom: '8px' }}>Choose a new password</h1>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.925rem', lineHeight: 1.6 }}>
            Create a new password for your account. Existing signed-in sessions will need to sign in again.
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
            <label className="form-label-premium">New Password</label>
            <div className="premium-input-container">
              <Lock size={16} style={{
                position: 'absolute',
                left: '16px',
                top: '50%',
                transform: 'translateY(-50%)',
                color: 'var(--text-muted)'
              }} />
              <input
                type="password"
                className="input-field"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                placeholder="At least 8 characters"
                style={{ paddingLeft: '48px' }}
                disabled={isSubmitting || !tokenRef.current}
              />
            </div>
          </div>
          <div className="form-group-premium">
            <label className="form-label-premium">Confirm Password</label>
            <div className="premium-input-container">
              <Lock size={16} style={{
                position: 'absolute',
                left: '16px',
                top: '50%',
                transform: 'translateY(-50%)',
                color: 'var(--text-muted)'
              }} />
              <input
                type="password"
                className="input-field"
                value={confirmPassword}
                onChange={(event) => setConfirmPassword(event.target.value)}
                placeholder="Re-enter your password"
                style={{ paddingLeft: '48px' }}
                disabled={isSubmitting || !tokenRef.current}
              />
            </div>
          </div>
          <button type="submit" className="btn btn-primary flex-center" style={{ padding: '14px' }} disabled={isSubmitting || !tokenRef.current}>
            {isSubmitting ? 'Updating...' : 'Update password'}
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
