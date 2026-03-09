import api from './axios'
import { API_ENDPOINTS } from '../constants/apiEndpoints'

export const authApi = {
  login: (data) => api.post(API_ENDPOINTS.LOGIN, data),

  firstChangePassword: (data) =>
    api.put(API_ENDPOINTS.FIRST_CHANGE_PASSWORD, data),

  forgotPassword: (data) => api.post(API_ENDPOINTS.FORGOT_PASSWORD, data),

  resetPassword: (data) => api.put(API_ENDPOINTS.RESET_PASSWORD, data),

  changePassword: (data) => api.put(API_ENDPOINTS.CHANGE_PASSWORD, data),

  refreshToken: (data) => api.post(API_ENDPOINTS.REFRESH_TOKEN, data),

  logout: (data) => api.post(API_ENDPOINTS.LOGOUT, data),

  getProfile: () => api.get(API_ENDPOINTS.PROFILE),

  updateProfile: (data) => api.put(API_ENDPOINTS.PROFILE, data),
}

