import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { sanitizeReturnTo } from '../utils/returnTo';

interface ProtectedRouteProps {
  children: React.ReactNode;
}

export const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ children }) => {
  const { isAuthenticated, isLoading } = useAuth();
  const location = useLocation();

  if (isLoading) {
    return (
      <div className="flex-center" style={{ height: '100vh' }}>
        <div style={{
          border: '4px solid var(--border)',
          borderTopColor: 'var(--accent)',
          borderRadius: 'var(--radius-full)',
          width: '40px',
          height: '40px',
          animation: 'spin 1s linear infinite'
        }} />
        <style>{`
          @keyframes spin {
            0% { transform: rotate(0deg); }
            100% { transform: rotate(360deg); }
          }
        `}</style>
      </div>
    );
  }

  if (!isAuthenticated) {
    const returnTo = sanitizeReturnTo(`${location.pathname}${location.search}`);
    return <Navigate to={returnTo ? `/login?returnTo=${encodeURIComponent(returnTo)}` : '/login'} replace />;
  }

  return <>{children}</>;
};
