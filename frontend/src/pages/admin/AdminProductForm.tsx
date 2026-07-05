import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { adminClient } from '../../api/adminClient';
import type { CategoryDto } from '../../api/adminClient';
import { request } from '../../api/apiClient';
import { ArrowLeft, Upload, Loader2 } from 'lucide-react';

export const AdminProductForm: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const isEditing = Boolean(id);
  const navigate = useNavigate();

  const [categories, setCategories] = useState<CategoryDto[]>([]);
  
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    categoryId: '',
    basePrice: '',
    isActive: true,
    imageUrl: ''
  });

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isUploading, setIsUploading] = useState(false);

  useEffect(() => {
    // Fetch categories
    request<CategoryDto[]>('/catalog/categories').then(setCategories).catch(console.error);

    if (isEditing && id) {
      adminClient.getProductById(parseInt(id, 10)).then((prod) => {
        setFormData({
          name: prod.name,
          description: prod.description,
          categoryId: prod.category.id.toString(),
          basePrice: prod.basePrice.toString(),
          isActive: prod.isActive,
          imageUrl: prod.imageUrl || ''
        });
      }).catch(console.error);
    }
  }, [isEditing, id]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
    const { name, value, type } = e.target;
    const val = type === 'checkbox' ? (e.target as HTMLInputElement).checked : value;
    setFormData(prev => ({ ...prev, [name]: val }));
  };

  const handleImageUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    try {
      setIsUploading(true);
      const res = await adminClient.uploadImage(file);
      setFormData(prev => ({ ...prev, imageUrl: res.url }));
    } catch (err) {
      alert('Image upload failed');
    } finally {
      setIsUploading(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.categoryId) {
      alert('Please select a category');
      return;
    }

    const payload = {
      name: formData.name,
      description: formData.description,
      categoryId: parseInt(formData.categoryId, 10),
      basePrice: parseFloat(formData.basePrice),
      isActive: formData.isActive,
      imageUrl: formData.imageUrl
    };

    try {
      setIsSubmitting(true);
      if (isEditing && id) {
        await adminClient.updateProduct(parseInt(id, 10), payload);
      } else {
        await adminClient.createProduct(payload);
      }
      navigate('/admin/products');
    } catch (err) {
      alert('Failed to save product');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="animate-fade-in" style={{ maxWidth: '800px', margin: '0 auto' }}>
      <button onClick={() => navigate('/admin/products')} style={{ display: 'flex', alignItems: 'center', gap: '8px', background: 'none', border: 'none', color: 'var(--text-secondary)', cursor: 'pointer', marginBottom: '24px', fontSize: '0.875rem' }}>
        <ArrowLeft size={16} />
        <span>Back to Products</span>
      </button>

      <h1 style={{ fontSize: '2rem', fontFamily: 'var(--font-title)', marginBottom: '32px' }}>
        {isEditing ? 'Edit Product' : 'Add New Product'}
      </h1>

      <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '32px' }}>
        
        {/* Basic Info */}
        <div style={{ backgroundColor: 'var(--bg-card)', padding: '32px', borderRadius: '12px', border: '1px solid var(--border)', display: 'flex', flexDirection: 'column', gap: '24px' }}>
          <h2 style={{ fontSize: '1.25rem', fontWeight: 600, borderBottom: '1px solid var(--border)', paddingBottom: '16px' }}>Basic Information</h2>
          
          <div className="form-group">
            <label htmlFor="name" className="form-label">Product Name</label>
            <input type="text" id="name" name="name" className="form-input" value={formData.name} onChange={handleChange} required />
          </div>
          
          <div className="form-group">
            <label htmlFor="description" className="form-label">Description</label>
            <textarea id="description" name="description" className="form-input" value={formData.description} onChange={handleChange} rows={5} required style={{ resize: 'vertical' }} />
          </div>

          <div className="grid grid-2" style={{ gap: '24px' }}>
            <div className="form-group">
              <label htmlFor="basePrice" className="form-label">Base Price (LKR)</label>
              <input type="number" step="0.01" id="basePrice" name="basePrice" className="form-input" value={formData.basePrice} onChange={handleChange} required />
            </div>
            
            <div className="form-group">
              <label htmlFor="categoryId" className="form-label">Category</label>
              <select id="categoryId" name="categoryId" className="form-input" value={formData.categoryId} onChange={handleChange} required>
                <option value="">Select Category...</option>
                {categories.map(cat => (
                  <option key={cat.id} value={cat.id}>{cat.name}</option>
                ))}
              </select>
            </div>
          </div>
        </div>

        {/* Media */}
        <div style={{ backgroundColor: 'var(--bg-card)', padding: '32px', borderRadius: '12px', border: '1px solid var(--border)', display: 'flex', flexDirection: 'column', gap: '24px' }}>
          <h2 style={{ fontSize: '1.25rem', fontWeight: 600, borderBottom: '1px solid var(--border)', paddingBottom: '16px' }}>Media</h2>
          
          <div style={{ display: 'flex', gap: '32px' }}>
            <div style={{ width: '200px', height: '266px', backgroundColor: 'var(--bg-secondary)', borderRadius: '8px', border: '1px dashed var(--border)', display: 'flex', alignItems: 'center', justifyContent: 'center', overflow: 'hidden' }}>
              {formData.imageUrl ? (
                <img src={formData.imageUrl} alt="Preview" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
              ) : (
                <span style={{ color: 'var(--text-muted)' }}>No Image</span>
              )}
            </div>

            <div style={{ flex: 1, display: 'flex', flexDirection: 'column', justifyContent: 'center', gap: '16px' }}>
              <label className="btn btn-secondary" style={{ width: 'fit-content', cursor: 'pointer' }}>
                {isUploading ? <Loader2 size={16} className="animate-spin" /> : <Upload size={16} />}
                <span>{isUploading ? 'Uploading...' : 'Upload Image'}</span>
                <input type="file" accept="image/jpeg, image/png, image/webp" style={{ display: 'none' }} onChange={handleImageUpload} disabled={isUploading} />
              </label>
              <p style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>Recommended size: 800x1200 (3:4 ratio). Max file size: 5MB.</p>
              
              <div className="form-group" style={{ marginTop: '16px' }}>
                <label className="form-label">Or Image URL</label>
                <input type="text" name="imageUrl" className="form-input" value={formData.imageUrl} onChange={handleChange} placeholder="https://..." />
              </div>
            </div>
          </div>
        </div>

        {/* Settings */}
        <div style={{ backgroundColor: 'var(--bg-card)', padding: '32px', borderRadius: '12px', border: '1px solid var(--border)', display: 'flex', flexDirection: 'column', gap: '24px' }}>
          <h2 style={{ fontSize: '1.25rem', fontWeight: 600, borderBottom: '1px solid var(--border)', paddingBottom: '16px' }}>Settings</h2>
          
          <label style={{ display: 'flex', alignItems: 'center', gap: '12px', cursor: 'pointer' }}>
            <input type="checkbox" name="isActive" checked={formData.isActive} onChange={handleChange} style={{ width: '20px', height: '20px' }} />
            <span style={{ fontWeight: 500 }}>Active (Visible on Storefront)</span>
          </label>
        </div>

        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '16px' }}>
          <button type="button" className="btn btn-secondary" onClick={() => navigate('/admin/products')}>Cancel</button>
          <button type="submit" className="btn btn-primary" disabled={isSubmitting}>
            {isSubmitting ? 'Saving...' : 'Save Product'}
          </button>
        </div>

      </form>
    </div>
  );
};
