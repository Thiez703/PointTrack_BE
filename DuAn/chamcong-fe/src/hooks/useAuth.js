import { useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import useAuthStore from '../store/authStore'
import { authApi } from '../api/authApi'
import { ROUTES } from '../constants/routes'

export function useAuth() {
  const navigate = useNavigate()
  const { setAuth, clearAuth, updateTokens, setForcePasswordChange, user, isAuthenticated, forcePasswordChange } =
    useAuthStore()

  const login = useCallback(
    async (credentials) => {
      const { data } = await authApi.login(credentials)
      const result = data.data

      setAuth({
        user: result.user,
        accessToken: result.accessToken,
        refreshToken: result.refreshToken,
        forcePasswordChange: result.forcePasswordChange,
      })

      if (result.forcePasswordChange) {
        navigate(ROUTES.FORCE_CHANGE_PASSWORD, { replace: true })
      } else {
        navigate(ROUTES.DASHBOARD, { replace: true })
      }

      return result
    },
    [setAuth, navigate]
  )

  const firstChangePassword = useCallback(
    async (passwords) => {
      const { data } = await authApi.firstChangePassword(passwords)
      const result = data.data

      updateTokens({
        accessToken: result.accessToken,
        refreshToken: result.refreshToken,
      })
      setForcePasswordChange(false)

      navigate(ROUTES.DASHBOARD, { replace: true })
      return result
    },
    [updateTokens, setForcePasswordChange, navigate]
  )

  const logout = useCallback(async () => {
    try {
      const refreshToken = useAuthStore.getState().refreshToken
      await authApi.logout({ refreshToken })
    } catch {
      // ignore
    } finally {
      clearAuth()
      navigate(ROUTES.LOGIN, { replace: true })
    }
  }, [clearAuth, navigate])

  return {
    user,
    isAuthenticated,
    forcePasswordChange,
    login,
    firstChangePassword,
    logout,
  }
}

