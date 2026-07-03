import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { request } from '../api/apiClient';
import { Compass, ShieldCheck, ArrowRight, Truck } from 'lucide-react';

interface Category {
  id: number;
  name: string;
  description: string;
  imageUrl?: string;
}

export const Home: React.FC = () => {
  const [categories, setCategories] = useState<Category[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchCategories = async () => {
      try {
        const data = await request<Category[]>('/categories');
        setCategories(data);
      } catch (err: any) {
        setError(err.message || 'Failed to load categories');
      } finally {
        setIsLoading(false);
      }
    };
    fetchCategories();
  }, []);

  return (
    <div className="animate-fade-in" style={{ paddingBottom: '60px' }}>
      {/* Hero Banner Section */}
      <section style={{
        position: 'relative',
        height: '500px',
        backgroundColor: 'var(--bg-secondary)',
        backgroundImage: 'linear-gradient(135deg, var(--bg-secondary) 0%, rgba(180, 83, 9, 0.05) 100%)',
        overflow: 'hidden',
        display: 'flex',
        alignItems: 'center',
        marginBottom: '60px'
      }}>
        <div className="container" style={{ position: 'relative', zIndex: 2 }}>
          <div style={{ maxWidth: '600px' }}>
            <span style={{
              color: 'var(--accent)',
              textTransform: 'uppercase',
              letterSpacing: '0.15em',
              fontWeight: 600,
              fontSize: '0.875rem',
              display: 'block',
              marginBottom: '12px'
            }}>New Collection 2026</span>
            <h1 className="title-large" style={{
              fontFamily: 'var(--font-title)',
              marginBottom: '20px',
              fontWeight: 800,
              lineHeight: 1.1
            }}>Elevate Your Everyday Style</h1>
            <p style={{
              fontSize: '1.125rem',
              color: 'var(--text-secondary)',
              marginBottom: '30px'
            }}>
              Discover premium, ethically crafted essentials designed to complement your life. Simplicity refined.
            </p>
            <Link to="/catalog" className="btn btn-primary btn-lg flex-center" style={{
              display: 'inline-flex',
              padding: '14px 28px',
              fontSize: '1rem',
              gap: '8px'
            }}>
              <span>Browse Catalog</span>
              <ArrowRight size={18} />
            </Link>
          </div>
        </div>
      </section>

      {/* Categories Grid */}
      <section className="container" style={{ marginBottom: '60px' }}>
        <div style={{ textAlign: 'center', marginBottom: '40px' }}>
          <h2 className="title-medium" style={{ marginBottom: '12px' }}>Shop By Category</h2>
          <p style={{ color: 'var(--text-secondary)' }}>Explore curated categories tailored for your wardrobe.</p>
        </div>

        {isLoading ? (
          <div className="flex-center" style={{ height: '200px' }}>
            <div style={{
              border: '4px solid var(--border)',
              borderTopColor: 'var(--accent)',
              borderRadius: 'var(--radius-full)',
              width: '40px',
              height: '40px',
              animation: 'spin 1s linear infinite'
            }} />
          </div>
        ) : error ? (
          <div className="alert alert-error">{error}</div>
        ) : categories.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '40px', color: 'var(--text-muted)' }}>
            No categories available at the moment.
          </div>
        ) : (
          <div className="grid grid-3">
            {categories.map((category) => (
              <Link
                key={category.id}
                to={`/catalog?categoryId=${category.id}`}
                className="card"
                style={{
                  display: 'flex',
                  flexDirection: 'column',
                  height: '350px',
                  position: 'relative'
                }}
              >
                {/* Image Placeholder/Background */}
                <div style={{
                  flexGrow: 1,
                  backgroundColor: 'var(--bg-secondary)',
                  backgroundImage: category.imageUrl ? `url(${category.imageUrl})` : 'none',
                  backgroundSize: 'cover',
                  backgroundPosition: 'center',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  color: 'var(--text-muted)'
                }}>
                  {!category.imageUrl && <Compass size={48} />}
                </div>

                {/* Details Footer */}
                <div style={{
                  padding: '24px',
                  backgroundColor: 'var(--bg-card)',
                  borderTop: '1px solid var(--border)'
                }}>
                  <h3 className="title-small" style={{ marginBottom: '6px', color: 'var(--text-primary)' }}>
                    {category.name}
                  </h3>
                  <p style={{
                    fontSize: '0.875rem',
                    color: 'var(--text-secondary)',
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                    whiteSpace: 'nowrap'
                  }}>
                    {category.description}
                  </p>
                </div>
              </Link>
            ))}
          </div>
        )}
      </section>

      {/* Brand Values */}
      <section style={{ backgroundColor: 'var(--bg-secondary)', padding: '60px 0', borderTop: '1px solid var(--border)', borderBottom: '1px solid var(--border)' }}>
        <div className="container grid grid-3">
          <div style={{ textAlign: 'center', padding: '20px' }}>
            <div className="flex-center" style={{
              color: 'var(--accent)',
              marginBottom: '16px'
            }}>
              <ShieldCheck size={36} />
            </div>
            <h3 style={{ marginBottom: '8px' }}>Premium Quality</h3>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.925rem' }}>
              We source high-grade sustainable fibers to ensure durability and comfort.
            </p>
          </div>
          <div style={{ textAlign: 'center', padding: '20px' }}>
            <div className="flex-center" style={{
              color: 'var(--accent)',
              marginBottom: '16px'
            }}>
              <Truck size={36} />
            </div>
            <h3 style={{ marginBottom: '8px' }}>Global Shipping</h3>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.925rem' }}>
              Reliable delivery to your doorstep with real-time tracking snapshots.
            </p>
          </div>
          <div style={{ textAlign: 'center', padding: '20px' }}>
            <div className="flex-center" style={{
              color: 'var(--accent)',
              marginBottom: '16px'
            }}>
              <Compass size={36} />
            </div>
            <h3 style={{ marginBottom: '8px' }}>Timeless Designs</h3>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.925rem' }}>
              Wardrobe staples designed to transcend passing seasonal trends.
            </p>
          </div>
        </div>
      </section>

      {/* Custom Spin Keyframes */}
      <style>{`
        @keyframes spin {
          0% { transform: rotate(0deg); }
          100% { transform: rotate(360deg); }
        }
      `}</style>
    </div>
  );
};
