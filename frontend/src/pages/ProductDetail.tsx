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
    <div className="product-detail-page container animate-fade-in">
      <Link to="/catalog" className="editorial-text-link" style={{ marginBottom: '32px' }}>
        <ArrowLeft size={16} style={{ marginRight: '8px' }} />
        <span>Back to Catalog</span>
      </Link>

      <div className="product-detail-grid">
        {/* Left Side: Product Image */}
        <div className="product-detail-media-container">
          <EditorialMedia
            src={product.imageUrl}
            alt={product.name}
            label={product.name}
          />
        </div>

        {/* Right Side: Product Details & Variant Picker */}
        <div className="product-detail-info">
          <div>
            <span className="product-detail-kicker">
              {product.category?.name}
            </span>
            <h1 className="product-detail-title">
              {product.name}
            </h1>
            
            {/* Price display */}
            <div className="product-detail-price-block">
              <span className="product-detail-price">
                {selectedVariant ? `$${selectedVariant.price.toFixed(2)}` : `$${startingPrice.toFixed(2)}`}
              </span>
              {!selectedVariant && (
                <span className="product-detail-starting">starting price</span>
              )}
            </div>

            {/* In Stock Badge */}
            {selectedVariant ? (
              selectedVariant.inStock ? (
                <span className="product-detail-stock-badge product-detail-stock-in">
                  <CheckCircle2 size={14} />
                  <span>In Stock</span>
                </span>
              ) : (
                <span className="product-detail-stock-badge product-detail-stock-out">
                  <XCircle size={14} />
                  <span>Out of Stock</span>
                </span>
              )
            ) : (
              <span className="product-detail-stock-prompt">
                Select options below to view availability
              </span>
            )}
          </div>

          <hr className="product-detail-divider" />

          {/* Description */}
          <div className="product-detail-description">
            <h3>Description</h3>
            <p>{product.description || 'No description provided for this product.'}</p>
          </div>

          {/* Option selectors */}
          {product.variants.length > 0 && (
            <div className="product-detail-selectors">
              {/* Color Picker */}
              <div className="product-detail-selector-group">
                <h4>
                  Color: <span>{selectedColor}</span>
                </h4>
                <div className="product-detail-options">
                  {colors.map((color) => {
                    const isSelected = selectedColor === color;
                    return (
                      <button
                        key={color}
                        onClick={() => setSelectedColor(color)}
                        className={`product-detail-option-btn ${isSelected ? 'is-selected' : ''}`}
                      >
                        {color}
                      </button>
                    );
                  })}
                </div>
              </div>

              {/* Size Picker */}
              <div className="product-detail-selector-group">
                <h4>
                  Size: <span>{selectedSize}</span>
                </h4>
                <div className="product-detail-options">
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
            <div className="alert alert-error animate-fade-in" style={{ marginTop: '32px' }}>
              <AlertCircle size={16} />
              <span>{addError}</span>
            </div>
          )}
          {addSuccess && (
            <div className="alert alert-success animate-fade-in" style={{ marginTop: '32px' }}>
              <Check size={16} />
              <span>Added to bag successfully!</span>
            </div>
          )}

          {/* Add to Bag Button */}
          <div className="product-detail-actions">
            <button
              disabled={!selectedVariant || !selectedVariant.inStock || isAdding}
              className="btn btn-primary"
              onClick={handleAddToBag}
            >
              <ShoppingBag size={18} style={{ marginRight: '8px' }} />
              <span>{isAdding ? 'Adding to Bag...' : 'Add to Bag'}</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
