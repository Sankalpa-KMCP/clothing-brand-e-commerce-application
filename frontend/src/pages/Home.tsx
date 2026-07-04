import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { request } from '../api/apiClient';
import { ArrowRight, Compass, ShieldCheck, Sparkles, Truck } from 'lucide-react';
import { EditorialMedia } from '../components/EditorialMedia';

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
  price !== undefined ? `$${price.toFixed(2)}` : 'N/A'
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
        const data = await request<PaginatedResponse<ProductSummary>>('/products?page=0&size=4');
        setFeaturedProducts(data.content || []);
      } catch (err) {
        console.error('Failed to load featured products', err);
      } finally {
        setIsProductsLoading(false);
      }
    };
    fetchFeaturedProducts();
  }, []);

  const heroCategory = categories[0];
  const heroProduct = featuredProducts[0];
  const heroImage = heroProduct?.imageUrl || heroCategory?.imageUrl;
  const heroLabel = heroProduct?.name || heroCategory?.name || 'THREAD atelier';

  return (
    <div className="home-page animate-fade-in">
      <section className="home-hero" aria-labelledby="home-title">
        <div className="container home-hero-grid">
          <div className="home-hero-copy">
            <span className="atelier-kicker">New Collection 2026</span>
            <h1 id="home-title" className="home-hero-title">Elevate Your Everyday Style</h1>
            <p className="home-hero-lede">
              Discover premium, ethically crafted essentials designed to complement your life. Simplicity refined.
            </p>
            <div className="home-hero-actions">
              <Link to="/catalog" className="btn btn-primary btn-lg">
                <span>Browse Catalog</span>
                <ArrowRight size={18} />
              </Link>
              {heroCategory && (
                <Link to={`/catalog?categoryId=${heroCategory.id}`} className="editorial-text-link">
                  Shop {heroCategory.name}
                </Link>
              )}
            </div>
          </div>

          <div className="home-hero-media" aria-label="Featured collection preview">
            <EditorialMedia src={heroImage} alt={heroLabel} label={heroLabel} className="home-hero-primary" />
            <div className="home-hero-note">
              <span>Atelier edit</span>
              <strong>{heroProduct?.categoryName || heroCategory?.name || 'Seasonal staples'}</strong>
            </div>
          </div>
        </div>
      </section>

      <section className="container home-category-section" aria-labelledby="category-title">
        <div className="section-heading">
          <span className="atelier-kicker">Wardrobe departments</span>
          <h2 id="category-title" className="title-medium">Shop By Category</h2>
          <p>Explore curated categories tailored for your wardrobe.</p>
        </div>

        {isLoading ? (
          <div className="atelier-loading" role="status" aria-label="Loading categories">
            <div className="loader-spinner" />
          </div>
        ) : error ? (
          <div className="alert alert-error">{error}</div>
        ) : categories.length === 0 ? (
          <div className="empty-state">No categories available at the moment.</div>
        ) : (
          <div className="home-category-grid">
            {categories.map((category, index) => (
              <Link
                key={category.id}
                to={`/catalog?categoryId=${category.id}`}
                className="category-editorial-card"
              >
                <EditorialMedia src={category.imageUrl} alt={category.name} label={category.name} />
                <div className="category-editorial-copy">
                  <span>{String(index + 1).padStart(2, '0')}</span>
                  <h3>{category.name}</h3>
                  <p>{category.description}</p>
                </div>
              </Link>
            ))}
          </div>
        )}
      </section>

      <section className="home-featured-band" aria-labelledby="featured-title">
        <div className="container">
          <div className="section-heading section-heading-split">
            <div>
              <span className="atelier-kicker">Selected pieces</span>
              <h2 id="featured-title" className="title-medium">Fresh from the rack</h2>
            </div>
            <Link to="/catalog" className="editorial-text-link">
              View full collection
            </Link>
          </div>

          {isProductsLoading ? (
            <div className="atelier-loading" role="status" aria-label="Loading featured products">
              <div className="loader-spinner" />
            </div>
          ) : featuredProducts.length > 0 ? (
            <div className="product-editorial-grid home-product-grid">
              {featuredProducts.map((product) => (
                <Link key={product.id} to={`/products/${product.id}`} className="product-editorial-card">
                  <EditorialMedia src={product.imageUrl} alt={product.name} label={product.name} />
                  <div className="product-editorial-copy">
                    <span>{product.categoryName}</span>
                    <h3>{product.name}</h3>
                    <p>
                      <strong>{formatPrice(product.startingPrice)}</strong>
                      <small>starting price</small>
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

      <section className="home-values-band" aria-label="Brand values">
        <div className="container home-values-grid">
          <article>
            <ShieldCheck size={28} />
            <h3>Premium Quality</h3>
            <p>We source high-grade sustainable fibers to ensure durability and comfort.</p>
          </article>
          <article>
            <Truck size={28} />
            <h3>Global Shipping</h3>
            <p>Reliable delivery to your doorstep with real-time tracking snapshots.</p>
          </article>
          <article>
            <Sparkles size={28} />
            <h3>Timeless Designs</h3>
            <p>Wardrobe staples designed to transcend passing seasonal trends.</p>
          </article>
          <article>
            <Compass size={28} />
            <h3>Considered Edits</h3>
            <p>Each release is arranged for easy mixing, repeating, and everyday wear.</p>
          </article>
        </div>
      </section>
    </div>
  );
};
