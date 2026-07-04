import React, { useEffect, useState } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { request } from '../api/apiClient';
import { useAuth } from '../context/AuthContext';
import { useCart } from '../context/CartContext';
import { ArrowLeft, CheckCircle2, XCircle, ShoppingBag, AlertCircle, Check } from 'lucide-react';
import { EditorialMedia } from '../components/EditorialMedia';

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
      <div className="product-detail-page container">
        <div className="atelier-loading">
          <div className="loader-spinner" />
        </div>
      </div>
    );
  }

  if (error || !product) {
    return (
      <div className="product-detail-page container">
        <div className="alert alert-error">
          <AlertCircle size={16} />
          <span>{error || 'Product not found'}</span>
        </div>
        <Link to="/catalog" className="editorial-text-link">
          <ArrowLeft size={16} style={{ marginRight: '8px' }} />
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

  const getColorHex = (colorName: string) => {
    const colorsMap: Record<string, string> = {
      indigo: '#3f51b5',
      charcoal: '#37474f',
      navy: '#1a237e',
      black: '#1c1917',
      white: '#ffffff',
      grey: '#8e8a86',
      gray: '#8e8a86',
      bronze: '#8c6239',
      beige: '#f4f2ec',
      red: '#991b1b',
      blue: '#1e3a8a',
      green: '#115e59'
    };
    return colorsMap[colorName.toLowerCase()] || '#8e8a86';
  };

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
    <div className="product-detail-page animate-fade-in" style={{ padding: 0 }}>
      <div style={{ display: 'flex', height: 'calc(100vh - 80px)', width: '100%' }}>
        {/* Left Side: Product Image */}
        <div style={{ flex: '1.2', position: 'relative', backgroundColor: 'var(--bg-secondary)', overflow: 'hidden' }}>
          <EditorialMedia
            src={product.imageUrl}
            alt={product.name}
            label={product.name}
            style={{ height: '100%', width: '100%', objectFit: 'cover', border: 'none' }}
          />
        </div>

        {/* Right Side: Product Details & Variant Picker */}
        <div style={{ flex: '1', padding: '60px 80px', overflowY: 'auto', backgroundColor: 'var(--bg-primary)' }}>
          <Link to="/catalog" className="editorial-text-link" style={{ marginBottom: '40px', display: 'inline-flex' }}>
            <ArrowLeft size={16} style={{ marginRight: '8px' }} />
            <span>Back to Catalog</span>
          </Link>

          <div>
            <span className="product-detail-kicker" style={{ fontSize: '0.85rem' }}>
              {product.category?.name}
            </span>
            <h1 className="product-detail-title" style={{ fontSize: '3.5rem', marginTop: '16px', marginBottom: '24px' }}>
              {product.name}
            </h1>
            
            {/* Price display */}
            <div className="product-detail-price-block" style={{ marginBottom: '24px' }}>
              <span className="product-detail-price" style={{ fontSize: '2.5rem' }}>
                {selectedVariant ? `LKR ${selectedVariant.price.toFixed(2)}` : `LKR ${startingPrice.toFixed(2)}`}
              </span>
              {!selectedVariant && (
                <span className="product-detail-starting" style={{ marginLeft: '12px' }}>starting price</span>
              )}
            </div>

            {/* In Stock Badge */}
            <div style={{ marginBottom: '40px' }}>
              {selectedVariant ? (
                selectedVariant.inStock ? (
                  <span className="product-detail-stock-badge product-detail-stock-in" style={{ padding: '8px 16px', borderRadius: '0' }}>
                    <CheckCircle2 size={16} />
                    <span>In Stock - Ready to Ship</span>
                  </span>
                ) : (
                  <span className="product-detail-stock-badge product-detail-stock-out" style={{ padding: '8px 16px', borderRadius: '0' }}>
                    <XCircle size={16} />
                    <span>Out of Stock</span>
                  </span>
                )
              ) : (
                <span className="product-detail-stock-prompt" style={{ fontStyle: 'normal', fontSize: '0.9rem', color: 'var(--text-secondary)' }}>
                  Select options below to view availability
                </span>
              )}
            </div>
          </div>

          <hr className="product-detail-divider" style={{ margin: '40px 0' }} />

          {/* Description */}
          <div className="product-detail-description" style={{ marginBottom: '40px' }}>
            <h3 style={{ fontSize: '1.2rem', textTransform: 'uppercase', letterSpacing: '0.1em', marginBottom: '16px' }}>Details</h3>
            <p style={{ fontSize: '1.05rem', lineHeight: '1.8' }}>{product.description || 'No description provided for this product.'}</p>
          </div>

          {/* Option selectors */}
          {product.variants.length > 0 && (
            <div className="product-detail-selectors" style={{ gap: '32px' }}>
              {/* Color Picker */}
              <div className="product-detail-selector-group">
                <h4 style={{ marginBottom: '16px' }}>
                  Color: <span style={{ textTransform: 'capitalize' }}>{selectedColor.toLowerCase()}</span>
                </h4>
                <div className="product-detail-options">
                  {colors.map((color) => {
                    const isSelected = selectedColor === color;
                    return (
                      <button
                        key={color}
                        onClick={() => setSelectedColor(color)}
                        className={`swatch-color-btn ${isSelected ? 'is-selected' : ''}`}
                        style={{ backgroundColor: getColorHex(color), width: '40px', height: '40px', borderRadius: '50%' }}
                        title={color}
                        aria-label={`Select Color: ${color}`}
                        aria-pressed={isSelected}
                      >
                        {color}
                      </button>
                    );
                  })}
                </div>
              </div>

              {/* Size Picker */}
              <div className="product-detail-selector-group">
                <h4 style={{ marginBottom: '16px' }}>
                  Size: <span>{selectedSize}</span>
                </h4>
                <div className="product-detail-options" style={{ gap: '16px' }}>
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
                        className={`product-detail-option-btn ${isSelected ? 'is-selected' : ''}`}
                        style={{ padding: '16px 24px', fontSize: '1rem', border: '1px solid var(--border)', borderRadius: '0' }}
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
            <div className="alert alert-error animate-fade-in" style={{ marginTop: '40px', borderRadius: '0' }}>
              <AlertCircle size={18} />
              <span>{addError}</span>
            </div>
          )}
          {addSuccess && (
            <div className="alert alert-success animate-fade-in" style={{ marginTop: '40px', borderRadius: '0' }}>
              <Check size={18} />
              <span>Added to bag successfully!</span>
            </div>
          )}

          {/* Add to Bag Button */}
          <div className="product-detail-actions" style={{ marginTop: '56px' }}>
            <button
              disabled={!selectedVariant || !selectedVariant.inStock || isAdding}
              className="btn btn-primary"
              onClick={handleAddToBag}
              style={{ padding: '24px', fontSize: '1.1rem', letterSpacing: '0.1em', borderRadius: '0' }}
            >
              <ShoppingBag size={20} style={{ marginRight: '12px' }} />
              <span>{isAdding ? 'Adding to Bag...' : 'Add to Bag'}</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
