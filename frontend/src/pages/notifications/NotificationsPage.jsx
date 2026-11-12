import { useCallback, useEffect, useRef, useState } from 'react';
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
import { useTranslation } from '../../i18n/I18nContext';
import { extractErrorMessage, formatDateTime } from '../../utils/formatters';

export default function NotificationsPage() {
  const dispatch = useDispatch();
  const { t } = useTranslation();
  const tRef = useRef(t);
  tRef.current = t;

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
          message: extractErrorMessage(err, tRef.current('notifications.loadError')),
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
          message: extractErrorMessage(err, t('notifications.markReadError')),
        })
      );
    }
  };

  const handleMarkAllRead = async () => {
    try {
      await notificationApi.markAllAsRead();
      dispatch(showToast({ type: 'success', message: t('notifications.markAllReadSuccess') }));
      await loadNotifications();
      syncHeaderNotifications();
    } catch (err) {
      dispatch(
        showToast({
          type: 'error',
          message: extractErrorMessage(err, t('notifications.markAllReadError')),
        })
      );
    }
  };

  const handleDelete = async (id) => {
    try {
      await notificationApi.deleteNotification(id);
      dispatch(showToast({ type: 'success', message: t('notifications.deleteSuccess') }));
      await loadNotifications();
      syncHeaderNotifications();
    } catch (err) {
      dispatch(
        showToast({
          type: 'error',
          message: extractErrorMessage(err, t('notifications.deleteError')),
        })
      );
    }
  };

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-2">
          {[
            { value: '', label: t('common.all') },
            { value: 'UNREAD', label: t('notifications.unread') },
            { value: 'READ', label: t('notifications.read') },
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
                  ? 'bg-slate-900 dark:bg-emerald-600 text-white'
                  : 'bg-white dark:bg-zinc-950 border border-slate-200 dark:border-zinc-800 text-slate-600 dark:text-slate-300 hover:bg-slate-50 dark:hover:bg-zinc-900'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>

        <button
          type="button"
          onClick={handleMarkAllRead}
          className="inline-flex items-center gap-1.5 rounded-xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-950 hover:bg-slate-50 dark:hover:bg-zinc-900 px-3.5 py-2 text-xs font-semibold text-slate-700 dark:text-slate-200"
        >
          <CheckIcon className="h-4 w-4 text-emerald-600 dark:text-emerald-400" />
          {t('notifications.markAllRead')}
        </button>
      </div>

      <div className="card-surface overflow-hidden">
        {loading ? (
          <LoadingSpinner label={t('notifications.loading')} />
        ) : !pageData.content?.length ? (
          <EmptyState
            icon={BellIcon}
            title={t('notifications.emptyTitle')}
            description={t('notifications.emptyDesc')}
          />
        ) : (
          <>
            <div className="divide-y divide-slate-100 dark:divide-zinc-800">
              {pageData.content.map((notif) => {
                const isRead = notif.isRead ?? notif.read ?? false;
                return (
                  <div
                    key={notif.id}
                    className={`p-4 sm:px-6 flex items-start justify-between gap-4 transition-colors ${
                      isRead ? 'bg-white dark:bg-zinc-950' : 'bg-emerald-50/40 dark:bg-emerald-950/30'
                    }`}
                  >
                    <div className="flex items-start gap-3 min-w-0">
                      <span
                        className={`h-2.5 w-2.5 rounded-full mt-1.5 shrink-0 ${
                          isRead ? 'bg-slate-300 dark:bg-zinc-600' : 'bg-emerald-500'
                        }`}
                      />
                      <div className="min-w-0">
                        <div className="flex flex-wrap items-center gap-2">
                          <h3 className="text-sm font-semibold text-slate-900 dark:text-white">{notif.title}</h3>
                          {(notif.notificationType || notif.type) && (
                            <span className="px-2 py-0.5 rounded bg-slate-100 dark:bg-zinc-900 text-slate-600 dark:text-slate-300 text-[10px] font-medium">
                              {notif.notificationType || notif.type}
                            </span>
                          )}
                        </div>
                        <p className="text-xs text-slate-600 dark:text-slate-300 mt-1 leading-relaxed">
                          {notif.message}
                        </p>
                        <p className="text-[11px] text-slate-400 dark:text-slate-500 mt-1.5">
                          {formatDateTime(notif.createdAt)}
                        </p>
                      </div>
                    </div>

                    <div className="flex items-center gap-1 shrink-0">
                      {!isRead && (
                        <button
                          type="button"
                          onClick={() => handleMarkAsRead(notif.id)}
                          className="p-1.5 rounded-lg text-emerald-600 dark:text-emerald-400 hover:bg-emerald-100 dark:hover:bg-emerald-950/60"
                          title={t('notifications.markReadBtn')}
                        >
                          <CheckCircleIcon className="h-4 w-4" />
                        </button>
                      )}
                      <button
                        type="button"
                        onClick={() => handleDelete(notif.id)}
                        className="p-1.5 rounded-lg text-slate-400 hover:text-rose-600 dark:hover:text-rose-400 hover:bg-rose-50 dark:hover:bg-rose-950/50"
                        title={t('notifications.deleteBtn')}
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
