export interface User {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
  emailVerified?: boolean;
}

let accessToken: string | null = null;
let refreshToken: string | null = null;
let currentUser: User | null = null;

type AuthListener = (user: User | null) => void;
const listeners = new Set<AuthListener>();

export const authStore = {
  getAccessToken() {
    return accessToken;
  },
  setAccessToken(token: string | null) {
    accessToken = token;
  },
  getRefreshToken() {
    return refreshToken;
  },
  setRefreshToken(token: string | null) {
    refreshToken = token;
  },
  getCurrentUser() {
    return currentUser;
  },
  setCurrentUser(user: User | null) {
    currentUser = user;
    listeners.forEach((listener) => listener(user));
  },
  subscribe(listener: AuthListener) {
    listeners.add(listener);
    return () => {
      listeners.delete(listener);
    };
  },
  clear() {
    accessToken = null;
    refreshToken = null;
    currentUser = null;
    listeners.forEach((listener) => listener(null));
  }
};
