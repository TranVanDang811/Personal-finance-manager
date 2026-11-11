import axiosInstance from './axiosInstance';

export const categoryApi = {
  async getCategories(type) {
    const params = type ? { type } : {};
    const res = await axiosInstance.get('/categories', { params });
    return res.data;
  },
  async createCategory(payload) {
    const res = await axiosInstance.post('/categories', payload);
    return res.data;
  },
  async updateCategory(id, payload) {
    const res = await axiosInstance.put(`/categories/${id}`, payload);
    return res.data;
  },
  async deleteCategory(id) {
    const res = await axiosInstance.delete(`/categories/${id}`);
    return res.data;
  },
};

