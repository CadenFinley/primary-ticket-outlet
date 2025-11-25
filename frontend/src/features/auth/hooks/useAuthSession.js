import { useCallback, useEffect, useMemo, useState } from 'react';
import { apiRequest } from '../../../api/client';

const STORAGE_KEY = 'ticket-auth';

export function useAuthSession() {
  const [authState, setAuthState] = useState(() => {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) {
      return { token: null, user: null, roles: [] };
    }
    try {
      return JSON.parse(raw);
    } catch {
      return { token: null, user: null, roles: [] };
    }
  });
  const [error, setError] = useState(null);
  const { token } = authState;

  useEffect(() => {
    if (!token) {
      if (authState.user !== null || authState.roles.length > 0) {
        setAuthState((prev) => ({ ...prev, user: null, roles: [] }));
      }
      localStorage.removeItem(STORAGE_KEY);
      return;
    }
    localStorage.setItem(STORAGE_KEY, JSON.stringify(authState));
  }, [token, authState]);

  useEffect(() => {
    if (!token) return;
    let cancelled = false;
    async function loadProfile() {
      try {
        const profile = await apiRequest('/me', {}, token);
        if (!cancelled) {
          setAuthState((prev) => ({
            ...prev,
            user: profile,
            roles: profile?.roles ?? [],
          }));
          setError(null);
        }
      } catch (err) {
        console.error(err);
        if (!cancelled) {
          setError('Failed to fetch profile');
        }
      }
    }
    loadProfile();
    return () => {
      cancelled = true;
    };
  }, [token]);

  const login = useCallback((authResponse) => {
    setAuthState({
      token: authResponse.token,
      user: {
        id: authResponse.userId,
        email: authResponse.email,
        displayName: authResponse.displayName,
        address: authResponse.address ?? null,
        phoneNumber: authResponse.phoneNumber ?? null,
        roles: authResponse.roles ?? [],
        managedVenues: [],
      },
      roles: authResponse.roles ?? [],
    });
    setError(null);
  }, []);

  const logout = useCallback(() => {
    setAuthState({ token: null, user: null, roles: [] });
    setError(null);
    localStorage.removeItem(STORAGE_KEY);
  }, []);

  const setUser = useCallback((user) => {
    setAuthState((prev) => ({
      ...prev,
      user,
      roles: user?.roles ?? prev.roles,
    }));
  }, []);

  const contextValue = useMemo(
    () => ({
      token,
      user: authState.user,
      roles: authState.roles,
      login,
      logout,
      error,
      setError,
      setUser,
    }),
    [token, authState.user, authState.roles, login, logout, error, setUser]
  );

  return contextValue;
}
