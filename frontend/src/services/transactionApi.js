import axiosInstance from './axiosInstance';
import { generateIdempotencyKey } from '../utils/formatters';

export const transactionApi = {
  async getTransactions(params = {}) {
    const cleanedParams = Object.fromEntries(
      Object.entries(params).filter(([, val]) => val !== '' && val !== null && val !== undefined)
    );
    const res = await axiosInstance.get('/transactions', { params: cleanedParams });
    return res.data;
  },
  async getTransactionById(id) {
    const res = await axiosInstance.get(`/transactions/${id}`);
    return res.data;
  },
  async createTransaction(payload, idempotencyKey = generateIdempotencyKey()) {
    const res = await axiosInstance.post('/transactions', payload, {
      headers: { 'X-Idempotency-Key': idempotencyKey },
    });
    return res.data;
  },
  async updateTransaction(id, payload) {
    const res = await axiosInstance.put(`/transactions/${id}`, payload);
    return res.data;
  },
  async deleteTransaction(id) {
    const res = await axiosInstance.delete(`/transactions/${id}`);
    return res.data;
  },
  async uploadAttachment(id, file) {
    const formData = new FormData();
    formData.append('file', file);
    const res = await axiosInstance.post(`/transactions/${id}/attachment`, formData, {
      headers: { 'Content-Type': undefined },
    });
    return res.data;
  },
};

