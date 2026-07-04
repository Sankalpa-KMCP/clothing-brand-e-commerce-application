import React, { useEffect, useState, useCallback } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { request } from '../api/apiClient';
import { ArrowLeft, ArrowRight, Compass, Search, SlidersHorizontal, AlertCircle } from 'lucide-react';
import { EditorialMedia } from '../components/EditorialMedia';

interface Category {
  id: number;
  name: string;
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

export const Catalog: React.FC = () => {
  const [searchParams, setSearchParams] = useSearchParams();

  // Search parameters from URL
  const categoryIdParam = searchParams.get('categoryId') || '';
  const queryParam = searchParams.get('query') || '';
  const pageParam = parseInt(searchParams.get('page') || '0', 10);

  const [categories, setCategories] = useState<Category[]>([]);
  const [productsResponse, setProductsResponse] = useState<PaginatedResponse<ProductSummary> | null>(null);
  const [searchInput, setSearchInput] = useState<string>(queryParam);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  // Fetch categories once
  useEffect(() => {
    const fetchCategories = async () => {
      try {
        const data = await request<Category[]>('/categories');
        setCategories(data);
      } catch (err: any) {
        console.error('Failed to load categories', err);
      }
    };
    fetchCategories();
  }, []);

  // Sync search input with query parameter on URL changes
  useEffect(() => {
    setSearchInput(queryParam);
  }, [queryParam]);

  // Fetch products
  const fetchProducts = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const params = new URLSearchParams();
      if (categoryIdParam) params.append('categoryId', categoryIdParam);
      if (queryParam) params.append('query', queryParam);
      params.append('page', pageParam.toString());
      params.append('size', '12'); // Fetch 12 items per page

      const data = await request<PaginatedResponse<ProductSummary>>(`/products?${params.toString()}`);
      setProductsResponse(data);
    } catch (err: any) {
      setError(err.message || 'Failed to load products');
    } finally {
      setIsLoading(false);
    }
  }, [categoryIdParam, queryParam, pageParam]);

  useEffect(() => {
    fetchProducts();
  }, [fetchProducts]);

  // Filters helpers
  const handleCategorySelect = (id: string) => {
    setSearchParams((prev) => {
      if (id) {
        prev.set('categoryId', id);
      } else {
        prev.delete('categoryId');
      }
      prev.set('page', '0'); // reset page
      return prev;
    });
  };

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setSearchParams((prev) => {
      if (searchInput.trim()) {
        prev.set('query', searchInput.trim());
      } else {
        prev.delete('query');
      }
      prev.set('page', '0'); // reset page
      return prev;
    });
  };

  const handlePageChange = (newPage: number) => {
    setSearchParams((prev) => {
      prev.set('page', newPage.toString());
      return prev;
    });
  };

  const selectedCategory = categories.find((cat) => categoryIdParam === cat.id.toString());
  const productCount = productsResponse?.totalElements ?? 0;

  return (
    <div className="catalog-page animate-fade-in" style={{ paddingTop: 0 }}>
      <section aria-label="Catalog Hero" style={{ position: 'relative', width: '100%', height: '450px', overflow: 'hidden', borderBottom: '1px solid var(--border)' }}>
        <img src="/assets/catalog_header.jpg" alt="VÉLURE Catalog" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
        <div style={{ position: 'absolute', inset: 0, backgroundColor: 'rgba(0,0,0,0.25)' }} />
        <div className="container" style={{ position: 'absolute', inset: 0, display: 'flex', flexDirection: 'column', justifyContent: 'flex-end', paddingBottom: '40px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end' }}>
            <div style={{ color: 'white' }}>
              <span style={{ fontSize: '0.85rem', letterSpacing: '0.2em', textTransform: 'uppercase', fontWeight: 600, display: 'block', marginBottom: '8px' }}>The Collection</span>
              <h1 id="catalog-title" style={{ fontFamily: 'var(--font-title)', fontSize: '3.5rem', fontWeight: 400, letterSpacing: '0.02em', lineHeight: 1 }}>{selectedCategory?.name || 'All Departments'}</h1>
            </div>
            <div style={{ color: 'white', textAlign: 'right' }}>
              <strong style={{ fontSize: '2.5rem', fontFamily: 'var(--font-title)', fontWeight: 400, lineHeight: 1, display: 'block' }}>{productCount}</strong>
              <small style={{ fontSize: '0.75rem', letterSpacing: '0.1em', textTransform: 'uppercase' }}>{productCount === 1 ? 'piece' : 'pieces'}</small>
            </div>
          </div>
        </div>
      </section>

      <div className="container" style={{ marginTop: '40px' }}>
        <section className="catalog-controls" aria-label="Catalog filters" style={{ border: 'none', borderBottom: '1px solid var(--border)', padding: '0 0 24px 0', marginBottom: '40px', backgroundColor: 'transparent' }}>
          <form onSubmit={handleSearchSubmit} className="catalog-search-form">
            <label className="catalog-search-label" htmlFor="catalog-search">
              Search products
            </label>
            <div className="catalog-search-input">
              <Search size={18} aria-hidden="true" />
              <input
                id="catalog-search"
                type="text"
                placeholder="Search products..."
                className="input-field"
                value={searchInput}
                onChange={(e) => setSearchInput(e.target.value)}
              />
            </div>
            <button type="submit" className="btn btn-primary">Search</button>
          </form>

          <div className="catalog-category-rail" style={{ marginTop: '24px' }}>
            <span className="catalog-filter-label">
              <SlidersHorizontal size={14} aria-hidden="true" />
              Categories:
            </span>

            <button
              onClick={() => handleCategorySelect('')}
              className={`catalog-filter-pill ${!categoryIdParam ? 'is-selected' : ''}`}
            >
              All Items
            </button>

            {categories.map((cat) => {
              const isSelected = categoryIdParam === cat.id.toString();
              return (
                <button
                  key={cat.id}
                  onClick={() => handleCategorySelect(cat.id.toString())}
                  className={`catalog-filter-pill ${isSelected ? 'is-selected' : ''}`}
                >
                  {cat.name}
                </button>
              );
            })}
          </div>
        </section>

        {isLoading ? (
          <div className="catalog-loading" role="status" aria-label="Loading products">
            <div className="loader-spinner" />
          </div>
        ) : error ? (
          <div className="catalog-error empty-state" role="alert">
            <AlertCircle size={44} aria-hidden="true" />
            <h3 className="title-small">An error occurred</h3>
            <p>{error}</p>
            <button
              onClick={() => fetchProducts()}
              className="btn btn-secondary"
            >
              Try Again
            </button>
          </div>
        ) : !productsResponse || productsResponse.content.length === 0 ? (
          <div className="catalog-empty empty-state">
            <Compass size={44} aria-hidden="true" />
            <h3 className="title-small">No products found</h3>
            <p>We couldn't find any products matching your current filters.</p>
            <button
              onClick={() => {
                setSearchParams({});
                setSearchInput('');
              }}
              className="btn btn-secondary"
            >
              Clear All Filters
            </button>
          </div>
        ) : (
          <section aria-label="Products">
            <div className="product-editorial-grid catalog-product-grid">
              {productsResponse.content.map((product) => (
                <Link
                  key={product.id}
                  to={`/products/${product.id}`}
                  className="product-editorial-card premium-product-card"
                  aria-label={`${product.name}, ${formatPrice(product.startingPrice)} starting price`}
                  style={{ border: 'none', boxShadow: 'none' }}
                >
                  <EditorialMedia src={product.imageUrl} alt={product.name} label={product.name} className="zoom-media" style={{ height: '380px' }} />
                  <div className="product-editorial-copy" style={{ padding: '16px 0' }}>
                    <span style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>{product.categoryName}</span>
                    <h3 style={{ fontSize: '1rem', marginTop: '4px' }}>{product.name}</h3>
                    <div className="product-card-meta" style={{ marginTop: '8px' }}>
                      <p>
                        <strong>{formatPrice(product.startingPrice)}</strong>
                      </p>
                    </div>
                  </div>
                </Link>
              ))}
            </div>

            {productsResponse.totalPages > 1 && (
              <nav className="catalog-pagination" aria-label="Catalog pagination">
                <button
                  onClick={() => handlePageChange(pageParam - 1)}
                  disabled={pageParam === 0}
                  className="btn btn-secondary"
                >
                  <ArrowLeft size={16} />
                  <span>Prev</span>
                </button>

                <span>
                  Page {pageParam + 1} of {productsResponse.totalPages}
                </span>

                <button
                  onClick={() => handlePageChange(pageParam + 1)}
                  disabled={pageParam >= productsResponse.totalPages - 1}
                  className="btn btn-secondary"
                >
                  <span>Next</span>
                  <ArrowRight size={16} />
                </button>
              </nav>
            )}
          </section>
        )}
      </div>
    </div>
  );
};
