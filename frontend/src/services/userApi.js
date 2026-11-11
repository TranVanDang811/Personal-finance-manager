import axiosInstance from './axiosInstance';

export const userApi = {
  async getCurrentProfile() {
    const res = await axiosInstance.get('/users/me');
    return res.data;
  },
  async updateProfile(payload) {
    const res = await axiosInstance.put('/users/me', payload);
    return res.data;
  },
  async changePassword(payload) {
    const res = await axiosInstance.post('/users/me/change-password', payload);
    return res.data;
  },
  async uploadAvatar(file) {
    const formData = new FormData();
    formData.append('file', file);
    const res = await axiosInstance.post('/users/me/avatar', formData, {
      headers: { 'Content-Type': undefined },
    });
    return res.data;
  },
};

