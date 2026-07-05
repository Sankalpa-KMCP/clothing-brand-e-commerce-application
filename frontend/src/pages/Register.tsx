import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Lock, Mail, User, ArrowRight } from 'lucide-react';

export const Register: React.FC = () => {
  const { register } = useAuth();
  const navigate = useNavigate();

  const [email, setEmail] = useState('');
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');

  const [validationError, setValidationError] = useState<string | null>(null);
  const [apiError, setApiError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setValidationError(null);
    setApiError(null);

    // Client side checks
    if (!email.trim() || !firstName.trim() || !lastName.trim() || !password || !confirmPassword) {
      setValidationError('Please fill in all fields.');
      return;
    }

    if (!/\S+@\S+\.\S+/.test(email)) {
      setValidationError('Please enter a valid email address.');
      return;
    }

    if (password.length < 8) {
      setValidationError('Password must be at least 8 characters long.');
      return;
    }

    if (password !== confirmPassword) {
      setValidationError('Passwords do not match.');
      return;
    }

    setIsSubmitting(true);
    try {
      const result = await register(
        email.trim(),
        firstName.trim(),
        lastName.trim(),
        password
      );
      if (result.verificationRequired) {
        navigate('/verification-sent', { state: { email: result.user.email }, replace: true });
        return;
      }
      navigate('/profile');
    } catch (err: any) {
      setApiError(err.message || 'Registration failed. Email may already be in use.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="animate-fade-in auth-split-layout-reverse" style={{ backgroundColor: 'var(--bg-primary)' }}>
      {/* Image Panel */}
      <div className="auth-image-panel">
        <img src="/assets/velure_register_hero_alt_1783237099499.jpg" alt="VÉLURE Lifestyle" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
        <div style={{ position: 'absolute', inset: 0, backgroundColor: 'rgba(0,0,0,0.1)' }} />
        <div style={{ position: 'absolute', bottom: '60px', left: '60px', color: 'white' }}>
          <h2 style={{ fontFamily: 'var(--font-title)', fontSize: '3rem', letterSpacing: '0.05em', marginBottom: '8px' }}>VÉLURE</h2>
          <p style={{ fontSize: '1rem', letterSpacing: '0.1em', textTransform: 'uppercase' }}>Discover Elegance</p>
        </div>
      </div>

      {/* Form Panel */}
      <div className="auth-form-panel">
        <div style={{ width: '100%', maxWidth: '460px' }}>
          {/* Header */}
          <div style={{ textAlign: 'center', marginBottom: '32px' }}>
            <h1 className="title-medium" style={{ marginBottom: '12px' }}>Create Account</h1>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.95rem' }}>
              Register to save addresses and track your orders.
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

          {/* Registration Form */}
          <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            
            {/* First Name & Last Name row */}
            <div className="grid grid-2" style={{ gap: '16px' }}>
              <div className="form-group-premium">
                <label className="form-label-premium">First Name</label>
                <div className="premium-input-container">
                  <User size={16} style={{ position: 'absolute', left: '16px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
                  <input
                    type="text"
                    placeholder="John"
                    className="input-field"
                    value={firstName}
                    onChange={(e) => setFirstName(e.target.value)}
                    style={{ paddingLeft: '48px', height: '48px' }}
                    disabled={isSubmitting}
                  />
                </div>
              </div>

              <div className="form-group-premium">
                <label className="form-label-premium">Last Name</label>
                <div className="premium-input-container">
                  <User size={16} style={{ position: 'absolute', left: '16px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
                  <input
                    type="text"
                    placeholder="Doe"
                    className="input-field"
                    value={lastName}
                    onChange={(e) => setLastName(e.target.value)}
                    style={{ paddingLeft: '48px', height: '48px' }}
                    disabled={isSubmitting}
                  />
                </div>
              </div>
            </div>

            {/* Email Address */}
            <div className="form-group-premium">
              <label className="form-label-premium">Email Address</label>
              <div className="premium-input-container">
                <Mail size={16} style={{ position: 'absolute', left: '16px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
                <input
                  type="email"
                  placeholder="john.doe@example.com"
                  className="input-field"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  style={{ paddingLeft: '48px', height: '48px' }}
                  disabled={isSubmitting}
                />
              </div>
            </div>

            {/* Password */}
            <div className="form-group-premium">
              <label className="form-label-premium">Password</label>
              <div className="premium-input-container">
                <Lock size={16} style={{ position: 'absolute', left: '16px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
                <input
                  type="password"
                  placeholder="At least 8 characters"
                  className="input-field"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  style={{ paddingLeft: '48px', height: '48px' }}
                  disabled={isSubmitting}
                />
              </div>
            </div>

            {/* Confirm Password */}
            <div className="form-group-premium">
              <label className="form-label-premium">Confirm Password</label>
              <div className="premium-input-container">
                <Lock size={16} style={{ position: 'absolute', left: '16px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
                <input
                  type="password"
                  placeholder="Re-enter your password"
                  className="input-field"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  style={{ paddingLeft: '48px', height: '48px' }}
                  disabled={isSubmitting}
                />
              </div>
            </div>

            {/* Submit */}
            <button
              type="submit"
              className="btn btn-primary flex-center"
              style={{ width: '100%', padding: '16px', marginTop: '16px' }}
              disabled={isSubmitting}
            >
              {isSubmitting ? (
                <span>Creating Account...</span>
              ) : (
                <span className="flex-center" style={{ gap: '8px' }}>
                  <span>Create Account</span>
                  <ArrowRight size={16} />
                </span>
              )}
            </button>
          </form>

          <div style={{ borderTop: '1px solid var(--border)', marginTop: '40px', paddingTop: '32px', textAlign: 'center', fontSize: '0.9rem', color: 'var(--text-secondary)' }}>
            Already have an account?{' '}
            <Link to="/login" style={{ color: 'var(--text-primary)', fontWeight: 600, borderBottom: '1px solid var(--text-primary)', paddingBottom: '2px' }}>
              Sign In
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
};
