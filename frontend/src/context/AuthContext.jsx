import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { authApi } from '../api/services';
import { clearAuth, getStoredAuth, setUnauthorizedHandler, storeAuth } from '../api/client';
import { useToast } from './ToastContext';

const AuthContext = createContext(null);

const isExpired = (token) => {
  try {
    const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')));
    return payload.exp * 1000 < Date.now();
  } catch {
    return true;
  }
};

const loadInitialAuth = () => {
  const stored = getStoredAuth();
  if (stored?.token && !isExpired(stored.token)) return stored;
  clearAuth();
  return null;
};

export const STAFF_ROLES = ['ADMIN', 'HR'];

export function AuthProvider({ children }) {
  const toast = useToast();
  const [auth, setAuth] = useState(loadInitialAuth);

  const login = useCallback(async (email, password) => {
    const result = await authApi.login({ email, password });
    storeAuth(result);
    setAuth(result);
    return result;
  }, []);

  const logout = useCallback(async () => {
    if (getStoredAuth()?.token) {
      try {
        await authApi.logout(); // blacklists the JWT on the server
      } catch {
        /* the token may already be invalid - ignore */
      }
    }
    clearAuth();
    setAuth(null);
  }, []);

  const updateUser = useCallback((changes) => {
    setAuth((current) => {
      const next = { ...current, ...changes };
      storeAuth(next);
      return next;
    });
  }, []);

  // Called by the axios interceptor when the server answers 401
  useEffect(() => {
    setUnauthorizedHandler(() => {
      clearAuth();
      setAuth(null);
      toast.info('Your session has expired. Please sign in again.');
    });
  }, [toast]);

  const value = useMemo(
    () => ({ auth, login, logout, updateUser, isStaff: !!auth && STAFF_ROLES.includes(auth.role), isAdmin: auth?.role === 'ADMIN' }),
    [auth, login, logout, updateUser],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export const useAuth = () => useContext(AuthContext);
