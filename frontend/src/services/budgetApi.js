import axiosInstance from './axiosInstance';

export const budgetApi = {
  async getBudgets(params = {}) {
    const res = await axiosInstance.get('/budgets', { params });
    return res.data;
  },
  async getBudgetSummary(params = {}) {
    const res = await axiosInstance.get('/budgets/summary', { params });
    return res.data;
  },
  async createBudget(payload) {
    const res = await axiosInstance.post('/budgets', payload);
    return res.data;
  },
  async updateBudget(id, payload) {
    const res = await axiosInstance.put(`/budgets/${id}`, payload);
    return res.data;
  },
  async deleteBudget(id) {
    const res = await axiosInstance.delete(`/budgets/${id}`);
    return res.data;
  },
};

