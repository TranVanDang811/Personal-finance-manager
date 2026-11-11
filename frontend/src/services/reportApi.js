import axiosInstance from './axiosInstance';

function cleanParams(params = {}) {
  return Object.fromEntries(
    Object.entries(params).filter(([, val]) => val !== '' && val !== null && val !== undefined)
  );
}

export const reportApi = {
  async getDashboardSummary(params = {}) {
    const res = await axiosInstance.get('/reports/dashboard', { params: cleanParams(params) });
    return res.data;
  },
  async getCategoryBreakdown(params = {}) {
    const res = await axiosInstance.get('/reports/category-breakdown', { params: cleanParams(params) });
    return res.data;
  },
  async getCashflowTrend(params = {}) {
    const res = await axiosInstance.get('/reports/cashflow-trend', { params: cleanParams(params) });
    return res.data;
  },
  async getPeriodSummary(params = {}) {
    const res = await axiosInstance.get('/reports/period-summary', { params: cleanParams(params) });
    return res.data;
  },
};

