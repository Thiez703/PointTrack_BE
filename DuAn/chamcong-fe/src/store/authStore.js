import { create } from 'zustand'
import { persist } from 'zustand/middleware'

const useAuthStore = create(
  persist(
    (set) => ({
      user: null,
      accessToken: null,
      refreshToken: null,
      isAuthenticated: false,
      forcePasswordChange: false,

      setAuth: ({ user, accessToken, refreshToken, forcePasswordChange }) =>
        set({
          user,
          accessToken,
          refreshToken,
          isAuthenticated: true,
          forcePasswordChange: forcePasswordChange || false,
        }),

      clearAuth: () =>
        set({
          user: null,
          accessToken: null,
          refreshToken: null,
          isAuthenticated: false,
          forcePasswordChange: false,
        }),

      updateTokens: ({ accessToken, refreshToken }) =>
        set({ accessToken, refreshToken }),

      setForcePasswordChange: (value) =>
        set({ forcePasswordChange: value }),

      updateUser: (userData) =>
        set((state) => ({
          user: state.user ? { ...state.user, ...userData } : userData,
        })),
    }),
    {
      name: 'auth-storage',
      partialize: (state) => ({
        accessToken: state.accessToken,
        refreshToken: state.refreshToken,
        isAuthenticated: state.isAuthenticated,
        user: state.user,
        forcePasswordChange: state.forcePasswordChange,
      }),
    }
  )
)

export default useAuthStore

