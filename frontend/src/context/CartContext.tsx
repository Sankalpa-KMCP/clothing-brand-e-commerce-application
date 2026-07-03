import React, { createContext, useContext, useState, useEffect, useRef, useCallback } from 'react';
import { useAuth } from './AuthContext';
import { cartApi, type CartResponse } from '../api/cartApi';

interface CartContextType {
  cart: CartResponse | null;
  cartCount: number;
  isLoading: boolean;
  error: string | null;
  fetchCart: () => Promise<void>;
  addItem: (variantId: number, quantity: number) => Promise<void>;
  updateQty: (cartItemId: number, quantity: number) => Promise<void>;
  removeItem: (cartItemId: number) => Promise<void>;
  clearCart: () => void;
}

const CartContext = createContext<CartContextType | undefined>(undefined);

export const CartProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { isAuthenticated, user } = useAuth();
  const [cart, setCart] = useState<CartResponse | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const updateSeq = useRef(0);

  const fetchCart = useCallback(async () => {
    if (!isAuthenticated) {
      setCart(null);
      return;
    }
    const currentSeq = ++updateSeq.current;
    setIsLoading(true);
    setError(null);
    try {
      const data = await cartApi.getCart();
      if (currentSeq === updateSeq.current) {
        setCart(data);
      }
    } catch (err: any) {
      if (currentSeq === updateSeq.current) {
        setError(err.message || 'Failed to fetch cart');
      }
    } finally {
      if (currentSeq === updateSeq.current) {
        setIsLoading(false);
      }
    }
  }, [isAuthenticated]);

  useEffect(() => {
    if (isAuthenticated) {
      fetchCart();
    } else {
      setCart(null);
      setError(null);
    }
  }, [isAuthenticated, user, fetchCart]);

  const addItem = useCallback(async (variantId: number, quantity: number) => {
    const currentSeq = ++updateSeq.current;
    setIsLoading(true);
    setError(null);
    try {
      const data = await cartApi.addCartItem(variantId, quantity);
      if (currentSeq === updateSeq.current) {
        setCart(data);
      }
    } catch (err: any) {
      setError(err.message || 'Failed to add item to cart');
      throw err;
    } finally {
      if (currentSeq === updateSeq.current) {
        setIsLoading(false);
      }
    }
  }, []);

  const updateQty = useCallback(async (cartItemId: number, quantity: number) => {
    const currentSeq = ++updateSeq.current;
    setIsLoading(true);
    setError(null);
    try {
      const data = await cartApi.updateCartItemQuantity(cartItemId, quantity);
      if (currentSeq === updateSeq.current) {
        setCart(data);
      }
    } catch (err: any) {
      setError(err.message || 'Failed to update quantity');
      throw err;
    } finally {
      if (currentSeq === updateSeq.current) {
        setIsLoading(false);
      }
    }
  }, []);

  const removeItem = useCallback(async (cartItemId: number) => {
    const currentSeq = ++updateSeq.current;
    setIsLoading(true);
    setError(null);
    try {
      await cartApi.removeCartItem(cartItemId);
      const data = await cartApi.getCart();
      if (currentSeq === updateSeq.current) {
        setCart(data);
      }
    } catch (err: any) {
      setError(err.message || 'Failed to remove item');
      throw err;
    } finally {
      if (currentSeq === updateSeq.current) {
        setIsLoading(false);
      }
    }
  }, []);

  const clearCart = useCallback(() => {
    setCart(null);
    setError(null);
  }, []);

  const cartCount = cart ? cart.totalQuantity : 0;

  return (
    <CartContext.Provider
      value={{
        cart,
        cartCount,
        isLoading,
        error,
        fetchCart,
        addItem,
        updateQty,
        removeItem,
        clearCart
      }}
    >
      {children}
    </CartContext.Provider>
  );
};

export const useCart = () => {
  const context = useContext(CartContext);
  if (context === undefined) {
    throw new Error('useCart must be used within a CartProvider');
  }
  return context;
};
