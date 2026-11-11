import { useCallback, useEffect, useState } from 'react';
import { useDispatch } from 'react-redux';
import { BellIcon, CheckCircleIcon, CheckIcon, TrashIcon } from '@heroicons/react/24/outline';
import { notificationApi } from '../../services/notificationApi';
import {
  fetchRecentNotifications,
  fetchUnreadCount,
} from '../../features/notifications/notificationSlice';
import { showToast } from '../../features/feedback/feedbackSlice';
import EmptyState from '../../components/common/EmptyState';
import Pagination from '../../components/common/Pagination';
import LoadingSpinner from '../../components/feedback/LoadingSpinner';
import { extractErrorMessage, formatDateTime } from '../../utils/formatters';

export default function NotificationsPage() {
  const dispatch = useDispatch();

  const [loading, setLoading] = useState(true);
  const [filterRead, setFilterRead] = useState('');
  const [page, setPage] = useState(0);
  const [pageData, setPageData] = useState({
    content: [],
    pageNumber: 0,
    pageSize: 15,
    totalElements: 0,
    totalPages: 0,
  });

  const syncHeaderNotifications = useCallback(() => {
    dispatch(fetchUnreadCount());
    dispatch(fetchRecentNotifications());
  }, [dispatch]);

  const loadNotifications = useCallback(async () => {
    setLoading(true);
    try {
      const params = {
        page,
        size: 15,
        isRead: filterRead === '' ? undefined : filterRead === 'READ',
      };
      const res = await notificationApi.getNotifications(params);
      setPageData(
        res.data || { content: [], pageNumber: 0, pageSize: 15, totalElements: 0, totalPages: 0 }
      );
    } catch (err) {
      dispatch(
        showToast({
          type: 'error',
          message: extractErrorMessage(err, 'Không thể tải danh sách thông báo.'),
        })
      );
    } finally {
      setLoading(false);
    }
  }, [dispatch, page, filterRead]);

  useEffect(() => {
    loadNotifications();
  }, [loadNotifications]);

  const handleMarkAsRead = async (id) => {
    try {
      await notificationApi.markAsRead(id);
      await loadNotifications();
      syncHeaderNotifications();
    } catch (err) {
      dispatch(
        showToast({
          type: 'error',
          message: extractErrorMessage(err, 'Không thể đánh dấu đã đọc.'),
        })
      );
    }
  };

  const handleMarkAllRead = async () => {
    try {
      await notificationApi.markAllAsRead();
      dispatch(showToast({ type: 'success', message: 'Đã đánh dấu tất cả thông báo là đã đọc.' }));
      await loadNotifications();
      syncHeaderNotifications();
    } catch (err) {
      dispatch(
        showToast({
          type: 'error',
          message: extractErrorMessage(err, 'Không thể đánh dấu đã đọc tất cả.'),
        })
      );
    }
  };

  const handleDelete = async (id) => {
    try {
      await notificationApi.deleteNotification(id);
      dispatch(showToast({ type: 'success', message: 'Đã xóa thông báo.' }));
      await loadNotifications();
      syncHeaderNotifications();
    } catch (err) {
      dispatch(
        showToast({
          type: 'error',
          message: extractErrorMessage(err, 'Không thể xóa thông báo.'),
        })
      );
    }
  };

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-2">
          {[
            { value: '', label: 'Tất cả' },
            { value: 'UNREAD', label: 'Chưa đọc' },
            { value: 'READ', label: 'Đã đọc' },
          ].map((tab) => (
            <button
              key={tab.value}
              type="button"
              onClick={() => {
                setFilterRead(tab.value);
                setPage(0);
              }}
              className={`px-3.5 py-2 rounded-xl text-xs font-semibold transition-colors ${
                filterRead === tab.value
                  ? 'bg-slate-900 text-white'
                  : 'bg-white border border-slate-200 text-slate-600 hover:bg-slate-50'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>

        <button
          type="button"
          onClick={handleMarkAllRead}
          className="inline-flex items-center gap-1.5 rounded-xl border border-slate-200 bg-white hover:bg-slate-50 px-3.5 py-2 text-xs font-semibold text-slate-700"
        >
          <CheckIcon className="h-4 w-4 text-emerald-600" />
          Đánh dấu tất cả đã đọc
        </button>
      </div>

      <div className="card-surface overflow-hidden">
        {loading ? (
          <LoadingSpinner label="Đang tải thông báo..." />
        ) : !pageData.content?.length ? (
          <EmptyState
            icon={BellIcon}
            title="Không có thông báo"
            description="Cảnh báo ngân sách, nhắc nhở định kỳ và tiến độ tiết kiệm sẽ xuất hiện tại đây."
          />
        ) : (
          <>
            <div className="divide-y divide-slate-100">
              {pageData.content.map((notif) => {
                const isRead = notif.isRead ?? notif.read ?? false;
                return (
                  <div
                    key={notif.id}
                    className={`p-4 sm:px-6 flex items-start justify-between gap-4 transition-colors ${
                      isRead ? 'bg-white' : 'bg-emerald-50/40'
                    }`}
                  >
                    <div className="flex items-start gap-3 min-w-0">
                      <span
                        className={`h-2.5 w-2.5 rounded-full mt-1.5 shrink-0 ${
                          isRead ? 'bg-slate-300' : 'bg-emerald-500'
                        }`}
                      />
                      <div className="min-w-0">
                        <div className="flex flex-wrap items-center gap-2">
                          <h3 className="text-sm font-semibold text-slate-900">{notif.title}</h3>
                          {(notif.notificationType || notif.type) && (
                            <span className="px-2 py-0.5 rounded bg-slate-100 text-slate-600 text-[10px] font-medium">
                              {notif.notificationType || notif.type}
                            </span>
                          )}
                        </div>
                        <p className="text-xs text-slate-600 mt-1 leading-relaxed">
                          {notif.message}
                        </p>
                        <p className="text-[11px] text-slate-400 mt-1.5">
                          {formatDateTime(notif.createdAt)}
                        </p>
                      </div>
                    </div>

                    <div className="flex items-center gap-1 shrink-0">
                      {!isRead && (
                        <button
                          type="button"
                          onClick={() => handleMarkAsRead(notif.id)}
                          className="p-1.5 rounded-lg text-emerald-600 hover:bg-emerald-100"
                          title="Đánh dấu đã đọc"
                        >
                          <CheckCircleIcon className="h-4 w-4" />
                        </button>
                      )}
                      <button
                        type="button"
                        onClick={() => handleDelete(notif.id)}
                        className="p-1.5 rounded-lg text-slate-400 hover:text-rose-600 hover:bg-rose-50"
                        title="Xóa thông báo"
                      >
                        <TrashIcon className="h-4 w-4" />
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>

            <Pagination
              pageNumber={pageData.pageNumber}
              totalPages={pageData.totalPages}
              totalElements={pageData.totalElements}
              pageSize={pageData.pageSize}
              onPageChange={setPage}
            />
          </>
        )}
      </div>
    </div>
  );
}
