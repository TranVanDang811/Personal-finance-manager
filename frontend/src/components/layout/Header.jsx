import { useEffect, useRef, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import {
  ArrowRightOnRectangleIcon,
  Bars3Icon,
  BellIcon,
  CheckIcon,
  ChevronDownIcon,
  UserCircleIcon,
} from '@heroicons/react/24/outline';
import { logoutUser } from '../../features/auth/authSlice';
import {
  fetchRecentNotifications,
  fetchUnreadCount,
  resetNotifications,
} from '../../features/notifications/notificationSlice';
import { notificationApi } from '../../services/notificationApi';
import { showToast } from '../../features/feedback/feedbackSlice';
import { formatDateTime } from '../../utils/formatters';
import { LanguageSwitcher, useTranslation } from '../../i18n/I18nContext';
import { ThemeSwitcher } from '../../theme/ThemeContext';

const PAGE_TITLE_KEYS = {
  '/dashboard': 'nav.titles.dashboard',
  '/accounts': 'nav.titles.accounts',
  '/categories': 'nav.titles.categories',
  '/transactions': 'nav.titles.transactions',
  '/budgets': 'nav.titles.budgets',
  '/saving-goals': 'nav.titles.savingGoals',
  '/recurring': 'nav.titles.recurring',
  '/reports': 'nav.titles.reports',
  '/notifications': 'nav.titles.notifications',
  '/profile': 'nav.titles.profile',
};

export default function Header({ onOpenMobileMenu }) {
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const location = useLocation();

  const user = useSelector((state) => state.auth.user);
  const { unreadCount, recent } = useSelector((state) => state.notifications);

  const [notifOpen, setNotifOpen] = useState(false);
  const [userMenuOpen, setUserMenuOpen] = useState(false);

  const notifRef = useRef(null);
  const userMenuRef = useRef(null);

  const titleKey = PAGE_TITLE_KEYS[location.pathname];
  const pageTitle = titleKey ? t(titleKey) : 'DangCap';

  useEffect(() => {
    const handleClickOutside = (e) => {
      if (notifRef.current && !notifRef.current.contains(e.target)) {
        setNotifOpen(false);
      }
      if (userMenuRef.current && !userMenuRef.current.contains(e.target)) {
        setUserMenuOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleToggleNotif = () => {
    const next = !notifOpen;
    setNotifOpen(next);
    setUserMenuOpen(false);
    if (next) {
      dispatch(fetchUnreadCount());
      dispatch(fetchRecentNotifications());
    }
  };

  const handleMarkAsRead = async (notif) => {
    const isRead = notif.isRead ?? notif.read ?? false;
    if (isRead) return;
    try {
      await notificationApi.markAsRead(notif.id);
      dispatch(fetchUnreadCount());
      dispatch(fetchRecentNotifications());
    } catch {
      dispatch(showToast({ type: 'error', message: t('header.markReadError') }));
    }
  };

  const handleMarkAllRead = async () => {
    try {
      await notificationApi.markAllAsRead();
      dispatch(fetchUnreadCount());
      dispatch(fetchRecentNotifications());
      dispatch(showToast({ type: 'success', message: t('header.markAllReadSuccess') }));
    } catch {
      dispatch(showToast({ type: 'error', message: t('header.markAllReadError') }));
    }
  };

  const handleLogout = async () => {
    await dispatch(logoutUser());
    dispatch(resetNotifications());
    navigate('/login', { replace: true });
  };

  return (
    <header className="sticky top-0 z-20 h-16 bg-white/90 dark:bg-zinc-950/90 backdrop-blur border-b border-slate-200/80 dark:border-zinc-800 px-4 sm:px-6 lg:px-8 flex items-center justify-between gap-4 transition-colors duration-200">
      <div className="flex items-center gap-3 min-w-0">
        <button
          type="button"
          onClick={onOpenMobileMenu}
          className="lg:hidden p-2 -ml-2 rounded-xl text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-zinc-800"
          aria-label={t('header.openNav')}
        >
          <Bars3Icon className="h-6 w-6" />
        </button>
        <h1 className="text-base sm:text-lg font-bold text-slate-900 dark:text-white truncate">{pageTitle}</h1>
      </div>

      <div className="flex items-center gap-2 sm:gap-3">
        <ThemeSwitcher showLabels={false} />
        <LanguageSwitcher />

        <div className="relative" ref={notifRef}>
          <button
            type="button"
            onClick={handleToggleNotif}
            className="relative p-2 rounded-xl text-slate-600 dark:text-slate-300 hover:text-slate-900 dark:hover:text-white hover:bg-slate-100 dark:hover:bg-zinc-800 transition-colors"
            aria-label={t('header.notificationsLabel')}
          >
            <BellIcon className="h-5 w-5" />
            {unreadCount > 0 && (
              <span className="absolute -top-0.5 -right-0.5 min-w-[18px] h-[18px] px-1 rounded-full bg-rose-500 text-white text-[10px] font-bold flex items-center justify-center tabular-nums">
                {unreadCount > 99 ? '99+' : unreadCount}
              </span>
            )}
          </button>

          {notifOpen && (
            <div className="absolute right-0 mt-2 w-80 sm:w-96 rounded-2xl bg-white dark:bg-zinc-950 border border-slate-200 dark:border-zinc-800 shadow-xl overflow-hidden z-40">
              <div className="flex items-center justify-between px-4 py-3 border-b border-slate-100 dark:border-zinc-800">
                <span className="text-sm font-semibold text-slate-900 dark:text-white">
                  {t('header.notificationsTitle', { count: unreadCount })}
                </span>
                {unreadCount > 0 && (
                  <button
                    type="button"
                    onClick={handleMarkAllRead}
                    className="inline-flex items-center gap-1 text-xs font-medium text-emerald-600 dark:text-emerald-400 hover:text-emerald-700"
                  >
                    <CheckIcon className="h-3.5 w-3.5" />
                    {t('header.markAllRead')}
                  </button>
                )}
              </div>

              <div className="max-h-80 overflow-y-auto divide-y divide-slate-100 dark:divide-zinc-800">
                {!recent.length ? (
                  <p className="py-8 text-center text-xs text-slate-500 dark:text-slate-400">
                    {t('header.noNotifications')}
                  </p>
                ) : (
                  recent.map((notif) => {
                    const isRead = notif.isRead ?? notif.read ?? false;
                    return (
                      <button
                        key={notif.id}
                        type="button"
                        onClick={() => handleMarkAsRead(notif)}
                        className={`w-full text-left px-4 py-3 hover:bg-slate-50 dark:hover:bg-zinc-900 transition-colors flex items-start gap-2.5 ${
                          isRead ? 'bg-white dark:bg-zinc-950' : 'bg-emerald-50/40 dark:bg-emerald-950/30'
                        }`}
                      >
                        <span
                          className={`h-2 w-2 rounded-full mt-1.5 shrink-0 ${
                            isRead ? 'bg-slate-300 dark:bg-zinc-600' : 'bg-emerald-500'
                          }`}
                        />
                        <div className="min-w-0 flex-1">
                          <p className="text-xs font-semibold text-slate-900 dark:text-white line-clamp-1">
                            {notif.title}
                          </p>
                          <p className="text-xs text-slate-600 dark:text-slate-300 mt-0.5 line-clamp-2">
                            {notif.message}
                          </p>
                          <p className="text-[11px] text-slate-400 dark:text-slate-500 mt-1">
                            {formatDateTime(notif.createdAt)}
                          </p>
                        </div>
                      </button>
                    );
                  })
                )}
              </div>

              <Link
                to="/notifications"
                onClick={() => setNotifOpen(false)}
                className="block text-center py-2.5 text-xs font-semibold text-emerald-600 dark:text-emerald-400 hover:bg-slate-50 dark:hover:bg-zinc-900 border-t border-slate-100 dark:border-zinc-800"
              >
                {t('header.viewAllNotifications')}
              </Link>
            </div>
          )}
        </div>

        <div className="relative" ref={userMenuRef}>
          <button
            type="button"
            onClick={() => {
              setUserMenuOpen((prev) => !prev);
              setNotifOpen(false);
            }}
            className="flex items-center gap-2 pl-2 pr-2.5 py-1.5 rounded-xl border border-slate-200 dark:border-zinc-800 hover:bg-slate-50 dark:hover:bg-zinc-900 transition-colors"
          >
            <div className="h-7 w-7 rounded-full bg-emerald-600 text-white text-xs font-semibold flex items-center justify-center overflow-hidden shrink-0">
              {user?.avatarUrl ? (
                <img src={user.avatarUrl} alt={user.fullName} className="h-full w-full object-cover" />
              ) : (
                (user?.fullName?.[0] || 'U').toUpperCase()
              )}
            </div>
            <span className="hidden sm:block text-xs font-semibold text-slate-700 dark:text-slate-200 max-w-[130px] truncate">
              {user?.fullName || t('header.accountFallback')}
            </span>
            <ChevronDownIcon className="h-4 w-4 text-slate-400" />
          </button>

          {userMenuOpen && (
            <div className="absolute right-0 mt-2 w-56 rounded-2xl bg-white dark:bg-zinc-950 border border-slate-200 dark:border-zinc-800 shadow-xl py-1.5 z-40">
              <div className="px-4 py-2.5 border-b border-slate-100 dark:border-zinc-800">
                <p className="text-xs font-semibold text-slate-900 dark:text-white truncate">
                  {user?.fullName || t('nav.userFallback')}
                </p>
                <p className="text-[11px] text-slate-500 dark:text-slate-400 truncate">{user?.email || ''}</p>
              </div>
              <Link
                to="/profile"
                onClick={() => setUserMenuOpen(false)}
                className="flex items-center gap-2.5 px-4 py-2 text-xs font-medium text-slate-700 dark:text-slate-200 hover:bg-slate-50 dark:hover:bg-zinc-900"
              >
                <UserCircleIcon className="h-4 w-4 text-slate-400" />
                {t('header.profile')}
              </Link>
              <button
                type="button"
                onClick={handleLogout}
                className="w-full flex items-center gap-2.5 px-4 py-2 text-xs font-medium text-rose-600 dark:text-rose-400 hover:bg-rose-50 dark:hover:bg-rose-950/40"
              >
                <ArrowRightOnRectangleIcon className="h-4 w-4" />
                {t('header.logout')}
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}
