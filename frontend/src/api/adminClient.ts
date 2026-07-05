import { request } from './apiClient';
import type { OrderDetailResponseDto, OrderHistoryPageResponseDto } from './orderApi';

export interface CategoryDto {
  id: number;
  name: string;
}

export interface ProductListDto {
  id: number;
  name: string;
  basePrice: number;
  imageUrl?: string;
  isActive: boolean;
}

export interface ProductDetailDto {
  id: number;
  name: string;
  description: string;
  basePrice: number;
  imageUrl?: string;
  isActive: boolean;
  category: CategoryDto;
}

export interface AdminProductRequest {
  name: string;
  description: string;
  categoryId: number;
  basePrice: number;
  imageUrl?: string;
  isActive: boolean;
}

export interface AdminProductVariantRequest {
  size: string;
  color: string;
  stockQuantity: number;
  sku: string;
}

export const adminClient = {
  // --- Products ---
  getProducts: async () => {
    return request<ProductListDto>('/admin/products');
  },
  
  getProductById: async (id: number) => {
    return request<ProductDetailDto>(`/admin/products/${id}`);
  },

  createProduct: async (data: AdminProductRequest) => {
    return request<ProductDetailDto>('/admin/products', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  updateProduct: async (id: number, data: AdminProductRequest) => {
    return request<ProductDetailDto>(`/admin/products/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  },

  deleteProduct: async (id: number) => {
    return request(`/admin/products/${id}`, {
      method: 'DELETE',
    });
  },

  // --- Variants ---
  createVariant: async (productId: number, data: AdminProductVariantRequest) => {
    return request(`/admin/products/${productId}/variants`, {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  updateVariantStock: async (productId: number, variantId: number, quantityAdjustment: number) => {
    return request(`/admin/products/${productId}/variants/${variantId}/stock`, {
      method: 'PATCH',
      body: JSON.stringify({ quantityAdjustment }),
    });
  },

  deleteVariant: async (productId: number, variantId: number) => {
    return request(`/admin/products/${productId}/variants/${variantId}`, {
      method: 'DELETE',
    });
  },

  // --- Images ---
  uploadImage: async (file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    
    // We cannot use the standard request() because it stringifies JSON by default 
    // and sets Content-Type to application/json. We need to use native fetch for FormData.
    const token = (await import('./authStore')).authStore.getAccessToken();
    const API_BASE = import.meta.env.VITE_API_BASE_URL || '/api';
    
    const response = await fetch(`${API_BASE}/admin/images`, {
      method: 'POST',
      headers: {
        ...(token ? { 'Authorization': `Bearer ${token}` } : {})
      },
      body: formData,
    });
    
    if (!response.ok) {
      throw new Error('Image upload failed');
    }
    
    return response.json() as Promise<{ url: string }>;
  },

  // --- Orders ---
  getOrders: async () => {
    return request<OrderHistoryPageResponseDto>('/admin/orders');
  },

  getOrderById: async (id: number) => {
    return request<OrderDetailResponseDto>(`/admin/orders/${id}`);
  },

  updateOrderStatus: async (id: number, status: string, note?: string) => {
    return request(`/admin/orders/${id}/status`, {
      method: 'PATCH',
      body: JSON.stringify({ status, note }),
    });
  },
};
