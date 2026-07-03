import { authStore } from './authStore';

const BASE_URL = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api').replace(/\/$/, '');

const PUBLIC_PATHS = [
  '/auth/login',
  '/auth/register',
  '/auth/refresh',
  '/auth/logout',
  '/auth/verification/resend',
  '/auth/verification/verify',
  '/auth/forgot-password',
  '/auth/reset-password',
  '/categories',
  '/products'
];

function isPublicPath(path: string): boolean {
  const cleanPath = path.split('?')[0];
  return PUBLIC_PATHS.some(
    (p) => cleanPath === p || cleanPath.startsWith(p + '/')
  );
}

interface RequestOptions extends RequestInit {
  headers?: Record<string, string>;
}

let isRefreshing = false;
let refreshSubscribers: ((token: string | null) => void)[] = [];

function subscribeTokenRefresh(cb: (token: string | null) => void) {
  refreshSubscribers.push(cb);
}

function onRefreshed(token: string | null) {
  refreshSubscribers.forEach((cb) => cb(token));
  refreshSubscribers = [];
}

export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const url = `${BASE_URL}${path}`;
  const headers = new Headers(options.headers || {});

  if (options.body && typeof options.body === 'string' && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }

  const token = authStore.getAccessToken();
  if (token && !isPublicPath(path)) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  const finalOptions: RequestInit = {
    ...options,
    headers
  };

  let response = await fetch(url, finalOptions);

  if (response.status === 401 && !isPublicPath(path)) {
    const originalRefreshToken = authStore.getRefreshToken();
    if (!originalRefreshToken) {
      authStore.clear();
      throw new Error('Unauthorized');
    }

    if (!isRefreshing) {
      isRefreshing = true;
      try {
        const refreshResponse = await fetch(`${BASE_URL}/auth/refresh`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({ refreshToken: originalRefreshToken })
        });

        if (refreshResponse.ok) {
          const data = await refreshResponse.json();
          authStore.setAccessToken(data.token);
          authStore.setRefreshToken(data.refreshToken);
          isRefreshing = false;
          onRefreshed(data.token);
        } else {
          isRefreshing = false;
          onRefreshed(null);
          authStore.clear();
          throw new Error('Session expired');
        }
      } catch (err) {
        isRefreshing = false;
        onRefreshed(null);
        authStore.clear();
        throw err;
      }
    }

    const newAccessToken = await new Promise<string | null>((resolve) => {
      subscribeTokenRefresh((token) => resolve(token));
    });

    if (newAccessToken) {
      headers.set('Authorization', `Bearer ${newAccessToken}`);
      response = await fetch(url, {
        ...options,
        headers
      });
    } else {
      throw new Error('Unauthorized');
    }
  }

  if (!response.ok) {
    let errorMessage = 'An error occurred';
    try {
      const errorData = await response.json();
      errorMessage = errorData.message || errorMessage;
    } catch {
      // ignore
    }
    throw new Error(errorMessage);
  }

  if (response.status === 204) {
    return {} as T;
  }

  return response.json();
}
