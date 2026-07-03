import React, { useEffect, useState, useCallback } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { request } from '../api/apiClient';
import { Search, Compass, SlidersHorizontal, ArrowLeft, ArrowRight } from 'lucide-react';

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

  return (
    <div className="container animate-fade-in" style={{ padding: '40px 20px 80px 20px' }}>
      <div style={{ marginBottom: '30px' }}>
        <h1 className="title-medium" style={{ marginBottom: '8px' }}>Store Collection</h1>
        <p style={{ color: 'var(--text-secondary)' }}>Explore our range of premium minimalist apparel.</p>
      </div>

      {/* Controls Bar: Search & Category Filters */}
      <div style={{
        display: 'flex',
        flexDirection: 'column',
        gap: '20px',
        marginBottom: '40px',
        backgroundColor: 'var(--bg-card)',
        padding: '24px',
        borderRadius: 'var(--radius-lg)',
        border: '1px solid var(--border)'
      }}>
        {/* Search Input Row */}
        <form onSubmit={handleSearchSubmit} style={{
          position: 'relative',
          display: 'flex',
          gap: '12px'
        }}>
          <div style={{ position: 'relative', flexGrow: 1 }}>
            <Search size={18} style={{
              position: 'absolute',
              left: '16px',
              top: '50%',
              transform: 'translateY(-50%)',
              color: 'var(--text-muted)'
            }} />
            <input
              type="text"
              placeholder="Search products..."
              className="input-field"
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
              style={{ paddingLeft: '48px' }}
            />
          </div>
          <button type="submit" className="btn btn-primary">Search</button>
        </form>

        {/* Category Pills Filters */}
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: '10px', alignItems: 'center' }}>
          <span style={{
            fontSize: '0.875rem',
            fontWeight: 600,
            color: 'var(--text-secondary)',
            marginRight: '8px',
            display: 'flex',
            alignItems: 'center',
            gap: '6px'
          }}>
            <SlidersHorizontal size={14} />
            Categories:
          </span>
          
          <button
            onClick={() => handleCategorySelect('')}
            className="btn"
            style={{
              padding: '6px 14px',
              fontSize: '0.875rem',
              borderRadius: 'var(--radius-full)',
              backgroundColor: !categoryIdParam ? 'var(--accent-light)' : 'transparent',
              color: !categoryIdParam ? 'var(--accent)' : 'var(--text-secondary)',
              border: !categoryIdParam ? '1px solid var(--accent)' : '1px solid var(--border)',
            }}
          >
            All Items
          </button>

          {categories.map((cat) => {
            const isSelected = categoryIdParam === cat.id.toString();
            return (
              <button
                key={cat.id}
                onClick={() => handleCategorySelect(cat.id.toString())}
                className="btn"
                style={{
                  padding: '6px 14px',
                  fontSize: '0.875rem',
                  borderRadius: 'var(--radius-full)',
                  backgroundColor: isSelected ? 'var(--accent-light)' : 'transparent',
                  color: isSelected ? 'var(--accent)' : 'var(--text-secondary)',
                  border: isSelected ? '1px solid var(--accent)' : '1px solid var(--border)',
                }}
              >
                {cat.name}
              </button>
            );
          })}
        </div>
      </div>

      {/* Error State */}
      {error && <div className="alert alert-error">{error}</div>}

      {/* Loading Grid */}
      {isLoading ? (
        <div className="flex-center" style={{ height: '300px' }}>
          <div style={{
            border: '4px solid var(--border)',
            borderTopColor: 'var(--accent)',
            borderRadius: 'var(--radius-full)',
            width: '40px',
            height: '40px',
            animation: 'spin 1s linear infinite'
          }} />
        </div>
      ) : !productsResponse || productsResponse.content.length === 0 ? (
        /* Empty State */
        <div style={{
          textAlign: 'center',
          padding: '80px 20px',
          backgroundColor: 'var(--bg-card)',
          borderRadius: 'var(--radius-lg)',
          border: '1px solid var(--border)'
        }}>
          <Compass size={48} style={{ color: 'var(--text-muted)', marginBottom: '16px' }} />
          <h3 className="title-small" style={{ marginBottom: '8px' }}>No products found</h3>
          <p style={{ color: 'var(--text-secondary)', marginBottom: '24px' }}>
            We couldn't find any products matching your current filters.
          </p>
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
        /* Products Grid */
        <div>
          <div className="grid grid-4" style={{ marginBottom: '50px' }}>
            {productsResponse.content.map((product) => (
              <Link
                key={product.id}
                to={`/products/${product.id}`}
                className="card"
                style={{
                  display: 'flex',
                  flexDirection: 'column',
                  height: '400px'
                }}
              >
                {/* Product Image */}
                <div style={{
                  height: '260px',
                  backgroundColor: 'var(--bg-secondary)',
                  backgroundImage: product.imageUrl ? `url(${product.imageUrl})` : 'none',
                  backgroundSize: 'cover',
                  backgroundPosition: 'center',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  color: 'var(--text-muted)'
                }}>
                  {!product.imageUrl && <Compass size={40} />}
                </div>

                {/* Details */}
                <div style={{
                  padding: '16px',
                  display: 'flex',
                  flexDirection: 'column',
                  justifyContent: 'space-between',
                  flexGrow: 1
                }}>
                  <div>
                    <span style={{
                      fontSize: '0.75rem',
                      textTransform: 'uppercase',
                      color: 'var(--text-muted)',
                      fontWeight: 600,
                      display: 'block',
                      marginBottom: '4px'
                    }}>
                      {product.categoryName}
                    </span>
                    <h3 style={{
                      fontSize: '1rem',
                      fontWeight: 600,
                      color: 'var(--text-primary)',
                      marginBottom: '8px',
                      overflow: 'hidden',
                      textOverflow: 'ellipsis',
                      whiteSpace: 'nowrap'
                    }}>
                      {product.name}
                    </h3>
                  </div>
                  <div>
                    <span style={{
                      fontSize: '1.125rem',
                      fontWeight: 700,
                      color: 'var(--accent)'
                    }}>
                      {product.startingPrice !== undefined ? (
                        `$${product.startingPrice.toFixed(2)}`
                      ) : (
                        'N/A'
                      )}
                    </span>
                    <span style={{
                      fontSize: '0.75rem',
                      color: 'var(--text-muted)',
                      marginLeft: '6px'
                    }}>
                      starting price
                    </span>
                  </div>
                </div>
              </Link>
            ))}
          </div>

          {/* Pagination Controls */}
          {productsResponse.totalPages > 1 && (
            <div className="flex-center" style={{ gap: '16px' }}>
              <button
                onClick={() => handlePageChange(pageParam - 1)}
                disabled={pageParam === 0}
                className="btn btn-secondary flex-center"
                style={{ padding: '8px 16px' }}
              >
                <ArrowLeft size={16} />
                <span>Prev</span>
              </button>

              <span style={{ fontSize: '0.925rem', fontWeight: 500 }}>
                Page {pageParam + 1} of {productsResponse.totalPages}
              </span>

              <button
                onClick={() => handlePageChange(pageParam + 1)}
                disabled={pageParam >= productsResponse.totalPages - 1}
                className="btn btn-secondary flex-center"
                style={{ padding: '8px 16px' }}
              >
                <span>Next</span>
                <ArrowRight size={16} />
              </button>
            </div>
          )}
        </div>
      )}

      <style>{`
        @keyframes spin {
          0% { transform: rotate(0deg); }
          100% { transform: rotate(360deg); }
        }
      `}</style>
    </div>
  );
};
