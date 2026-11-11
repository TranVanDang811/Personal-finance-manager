import axiosInstance from './axiosInstance';

export const authApi = {
  async register(payload) {
    const res = await axiosInstance.post('/auth/register', payload);
    return res.data;
  },
  async login(payload) {
    const res = await axiosInstance.post('/auth/login', payload);
    return res.data;
  },
  async refreshToken(refreshToken) {
    const res = await axiosInstance.post('/auth/refresh-token', { refreshToken });
    return res.data;
  },
  async logout(refreshToken) {
    const res = await axiosInstance.post('/auth/logout', refreshToken ? { refreshToken } : {});
    return res.data;
  },
  async forgotPassword(email) {
    const res = await axiosInstance.post('/auth/forgot-password', { email });
    return res.data;
  },
  async resetPassword(payload) {
    const res = await axiosInstance.post('/auth/reset-password', payload);
    return res.data;
  },
};

