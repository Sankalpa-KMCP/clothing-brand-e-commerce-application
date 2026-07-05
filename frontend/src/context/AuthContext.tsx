import React, { createContext, useContext, useState, useEffect } from 'react';
import { authStore, type User } from '../api/authStore';
import { request } from '../api/apiClient';

interface AuthContextType {
  user: User | null;
  isAuthenticated: boolean;
  isAdmin: boolean;
  isLoading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (
    email: string,
    firstName: string,
    lastName: string,
    password: string
  ) => Promise<RegisterResult>;
  resendVerification: (email: string) => Promise<string>;
  verifyEmail: (token: string) => Promise<string>;
  forgotPassword: (email: string) => Promise<string>;
  resetPassword: (token: string, password: string) => Promise<string>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

interface AuthResponse {
  token: string | null;
  refreshToken: string | null;
  user: User;
  verificationRequired?: boolean;
}

interface MessageResponse {
  message: string;
}

export interface RegisterResult {
  verificationRequired: boolean;
  user: User;
}

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(authStore.getCurrentUser());
  const [isLoading, setIsLoading] = useState<boolean>(false);

  useEffect(() => {
    // Subscribe to in-memory store changes (e.g. self-revocation on 401)
    const unsubscribe = authStore.subscribe((updatedUser) => {
      setUser(updatedUser);
    });
    return unsubscribe;
  }, []);

  const login = async (email: string, password: string) => {
    setIsLoading(true);
    try {
      const response = await request<AuthResponse>('/auth/login', {
        method: 'POST',
        body: JSON.stringify({ email, password })
      });
      
      authStore.setAccessToken(response.token);
      authStore.setRefreshToken(response.refreshToken);
      authStore.setCurrentUser(response.user);
    } finally {
      setIsLoading(false);
    }
  };
 
  const register = async (
    email: string,
    firstName: string,
    lastName: string,
    password: string
  ) => {
    setIsLoading(true);
    try {
      const response = await request<AuthResponse>('/auth/register', {
        method: 'POST',
        body: JSON.stringify({ email, firstName, lastName, password })
      });
 
      if (response.token && response.refreshToken) {
        authStore.setAccessToken(response.token);
        authStore.setRefreshToken(response.refreshToken);
        authStore.setCurrentUser(response.user);
      }

      return {
        verificationRequired: response.verificationRequired === true,
        user: response.user
      };
    } finally {
      setIsLoading(false);
    }
  };

  const resendVerification = async (email: string) => {
    const response = await request<MessageResponse>('/auth/verification/resend', {
      method: 'POST',
      body: JSON.stringify({ email })
    });
    return response.message;
  };

  const verifyEmail = async (token: string) => {
    const response = await request<MessageResponse>('/auth/verification/verify', {
      method: 'POST',
      body: JSON.stringify({ token })
    });
    return response.message;
  };

  const forgotPassword = async (email: string) => {
    const response = await request<MessageResponse>('/auth/forgot-password', {
      method: 'POST',
      body: JSON.stringify({ email })
    });
    return response.message;
  };

  const resetPassword = async (token: string, password: string) => {
    const response = await request<MessageResponse>('/auth/reset-password', {
      method: 'POST',
      body: JSON.stringify({ token, password })
    });
    return response.message;
  };

  const logout = async () => {
    setIsLoading(true);
    try {
      const currentRefreshToken = authStore.getRefreshToken();
      if (currentRefreshToken) {
        // Safe logout call to backend to invalidate token
        await request('/auth/logout', {
          method: 'POST',
          body: JSON.stringify({ refreshToken: currentRefreshToken })
        }).catch(() => {
          // ignore backend failure on logout
        });
      }
    } finally {
      authStore.clear();
      setIsLoading(false);
    }
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        isAuthenticated: !!user,
        isAdmin: user?.role === 'ROLE_ADMIN' || user?.role === 'ADMIN',
        isLoading,
        login,
        register,
        resendVerification,
        verifyEmail,
        forgotPassword,
        resetPassword,
        logout
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
