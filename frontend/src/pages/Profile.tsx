import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { request } from '../api/apiClient';
import { User, Mail, Shield, LogOut, ArrowRight, Upload, Copy, Check } from 'lucide-react';

const AdminImageUploader: React.FC = () => {
  const [file, setFile] = useState<File | null>(null);
  const [imageUrl, setImageUrl] = useState<string>('');
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [copied, setCopied] = useState<boolean>(false);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      const selectedFile = e.target.files[0];
      const ext = selectedFile.name.split('.').pop()?.toLowerCase();
      if (ext !== 'png' && ext !== 'jpg' && ext !== 'jpeg') {
        setError('Only JPEG and PNG images are allowed.');
        setFile(null);
        return;
      }
      setFile(selectedFile);
      setError(null);
      setImageUrl('');
    }
  };

  const handleUpload = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!file) return;

    setIsLoading(true);
    setError(null);
    setImageUrl('');

    try {
      const formData = new FormData();
      formData.append('file', file);

      const data = await request<{ imageUrl: string }>('/admin/images', {
        method: 'POST',
        body: formData as any
      });
      setImageUrl(data.imageUrl);
      setFile(null);
    } catch (err: any) {
      setError(err.message || 'Failed to upload image.');
    } finally {
      setIsLoading(false);
    }
  };

  const copyToClipboard = () => {
    if (imageUrl) {
      navigator.clipboard.writeText(imageUrl);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  return (
    <div className="card" style={{ padding: '32px', marginTop: '24px', display: 'flex', flexDirection: 'column', gap: '20px' }}>
      <div>
        <h2 className="title-small" style={{ fontWeight: 700, display: 'flex', alignItems: 'center', gap: '8px' }}>
          <Shield size={20} style={{ color: 'var(--accent)' }} />
          Admin Image Uploader
        </h2>
        <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', marginTop: '4px' }}>
          Upload product or category images to the local server.
        </p>
      </div>

      <form onSubmit={handleUpload} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
        <div style={{
          border: '2px dashed var(--border)',
          borderRadius: 'var(--radius-lg)',
          padding: '24px',
          textAlign: 'center',
          backgroundColor: 'var(--bg-secondary)',
          cursor: 'pointer',
          position: 'relative'
        }}>
          <input
            type="file"
            accept="image/png, image/jpeg"
            onChange={handleFileChange}
            style={{
              position: 'absolute',
              top: 0,
              left: 0,
              width: '100%',
              height: '100%',
              opacity: 0,
              cursor: 'pointer'
            }}
          />
          <div className="flex-center" style={{ flexDirection: 'column', gap: '8px' }}>
            <Upload size={32} style={{ color: 'var(--text-muted)' }} />
            <span style={{ fontSize: '0.875rem', color: 'var(--text-secondary)' }}>
              {file ? file.name : 'Click to select JPEG/PNG image'}
            </span>
          </div>
        </div>

        {error && <div className="alert alert-error" style={{ fontSize: '0.875rem', padding: '10px 16px' }}>{error}</div>}

        <button
          type="submit"
          disabled={!file || isLoading}
          className="btn btn-primary flex-center"
          style={{ width: '100%', padding: '12px' }}
        >
          {isLoading ? 'Uploading...' : 'Upload Image'}
        </button>
      </form>

      {imageUrl && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', marginTop: '10px' }}>
          <span style={{ fontSize: '0.875rem', fontWeight: 600, color: 'var(--text-primary)' }}>
            Uploaded Image Link:
          </span>
          <div style={{ display: 'flex', gap: '8px' }}>
            <input
              type="text"
              readOnly
              value={imageUrl}
              className="input-field"
              style={{ flexGrow: 1, fontSize: '0.875rem' }}
            />
            <button
              onClick={copyToClipboard}
              className="btn btn-secondary flex-center"
              style={{ padding: '0 16px' }}
            >
              {copied ? <Check size={16} /> : <Copy size={16} />}
            </button>
          </div>

          <div style={{
            marginTop: '10px',
            borderRadius: 'var(--radius-lg)',
            border: '1px solid var(--border)',
            overflow: 'hidden',
            backgroundColor: 'var(--bg-secondary)',
            height: '200px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center'
          }}>
            <img
              src={imageUrl}
              alt="Uploaded Preview"
              style={{ maxHeight: '100%', maxWidth: '100%', objectFit: 'contain' }}
            />
          </div>
        </div>
      )}
    </div>
  );
};

export const Profile: React.FC = () => {
  const { user, logout, isAuthenticated } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    // If not authenticated, redirect to login
    if (!isAuthenticated) {
      navigate('/login');
    }
  }, [isAuthenticated, navigate]);

  if (!user) {
    return null;
  }

  const handleLogout = async () => {
    await logout();
    navigate('/');
  };

  return (
    <div className="container animate-fade-in" style={{ padding: '60px 20px 80px 20px' }}>
      <div style={{ maxWidth: '600px', margin: '0 auto' }}>
        <div style={{ marginBottom: '30px' }}>
          <span className="atelier-kicker">Member Space</span>
          <h1 className="title-medium" style={{ marginTop: '6px' }}>My Account</h1>
        </div>

        {/* Profile Card */}
        <div className="card" style={{ padding: '40px 32px', display: 'flex', flexDirection: 'column', gap: '24px' }}>
          {/* User Icon/Greeting Header */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '20px' }}>
            <div className="flex-center" style={{
              width: '64px',
              height: '64px',
              borderRadius: 'var(--radius-full)',
              backgroundColor: 'var(--bg-secondary)',
              border: '1px solid var(--border)',
              color: 'var(--accent)'
            }}>
              <User size={28} strokeWidth={1.5} />
            </div>
            <div>
              <h2 className="title-small" style={{ fontWeight: 600 }}>
                {user.firstName} {user.lastName}
              </h2>
              <span style={{
                fontSize: '0.75rem',
                color: 'var(--text-secondary)',
                backgroundColor: 'var(--bg-secondary)',
                padding: '4px 10px',
                borderRadius: 'var(--radius-sm)',
                fontWeight: 600,
                letterSpacing: '0.05em',
                textTransform: 'uppercase'
              }}>
                Customer Account
              </span>
            </div>
          </div>

          <hr style={{ border: 'none', borderTop: '1px solid var(--border)', margin: '12px 0' }} />

          {/* Details fields */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
            {/* Email Address */}
            <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
              <div style={{ color: 'var(--text-muted)' }}><Mail size={20} strokeWidth={1.5} /></div>
              <div>
                <span style={{
                  fontSize: '0.75rem',
                  color: 'var(--text-muted)',
                  display: 'block',
                  fontWeight: 600,
                  textTransform: 'uppercase',
                  letterSpacing: '0.05em'
                }}>
                  Email Address
                </span>
                <span style={{ color: 'var(--text-primary)', fontWeight: 500, fontSize: '0.975rem' }}>
                  {user.email}
                </span>
              </div>
            </div>

            {/* Role Group */}
            <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
              <div style={{ color: 'var(--text-muted)' }}><Shield size={20} strokeWidth={1.5} /></div>
              <div>
                <span style={{
                  fontSize: '0.75rem',
                  color: 'var(--text-muted)',
                  display: 'block',
                  fontWeight: 600,
                  textTransform: 'uppercase',
                  letterSpacing: '0.05em'
                }}>
                  Access Role
                </span>
                <span style={{ color: 'var(--text-primary)', fontWeight: 500, fontSize: '0.975rem' }}>
                  {user.role}
                </span>
              </div>
            </div>
          </div>

          <hr style={{ border: 'none', borderTop: '1px solid var(--border)', margin: '12px 0' }} />

          {/* Action links */}
          <div style={{ display: 'flex', gap: '16px', marginTop: '10px' }}>
            <button onClick={handleLogout} className="btn btn-secondary flex-center" style={{ flexGrow: 1, padding: '14px' }}>
              <LogOut size={16} />
              <span>Sign Out</span>
            </button>
            <button onClick={() => navigate('/catalog')} className="btn btn-primary flex-center" style={{ flexGrow: 1, padding: '14px' }}>
              <span>Shop Collection</span>
              <ArrowRight size={16} />
            </button>
          </div>
        </div>

        {user.role === 'ROLE_ADMIN' && <AdminImageUploader />}
      </div>
    </div>
  );
};
