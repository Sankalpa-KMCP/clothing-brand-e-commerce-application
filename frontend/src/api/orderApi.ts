import { request } from './apiClient';

export interface CheckoutRequestDto {
  addressId?: number;
}

export interface OrderItemResponseDto {
  productName: string;
  productImageUrl: string;
  size: string;
  color: string;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
}

export interface OrderResponseDto {
  id: number;
  status: string;
  subtotal: number;
  total: number;
  items: OrderItemResponseDto[];
}

export interface StripeCheckoutResponseDto {
  orderId: number;
  stripeCheckoutUrl: string;
  reservationExpiresAt: string | null;
}

export interface OrderSummaryResponseDto {
  id: number;
  status: string;
  subtotal: number;
  total: number;
  createdAt: string;
}

export interface OrderHistoryPageResponseDto {
  content: OrderSummaryResponseDto[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface OrderDeliveryAddressResponseDto {
  recipientName: string;
  phoneNumber: string;
  addressLine1: string;
  addressLine2: string | null;
  city: string;
  region: string;
  postalCode: string;
  country: string;
}

export interface OrderStatusHistoryResponseDto {
  previousStatus: string | null;
  newStatus: string;
  actorType: string;
  createdAt: string;
}

export interface OrderDetailResponseDto {
  id: number;
  status: string;
  subtotal: number;
  total: number;
  createdAt: string;
  items: OrderItemResponseDto[];
  statusHistory: OrderStatusHistoryResponseDto[];
  deliveryAddress: OrderDeliveryAddressResponseDto;
}

export const orderApi = {
  checkout(addressId?: number) {
    const body: CheckoutRequestDto | undefined = addressId ? { addressId } : undefined;
    return request<OrderResponseDto>('/orders/checkout', {
      method: 'POST',
      body: body ? JSON.stringify(body) : undefined
    });
  },

  reserveCheckoutSession(addressId?: number) {
    const body: CheckoutRequestDto | undefined = addressId ? { addressId } : undefined;
    return request<StripeCheckoutResponseDto>('/orders/checkout/reserve', {
      method: 'POST',
      body: body ? JSON.stringify(body) : undefined
    });
  },
  
  getMyOrders(page: number = 0, size: number = 20) {
    // Note: Request supports query params in URL, need to append manually or update apiClient if missing.
    // Assuming apiClient doesn't have a `params` feature from the simple fetch wrapper.
    return request<OrderHistoryPageResponseDto>(`/orders?page=${page}&size=${size}`);
  },

  getMyOrder(orderId: number) {
    return request<OrderDetailResponseDto>(`/orders/${orderId}`);
  },

  cancelMyOrder(orderId: number) {
    return request<OrderDetailResponseDto>(`/orders/${orderId}/cancel`, {
      method: 'POST'
    });
  }
};
