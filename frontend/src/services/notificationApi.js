import axiosInstance from './axiosInstance';

export const notificationApi = {
  async getNotifications(params = {}) {
    const cleaned = Object.fromEntries(
      Object.entries(params).filter(([, val]) => val !== '' && val !== null && val !== undefined)
    );
    const res = await axiosInstance.get('/notifications', { params: cleaned });
    return res.data;
  },
  async getUnreadCount() {
    const res = await axiosInstance.get('/notifications/unread-count');
    return res.data;
  },
  async markAsRead(id) {
    const res = await axiosInstance.patch(`/notifications/${id}/read`);
    return res.data;
  },
  async markAllAsRead() {
    const res = await axiosInstance.patch('/notifications/read-all');
    return res.data;
  },
  async deleteNotification(id) {
    const res = await axiosInstance.delete(`/notifications/${id}`);
    return res.data;
  },
};

