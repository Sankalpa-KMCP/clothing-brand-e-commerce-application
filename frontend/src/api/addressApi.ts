import { request } from './apiClient';

export interface AddressRequest {
  label?: string;
  recipientName: string;
  phoneNumber: string;
  addressLine1: string;
  addressLine2?: string;
  city: string;
  region?: string;
  postalCode?: string;
  country: string;
}

export interface AddressResponse {
  id: number;
  label?: string;
  recipientName: string;
  phoneNumber: string;
  addressLine1: string;
  addressLine2?: string;
  city: string;
  region?: string;
  postalCode?: string;
  country: string;
  isDefault: boolean;
}

export const addressApi = {
  getAddresses() {
    return request<AddressResponse[]>('/addresses');
  },
  getAddress(addressId: number) {
    return request<AddressResponse>(`/addresses/${addressId}`);
  },
  createAddress(address: AddressRequest) {
    return request<AddressResponse>('/addresses', {
      method: 'POST',
      body: JSON.stringify(address)
    });
  },
  updateAddress(addressId: number, address: AddressRequest) {
    return request<AddressResponse>(`/addresses/${addressId}`, {
      method: 'PUT',
      body: JSON.stringify(address)
    });
  },
  deleteAddress(addressId: number) {
    return request<void>(`/addresses/${addressId}`, {
      method: 'DELETE'
    });
  },
  setDefaultAddress(addressId: number) {
    return request<AddressResponse>(`/addresses/${addressId}/default`, {
      method: 'PATCH'
    });
  }
};
