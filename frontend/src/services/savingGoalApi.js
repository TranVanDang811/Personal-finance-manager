import axiosInstance from './axiosInstance';

export const savingGoalApi = {
  async getSavingGoals(status) {
    const params = status ? { status } : {};
    const res = await axiosInstance.get('/saving-goals', { params });
    return res.data;
  },
  async createSavingGoal(payload) {
    const res = await axiosInstance.post('/saving-goals', payload);
    return res.data;
  },
  async updateSavingGoal(id, payload) {
    const res = await axiosInstance.put(`/saving-goals/${id}`, payload);
    return res.data;
  },
  async cancelSavingGoal(id) {
    const res = await axiosInstance.patch(`/saving-goals/${id}/cancel`);
    return res.data;
  },
  async deleteSavingGoal(id) {
    const res = await axiosInstance.delete(`/saving-goals/${id}`);
    return res.data;
  },
  async deposit(id, payload) {
    const res = await axiosInstance.post(`/saving-goals/${id}/deposit`, payload);
    return res.data;
  },
  async withdraw(id, payload) {
    const res = await axiosInstance.post(`/saving-goals/${id}/withdraw`, payload);
    return res.data;
  },
  async getContributions(id, params = {}) {
    const res = await axiosInstance.get(`/saving-goals/${id}/contributions`, { params });
    return res.data;
  },
};

