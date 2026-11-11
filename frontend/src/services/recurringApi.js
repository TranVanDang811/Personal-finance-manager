import axiosInstance from './axiosInstance';

export const recurringApi = {
  async getRecurringTransactions(status) {
    const params = status ? { status } : {};
    const res = await axiosInstance.get('/recurring-transactions', { params });
    return res.data;
  },
  async createRecurringTransaction(payload) {
    const res = await axiosInstance.post('/recurring-transactions', payload);
    return res.data;
  },
  async updateRecurringTransaction(id, payload) {
    const res = await axiosInstance.put(`/recurring-transactions/${id}`, payload);
    return res.data;
  },
  async pauseRecurringTransaction(id) {
    const res = await axiosInstance.patch(`/recurring-transactions/${id}/pause`);
    return res.data;
  },
  async resumeRecurringTransaction(id) {
    const res = await axiosInstance.patch(`/recurring-transactions/${id}/resume`);
    return res.data;
  },
  async cancelRecurringTransaction(id) {
    const res = await axiosInstance.patch(`/recurring-transactions/${id}/cancel`);
    return res.data;
  },
  async executeNow(id, allowEarly = false) {
    const res = await axiosInstance.post(`/recurring-transactions/${id}/execute`, null, {
      params: { allowEarly },
    });
    return res.data;
  },
  async deleteRecurringTransaction(id) {
    const res = await axiosInstance.delete(`/recurring-transactions/${id}`);
    return res.data;
  },
};

