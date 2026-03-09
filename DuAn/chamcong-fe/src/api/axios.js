import axios from 'axios'
import useAuthStore from '../store/authStore'
import { API_ENDPOINTS } from '../constants/apiEndpoints'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  headers: { 'Content-Type': 'application/json' },
})

let isRefreshing = false
let failedQueue = []

const processQueue = (error, token = null) => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error)
    } else {
      prom.resolve(token)
    }
  })
  failedQueue = []
}

api.interceptors.request.use((config) => {
  const accessToken = useAuthStore.getState().accessToken
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config

    if (
      error.response?.status === 401 &&
      !originalRequest._retry &&
      !originalRequest.url?.includes(API_ENDPOINTS.REFRESH_TOKEN) &&
      !originalRequest.url?.includes(API_ENDPOINTS.LOGIN)
    ) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject })
        }).then((token) => {
          originalRequest.headers.Authorization = `Bearer ${token}`
          return api(originalRequest)
        })
      }

      originalRequest._retry = true
      isRefreshing = true

      try {
        const currentRefreshToken = useAuthStore.getState().refreshToken
        if (!currentRefreshToken) {
          throw new Error('No refresh token')
        }

        const { data } = await axios.post(
          `${import.meta.env.VITE_API_BASE_URL}${API_ENDPOINTS.REFRESH_TOKEN}`,
          { refreshToken: currentRefreshToken }
        )

        const newAccessToken = data.data.accessToken
        const newRefreshToken = data.data.refreshToken

        useAuthStore.getState().updateTokens({
          accessToken: newAccessToken,
          refreshToken: newRefreshToken,
        })

        processQueue(null, newAccessToken)

        originalRequest.headers.Authorization = `Bearer ${newAccessToken}`
        return api(originalRequest)
      } catch (refreshError) {
        processQueue(refreshError, null)
        useAuthStore.getState().clearAuth()
        window.location.href = '/login'
        return Promise.reject(refreshError)
      } finally {
        isRefreshing = false
      }
    }

    return Promise.reject(error)
  }
)

export default api

