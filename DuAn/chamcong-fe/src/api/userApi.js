import api from './axios'
import { API_ENDPOINTS } from '../constants/apiEndpoints'

export const userApi = {
  getProfile: () => api.get(API_ENDPOINTS.PROFILE),
  updateProfile: (data) => api.put(API_ENDPOINTS.PROFILE, data),
  changePassword: (data) => api.put(API_ENDPOINTS.CHANGE_PASSWORD, data),
}

