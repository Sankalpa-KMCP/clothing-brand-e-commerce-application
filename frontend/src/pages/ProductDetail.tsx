import React, { useEffect, useState } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { request } from '../api/apiClient';
import { useAuth } from '../context/AuthContext';
import { useCart } from '../context/CartContext';
import { ArrowLeft, Compass, CheckCircle2, XCircle, ShoppingBag, AlertCircle, Check } from 'lucide-react';

interface ProductVariant {
  id: number;
  size: string;
  color: string;
  price: number;
  inStock: boolean;
}

interface Category {
  id: number;
  name: string;
  description: string;
}

interface ProductDetail {
  id: number;
  name: string;
  description: string;
  imageUrl?: string;
  category: Category;
  variants: ProductVariant[];
}

export const ProductDetail: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();
  const { addItem } = useCart();

  const [product, setProduct] = useState<ProductDetail | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  // User variant selections
  const [selectedSize, setSelectedSize] = useState<string>('');
  const [selectedColor, setSelectedColor] = useState<string>('');

  const [isAdding, setIsAdding] = useState(false);
  const [addSuccess, setAddSuccess] = useState(false);
  const [addError, setAddError] = useState<string | null>(null);

  useEffect(() => {
    const fetchProductDetail = async () => {
      setIsLoading(true);
      setError(null);
      try {
        const data = await request<ProductDetail>(`/products/${id}`);
        setProduct(data);
        
        // Auto-select first variant options if available
        if (data.variants.length > 0) {
          setSelectedSize(data.variants[0].size);
          setSelectedColor(data.variants[0].color);
        }
      } catch (err: any) {
        setError(err.message || 'Failed to load product detail');
      } finally {
        setIsLoading(false);
      }
    };
    fetchProductDetail();
  }, [id]);

  if (isLoading) {
    return (
      <div className="flex-center" style={{ height: '400px' }}>
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

  if (error || !product) {
    return (
      <div className="container" style={{ padding: '40px 20px' }}>
        <div className="alert alert-error" style={{ marginBottom: '20px' }}>
          {error || 'Product not found'}
        </div>
        <Link to="/catalog" className="btn btn-secondary flex-center" style={{ display: 'inline-flex' }}>
          <ArrowLeft size={16} />
          <span>Back to Catalog</span>
        </Link>
      </div>
    );
  }

  // Get unique sizes and colors from variants
  const sizes = Array.from(new Set(product.variants.map((v) => v.size)));
  const colors = Array.from(new Set(product.variants.map((v) => v.color)));

  // Find currently selected variant based on selection
  const selectedVariant = product.variants.find(
    (v) => v.size === selectedSize && v.color === selectedColor
  );

  // Fallback to get minimum price across all variants as starting price
  const startingPrice = product.variants.reduce(
    (min, v) => (v.price < min ? v.price : min),
    product.variants[0]?.price || 0
  );

  const handleAddToBag = async () => {
    if (!isAuthenticated) {
      navigate('/login');
      return;
    }
    if (!selectedVariant) return;

    setAddError(null);
    setAddSuccess(false);
    setIsAdding(true);
    try {
      await addItem(selectedVariant.id, 1);
      setAddSuccess(true);
    } catch (err: any) {
      setAddError(err.message || 'Failed to add item to bag');
    } finally {
      setIsAdding(false);
    }
  };

  return (
    <div className="container animate-fade-in" style={{ padding: '40px 20px 80px 20px' }}>
      {/* Back Button */}
      <Link to="/catalog" className="btn btn-secondary flex-center" style={{
        display: 'inline-flex',
        marginBottom: '30px',
        padding: '8px 16px',
        gap: '6px'
      }}>
        <ArrowLeft size={16} />
        <span>Back to Catalog</span>
      </Link>

      {/* Main Grid split */}
      <div className="grid grid-2" style={{ gap: '40px' }}>
        {/* Left Side: Product Image */}
        <div style={{
          backgroundColor: 'var(--bg-secondary)',
          backgroundImage: product.imageUrl ? `url(${product.imageUrl})` : 'none',
          backgroundSize: 'cover',
          backgroundPosition: 'center',
          borderRadius: 'var(--radius-lg)',
          border: '1px solid var(--border)',
          height: '500px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          color: 'var(--text-muted)'
        }}>
          {!product.imageUrl && <Compass size={64} />}
        </div>

        {/* Right Side: Product Details & Variant Picker */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
          <div>
            <span style={{
              fontSize: '0.875rem',
              color: 'var(--accent)',
              fontWeight: 600,
              textTransform: 'uppercase',
              letterSpacing: '0.05em'
            }}>
              {product.category?.name}
            </span>
            <h1 className="title-medium" style={{ marginTop: '4px', marginBottom: '12px' }}>
              {product.name}
            </h1>
            
            {/* Price display */}
            <div style={{ display: 'flex', alignItems: 'baseline', gap: '8px', marginBottom: '8px' }}>
              <span style={{
                fontSize: '2rem',
                fontWeight: 800,
                color: 'var(--text-primary)'
              }}>
                {selectedVariant ? `$${selectedVariant.price.toFixed(2)}` : `$${startingPrice.toFixed(2)}`}
              </span>
              {!selectedVariant && (
                <span style={{ fontSize: '0.875rem', color: 'var(--text-muted)' }}>starting price</span>
              )}
            </div>

            {/* In Stock Badge */}
            {selectedVariant ? (
              selectedVariant.inStock ? (
                <span className="flex-center" style={{
                  display: 'inline-flex',
                  gap: '6px',
                  backgroundColor: 'var(--success-bg)',
                  color: 'var(--success)',
                  padding: '4px 10px',
                  borderRadius: 'var(--radius-full)',
                  fontSize: '0.875rem',
                  fontWeight: 600
                }}>
                  <CheckCircle2 size={14} />
                  <span>In Stock</span>
                </span>
              ) : (
                <span className="flex-center" style={{
                  display: 'inline-flex',
                  gap: '6px',
                  backgroundColor: 'var(--error-bg)',
                  color: 'var(--error)',
                  padding: '4px 10px',
                  borderRadius: 'var(--radius-full)',
                  fontSize: '0.875rem',
                  fontWeight: 600
                }}>
                  <XCircle size={14} />
                  <span>Out of Stock</span>
                </span>
              )
            ) : (
              <span style={{
                fontSize: '0.875rem',
                color: 'var(--text-muted)',
                fontStyle: 'italic'
              }}>
                Select options below to view availability
              </span>
            )}
          </div>

          <hr style={{ border: 'none', borderTop: '1px solid var(--border)' }} />

          {/* Description */}
          <div>
            <h3 style={{ fontSize: '1rem', fontWeight: 600, marginBottom: '8px' }}>Description</h3>
            <p style={{ color: 'var(--text-secondary)', lineHeight: 1.6 }}>
              {product.description || 'No description provided for this product.'}
            </p>
          </div>

          {/* Option selectors */}
          {product.variants.length > 0 && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
              {/* Color Picker */}
              <div>
                <span style={{
                  fontSize: '0.875rem',
                  fontWeight: 600,
                  color: 'var(--text-secondary)',
                  display: 'block',
                  marginBottom: '10px'
                }}>
                  Color: <span style={{ fontWeight: 500, color: 'var(--text-primary)' }}>{selectedColor}</span>
                </span>
                <div style={{ display: 'flex', gap: '10px' }}>
                  {colors.map((color) => {
                    const isSelected = selectedColor === color;
                    return (
                      <button
                        key={color}
                        onClick={() => setSelectedColor(color)}
                        className="btn"
                        style={{
                          padding: '8px 16px',
                          fontSize: '0.875rem',
                          border: isSelected ? '2px solid var(--border-focus)' : '1px solid var(--border)',
                          backgroundColor: isSelected ? 'var(--bg-secondary)' : 'var(--bg-card)',
                          fontWeight: isSelected ? 600 : 400
                        }}
                      >
                        {color}
                      </button>
                    );
                  })}
                </div>
              </div>

              {/* Size Picker */}
              <div>
                <span style={{
                  fontSize: '0.875rem',
                  fontWeight: 600,
                  color: 'var(--text-secondary)',
                  display: 'block',
                  marginBottom: '10px'
                }}>
                  Size: <span style={{ fontWeight: 500, color: 'var(--text-primary)' }}>{selectedSize}</span>
                </span>
                <div style={{ display: 'flex', gap: '10px' }}>
                  {sizes.map((size) => {
                    const isSelected = selectedSize === size;
                    // Check if this specific size is available in currently selected color
                    const hasVariant = product.variants.some(
                      (v) => v.size === size && v.color === selectedColor
                    );
                    return (
                      <button
                        key={size}
                        disabled={!hasVariant}
                        onClick={() => setSelectedSize(size)}
                        className="btn"
                        style={{
                          padding: '8px 16px',
                          minWidth: '48px',
                          fontSize: '0.875rem',
                          border: isSelected ? '2px solid var(--border-focus)' : '1px solid var(--border)',
                          backgroundColor: isSelected ? 'var(--bg-secondary)' : 'var(--bg-card)',
                          fontWeight: isSelected ? 600 : 400,
                          opacity: hasVariant ? 1 : 0.3,
                          cursor: hasVariant ? 'pointer' : 'not-allowed'
                        }}
                      >
                        {size}
                      </button>
                    );
                  })}
                </div>
              </div>
            </div>
          )}

          {/* Add to Bag Feedback Alerts */}
          {addError && (
            <div className="alert alert-error animate-fade-in" style={{ marginTop: '20px', fontSize: '0.875rem', padding: '12px' }}>
              <AlertCircle size={16} />
              <span>{addError}</span>
            </div>
          )}
          {addSuccess && (
            <div className="alert alert-success animate-fade-in" style={{ marginTop: '20px', fontSize: '0.875rem', padding: '12px' }}>
              <Check size={16} />
              <span>Added to bag successfully!</span>
            </div>
          )}

          {/* Add to Bag Button */}
          <div style={{ marginTop: '20px' }}>
            <button
              disabled={!selectedVariant || !selectedVariant.inStock || isAdding}
              className="btn btn-primary"
              style={{
                width: '100%',
                padding: '14px',
                fontSize: '1rem',
                fontWeight: 600,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '8px'
              }}
              onClick={handleAddToBag}
            >
              <ShoppingBag size={18} />
              <span>{isAdding ? 'Adding to Bag...' : 'Add to Bag'}</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
