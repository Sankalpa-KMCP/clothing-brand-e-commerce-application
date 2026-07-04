import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { request } from '../api/apiClient';
import { Compass, ShieldCheck, Sparkles, Truck, Mail } from 'lucide-react';

interface Category {
  id: number;
  name: string;
  description: string;
  imageUrl?: string;
}

interface ProductSummary {
  id: number;
  name: string;
  categoryName: string;
  imageUrl?: string;
  startingPrice?: number;
}

interface PaginatedResponse<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  size: number;
  number: number;
}

const formatPrice = (price?: number) => (
  price !== undefined ? `LKR ${price.toFixed(2)}` : 'N/A'
);

export const Home: React.FC = () => {
  const [categories, setCategories] = useState<Category[]>([]);
  const [featuredProducts, setFeaturedProducts] = useState<ProductSummary[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [isProductsLoading, setIsProductsLoading] = useState<boolean>(true);
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

  useEffect(() => {
    const fetchFeaturedProducts = async () => {
      try {
        // Fetch up to 8 products now
        const data = await request<PaginatedResponse<ProductSummary>>('/products?page=0&size=8');
        setFeaturedProducts(data.content || []);
      } catch (err) {
        console.error('Failed to load featured products', err);
      } finally {
        setIsProductsLoading(false);
      }
    };
    fetchFeaturedProducts();
  }, []);

  return (
    <div className="home-page animate-fade-in" style={{ paddingBottom: '0' }}>
      
      {/* Dynamic Split Hero */}
      <section className="hero-split-layout" style={{ display: 'flex', minHeight: '90vh', backgroundColor: 'var(--bg-primary)', flexWrap: 'wrap' }}>
        <div style={{ flex: '1 1 50%', position: 'relative', overflow: 'hidden', minHeight: '500px' }}>
          <img src="/assets/home_hero.jpg" alt="VÉLURE Main Collection" style={{ width: '100%', height: '100%', objectFit: 'cover', filter: 'brightness(0.85)' }} />
          <div style={{ position: 'absolute', bottom: '40px', left: '40px', color: 'white' }}>
            <h2 style={{ fontFamily: 'var(--font-title)', fontSize: '2.5rem', marginBottom: '16px' }}>The Spring Edit</h2>
            <Link to="/catalog" className="btn" style={{ padding: '12px 24px', backgroundColor: 'white', color: 'black', textTransform: 'uppercase', fontSize: '0.85rem', letterSpacing: '0.05em', fontWeight: 600 }}>Explore</Link>
          </div>
        </div>
        <div style={{ flex: '1 1 50%', display: 'flex', flexDirection: 'column' }}>
          <div style={{ flex: 1, display: 'flex', flexDirection: 'column', justifyContent: 'center', padding: '60px 8%', minHeight: '400px' }}>
            <span style={{ textTransform: 'uppercase', letterSpacing: '0.2em', fontSize: '0.75rem', marginBottom: '16px', color: 'var(--accent)' }}>Maison Vélure</span>
            <h1 style={{ fontFamily: 'var(--font-title)', fontSize: 'clamp(3rem, 6vw, 5rem)', lineHeight: 1.1, fontWeight: 700, marginBottom: '24px', letterSpacing: '-0.02em' }}>
              Refined <br/> for the modern <br/> aesthete.
            </h1>
            <p style={{ color: 'var(--text-secondary)', fontSize: '1.1rem', maxWidth: '400px', lineHeight: 1.6, marginBottom: '32px' }}>
              Discover our latest collection of meticulously crafted garments, designed to transcend seasons and elevate your daily wardrobe.
            </p>
            <div style={{ display: 'flex', gap: '16px', flexWrap: 'wrap' }}>
              <Link to="/catalog" className="btn btn-primary" style={{ padding: '14px 28px' }}>Shop Collection</Link>
              <a href="#story" className="btn btn-secondary" style={{ padding: '14px 28px' }}>Our Story</a>
            </div>
          </div>
          <div style={{ flex: 1, position: 'relative', overflow: 'hidden', minHeight: '300px' }}>
            <img src="/assets/hero_secondary.jpg" alt="VÉLURE Lifestyle" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
          </div>
        </div>
      </section>

      {/* Press / Social Proof Band */}
      <section style={{ borderBottom: '1px solid var(--border)', padding: '40px 0' }}>
        <div className="container">
          <p style={{ textAlign: 'center', textTransform: 'uppercase', fontSize: '0.75rem', color: 'var(--text-muted)', letterSpacing: '0.1em', marginBottom: '24px' }}>As Featured In</p>
          <div style={{ display: 'flex', justifyContent: 'center', gap: 'clamp(30px, 8vw, 80px)', flexWrap: 'wrap', opacity: 0.5, filter: 'grayscale(100%)' }}>
            <span style={{ fontFamily: 'var(--font-title)', fontSize: '1.5rem', fontWeight: 700, letterSpacing: '0.1em' }}>VOGUE</span>
            <span style={{ fontFamily: 'var(--font-title)', fontSize: '1.5rem', fontWeight: 300, letterSpacing: '0.2em' }}>BAZAAR</span>
            <span style={{ fontFamily: 'var(--font-title)', fontSize: '1.5rem', fontWeight: 600, letterSpacing: '0.05em' }}>GQ</span>
            <span style={{ fontFamily: 'var(--font-title)', fontSize: '1.5rem', fontWeight: 500, fontStyle: 'italic' }}>MONOCLE</span>
          </div>
        </div>
      </section>

      {/* Categories Section */}
      <section className="container" style={{ padding: '120px 24px' }} aria-labelledby="category-title">
        <div style={{ textAlign: 'center', marginBottom: '80px' }}>
          <h2 id="category-title" className="title-large">Curated Departments</h2>
          <p style={{ color: 'var(--text-secondary)', marginTop: '16px', fontSize: '1.1rem', maxWidth: '600px', margin: '16px auto 0' }}>
            Explore our defining categories. Masterfully crafted using sustainable materials for uncompromising quality.
          </p>
        </div>

        {isLoading ? (
          <div className="flex-center" style={{ height: '200px' }}>
            <div style={{ border: '3px solid var(--border)', borderTopColor: 'var(--accent)', borderRadius: '50%', width: '32px', height: '32px', animation: 'spin 1s linear infinite' }} />
          </div>
        ) : error ? (
          <div className="alert alert-error">{error}</div>
        ) : categories.length === 0 ? (
          <div className="empty-state">No categories available at the moment.</div>
        ) : (
          <div className="asymmetric-grid">
            {categories.map((cat, idx) => (
              <Link
                key={cat.id}
                to={`/catalog?categoryId=${cat.id}`}
                className="zoom-card"
                style={{ marginTop: idx % 2 !== 0 ? '60px' : '0' }}
              >
                <div className="zoom-card-image-wrapper">
                  {cat.imageUrl ? (
                    <img src={cat.imageUrl} alt={cat.name} className="zoom-card-image" />
                  ) : (
                    <div className="flex-center" style={{ width: '100%', height: '100%', backgroundColor: 'var(--bg-secondary)', color: 'var(--text-muted)' }}>No Image</div>
                  )}
                </div>
                <div className="zoom-card-content">
                  <span style={{ fontSize: '0.85rem', color: 'var(--accent)', letterSpacing: '0.1em', textTransform: 'uppercase' }}>{String(idx + 1).padStart(2, '0')}</span>
                  <h3 className="zoom-card-title">{cat.name}</h3>
                  <p className="zoom-card-desc">{cat.description}</p>
                </div>
              </Link>
            ))}
          </div>
        )}
      </section>

      {/* Brand Story Section */}
      <section id="story" style={{ backgroundColor: 'var(--bg-card)', padding: '120px 0' }}>
        <div className="container" style={{ display: 'flex', alignItems: 'center', gap: '80px', flexWrap: 'wrap' }}>
          <div style={{ flex: '1 1 400px' }}>
            <div style={{ position: 'relative', aspectRatio: '4/5', overflow: 'hidden' }}>
              <img src="/assets/atelier_story.jpg" alt="VÉLURE Atelier Craftsmanship" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
            </div>
          </div>
          <div style={{ flex: '1 1 400px', paddingRight: '40px' }}>
            <span style={{ textTransform: 'uppercase', letterSpacing: '0.15em', fontSize: '0.85rem', color: 'var(--accent)', marginBottom: '16px', display: 'block' }}>Heritage & Craft</span>
            <h2 className="title-large" style={{ marginBottom: '32px', lineHeight: 1.1 }}>The Atelier <br/> Philosophy.</h2>
            <p style={{ color: 'var(--text-secondary)', fontSize: '1.1rem', lineHeight: 1.8, marginBottom: '24px' }}>
              At VÉLURE, we believe in the art of slow fashion. Every garment is a testament to meticulous craftsmanship, born from a desire to create pieces that not only withstand the test of time but grow more beautiful with it.
            </p>
            <p style={{ color: 'var(--text-secondary)', fontSize: '1.1rem', lineHeight: 1.8, marginBottom: '40px' }}>
              We partner with multi-generational mills and ethically-minded artisans to ensure that from the first sketch to the final stitch, our impact on the world is as thoughtful as our designs.
            </p>
            <Link to="/catalog" className="btn btn-secondary" style={{ padding: '12px 32px' }}>Discover the Process</Link>
          </div>
        </div>
      </section>

      {/* Featured Products Band */}
      <section style={{ backgroundColor: 'var(--bg-secondary)', padding: '120px 0' }} aria-labelledby="featured-title">
        <div className="container">
          <div className="flex-between" style={{ alignItems: 'flex-end', marginBottom: '60px', flexWrap: 'wrap', gap: '24px' }}>
            <div>
              <span className="atelier-kicker">New Arrivals</span>
              <h2 id="featured-title" className="title-medium" style={{ marginTop: '8px' }}>Recently Added</h2>
            </div>
            <Link to="/catalog" className="btn btn-primary" style={{ padding: '12px 24px' }}>
              Shop All Products
            </Link>
          </div>

          {isProductsLoading ? (
             <div className="flex-center" style={{ height: '200px' }}>
               <div style={{ border: '3px solid var(--border)', borderTopColor: 'var(--accent)', borderRadius: '50%', width: '32px', height: '32px', animation: 'spin 1s linear infinite' }} />
             </div>
          ) : featuredProducts.length > 0 ? (
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(260px, 1fr))', gap: '32px' }}>
              {featuredProducts.map((product) => (
                <Link key={product.id} to={`/products/${product.id}`} className="premium-product-card" style={{ display: 'block', textDecoration: 'none', color: 'inherit' }}>
                  <div style={{ position: 'relative', aspectRatio: '3/4', overflow: 'hidden', backgroundColor: '#f9f9f9' }} className="zoom-media">
                    {product.imageUrl ? (
                      <img src={product.imageUrl} alt={product.name} style={{ width: '100%', height: '100%', objectFit: 'cover', mixBlendMode: 'multiply' }} />
                    ) : (
                       <div className="flex-center" style={{ width: '100%', height: '100%', backgroundColor: 'var(--bg-secondary)', color: 'var(--text-muted)' }}>No Image</div>
                    )}
                  </div>
                  <div style={{ padding: '24px 20px' }}>
                    <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>{product.categoryName}</span>
                    <h3 style={{ fontSize: '1.2rem', fontFamily: 'var(--font-title)', marginTop: '4px', marginBottom: '8px' }}>{product.name}</h3>
                    <p style={{ fontWeight: 600 }}>
                      {formatPrice(product.startingPrice)}
                    </p>
                  </div>
                </Link>
              ))}
            </div>
          ) : (
            <div className="empty-state">No products available at the moment.</div>
          )}
        </div>
      </section>

      {/* Values Band */}
      <section className="container" style={{ padding: '80px 24px', borderBottom: '1px solid var(--border)' }} aria-label="Brand values">
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))', gap: '60px' }}>
          <article style={{ display: 'flex', flexDirection: 'column', gap: '16px', alignItems: 'center', textAlign: 'center' }}>
            <ShieldCheck size={32} style={{ color: 'var(--accent)' }} strokeWidth={1.5} />
            <h3 style={{ fontSize: '1.1rem' }}>Premium Quality</h3>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.95rem' }}>We source high-grade sustainable fibers to ensure durability.</p>
          </article>
          <article style={{ display: 'flex', flexDirection: 'column', gap: '16px', alignItems: 'center', textAlign: 'center' }}>
            <Truck size={32} style={{ color: 'var(--accent)' }} strokeWidth={1.5} />
            <h3 style={{ fontSize: '1.1rem' }}>Global Shipping</h3>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.95rem' }}>Complimentary shipping on international orders over LKR 100,000.</p>
          </article>
          <article style={{ display: 'flex', flexDirection: 'column', gap: '16px', alignItems: 'center', textAlign: 'center' }}>
            <Sparkles size={32} style={{ color: 'var(--accent)' }} strokeWidth={1.5} />
            <h3 style={{ fontSize: '1.1rem' }}>Timeless Designs</h3>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.95rem' }}>Wardrobe staples designed to transcend passing trends.</p>
          </article>
          <article style={{ display: 'flex', flexDirection: 'column', gap: '16px', alignItems: 'center', textAlign: 'center' }}>
            <Compass size={32} style={{ color: 'var(--accent)' }} strokeWidth={1.5} />
            <h3 style={{ fontSize: '1.1rem' }}>Considered Edits</h3>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.95rem' }}>Each release is arranged for easy mixing and repeating.</p>
          </article>
        </div>
      </section>

      {/* Newsletter Signup */}
      <section style={{ backgroundColor: 'var(--bg-primary)', padding: '120px 24px', textAlign: 'center' }}>
        <div className="container" style={{ maxWidth: '600px' }}>
          <Mail size={32} style={{ color: 'var(--accent)', margin: '0 auto 24px' }} strokeWidth={1.5} />
          <h2 className="title-medium" style={{ marginBottom: '16px' }}>Join the Inner Circle</h2>
          <p style={{ color: 'var(--text-secondary)', marginBottom: '40px' }}>
            Subscribe to receive exclusive access to early releases, private sales, and curated editorial content.
          </p>
          <form style={{ display: 'flex', gap: '12px', flexWrap: 'wrap' }} onSubmit={(e) => e.preventDefault()}>
            <input 
              type="email" 
              placeholder="Enter your email address" 
              style={{ flex: '1 1 200px', padding: '16px 20px', border: '1px solid var(--border)', backgroundColor: 'transparent', outline: 'none' }}
              required 
            />
            <button type="submit" className="btn btn-primary" style={{ padding: '0 32px', flex: '1 1 auto' }}>Subscribe</button>
          </form>
        </div>
      </section>
    </div>
  );
};
