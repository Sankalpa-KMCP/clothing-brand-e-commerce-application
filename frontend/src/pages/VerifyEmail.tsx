import React, { useEffect, useRef, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { CheckCircle2, MailWarning } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export const VerifyEmail: React.FC = () => {
  const { verifyEmail } = useAuth();
  const [searchParams] = useSearchParams();
  const tokenRef = useRef<string | null>(searchParams.get('token'));
  const didSubmitRef = useRef(false);
  const [status, setStatus] = useState<'loading' | 'success' | 'error'>(
    tokenRef.current ? 'loading' : 'error'
  );
  const [message, setMessage] = useState(
    tokenRef.current ? 'Verifying your email address...' : 'This verification link is invalid or expired.'
  );

  useEffect(() => {
    const token = tokenRef.current;
    if (!token || didSubmitRef.current) {
      return;
    }
    didSubmitRef.current = true;
    window.history.replaceState(null, '', '/verify-email');

    verifyEmail(token)
      .then((responseMessage) => {
        tokenRef.current = null;
        setStatus('success');
        setMessage(responseMessage);
      })
      .catch((err: any) => {
        setStatus('error');
        setMessage(err.message || 'This verification link is invalid or expired.');
      });
  }, [verifyEmail]);

  return (
    <div className="flex-center animate-fade-in" style={{ minHeight: 'calc(100vh - 180px)', padding: '40px 20px' }}>
      <div className="verification-card-premium">
        {status === 'success' ? (
          <CheckCircle2 size={44} style={{ color: 'var(--success)', marginBottom: '18px' }} />
        ) : (
          <MailWarning size={44} style={{ color: status === 'loading' ? 'var(--accent)' : 'var(--error)', marginBottom: '18px' }} />
        )}
        <h1 className="title-medium" style={{ marginBottom: '12px' }}>
          {status === 'success' ? 'Email verified' : status === 'loading' ? 'Verifying email' : 'Verification unavailable'}
        </h1>
        <p style={{ color: 'var(--text-secondary)', lineHeight: 1.6, marginBottom: '24px' }}>
          {message}
        </p>
        <div className="flex-center" style={{ gap: '12px', flexWrap: 'wrap', justifyContent: 'center' }}>
          <Link to="/login" className="btn btn-primary" style={{ padding: '12px 24px' }}>
            Sign in
          </Link>
          {status === 'error' && (
            <Link to="/verification-sent" className="btn btn-secondary" style={{ padding: '12px 24px' }}>
              Request a new link
            </Link>
          )}
        </div>
      </div>
    </div>
  );
};
