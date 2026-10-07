import axios from 'axios';

const AUTH_KEY = 'elms_auth';
export const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

const api = axios.create({
  baseURL: API_URL,
  headers: { 'Content-Type': 'application/json' },
  timeout: 20000,
});

export const getStoredAuth = () => {
  try {
    return JSON.parse(localStorage.getItem(AUTH_KEY));
  } catch {
    return null;
  }
};
export const storeAuth = (auth) => localStorage.setItem(AUTH_KEY, JSON.stringify(auth));
export const clearAuth = () => localStorage.removeItem(AUTH_KEY);

let unauthorizedHandler = null;
export const setUnauthorizedHandler = (fn) => {
  unauthorizedHandler = fn;
};

// Attach the JWT to every request
api.interceptors.request.use((config) => {
  const auth = getStoredAuth();
  if (auth?.token) config.headers.Authorization = `Bearer ${auth.token}`;
  return config;
});

// A 401 on any protected call means the token is missing / expired / logged out
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const url = error.config?.url || '';
    const isAuthCall = url.includes('/api/auth/login') || url.includes('/api/auth/register');
    if (error.response?.status === 401 && !isAuthCall && getStoredAuth()?.token) {
      unauthorizedHandler?.();
    }
    return Promise.reject(error);
  },
);

/** Human readable message from the backend's consistent error body. */
export const getErrorMessage = (error) => {
  if (!error.response) return `Cannot reach the server at ${API_URL}. Is the backend running?`;
  const data = error.response.data;
  if (data?.errors && typeof data.errors === 'object') {
    const first = Object.values(data.errors)[0];
    if (first) return String(first);
  }
  return data?.message || `Request failed (${error.response.status})`;
};

/** Field-level validation errors ({ email: "...", firstName: "..." }) or an empty object. */
export const getFieldErrors = (error) => error.response?.data?.errors || {};

export default api;
