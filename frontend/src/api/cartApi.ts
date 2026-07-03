import { request } from './apiClient';

export interface CartItemResponse {
  cartItemId: number;
  productId: number;
  productName: string;
  imageUrl?: string;
  variantId: number;
  size: string;
  color: string;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
  available: boolean;
}

export interface CartResponse {
  items: CartItemResponse[];
  cartTotal: number;
  totalQuantity: number;
}

export const cartApi = {
  getCart() {
    return request<CartResponse>('/cart');
  },
  addCartItem(variantId: number, quantity: number) {
    return request<CartResponse>('/cart/items', {
      method: 'POST',
      body: JSON.stringify({ variantId, quantity })
    });
  },
  updateCartItemQuantity(cartItemId: number, quantity: number) {
    return request<CartResponse>(`/cart/items/${cartItemId}`, {
      method: 'PUT',
      body: JSON.stringify({ quantity })
    });
  },
  removeCartItem(cartItemId: number) {
    return request<void>(`/cart/items/${cartItemId}`, {
      method: 'DELETE'
    });
  }
};
