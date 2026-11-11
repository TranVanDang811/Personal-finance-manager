import axios from 'axios';
import { storage } from '../utils/storage';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api/v1';

const axiosInstance = axios.create({
  baseURL: API_BASE_URL,
  timeout: 20000,
  headers: {
    'Content-Type': 'application/json',
  },
});

let isRefreshing = false;
let failedQueue = [];
let onTokensRefreshed = null;
let onSessionExpired = null;

export function configureAxiosAuthCallbacks({ handleTokensRefreshed, handleSessionExpired }) {
  onTokensRefreshed = handleTokensRefreshed;
  onSessionExpired = handleSessionExpired;
}

function processQueue(error, token = null) {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });
  failedQueue = [];
}

const AUTH_SKIP_URLS = [
  '/auth/login',
  '/auth/register',
  '/auth/refresh-token',
  '/auth/forgot-password',
  '/auth/reset-password',
];

axiosInstance.interceptors.request.use(
  (config) => {
    const token = storage.getAccessToken();
    if (token && !config.headers.Authorization) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    if (config.data instanceof FormData) {
      if (typeof config.headers?.delete === 'function') {
        config.headers.delete('Content-Type');
      } else if (config.headers) {
        delete config.headers['Content-Type'];
      }
    }
    return config;
  },
  (error) => Promise.reject(error)
);

axiosInstance.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    const status = error.response?.status;
    const requestUrl = originalRequest?.url || '';

    const shouldSkipRefresh = AUTH_SKIP_URLS.some((path) => requestUrl.includes(path));

    if (status === 401 && originalRequest && !originalRequest._retry && !shouldSkipRefresh) {
      const refreshToken = storage.getRefreshToken();
      if (!refreshToken) {
        storage.clearAuthSession();
        onSessionExpired?.();
        return Promise.reject(error);
      }

      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        })
          .then((newToken) => {
            originalRequest.headers.Authorization = `Bearer ${newToken}`;
            return axiosInstance(originalRequest);
          })
          .catch((queueErr) => Promise.reject(queueErr));
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        const refreshRes = await axios.post(`${API_BASE_URL}/auth/refresh-token`, {
          refreshToken,
        });
        const authData = refreshRes.data?.data;
        if (!authData?.accessToken) {
          throw new Error('Refresh token không trả về access token');
        }

        storage.setAuthSession(authData);
        onTokensRefreshed?.(authData);
        processQueue(null, authData.accessToken);

        originalRequest.headers.Authorization = `Bearer ${authData.accessToken}`;
        return axiosInstance(originalRequest);
      } catch (refreshError) {
        processQueue(refreshError, null);
        storage.clearAuthSession();
        onSessionExpired?.();
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  }
);

export default axiosInstance;

