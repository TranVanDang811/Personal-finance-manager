import axiosInstance from './axiosInstance';

export const accountApi = {
  async getAccounts(params = {}) {
    const res = await axiosInstance.get('/accounts', { params });
    return res.data;
  },
  async getAccountSummary() {
    const res = await axiosInstance.get('/accounts/summary');
    return res.data;
  },
  async getAccountById(id) {
    const res = await axiosInstance.get(`/accounts/${id}`);
    return res.data;
  },
  async getAccountTransactions(id, params = {}) {
    const res = await axiosInstance.get(`/accounts/${id}/transactions`, { params });
    return res.data;
  },
  async createAccount(payload) {
    const res = await axiosInstance.post('/accounts', payload);
    return res.data;
  },
  async updateAccount(id, payload) {
    const res = await axiosInstance.put(`/accounts/${id}`, payload);
    return res.data;
  },
  async archiveAccount(id) {
    const res = await axiosInstance.patch(`/accounts/${id}/archive`);
    return res.data;
  },
  async unarchiveAccount(id) {
    const res = await axiosInstance.patch(`/accounts/${id}/unarchive`);
    return res.data;
  },
  async deleteAccount(id) {
    const res = await axiosInstance.delete(`/accounts/${id}`);
    return res.data;
  },
};

